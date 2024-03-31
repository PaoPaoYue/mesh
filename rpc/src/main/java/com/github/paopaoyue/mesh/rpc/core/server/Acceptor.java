package com.github.paopaoyue.mesh.rpc.core.server;


import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class Acceptor {

    private SubReactor[] subReactors;

    public Acceptor(SubReactor[] subReactors) {
        this.subReactors = subReactors;
    }

    public void accept(SocketChannel channel) {
        // LFU subReactor to handle the request
        // Frequency estimated via the active listening keys
        SubReactor lfu_subReactor = Arrays.stream(subReactors).reduce((a, b) -> a.getSelector().keys().size() < b.getSelector().keys().size() ? a : b).orElse(null);
        if (lfu_subReactor != null) {
            lfu_subReactor.dispatch(channel);
        }
    }
}
