package com.github.paopaoyue.demo.service;

import com.github.paopaoyue.demo.proto.DemoProto;

public interface IDemoService {

    DemoProto.EchoResponse echo(DemoProto.EchoRequest request);
}
