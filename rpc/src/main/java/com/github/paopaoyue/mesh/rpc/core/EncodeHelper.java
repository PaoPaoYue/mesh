package com.github.paopaoyue.mesh.rpc.core;

public class EncodeHelper {
    public static byte[] convertFixedInt32ToBytes(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (value >> (i * 8) & 0xff);
        }
        return bytes;
    }

    public static int convertBytesToFixedInt32(byte[] value, int offset) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += value[i + offset] << (i * 8);
        }
        return result;
    }
}
