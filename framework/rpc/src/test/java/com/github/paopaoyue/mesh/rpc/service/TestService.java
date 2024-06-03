package com.github.paopaoyue.mesh.rpc.service;

import com.github.paopaoyue.mesh.rpc.proto.RpcTest;

@RpcService(serviceName = "test")
public class TestService implements ITestService {

    @Override
    public RpcTest.EchoResponse echo(RpcTest.EchoRequest request) {
        return RpcTest.EchoResponse.newBuilder().build();
    }
}
