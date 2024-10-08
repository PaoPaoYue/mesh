package io.github.paopaoyue.mesh.rpc.core.server;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import io.github.paopaoyue.mesh.rpc.core.util.SelectionKeyPatch;
import io.github.paopaoyue.mesh.rpc.exception.HandlerException;
import io.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;
import io.github.paopaoyue.mesh.rpc.util.Flag;
import io.github.paopaoyue.mesh.rpc.core.util.ModeByteBuffer;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler {

    private static final Logger logger = LogManager.getLogger(ConnectionHandler.class);

    private static final int LENGTH_FIELD_OFFSET = 3;
    private static final int LENGTH_FIELD_LENGTH = 4;

    private SubReactor reactor;
    private SelectionKey key;
    private SocketChannel socketChannel;
    private Lock writeLock;
    private int bufferLen;
    private ModeByteBuffer readBuffer;
    private ModeByteBuffer writeBuffer;
    private LinkedBlockingQueue<Protocol.Packet> writeQueue;
    private volatile Status status;
    private volatile long lastActiveTime;
    private AtomicInteger activeWorkerNum;

    public ConnectionHandler(SubReactor reactor, SelectionKey key) {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.IDLE;
        this.writeLock = new ReentrantLock();
        this.bufferLen = prop.getPacketMaxSize();
        this.readBuffer = new ModeByteBuffer(bufferLen);
        this.writeBuffer = new ModeByteBuffer(bufferLen);
        this.writeQueue = new LinkedBlockingQueue<>();
        this.lastActiveTime = System.currentTimeMillis();
        this.activeWorkerNum = new AtomicInteger(0);

        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
        this.reactor = reactor;
    }

    // not thread safe, run only in single thread
    public void process() {
        if (status == Status.TERMINATED) {
            return;
        }
        lastActiveTime = System.currentTimeMillis();
        if (status != Status.TERMINATING) {
            status = Status.PROCESSING;
            if (key.isValid() && key.isReadable()) {
                read();
            }
            if (key.isValid() && key.isWritable()) {
                write();
            }
        } else {
            if (key.isWritable()) {
                write();
            }
        }
    }

    private void read() {
        try {
            int n = socketChannel.read(readBuffer.getBuffer());
            if (n == -1) {
                logger.warn("{} disconnected at remote", this);
                stopNow();
                return;
            }
        } catch (ClosedChannelException | SocketException e) {
            logger.error("{} disconnected abruptly: {}", this, e.getMessage(), e);
            stopNow();
            return;
        } catch (Exception e) {
            logger.error("{} counter unexpected exception: {}", this, e.getMessage(), e);
            stopNow();
            return;
        }
        readBuffer.flip();
        int currentLimit = readBuffer.limit();
        while (readBuffer.hasReadRemaining()) {
            if (readBuffer.readRemaining() < LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH) {
                logger.debug("{} received header not complete, received: {} bytes", this, readBuffer.readRemaining());
                readBuffer.compact();
                return;
            }
            // extract packet length before parsing
            int currentPacketLength = 0;
            for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                currentPacketLength += (readBuffer.array()[readBuffer.position() + LENGTH_FIELD_OFFSET + i] & 0xFF) << (i * 8);
            }
            if (bufferLen < currentPacketLength) {
                // TODO: discard corresponding bytes instead of closing the connection -- low priority
                logger.error("{} request of {} bytes is too large, closing connection", this, currentPacketLength);
                stopNow();
                return;
            }
            if (readBuffer.readRemaining() < currentPacketLength) {
                logger.debug("{} received body not complete, received: {} bytes, want: {} bytes", this, readBuffer.readRemaining(), currentPacketLength);
                readBuffer.compact();
                return;
            }
            Protocol.Packet currentPacket = null;
            try {
                readBuffer.limit(readBuffer.position() + currentPacketLength);
                currentPacket = Protocol.Packet.parseFrom(readBuffer.getBuffer());
            } catch (InvalidProtocolBufferException e) {
                logger.error("{} request packet not following protocol, closing connection", this);
                stopNow();
                return;
            } finally {
                readBuffer.limit(currentLimit);
                readBuffer.position(readBuffer.position() + currentPacketLength);
            }
            if (currentPacket != null) {
                logger.debug("{} received request {} : \n {}", this, currentPacket.getHeader().getRequestId(), currentPacket);
                try {
                    activeWorkerNum.incrementAndGet();
                    RpcAutoConfiguration.getRpcServer().getThreadPool().execute(new Worker(currentPacket));
                } catch (Exception e) {
                    activeWorkerNum.decrementAndGet();
                    logger.error("{} submit worker failed: {}", this, e.getMessage(), e);
                }
                if ((currentPacket.getHeader().getFlag() & Flag.FIN) != 0) {
                    status = Status.TERMINATING;
                }
            }
        }
    }

    private void write() {
        while (true) {
            if (writeBuffer.hasReadRemaining()) {
                try {
                    socketChannel.write(writeBuffer.getBuffer());
                    if (writeBuffer.hasReadRemaining()) {
                        logger.warn("{} write waiting for socket buffer, available: {} bytes, want: {} bytes", this, socketChannel.socket().getSendBufferSize(), writeBuffer.readRemaining());
                        break;
                    }
                    writeBuffer.clear();
                } catch (ClosedChannelException | SocketException e) {
                    logger.error("{} disconnected abruptly: {}", this, e.getMessage(), e);
                    stopNow();
                    return;
                } catch (Exception e) {
                    logger.error("{} counter unexpected exception: {}", this, e.getMessage(), e);
                    stopNow();
                    return;
                }
            }
            if (writeQueue.isEmpty()) {
                break;
            }
            Protocol.Packet packet = writeQueue.poll();
            try {
                CodedOutputStream outputStream = CodedOutputStream.newInstance(writeBuffer.getBuffer());
                packet.writeTo(outputStream);
                outputStream.flush();
                // inject packet length
                int currentPacketLength = writeBuffer.position();
                for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                    writeBuffer.array()[LENGTH_FIELD_OFFSET + i] = (byte) (currentPacketLength >> (i * 8) & 0xff);
                }
                logger.debug("{} buffer write done with request {}", this, packet.getHeader().getRequestId());
                writeBuffer.flip();
            } catch (IOException e) {
                logger.error("{} response packet {} encode failed, skip this packet: {}", this, packet, e.getMessage(), e);
                continue;
            }
        }
        if (writeQueue.isEmpty() && !writeBuffer.hasReadRemaining()) {
            writeLock.lock();
            try {
                if (writeQueue.isEmpty() && !writeBuffer.hasReadRemaining()) {
                    if (status == Status.TERMINATING && activeWorkerNum.get() == 0) {
                        stopNow();
                    } else {
                        status = Status.IDLE;
                        SelectionKeyPatch.interestOpsAnd(key, SelectionKey.OP_READ);
                    }
                }
            } catch (Exception e) {
                logger.error("{} change key listening status failed: {}", this, e.getMessage(), e);
            } finally {
                writeLock.unlock();
            }
        }
    }

    // not thread safe, called by other thread can result in interrupting processing thread
    // called only when the connection is idle for a long time
    public boolean checkKeepAliveTimeout() {
        int keepAliveTimeout = RpcAutoConfiguration.getProp().getKeepAliveTimeout();
        long current = System.currentTimeMillis();
        if (this.status == Status.IDLE && current - lastActiveTime > keepAliveTimeout * 1000L) {
            logger.warn("{} inactive for {} ms, closing connection", this, current - lastActiveTime);
            this.stopNow();
            return true;
        }
        return false;
    }

    public void stop() {
        if (status != Status.TERMINATING) {
            this.status = Status.TERMINATING;
            writeLock.lock();
            try {
                if (writeQueue.isEmpty() && !writeBuffer.hasReadRemaining()) {
                    if (activeWorkerNum.get() == 0) {
                        stopNow();
                    } else {
                        SelectionKeyPatch.interestOpsOr(key, SelectionKey.OP_WRITE);
                    }
                }
            } catch (Exception e) {
                logger.error("{} change key listening status failed: {}", this, e.getMessage(), e);
            } finally {
                writeLock.unlock();
            }
        }
    }

    public void stopNow() {
        this.status = Status.TERMINATING;
        try {
            logger.debug("{} connection stop now", this);
            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();

            reactor.getSelector().wakeup();
            this.status = Status.TERMINATED;
        } catch (IOException e) {
            logger.error("{} connection stop failure: {}", this, e.getMessage(), e);
        }
    }

    public Socket getSocket() {
        return socketChannel.socket();
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        try {
            return "ServerConnectionHandler{" +
                    "clientIp=" + socketChannel.getRemoteAddress() +
                    ", status=" + status +
                    '}';
        } catch (IOException e) {
            logger.error("failed to get remote address", e);
            return "ServerConnectionHandler{" +
                    "status=" + status +
                    '}';
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
                    logger.error("{} send a packet with no specific SYSTEM_CALL or SERVICE_CALL", ConnectionHandler.this);
                }
                logger.debug("{} worker done with request {}", ConnectionHandler.this, packet.getHeader().getRequestId());
            } catch (InterruptedException e) {
                logger.error("{} interrupted while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            } catch (HandlerNotFoundException | HandlerException e) {
                logger.error("{} handler exception caught while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            } catch (Exception e) {
                logger.error("{} unknown exception caught while processing request: {}", ConnectionHandler.this, e.getMessage(), e);
            }
            writeLock.lock();
            activeWorkerNum.decrementAndGet();
            try {
                if ((key.interestOps() & SelectionKey.OP_WRITE) == 0 && (status == Status.TERMINATING || !writeQueue.isEmpty() || writeBuffer.hasReadRemaining())) {
                    SelectionKeyPatch.interestOpsOr(key, SelectionKey.OP_WRITE);
                    reactor.getSelector().wakeup();
                }
            } catch (Exception e) {
                logger.error("{} change key listening status failed: {}", ConnectionHandler.this, e.getMessage(), e);
            } finally {
                writeLock.unlock();
            }
        }
    }
}
