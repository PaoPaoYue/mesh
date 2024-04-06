package com.github.paopaoyue.mesh.rpc;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.proto.Base;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@SpringBootTest(classes = RpcAutoConfiguration.class)
class RpcTests {

    Logger logger = LoggerFactory.getLogger(RpcTests.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    Properties prop;

    @Test
    void contextLoads() {
        logger.info("context: {}", context);
        logger.info("prop: {}", prop);
    }

}
