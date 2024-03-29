package com.github.paopaoyue.mesh.rpc.util;

import com.github.paopaoyue.mesh.rpc.proto.Base;

public class RespBaseUtil {
    public static Base.RespBase SuccessRespBase() {
        return Base.RespBase.newBuilder().setCode(Base.StatusCode.OK_VALUE).build();
    }

    public static Base.RespBase ErrorRespBase(int code, String message) {
        return Base.RespBase.newBuilder().setCode(code).setMessage(message).build();
    }
}
