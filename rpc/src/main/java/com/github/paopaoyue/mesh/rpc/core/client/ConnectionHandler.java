package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.exception.ServiceUnavailableException;
import com.github.paopaoyue.mesh.rpc.exception.TimeoutException;
import com.github.paopaoyue.mesh.rpc.exception.TransportErrorException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.ModeByteBuffer;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.google.protobuf.Any;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
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

    private boolean keepAlive;
    private String serviceName;
    private String tag;
    private Reactor reactor;
    private SelectionKey key;
    private SocketChannel socketChannel;
    private int bufferLen;
    private ModeByteBuffer readBuffer;
    private ModeByteBuffer writeBuffer;
    private Lock writeLock;
    private LinkedBlockingQueue<Waiter> writeQueue;
    private Map<Long, Waiter> requestWaiterMap;
    private volatile Status status;

    public ConnectionHandler(Reactor reactor, boolean keepAlive, String serviceName, String tag, SelectionKey key) {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.IDLE;
        this.bufferLen = prop.getPacketMaxSize();
        this.readBuffer = new ModeByteBuffer(bufferLen);
        this.writeBuffer = new ModeByteBuffer(bufferLen);
        this.writeLock = new ReentrantLock();
        this.writeQueue = new LinkedBlockingQueue<>();
        this.requestWaiterMap = new HashMap<>();

        this.keepAlive = keepAlive;
        this.serviceName = serviceName;
        this.tag = tag;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
        this.reactor = reactor;

        if (keepAlive) {
            RpcAutoConfiguration.getRpcClient().getReactor().addConnection(serviceName, tag, this);
        }
    }

    // not thread safe, run only in single thread
    public void process() {
        if (status == Status.TERMINATED) {
            return;
        }
        if (status == Status.IDLE) {
            status = Status.PROCESSING;
        }
        if (key.isValid() && key.isReadable()) {
            read();
        }
        if (key.isValid() && key.isWritable()) {
            write();
        }
    }

    // thread safe, can be called by multiple threads
    public Waiter sendPacket(Protocol.Packet packet) {
        if ((status == Status.TERMINATED || status == Status.TERMINATING) && (packet.getHeader().getFlag() & Flag.FIN) == 0) {
            throw new IllegalStateException("Connection is terminated or terminating, no new request allowed");
        }
        if ((packet.getHeader().getFlag() & Flag.FIN) != 0) {
            this.status = Status.TERMINATING;
        }
        Waiter waiter = new Waiter(packet.getHeader().getRequestId(), packet);
        writeLock.lock();
        try {
            writeQueue.offer(waiter);
            if ((key.interestOps() & SelectionKey.OP_WRITE) == 0 && (!writeQueue.isEmpty() || writeBuffer.hasReadRemaining())) {
                key.interestOpsOr(SelectionKey.OP_WRITE);
                reactor.getSelector().wakeup();
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
            int n = socketChannel.read(readBuffer.getBuffer());
            if (n == -1) {
                logger.warn("{} disconnected at remote", this);
                stopNow(new ServiceUnavailableException("Connection disconnected at remote"));
                return;
            }
        } catch (ClosedChannelException | SocketException e) {
            logger.error("{} disconnected abruptly: {}", this, e.getMessage(), e);
            stopNow(new ServiceUnavailableException("Connection disconnected abruptly", e));
            return;
        } catch (Exception e) {
            logger.error("{} counter unexpected exception: {}", this, e.getMessage(), e);
            stopNow(e);
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
                logger.error("{} response of {} bytes is too large, closing connection", this, currentPacketLength);
                stopNow(new TransportErrorException("Response too large"));
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
                logger.error("{} request packet parsing failed, skip this packet: {}", this, e.getMessage(), e);
                currentPacketLength = 0;
            } finally {
                readBuffer.limit(currentLimit);
                readBuffer.position(readBuffer.position() + currentPacketLength);
            }
            if (currentPacket != null) {
                logger.debug("{} received response of request: {}", this, currentPacket.getHeader().getRequestId());
                Waiter waiter = requestWaiterMap.get(currentPacket.getHeader().getRequestId());
                if (waiter == null) {
                    logger.error("{} request {} not found, skip this packet", this, currentPacket.getHeader().getRequestId());
                    continue;
                }
                waiter.signal(currentPacket);
                requestWaiterMap.remove(currentPacket.getHeader().getRequestId());
            }
        }
        if (requestWaiterMap.isEmpty()) {
            if (status == Status.TERMINATING) {
                stopNow();
            } else {
                status = Status.IDLE;
            }
        }
    }

    private void write() {
        while (true) {
            if (writeBuffer.hasReadRemaining()) {
                try {
                    socketChannel.write(writeBuffer.getBuffer());
                    if (writeBuffer.hasReadRemaining()) {
                        logger.debug("{} write waiting for socket buffer, available: {} bytes, want: {} bytes", this, socketChannel.socket().getSendBufferSize(), writeBuffer.readRemaining());
                        break;
                    }
                    writeBuffer.clear();
                } catch (ClosedChannelException | SocketException e) {
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
                CodedOutputStream outputStream = CodedOutputStream.newInstance(writeBuffer.getBuffer());
                waiter.request.writeTo(outputStream);
                outputStream.flush();
                // inject packet length
                int currentPacketLength = writeBuffer.position();
                for (int i = 0; i < LENGTH_FIELD_LENGTH; i++) {
                    writeBuffer.array()[LENGTH_FIELD_OFFSET + i] = (byte) (currentPacketLength >> (i * 8) & 0xff);
                }
                logger.debug("{} send request: {}", this, waiter.request.getHeader().getRequestId());
                writeBuffer.flip();
            } catch (IOException e) {
                logger.error("{} response packet {} encode failed, skip this packet: {}", this, waiter.request, e.getMessage(), e);
                waiter.signal(new TransportErrorException("Response encode failed", e));
                continue;
            }
        }
        if (writeQueue.isEmpty() && !writeBuffer.hasReadRemaining()) {
            writeLock.lock();
            try {
                if (writeQueue.isEmpty() && !writeBuffer.hasReadRemaining()) {
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
            if (keepAlive) RpcAutoConfiguration.getRpcClient().getReactor().removeConnection(serviceName, tag);
            this.status = Status.TERMINATING;
            Context context = Context.getContext();
            Waiter waiter = sendPacket(Protocol.Packet.newBuilder()
                    .setHeader(Protocol.PacketHeader.newBuilder()
                            .setLength(1)
                            .setService(serviceName)
                            .setHandler("ping")
                            .setRequestId(context.getRequestId())
                            .setFlag(Flag.SYSTEM_CALL | Flag.FIN | (keepAlive ? Flag.KEEP_ALIVE : 0))
                            .build())
                    .setTraceInfo(TraceInfoUtil.createTraceInfo(context))
                    .setBody(Any.pack(System.PingRequest.newBuilder().setMessage("FIN").build())).build());
            try {
                waiter.getResponse(Duration.ofSeconds(1));
            } catch (Exception e) {
                logger.error("{} send fin packet failed: {}", this, e.getMessage(), e);
            }
        }
    }

    public void stopNow() {
        stopNow(null);
    }

    public void stopNow(Exception error) {
        if (keepAlive) RpcAutoConfiguration.getRpcClient().getReactor().removeConnection(serviceName, tag);
        this.status = Status.TERMINATING;
        try {
            for (Waiter waiter : requestWaiterMap.values()) {
                waiter.signal(error);
            }

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
                if (condition.await(timeout.getSeconds(), TimeUnit.SECONDS)) {
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
