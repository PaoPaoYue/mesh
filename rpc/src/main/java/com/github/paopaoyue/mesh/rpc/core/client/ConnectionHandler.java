package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.exception.ServiceUnavailableException;
import com.github.paopaoyue.mesh.rpc.exception.TimeoutException;
import com.github.paopaoyue.mesh.rpc.exception.TransportErrorException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private static final int LENGTH_FIELD_OFFSET = 3;
    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final String PING_MESSAGE = "FIN";
    private static final System.PingRequest FIN_REQUEST = System.PingRequest.newBuilder().setMessage(PING_MESSAGE).build();
    private static final CallOption FIN_CALLOPTION = new CallOption().setFin(true);

    private boolean keepAlive;
    private String serviceName;
    private String tag;
    private SelectionKey key;
    private SocketChannel socketChannel;
    private int bufferLen;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private Lock writeLock;
    private LinkedBlockingQueue<Waiter> writeQueue;
    private Map<Long, Waiter> requestWaiterMap;

    private volatile Status status;

    public ConnectionHandler(boolean keepAlive, String serviceName, String tag, SelectionKey key) {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.IDLE;
        this.bufferLen = prop.getPacketMaxSize();
        this.readBuffer = ByteBuffer.allocate(bufferLen);
        this.writeBuffer = ByteBuffer.allocate(bufferLen);
        this.writeLock = new ReentrantLock();
        this.writeQueue = new LinkedBlockingQueue<>();
        this.requestWaiterMap = new HashMap<>();

        this.keepAlive = keepAlive;
        this.serviceName = serviceName;
        this.tag = tag;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();

        if (keepAlive) {
            RpcAutoConfiguration.getRpcClient().getReactor().addConnection(serviceName, tag, this);
        }
    }

    // not thread safe, run only in single thread
    public void process() {
        if (status == Status.TERMINATED) {
            return;
        }
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
            if (key.isReadable()) {
                read();
            }
        }
    }

    // thread safe, can be called by multiple threads
    public Waiter sendPacket(Protocol.Packet packet) {
        if (status == Status.TERMINATED || status == Status.TERMINATING) {
            throw new IllegalStateException("Connection is terminated or terminating, no new request allowed");
        }
        if ((packet.getHeader().getFlag() & Flag.FIN) != 0) {
            this.status = Status.TERMINATING;
        }
        Waiter waiter = new Waiter(packet.getHeader().getRequestId(), packet);
        writeLock.lock();
        try {
            writeQueue.offer(waiter);
            if ((key.interestOps() & SelectionKey.OP_WRITE) == 0 && (!writeQueue.isEmpty() || writeBuffer.hasRemaining())) {
                key.interestOpsOr(SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            logger.error("{} change key listening status failed: {}", ConnectionHandler.this, e.getMessage(), e);
            stopNow(e);
            throw e;
        } finally {
            writeLock.unlock();
        }
        return waiter;
    }

    private void read() {
        try {
            int n = socketChannel.read(readBuffer);
            if (n == -1) {
                logger.warn("{} disconnected at remote", this);
                stopNow(new ServiceUnavailableException("Connection disconnected at remote"));
                return;
            }
        } catch (ClosedChannelException e) {
            logger.error("{} disconnected abruptly: {}", this, e.getMessage(), e);
            stopNow(new ServiceUnavailableException("Connection disconnected abruptly", e));
            return;
        } catch (Exception e) {
            logger.error("{} counter unexpected exception: {}", this, e.getMessage(), e);
            stopNow(e);
            return;
        }
        readBuffer.flip();
        while (readBuffer.hasRemaining()) {
            if (readBuffer.remaining() < LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH) {
                logger.debug("{} received header not complete, received: {} bytes", this, readBuffer.remaining());
                readBuffer.compact();
                return;
            }
            // extract packet length before parsing
            int currentPacketLength = 0;
            for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                currentPacketLength += readBuffer.array()[readBuffer.position() + LENGTH_FIELD_OFFSET + i] << (i * 8);
            }
            if (bufferLen < currentPacketLength) {
                // TODO: discard corresponding bytes instead of closing the connection -- low priority
                logger.error("{} response of {} bytes is too large, closing connection", this, currentPacketLength);
                stopNow(new TransportErrorException("Response too large"));
                return;
            }
            if (readBuffer.remaining() < currentPacketLength) {
                logger.debug("{} received body not complete, received: {} bytes, want: {} bytes", this, readBuffer.remaining(), currentPacketLength);
                readBuffer.compact();
                return;
            }
            Protocol.Packet currentPacket = null;
            try {
                currentPacket = Protocol.Packet.parseFrom(readBuffer);
            } catch (InvalidProtocolBufferException e) {
                logger.error("{} request packet parsing failed, skip this packet: {}", this, e.getMessage(), e);
                currentPacketLength = 0;
            } finally {
                readBuffer.position(currentPacketLength);
            }
            if (currentPacket != null) {
                Waiter waiter = requestWaiterMap.get(currentPacket.getHeader().getRequestId());
                if (waiter == null) {
                    logger.error("{} request {} not found, skip this packet", this, currentPacket.getHeader().getRequestId());
                    continue;
                }
                waiter.signal(currentPacket);
                requestWaiterMap.remove(currentPacket.getHeader().getRequestId());
            }
        }
        if (status == Status.TERMINATING && requestWaiterMap.isEmpty()) {
            stopNow();
        }
    }

    private void write() {
        while (true) {
            if (writeBuffer.hasRemaining()) {
                try {
                    socketChannel.write(writeBuffer);
                    if (writeBuffer.hasRemaining()) {
                        logger.debug("{} write waiting for socket buffer, available: {} bytes, want: {} bytes", this, socketChannel.socket().getSendBufferSize(), writeBuffer.remaining());
                        break;
                    }
                    writeBuffer.clear();
                } catch (ClosedChannelException e) {
                    logger.error("{} disconnected abruptly: {}", this, e.getMessage(), e);
                    stopNow(new ServiceUnavailableException("Connection disconnected abruptly", e));
                    return;
                } catch (Exception e) {
                    logger.error("{} counter unexpected exception: {}", this, e.getMessage(), e);
                    stopNow(e);
                    return;
                }
            }
            if (writeQueue.isEmpty()) {
                break;
            }
            Waiter waiter = writeQueue.poll();
            try {
                requestWaiterMap.put(waiter.requestId, waiter);
                waiter.request.writeTo(CodedOutputStream.newInstance(writeBuffer));
                // inject packet length
                int currentPacketLength = writeBuffer.position();
                writeBuffer.putInt(LENGTH_FIELD_OFFSET, currentPacketLength - LENGTH_FIELD_OFFSET - LENGTH_FIELD_LENGTH);
                for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                    writeBuffer.array()[LENGTH_FIELD_OFFSET + i] = (byte) (currentPacketLength >> (i * 8) & 0xff);
                }
                writeBuffer.flip();
            } catch (IOException e) {
                logger.error("{} response packet {} encode failed, skip this packet: {}", this, waiter.request, e.getMessage(), e);
                waiter.signal(new TransportErrorException("Response encode failed", e));
                continue;
            }
        }
        if (writeQueue.isEmpty() && !writeBuffer.hasRemaining()) {
            writeLock.lock();
            try {
                if (writeQueue.isEmpty() && !writeBuffer.hasRemaining()) {
                    key.interestOpsAnd(SelectionKey.OP_READ);
                }
            } catch (Exception e) {
                logger.error("{} change key listening status failed: {}", this, e.getMessage(), e);
            } finally {
                writeLock.unlock();
            }
        }
    }

    public void stop() {
        if (this.status != Status.TERMINATING) {
            System.PingResponse response = RpcAutoConfiguration.getRpcClient().getSystemStub().process(System.PingResponse.class, FIN_REQUEST, serviceName, FIN_CALLOPTION);
            if (!RespBaseUtil.isOK(response.getBase())) {
                logger.error("{} fin ping failed: code={}, {}", this, response.getBase().getCode(), response.getBase().getMessage());
            }
        }
    }

    public void stopNow() {
        stopNow(null);
    }

    public void stopNow(Exception error) {
        this.status = Status.TERMINATING;
        try {
            if (keepAlive) RpcAutoConfiguration.getRpcClient().getReactor().removeConnection(serviceName, tag);

            for (Waiter waiter : requestWaiterMap.values()) {
                waiter.signal(error);
            }

            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();

            this.status = Status.TERMINATED;
        } catch (IOException e) {
            logger.error("{} connection stop failure: {}", this, e.getMessage(), e);
        }
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTag() {
        return tag;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        try {
            return "ClientConnectionHandler{" +
                    "serverIp=" + socketChannel.getRemoteAddress() +
                    ", status=" + status +
                    '}';
        } catch (IOException e) {
            logger.error("Failed to get remote address", e);
            return "ClientConnectionHandler{" +
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

    public class Waiter {
        private final long requestId;
        private final Lock lock;
        private final Condition condition;
        private boolean success;
        private Protocol.Packet request;
        private Protocol.Packet response;
        private Exception error;

        public Waiter(long RequestId, Protocol.Packet request) {
            this.requestId = RequestId;
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
            this.success = false;
            this.request = request;
            this.response = null;
            this.error = null;
        }


        public boolean isSuccess() {
            return success;
        }

        private void signal(Protocol.Packet packet) {
            lock.lock();
            try {
                this.response = packet;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        private void signal(Exception error) {
            lock.lock();
            try {
                this.error = error;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }

        public Protocol.Packet getResponse(Duration timeout) throws Exception {
            lock.lock();
            try {
                if (error != null) {
                    throw error;
                }
                if (response != null) {
                    this.success = true;
                    return response;
                }
                if (condition.await(timeout.getNano(), TimeUnit.NANOSECONDS)) {
                    if (error != null) {
                        throw error;
                    }
                    this.success = true;
                    return response;
                } else {
                    logger.debug("{} request {} timeout", ConnectionHandler.this, requestId);
                    throw new TimeoutException("Request timeout");
                }
            } finally {
                lock.unlock();
            }
        }

    }
}
