package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

public class SystemServerStub implements IServerStub {

    public SystemServerStub() {
    }

    @Override
    public Protocol.Packet process(Protocol.Packet packet) throws HandlerException, HandlerNotFoundException {
        Context context = new Context(packet);
        Context.setContext(context);

//        if (!context.getService().equals(SERVICE_NAME)) {
//            throw new HandlerNotFoundException(context.getService(), context.getHandler());
//        }

        Any responseBody;
        try {
            switch (context.getHandler()) {
                case "ping":
                    System.PingRequest request = packet.getBody().unpack(System.PingRequest.class);
                    System.PingResponse response = System.PingResponse.newBuilder()
                            .setMessage("OK")
                            .setBase(RespBaseUtil.SuccessRespBase())
                            .build();
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
