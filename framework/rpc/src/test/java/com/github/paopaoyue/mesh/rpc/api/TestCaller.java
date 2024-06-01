package com.github.paopaoyue.mesh.rpc.api;

import com.github.paopaoyue.mesh.rpc.proto.RpcTest;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;

@RpcCaller(serviceName = "test")
public class TestCaller implements ITestCaller {

    IClientStub clientStub;

    @Override
    public RpcTest.EchoResponse echo(RpcTest.EchoRequest request, CallOption option) {
        return clientStub.process(RpcTest.EchoResponse.class, request, option);
    }
}