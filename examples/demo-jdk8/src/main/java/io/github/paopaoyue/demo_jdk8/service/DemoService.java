package io.github.paopaoyue.demo_jdk8.service;

import io.github.paopaoyue.mesh.rpc.service.RpcService;
import io.github.paopaoyue.mesh.rpc.util.Context;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;

@RpcService(serviceName = "demo-service")
public class DemoService implements IDemoService {

    @Override
    public DemoProto.EchoResponse echo(DemoProto.EchoRequest request) {
         return DemoProto.EchoResponse.newBuilder().setText(request.getText()).build();
    }
}
