package com.github.paopaoyue.mesh.rpc.service;

import com.github.paopaoyue.mesh.rpc.proto.RpcTest;

public interface ITestService {

    RpcTest.EchoResponse echo(RpcTest.EchoRequest request);
}
