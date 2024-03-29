package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.core.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;

public class ServerStub implements IServerStub {
    @Override
    public Protocol.Packet process(Protocol.Packet packet) throws HandlerException {
        return null;
    }
}
