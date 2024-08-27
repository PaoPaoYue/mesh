package io.github.paopaoyue.mesh.rpc.stub;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.proto.RpcTest;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "test")
public class TestClientStub implements IClientStub {

    private static final String SERVICE_NAME = "test";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        String handlerName;
        switch (request.getClass().getSimpleName()) {
            case "EchoRequest":
                handlerName = "echo";
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
        }

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender()
                    .send(SERVICE_NAME, handlerName, Any.pack(request), false, option).getBody().unpack(respClass));
        } catch (Exception e) {
            switch (handlerName) {
                case "echo":
                    return respClass.cast(RpcTest.EchoResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default:
                    throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            }
        }
    }
}
