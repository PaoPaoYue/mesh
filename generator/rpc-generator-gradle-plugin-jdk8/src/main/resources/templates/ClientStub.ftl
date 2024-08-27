package ${info.rootPackage}.stub;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;
import io.github.paopaoyue.mesh.rpc.stub.IClientStub;
import io.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import io.github.paopaoyue.mesh.rpc.util.Context;
import io.github.paopaoyue.mesh.rpc.util.Flag;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import io.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import ${info.rootPackage}.proto.${info.protoObject};
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "${info.service}")
public class ${info.serviceClass}ClientStub implements IClientStub {

    private static final String SERVICE_NAME = "${info.service}";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        String handlerName;
        switch (request.getClass().getSimpleName()) {
        <#list info.methodMap?keys as key>
            case "${info.methodMap[key].input.structName}":
                handlerName = "${key}";
                break;
        </#list>
            default:
                throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
        }

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender()
                    .send(SERVICE_NAME, handlerName, Any.pack(request), false, option).getBody().unpack(respClass));
        } catch (Exception e) {
            switch (handlerName) {
            <#list info.methodMap?keys as key>
                case "${key}":
                    return respClass.cast(${info.protoObject}.${info.methodMap[key].output.structName}.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
            </#list>
                default:
                    throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            }
        }
    }
}
