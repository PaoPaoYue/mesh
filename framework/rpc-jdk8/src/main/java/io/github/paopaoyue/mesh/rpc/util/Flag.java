package io.github.paopaoyue.mesh.rpc.util;

public class Flag {
    public static final int SYSTEM_CALL = 1 << 7;
    public static final int SERVICE_CALL = 1 << 6;
    public static final int KEEP_ALIVE = 1;
    public static final int FIN = 1 << 1;

    private int value;

    public Flag() {
        this.value = 0;
    }

    public Flag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean is(int flag) {
        return (value & flag) != 0;
    }

    public Flag set(int flag) {
        value |= flag;
        return new Flag(value);
    }

    public void unset(int flag) {
        value &= ~flag;
    }
}
