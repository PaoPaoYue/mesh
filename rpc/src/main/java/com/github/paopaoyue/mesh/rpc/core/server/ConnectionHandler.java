package com.github.paopaoyue.mesh.rpc.core.server;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.core.EncodeHelper;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConectionHandler.class);

    private static final int LENGTH_FIELD_OFFSET = 3;
    private static final int LENGTH_FIELD_LENGTH = 4;

    private Properties prop;

    private SelectionKey key;
    private SocketChannel socketChannel;
    private Lock readLock;
    private Lock writeLock;
    private ByteBuffer readBuffer;
    private LinkedBlockingQueue<Protocol.Packet> writeQueue;
    private volatile WorkerStatus status;
    private long lastActiveTime;

    public ConectionHandler(SelectionKey key) {
        this.prop = RpcAutoConfiguration.getProp();

        this.status = WorkerStatus.IDLE;
        this.readLock = new ReentrantLock();
        this.writeLock = new ReentrantLock();
        this.readBuffer = ByteBuffer.allocate(prop.getServerWorkerBufferSize());
        this.writeQueue = new LinkedBlockingQueue<>();
        this.lastActiveTime = 0;

        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
    }

    private void read() {
        readLock.lock();
        try {
            if (status == ConectionHandler.WorkerStatus.TERMINATING || status == ConectionHandler.WorkerStatus.TERMINATED) {
                return;
            }
            try {
                int n = ((SocketChannel) key.channel()).read(readBuffer);
                if (n == -1) {
                    logger.warn("Client {} disconnected before request processing.", this);
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
                    readBuffer.compact();
                    this.parking = true;
                    return;
                }
                if (currentPacketLength == 0) {
                    currentPacketLength = EncodeHelper.convertBytesToFixedInt32(readBuffer.array(), LENGTH_FIELD_OFFSET);
                }
                if (prop.getServerWorkerBufferSize() < currentPacketLength) {
                    // TODO: discard corresponding bytes instead of closing the connection -- low priority
                    logger.error("Client {} request of {} bytes is too large, closing connection.", this, currentPacketLength);
                    stop();
                    return;
                }
                if (readBuffer.remaining() < currentPacketLength) {
                    readBuffer.compact();
                    this.parking = true;
                    return;
                }
                try {
                    currentPacket = Protocol.Packet.parseFrom(readBuffer);
                } catch (InvalidProtocolBufferException e) {
                    logger.error("Client {} request parsing failed, skip this packet: {}", this, e.getMessage(), e);
                    currentPacketLength = 0;
                    currentPacket = null;
                } finally {
                    readBuffer.position(currentPacketLength);
                }
                if (currentPacket != null) {
                    status = WorkerStatus.PROCESSING;
                }
            }
        } finally {
            getReadLock().unlock();
        }


    }

    private void write() {
        writeLock.lock();
        try {
            currentPacket
            writeBuffer.clear();
            socketChannel.write(writeBuffer);
        } catch (IOException e) {
            logger.error("Client {} response writing failed: {}", this, e.getMessage(), e);
            stop();
        } finally {
            writeLock.unlock();
        }
    }


    public boolean checkAlive() {
        if (this.status == WorkerStatus.IDLE && System.currentTimeMillis() - lastActiveTime > prop.getKeepAliveTimeout()) {
            logger.warn("Client {} inactive for {} ms, closing connection.", this, prop.getKeepAliveTimeout());
            stop();
            return false;
        }
        return true;
    }

    public void stop() {
        readLock.lock();
        writeLock.lock();
        if (this.status == WorkerStatus.TERMINATING || this.status == WorkerStatus.TERMINATED) {
            return;
        }
        this.status = WorkerStatus.TERMINATING;
        try {
            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();

            this.status = WorkerStatus.TERMINATED;
        } catch (IOException e) {
            logger.error("Client {} connection stop failure: {}", this, e.getMessage(), e);
        } finally {
            readLock.unlock();
            writeLock.unlock();
        }
    }


    public WorkerStatus getStatus() {
        return status;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public Lock getReadLock() {
        return readLock;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    @Override
    public String toString() {
        try {
            return "ClientHandler{" +
                    "clientIp=" + socketChannel.getRemoteAddress() +
                    ", status=" + status +
                    '}';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum WorkerStatus {
        IDLE,
        READING,
        WRITING,
        TERMINATING,
        TERMINATED,
    }
}
