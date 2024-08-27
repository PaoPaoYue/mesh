package io.github.paopaoyue.demo_jdk8.stub;

import io.github.paopaoyue.mesh.rpc.exception.HandlerException;
import io.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;
import io.github.paopaoyue.mesh.rpc.stub.IServerStub;
import io.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import io.github.paopaoyue.mesh.rpc.util.Context;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;
import io.github.paopaoyue.demo_jdk8.service.IDemoService;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "demo-service")
public class DemoServerStub implements IServerStub {

    private static final String SERVICE_NAME = "demo-service";

    private IDemoService service;

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
                case "echo":
                    responseBody = Any.pack(service.echo(packet.getBody().unpack(DemoProto.EchoRequest.class)));
                    break;
                default:
                    throw new HandlerNotFoundException(context.getService(), context.getHandler());
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
