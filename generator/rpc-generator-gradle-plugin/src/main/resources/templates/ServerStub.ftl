package ${info.rootPackage}.stub;

import io.github.paopaoyue.mesh.rpc.exception.HandlerException;
import io.github.paopaoyue.mesh.rpc.exception.HandlerNotFoundException;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;
import io.github.paopaoyue.mesh.rpc.stub.IServerStub;
import io.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import io.github.paopaoyue.mesh.rpc.util.Context;
import ${info.rootPackage}.proto.${info.protoObject};
import ${info.rootPackage}.service.I${info.serviceClass}Service;
import com.google.protobuf.Any;

@ServiceServerStub(serviceName = "${info.service}")
public class ${info.serviceClass}ServerStub implements IServerStub {

    private static final String SERVICE_NAME = "${info.service}";

    private I${info.serviceClass}Service service;

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
            <#list info.methodMap?keys as key>
                case "${key}" ->
                    responseBody = Any.pack(service.${info.methodMap[key].methodName}(packet.getBody().unpack(${info.protoObject}.${info.methodMap[key].input.structName}.class)));
            </#list>
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
