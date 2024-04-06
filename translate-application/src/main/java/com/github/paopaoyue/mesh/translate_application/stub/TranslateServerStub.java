package com.github.paopaoyue.mesh.translate_application.stub;

import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.translate_application.proto.Translate;
import com.github.paopaoyue.mesh.translate_application.service.ITranslateService;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "translate-application")
public class TranslateServerStub implements IServerStub {

    private static final String SERVICE_NAME = "translate-application";

    private ITranslateService service;

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
                case "translate" ->
                        responseBody = Any.pack(service.translate(packet.getBody().unpack(Translate.TranslateRequest.class)));
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
