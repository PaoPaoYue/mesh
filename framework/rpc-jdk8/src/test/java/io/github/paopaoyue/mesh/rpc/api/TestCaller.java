package io.github.paopaoyue.mesh.rpc.api;

import io.github.paopaoyue.mesh.rpc.proto.RpcTest;
import io.github.paopaoyue.mesh.rpc.stub.IClientStub;

@RpcCaller(serviceName = "test")
public class TestCaller implements ITestCaller {

    IClientStub clientStub;

    @Override
    public RpcTest.EchoResponse echo(RpcTest.EchoRequest request, CallOption option) {
        return clientStub.process(RpcTest.EchoResponse.class, request, option);
    }
}