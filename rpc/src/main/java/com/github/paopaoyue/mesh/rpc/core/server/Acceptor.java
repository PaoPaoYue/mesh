package com.github.paopaoyue.mesh.rpc.core.server;


import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;

import java.nio.channels.SelectionKey;
import java.util.Arrays;

public class Acceptor {

    private final SubReactor[] subReactors;

    public Acceptor() {
        subReactors = RpcAutoConfiguration.getRpcServer().getSubReactors();
    }

    public void accept(SelectionKey key) {
        // LFU subReactor to handle the request
        // Frequency estimated via the active listening keys
        SubReactor lfu_subReactor = Arrays.stream(subReactors).reduce((a, b) -> a.getSelector().keys().size() < b.getSelector().keys().size() ? a : b).orElse(null);
        if (lfu_subReactor != null) {
            lfu_subReactor.dispatch(key.channel());
        }
    }
}
