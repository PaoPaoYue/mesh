package com.github.paopaoyue.mesh.translate_application.stub;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.github.paopaoyue.mesh.translate_application.proto.Translate;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "translate-application")
public class TranslateClientStub implements IClientStub {

    private static final String SERVICE_NAME = "translate-application";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        Context context = Context.getContext();

        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "TranslateRequest" -> "translate";
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
                case "translate" ->
                        respClass.cast(Translate.TranslateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }
}
