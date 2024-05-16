package com.github.paopaoyue.mesh.canvas_application.stub;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "canvas-application")
public class Canvas-applicationClientStub implements IClientStub {

    private static final String SERVICE_NAME = "canvas-application";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        Context context = Context.getContext();

        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "KickUserRequest" -> "kickUser";
                    case "SendTextMessageRequest" -> "sendTextMessage";
                    case "LoginRequest" -> "login";
                    case "SyncRequest" -> "sync";
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
                case "kickUser" ->
                        respClass.cast(CanvasProto.KickUserResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "sendTextMessage" ->
                        respClass.cast(CanvasProto.SendTextMessageResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "login" ->
                        respClass.cast(CanvasProto.LoginResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "sync" ->
                        respClass.cast(CanvasProto.SyncResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }
}
