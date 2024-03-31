package com.github.paopaoyue.mesh.dictionary_application;

import com.github.paopaoyue.mesh.dictionary_application.api.IDictionaryCaller;
import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

@SpringBootTest
class ApplicationTests {

    Logger logger = Logger.getLogger(ApplicationTests.class.getName());

    @Autowired
    ApplicationContext context;

    @Autowired
    IDictionaryCaller dictionaryCaller;

    @Test
    void contextLoads() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var resp = dictionaryCaller.get(Dictionary.GetRequest.newBuilder().setKey("hello").build(), new CallOption());
        logger.info(resp.toString());
    }

}
