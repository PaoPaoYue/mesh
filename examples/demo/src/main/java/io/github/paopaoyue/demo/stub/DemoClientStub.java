package io.github.paopaoyue.demo.stub;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;
import io.github.paopaoyue.mesh.rpc.stub.IClientStub;
import io.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import io.github.paopaoyue.mesh.rpc.util.Context;
import io.github.paopaoyue.mesh.rpc.util.Flag;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import io.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import io.github.paopaoyue.demo.proto.DemoProto;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "demo-service")
public class DemoClientStub implements IClientStub {

    private static final String SERVICE_NAME = "demo-service";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "EchoRequest" -> "echo";
                    default ->
                            throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
                };

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender()
                    .send(SERVICE_NAME, handlerName, Any.pack(request), false, option).getBody().unpack(respClass));
        } catch (Exception e) {
            return switch (handlerName) {
                case "echo" ->
                        respClass.cast(DemoProto.EchoResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }
}
