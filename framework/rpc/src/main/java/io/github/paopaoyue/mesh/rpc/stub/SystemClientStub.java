package io.github.paopaoyue.mesh.rpc.stub;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import io.github.paopaoyue.mesh.rpc.proto.System;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

public class SystemClientStub implements IClientStub {

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, String serviceName, CallOption option) {
        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "PingRequest" -> "ping";
                    default ->
                            throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
                };

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender()
                    .send(serviceName, handlerName, Any.pack(request), true, option).getBody().unpack(respClass));
        } catch (Exception e) {
            return switch (handlerName) {
                case "ping" ->
                        respClass.cast(System.PingResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }

    @Override
    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        return process(respClass, request, RpcAutoConfiguration.getProp().getClientServices().stream().map(ServiceProperties::getName).findAny().orElse(""), option);
    }
}
