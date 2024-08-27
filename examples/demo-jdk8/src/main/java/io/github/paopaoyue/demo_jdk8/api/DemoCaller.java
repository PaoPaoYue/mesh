package io.github.paopaoyue.demo_jdk8.api;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.api.RpcCaller;
import io.github.paopaoyue.mesh.rpc.stub.IClientStub;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;

@RpcCaller(serviceName = "demo-service")
public class DemoCaller implements IDemoCaller {

    IClientStub clientStub;

    @Override
    public DemoProto.EchoResponse echo(DemoProto.EchoRequest request, CallOption option) {
        return clientStub.process(DemoProto.EchoResponse.class, request, option);
    }
}