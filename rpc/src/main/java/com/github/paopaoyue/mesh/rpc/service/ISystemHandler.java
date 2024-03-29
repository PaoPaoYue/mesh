package com.github.paopaoyue.mesh.rpc.service;

import com.github.paopaoyue.mesh.rpc.proto.System;

public interface ISystemHandler {
    public System.PingResponse ping(System.PingRequest request);
}
