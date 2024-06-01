package ${info.rootPackage}.stub;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import ${info.rootPackage}.proto.${info.protoObject};
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "${info.service}")
public class ${info.serviceClass}ClientStub implements IClientStub {

    private static final String SERVICE_NAME = "${info.service}";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        Context context = Context.getContext();

        String handlerName =
                switch (request.getClass().getSimpleName()) {
                <#list info.methodMap?keys as key>
                    case "${info.methodMap[key].input.structName}" -> "${key}";
                </#list>
                    default ->
                            throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
                };

        Protocol.Packet packet = Protocol.Packet.newBuilder()
                .setHeader(Protocol.PacketHeader.newBuilder()
                        .setLength(1)
                        .setService(SERVICE_NAME)
                        .setHandler(handlerName)
                        .setRequestId(context.getRequestId())
                        .setFlag(Flag.SERVICE_CALL | (option.isKeepAlive() ? Flag.KEEP_ALIVE : 0) | (option.isFin() ? Flag.FIN : 0))
                        .build())
                .setTraceInfo(TraceInfoUtil.createTraceInfo(context))
                .setBody(Any.pack(request))
                .build();

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender().send(packet, option).getBody().unpack(respClass));
        } catch (Exception e) {
            return switch (handlerName) {
            <#list info.methodMap?keys as key>
                case "${key}" ->
                        respClass.cast(${info.protoObject}.${info.methodMap[key].output.structName}.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
            </#list>
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }
}
