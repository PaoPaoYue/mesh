package com.github.paopaoyue.mesh.rpc.util;

import java.nio.charset.StandardCharsets;

public class ByteArrayConvertor {
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String convertToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return '[' + new String(hexChars, StandardCharsets.UTF_8) + ']';
    }
}
