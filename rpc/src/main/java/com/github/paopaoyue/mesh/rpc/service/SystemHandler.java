package com.github.paopaoyue.mesh.rpc.service;

import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;

public class SystemHandler implements ISystemHandler {
    @Override
    public System.PingResponse ping(System.PingRequest request) {
        return System.PingResponse.newBuilder()
                .setMessage("OK")
                .setBase(RespBaseUtil.SuccessRespBase())
                .build();
    }
}
