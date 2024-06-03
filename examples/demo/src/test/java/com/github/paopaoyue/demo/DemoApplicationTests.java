package com.github.paopaoyue.demo;

import com.github.paopaoyue.demo.api.IDemoCaller;
import com.github.paopaoyue.demo.proto.DemoProto;
import com.github.paopaoyue.demo.service.DemoService;
import com.github.paopaoyue.demo.service.IDemoService;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.service.MockRpcService;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    IDemoCaller demoCaller;

    @Test
    void MockTest() {
        var response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
        assertThat(response.getText()).isEqualTo("noop!");
    }

    @TestConfiguration
    public static class TestConfig {

        @MockRpcService(serviceName = "demo-service")
        public IDemoService mockDemoService() {
            IDemoService service = mock(DemoService.class);
            when(service.echo(any())).thenAnswer(invocation -> {
                DemoProto.EchoRequest request = invocation.getArgument(0);
                return DemoProto.EchoResponse.newBuilder().setText("noop!").build();
            });
            return service;
        }
    }
}
