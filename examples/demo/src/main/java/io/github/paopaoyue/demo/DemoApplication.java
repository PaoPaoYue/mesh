package io.github.paopaoyue.demo;

import io.github.paopaoyue.demo.api.IDemoCaller;
import io.github.paopaoyue.demo.proto.DemoProto;
import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);

        if (RpcAutoConfiguration.getProp().isClientEnabled())
            context.getBean(DemoRunner.class).run();
    }

    @Component
    @ConditionalOnBean(IDemoCaller.class)
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
