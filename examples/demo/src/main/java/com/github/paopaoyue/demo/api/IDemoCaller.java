package com.github.paopaoyue.demo.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.demo.proto.DemoProto;

public interface IDemoCaller {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request, CallOption option);
}
