package com.github.paopaoyue.mesh.canvas_application.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.api.RpcCaller;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;

@RpcCaller(serviceName = "canvas-application")
public class CanvasCaller implements ICanvasCaller {

    IClientStub clientStub;

    @Override
    public CanvasProto.KickUserResponse kickUser(CanvasProto.KickUserRequest request, CallOption option) {
        return clientStub.process(CanvasProto.KickUserResponse.class, request, option);
    }
    @Override
    public CanvasProto.SendTextMessageResponse sendTextMessage(CanvasProto.SendTextMessageRequest request, CallOption option) {
        return clientStub.process(CanvasProto.SendTextMessageResponse.class, request, option);
    }
    @Override
    public CanvasProto.LoginResponse login(CanvasProto.LoginRequest request, CallOption option) {
        return clientStub.process(CanvasProto.LoginResponse.class, request, option);
    }
    @Override
    public CanvasProto.SyncResponse sync(CanvasProto.SyncRequest request, CallOption option) {
        return clientStub.process(CanvasProto.SyncResponse.class, request, option);
    }
}