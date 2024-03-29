package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.core.Context;
import com.github.paopaoyue.mesh.rpc.core.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.core.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.service.ISystemHandler;
import com.github.paopaoyue.mesh.rpc.service.SystemHandler;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class SystemServerStub implements IServerStub {

    private final String SERVICE_NAME = "system";

    private ISystemHandler handler;

    public SystemServerStub() {
        this.handler = new SystemHandler();
    }

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
                case "ping":
                    System.PingRequest request = packet.getBody().unpack(System.PingRequest.class);
                    System.PingResponse response = handler.ping(request);
                    if (response == null) {
                        throw new HandlerException("Ping Handler response is null");
                    }
                    responseBody = Any.pack(response);
                    break;
                default:
                    throw new HandlerNotFoundException(context.getService(), context.getHandler());
            }
        } catch (InvalidProtocolBufferException e) {
            throw new HandlerException("Invalid request body", e);
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
