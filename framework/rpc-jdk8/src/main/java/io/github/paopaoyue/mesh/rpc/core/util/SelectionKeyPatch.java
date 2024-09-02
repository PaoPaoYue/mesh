package io.github.paopaoyue.mesh.rpc.core.util;

import java.nio.channels.SelectionKey;

public class SelectionKeyPatch {
    public static int interestOpsOr(SelectionKey key, int ops) {
        synchronized (key) {
            int oldVal = key.interestOps();
            key.interestOps(oldVal | ops);
            return oldVal;
        }
    }

    public static int interestOpsAnd(SelectionKey key, int ops) {
        synchronized (key) {
            int oldVal = key.interestOps();
            key.interestOps(oldVal & ops);
            return oldVal;
        }
    }
}
