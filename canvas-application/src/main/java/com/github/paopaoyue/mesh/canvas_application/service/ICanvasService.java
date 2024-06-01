package com.github.paopaoyue.mesh.canvas_application.service;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;

public interface ICanvasService {

    CanvasProto.KickUserResponse kickUser(CanvasProto.KickUserRequest request);
    CanvasProto.SendTextMessageResponse sendTextMessage(CanvasProto.SendTextMessageRequest request);
    CanvasProto.LoginResponse login(CanvasProto.LoginRequest request);
    CanvasProto.SyncResponse sync(CanvasProto.SyncRequest request);
}
