package com.github.paopaoyue.mesh.canvas_application.stub;

import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.github.paopaoyue.mesh.canvas_application.service.ICanvasService;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "canvas-application")
public class CanvasServerStub implements IServerStub {

    private static final String SERVICE_NAME = "canvas-application";

    private ICanvasService service;

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
                case "kickUser" ->
                    responseBody = Any.pack(service.kickUser(packet.getBody().unpack(CanvasProto.KickUserRequest.class)));
                case "sendTextMessage" ->
                    responseBody = Any.pack(service.sendTextMessage(packet.getBody().unpack(CanvasProto.SendTextMessageRequest.class)));
                case "login" ->
                    responseBody = Any.pack(service.login(packet.getBody().unpack(CanvasProto.LoginRequest.class)));
                case "sync" ->
                    responseBody = Any.pack(service.sync(packet.getBody().unpack(CanvasProto.SyncRequest.class)));
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
