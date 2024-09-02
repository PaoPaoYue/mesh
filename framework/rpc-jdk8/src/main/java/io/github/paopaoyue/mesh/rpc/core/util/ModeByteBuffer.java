package io.github.paopaoyue.mesh.rpc.core.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class ModeByteBuffer {
    private final ByteBuffer buffer;
    private boolean readMode; // whether the buffer is read for reading out the content inside

    public ModeByteBuffer(int capacity) {
        this.buffer = ByteBuffer.allocate(capacity);
        this.readMode = false;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public boolean isReadMode() {
        return readMode;
    }

    public boolean isWriteMode() {
        return !readMode;
    }

    public void flip() {
        ((Buffer)this.buffer).flip();
        this.readMode = !this.readMode;
    }

    public void clear() {
        ((Buffer)this.buffer).clear();
        this.readMode = false;
    }

    public boolean hasReadRemaining() {
        return this.readMode && ((Buffer)this.buffer).hasRemaining();
    }

    public boolean hasWriteRemaining() {
        return !this.readMode && ((Buffer)this.buffer).hasRemaining();
    }

    public int readRemaining() {
        return this.readMode ? ((Buffer)this.buffer).remaining() : 0;
    }

    public int writeRemaining() {
        return !this.readMode ? ((Buffer)this.buffer).remaining() : 0;
    }

    public void compact() {
        this.buffer.compact();
        this.readMode = false;
    }

    public int position() {
        return ((Buffer)this.buffer).position();
    }

    public int limit() {
        return ((Buffer)this.buffer).limit();
    }

    public void position(int newPosition) {
        ((Buffer)this.buffer).position(newPosition);
    }

    public void limit(int newLimit) {
        ((Buffer)this.buffer).limit(newLimit);
    }

    public byte[] array() {
        return this.buffer.array();
    }

}
