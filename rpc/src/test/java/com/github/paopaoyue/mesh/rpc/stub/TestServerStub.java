package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.RpcTest;
import com.github.paopaoyue.mesh.rpc.service.ITestService;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "test")
public class TestServerStub implements IServerStub {

    private static final String SERVICE_NAME = "test";

    private ITestService service;

    @Override
    public Protocol.Packet process(Protocol.Packet packet) throws HandlerException, HandlerNotFoundException {
        Context context = new Context(packet);
        Context.setContext(context);

        if (!context.getService().equals(SERVICE_NAME)) {
            throw new HandlerNotFoundException(context.getService(), context.getHandler());
        }

        Any responseBody;
        try {
            switch (context.getHandler()) {
                case "echo" ->
                        responseBody = Any.pack(service.echo(packet.getBody().unpack(RpcTest.EchoRequest.class)));
                default -> throw new HandlerNotFoundException(context.getService(), context.getHandler());
            }
        } catch (Exception e) {
            throw new HandlerException("Handler error", e);
        }

        Protocol.Packet out = Protocol.Packet.newBuilder()
                .setHeader(packet.getHeader())
                .setTraceInfo(packet.getTraceInfo())
                .setBody(responseBody)
                .build();

        Context.removeContext();
        return out;
    }
}
