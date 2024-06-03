package com.github.paopaoyue.demo;

import com.github.paopaoyue.demo.api.IDemoCaller;
import com.github.paopaoyue.demo.proto.DemoProto;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);
        context.getBean(DemoRunner.class).run();
    }

    @Component
    public static class DemoRunner {

        Logger logger = LoggerFactory.getLogger(DemoRunner.class);

        @Autowired
        private IDemoCaller demoCaller;

        public void run() {
            var response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
            logger.info("echo response: {}", response.getText());
        }
    }

}
