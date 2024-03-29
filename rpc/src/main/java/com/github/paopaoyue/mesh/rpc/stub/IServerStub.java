package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.core.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.core.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;

public interface IServerStub {
    Protocol.Packet process(Protocol.Packet packet) throws HandlerNotFoundException, HandlerException;
}
