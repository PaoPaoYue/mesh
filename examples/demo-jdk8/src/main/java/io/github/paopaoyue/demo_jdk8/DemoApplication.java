package io.github.paopaoyue.demo_jdk8;

import io.github.paopaoyue.demo_jdk8.api.IDemoCaller;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;
import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

        Logger logger = LogManager.getLogger(DemoRunner.class);

        @Autowired
        private IDemoCaller demoCaller;

        public void run() {
            DemoProto.EchoResponse response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
            if (RespBaseUtil.isOK(response.getBase())) {
                logger.info("echo response: {}", response.getText());
            } else {
                logger.error("echo failed: {}", response.getBase().getMessage());
            }
        }
    }

}
