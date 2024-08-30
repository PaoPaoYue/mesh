package io.github.paopaoyue.mesh.rpc.core.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationPatch {
    public static boolean isPositive(Duration duration) {
        return (duration.get(ChronoUnit.SECONDS) | duration.get(ChronoUnit.NANOS)) > 0;
    }
}
