package io.github.paopaoyue.demo.api;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.demo.proto.DemoProto;

public interface IDemoCaller {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request, CallOption option);
}
