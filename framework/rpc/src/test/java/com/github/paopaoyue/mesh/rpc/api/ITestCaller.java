package com.github.paopaoyue.mesh.rpc.api;

import com.github.paopaoyue.mesh.rpc.proto.RpcTest;

public interface ITestCaller {

    RpcTest.EchoResponse echo(RpcTest.EchoRequest request, CallOption option);
}
