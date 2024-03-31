package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;

public class SystemServerStub implements IServerStub {

    public SystemServerStub() {
    }

    @Override
    public Protocol.Packet process(Protocol.Packet packet) throws HandlerException, HandlerNotFoundException {
        Context context = new Context(packet);
        Context.setContext(context);

        Any responseBody;
        try {
            switch (context.getHandler()) {
                case "ping" -> {
                    System.PingRequest request = packet.getBody().unpack(System.PingRequest.class);
                    System.PingResponse response = System.PingResponse.newBuilder()
                            .setMessage("OK")
                            .setBase(RespBaseUtil.SuccessRespBase())
                            .build();
                    responseBody = Any.pack(response);
                }
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
