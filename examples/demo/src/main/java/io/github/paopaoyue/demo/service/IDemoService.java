package io.github.paopaoyue.demo.service;

import io.github.paopaoyue.demo.proto.DemoProto;

public interface IDemoService {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request);
}
