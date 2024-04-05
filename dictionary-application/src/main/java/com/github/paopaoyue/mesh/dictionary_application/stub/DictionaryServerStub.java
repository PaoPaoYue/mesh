package com.github.paopaoyue.mesh.dictionary_application.stub;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.dictionary_application.service.IDictionaryService;
import com.github.paopaoyue.mesh.rpc.exception.HandlerException;
import com.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "dictionary-application")
public class DictionaryServerStub implements IServerStub {

    private static final String SERVICE_NAME = "dictionary-application";

    private final IDictionaryService service;

    public DictionaryServerStub(IDictionaryService service) {
        this.service = service;
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
                case "get" ->
                        responseBody = Any.pack(service.get(packet.getBody().unpack(Dictionary.GetRequest.class)));
                case "add" ->
                        responseBody = Any.pack(service.add(packet.getBody().unpack(Dictionary.AddRequest.class)));
                case "remove" ->
                        responseBody = Any.pack(service.remove(packet.getBody().unpack(Dictionary.RemoveRequest.class)));
                case "update" ->
                        responseBody = Any.pack(service.update(packet.getBody().unpack(Dictionary.UpdateRequest.class)));
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
