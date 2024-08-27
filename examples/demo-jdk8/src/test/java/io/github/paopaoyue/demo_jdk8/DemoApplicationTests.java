package io.github.paopaoyue.demo_jdk8;

import io.github.paopaoyue.demo_jdk8.api.IDemoCaller;
import io.github.paopaoyue.demo_jdk8.proto.DemoProto;
import io.github.paopaoyue.demo_jdk8.service.DemoService;
import io.github.paopaoyue.demo_jdk8.service.IDemoService;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.service.MockRpcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

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
        DemoProto.EchoResponse response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
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
