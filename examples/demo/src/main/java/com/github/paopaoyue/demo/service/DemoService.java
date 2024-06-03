package com.github.paopaoyue.demo.service;

import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.demo.proto.DemoProto;

@RpcService(serviceName = "demo-service")
public class DemoService implements IDemoService {

    @Override
    public DemoProto.EchoResponse echo(DemoProto.EchoRequest request) {
         return DemoProto.EchoResponse.newBuilder().setText(request.getText()).build();
    }
}
