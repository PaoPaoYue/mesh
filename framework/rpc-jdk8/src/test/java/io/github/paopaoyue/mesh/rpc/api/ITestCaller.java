package io.github.paopaoyue.mesh.rpc.api;

import io.github.paopaoyue.mesh.rpc.proto.RpcTest;

public interface ITestCaller {

    RpcTest.EchoResponse echo(RpcTest.EchoRequest request, CallOption option);
}
