package io.github.paopaoyue.demo_jdk8.service;

import io.github.paopaoyue.demo_jdk8.proto.DemoProto;

public interface IDemoService {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request);
}
