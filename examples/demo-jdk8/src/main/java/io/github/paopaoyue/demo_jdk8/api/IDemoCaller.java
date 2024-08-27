package io.github.paopaoyue.demo_jdk8.api;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;

public interface IDemoCaller {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request, CallOption option);
}
