package com.github.paopaoyue.mesh.rpc.core.server;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.core.Flag;
import com.github.paopaoyue.mesh.rpc.core.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.core.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private static final int LENGTH_FIELD_OFFSET = 3;
    private static final int LENGTH_FIELD_LENGTH = 4;

    private Properties prop;

    private SelectionKey key;
    private SocketChannel socketChannel;
    private Lock writeLock;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private LinkedBlockingQueue<Protocol.Packet> writeQueue;
    private volatile Status status;
    private long lastActiveTime;
    private AtomicInteger activeWorkerNum;

    public ConnectionHandler(SelectionKey key) {
        this.prop = RpcAutoConfiguration.getProp();

        this.status = Status.IDLE;
        this.writeLock = new ReentrantLock();
        this.readBuffer = ByteBuffer.allocate(prop.getServerWorkerBufferSize());
        this.writeBuffer = ByteBuffer.allocate(prop.getServerWorkerBufferSize());
        this.writeQueue = new LinkedBlockingQueue<>();
        this.lastActiveTime = 0;
        this.activeWorkerNum = new AtomicInteger(0);

        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
    }

    // not thread safe, run only in single thread
    public void process() {
        if (status == Status.TERMINATED) {
            return;
        }
        lastActiveTime = System.currentTimeMillis();
        if (status != Status.TERMINATING) {
            status = Status.PROCESSING;
            if (key.isReadable()) {
                read();
            }
            if (key.isWritable()) {
                write();
            }
            if (status == Status.PROCESSING) {
                status = Status.IDLE;
            }
        } else {
            if (key.isWritable()) {
                write();
            }
        }
    }

    private void read() {
        try {
            int n = socketChannel.read(readBuffer);
            if (n == -1) {
                logger.warn("Client {} disconnected before request processing", this);
                stop();
                return;
            }
        } catch (ClosedChannelException e) {
            logger.error("Client {} disconnected abruptly: {}", this, e.getMessage(), e);
            stop();
            return;
        } catch (Exception e) {
            logger.error("Client {} counter unexpected exception: {}", this, e.getMessage(), e);
            stop();
            return;
        }
        readBuffer.flip();
        while (readBuffer.hasRemaining()) {
            if (readBuffer.remaining() < LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH) {
                logger.debug("Client {} received header not complete, received: {} bytes", this, readBuffer.remaining());
                readBuffer.compact();
                return;
            }
            // extract packet length before parsing
            int currentPacketLength = 0;
            for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                currentPacketLength += readBuffer.array()[readBuffer.position() + LENGTH_FIELD_OFFSET + i] << (i * 8);
            }
            if (prop.getServerWorkerBufferSize() < currentPacketLength) {
                // TODO: discard corresponding bytes instead of closing the connection -- low priority
                logger.error("Client {} request of {} bytes is too large, closing connection", this, currentPacketLength);
                stop();
                return;
            }
            if (readBuffer.remaining() < currentPacketLength) {
                logger.debug("Client {} received body not complete, received: {} bytes, want: {} bytes", this, readBuffer.remaining(), currentPacketLength);
                readBuffer.compact();
                return;
            }
            Protocol.Packet currentPacket = null;
            try {
                currentPacket = Protocol.Packet.parseFrom(readBuffer);
            } catch (InvalidProtocolBufferException e) {
                logger.error("Client {} request packet parsing failed, skip this packet: {}", this, e.getMessage(), e);
                currentPacketLength = 0;
            } finally {
                readBuffer.position(currentPacketLength);
            }
            if (currentPacket != null) {
                activeWorkerNum.incrementAndGet();
                RpcAutoConfiguration.getRpcServer().getWorkerPool().submit(new Worker(currentPacket));
                if ((currentPacket.getHeader().getFlag() & Flag.FIN) != 0) {
                    status = Status.TERMINATING;
                }
            }
        }
    }

    private void write() {
        Protocol.Packet packet = null;
        while (true) {
            if (writeBuffer.hasRemaining()) {
                try {
                    socketChannel.write(writeBuffer);
                    if (writeBuffer.hasRemaining()) {
                        logger.debug("Client {} write waiting for socket buffer, available: {} bytes, want: {} bytes", this, socketChannel.socket().getSendBufferSize(), writeBuffer.remaining());
                        break;
                    }
                    writeBuffer.clear();
                } catch (ClosedChannelException e) {
                    logger.error("Client {} disconnected abruptly: {}", this, e.getMessage(), e);
                    stop();
                    return;
                } catch (Exception e) {
                    logger.error("Client {} counter unexpected exception: {}", this, e.getMessage(), e);
                    stop();
                    return;
                }
            }
            if (writeQueue.isEmpty()) {
                break;
            }
            packet = writeQueue.poll();
            try {
                packet.writeTo(CodedOutputStream.newInstance(writeBuffer));
                // inject packet length
                int currentPacketLength = writeBuffer.position();
                writeBuffer.putInt(LENGTH_FIELD_OFFSET, currentPacketLength - LENGTH_FIELD_OFFSET - LENGTH_FIELD_LENGTH);
                for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                    writeBuffer.array()[LENGTH_FIELD_OFFSET + i] = (byte) (currentPacketLength >> (i * 8) & 0xff);
                }
                writeBuffer.flip();
            } catch (IOException e) {
                logger.error("Client {} response packet {} encode failed, skip this packet: {}", this, packet, e.getMessage(), e);
                continue;
            }
        }

        writeLock.lock();
        try {
            if (writeQueue.isEmpty() && !writeBuffer.hasRemaining()) {
                if (status == Status.TERMINATING && activeWorkerNum.get() == 0) {
                    stop();
                } else {
                    key.interestOpsAnd(SelectionKey.OP_READ);
                }
            }
        } catch (Exception e) {
            logger.error("Client {} change key listening status failed: {}", this, e.getMessage(), e);
        } finally {
            writeLock.unlock();
        }
    }

    // not thread safe, called by other thread only when the connection is idle for a long time
    public boolean checkAlive() {
        if (this.status == Status.IDLE && System.currentTimeMillis() - lastActiveTime > prop.getKeepAliveTimeout()) {
            logger.warn("Client {} inactive for {} ms, closing connection", this, prop.getKeepAliveTimeout());
            stop();
            return false;
        }
        return true;
    }

    public void stop() {
        this.status = Status.TERMINATING;
        try {
            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();

            this.status = Status.TERMINATED;
        } catch (IOException e) {
            logger.error("Client {} connection stop failure: {}", this, e.getMessage(), e);
        }
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        try {
            return "ConnectionHandler{" +
                    "clientIp=" + socketChannel.getRemoteAddress() +
                    ", status=" + status +
                    '}';
        } catch (IOException e) {

            throw new RuntimeException(e);
        }
    }


    public enum Status {
        IDLE,
        PROCESSING,
        TERMINATING,
        TERMINATED,
    }

    public class Worker implements Runnable {
        private final Protocol.Packet packet;

        public Worker(Protocol.Packet packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            try {
                if ((packet.getHeader().getFlag() & Flag.SYSTEM_CALL) != 0) {
                    Protocol.Packet out = RpcAutoConfiguration.getRpcServer().getSystemStub().process(packet);
                    writeQueue.put(out);
                } else if ((packet.getHeader().getFlag() & Flag.SERVICE_CALL) != 0) {
                    Protocol.Packet out = RpcAutoConfiguration.getRpcServer().getServiceStub().process(packet);
                    writeQueue.put(out);
                } else {
                    logger.error("Client {} send a packet with no specific SYSTEM_CALL or SERVICE_CALL", ConnectionHandler.this);
                    ;
                }
            } catch (InterruptedException e) {
                logger.error("Client {} interrupted while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            } catch (HandlerNotFoundException | HandlerException e) {
                logger.error("Client {} handler exception caught while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Client {} unknown exception caught while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            }
            writeLock.lock();
            activeWorkerNum.decrementAndGet();
            try {
                if ((key.interestOps() & SelectionKey.OP_WRITE) == 0 && (status == Status.TERMINATING || !writeQueue.isEmpty() || writeBuffer.hasRemaining())) {
                    key.interestOpsOr(SelectionKey.OP_WRITE);
                }
            } catch (Exception e) {
                logger.error("Client {} change key listening status failed: {}", ConnectionHandler.this, e.getMessage(), e);
            } finally {
                writeLock.unlock();
            }
        }
    }
}
