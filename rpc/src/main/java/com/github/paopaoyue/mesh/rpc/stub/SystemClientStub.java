package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.call.CallOption;
import com.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

public class SystemClientStub implements IClientStub {

    private static final Flag SYSTEM_FLAG = new Flag().set(Flag.SYSTEM_CALL);

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, String serviceName, CallOption option) {
        Context context = Context.getContext();

        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "PingRequest" -> "ping";
                    default ->
                            throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
                };

        Protocol.Packet packet = Protocol.Packet.newBuilder()
                .setHeader(Protocol.PacketHeader.newBuilder()
                        .setService(serviceName)
                        .setHandler(handlerName)
                        .setRequestId(context.getRequestId())
                        .setFlag(SYSTEM_FLAG.set(option.isKeepAlive() ? Flag.KEEP_ALIVE : 0).getValue())
                        .build())
                .setTraceInfo(TraceInfoUtil.createTraceInfo(context))
                .setBody(Any.pack(request))
                .build();

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender().send(packet, option).getBody().unpack(respClass));
        } catch (Exception e) {
            switch (handlerName) {
                case "ping" -> {
                    return respClass.cast(System.PingResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                }
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            }
        }
    }

    @Override
    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        return process(respClass, request, RpcAutoConfiguration.getProp().getClientServices().stream().map(ServiceProperties::getName).findAny().orElse(""), option);
    }
}
