package com.github.paopaoyue.mesh.dictionary_application.stub;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;

@ServiceClientStub(serviceName = "dictionary-application")
public class DictionaryClientStub implements IClientStub {

    private static final String SERVICE_NAME = "dictionary-application";

    public <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option) {
        Context context = Context.getContext();

        String handlerName =
                switch (request.getClass().getSimpleName()) {
                    case "GetRequest" -> "get";
                    case "AddRequest" -> "add";
                    case "RemoveRequest" -> "remove";
                    case "UpdateRequest" -> "update";
                    default ->
                            throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
                };

        Protocol.Packet packet = Protocol.Packet.newBuilder()
                .setHeader(Protocol.PacketHeader.newBuilder()
                        .setService(SERVICE_NAME)
                        .setHandler(handlerName)
                        .setRequestId(context.getRequestId())
                        .setFlag(Flag.SERVICE_CALL | (option.isKeepAlive() ? Flag.KEEP_ALIVE : 0))
                        .build())
                .setTraceInfo(TraceInfoUtil.createTraceInfo(context))
                .setBody(Any.pack(request))
                .build();

        try {
            return respClass.cast(RpcAutoConfiguration.getRpcClient().getSender().send(packet, option).getBody().unpack(respClass));
        } catch (Exception e) {
            return switch (handlerName) {
                case "get" ->
                        respClass.cast(Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "add" ->
                        respClass.cast(Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "remove" ->
                        respClass.cast(Dictionary.RemoveResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                case "update" ->
                        respClass.cast(Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(e)).build());
                default ->
                        throw new IllegalArgumentException("Invalid request type: " + request.getClass().getSimpleName());
            };
        }
    }
}
