package io.github.paopaoyue.mesh.rpc.stub;

import io.github.paopaoyue.mesh.rpc.exception.HandlerException;
import io.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;

public interface IServerStub {
    Protocol.Packet process(Protocol.Packet packet) throws HandlerNotFoundException, HandlerException;
}
