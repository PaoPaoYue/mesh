package com.github.paopaoyue.mesh.canvas_application.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;

public interface ICanvasCaller {

    CanvasProto.KickUserResponse kickUser(CanvasProto.KickUserRequest request, CallOption option);
    CanvasProto.SendTextMessageResponse sendTextMessage(CanvasProto.SendTextMessageRequest request, CallOption option);
    CanvasProto.LoginResponse login(CanvasProto.LoginRequest request, CallOption option);
    CanvasProto.SyncResponse sync(CanvasProto.SyncRequest request, CallOption option);
}
