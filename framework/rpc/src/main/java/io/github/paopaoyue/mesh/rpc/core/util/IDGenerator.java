package io.github.paopaoyue.mesh.rpc.core.util;

import java.util.UUID;

public class IDGenerator {

    public static long generateTraceId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    public static long generateRequestId() {
        return UUID.randomUUID().getLeastSignificantBits() & Long.MAX_VALUE;
    }

}
