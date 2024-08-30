package io.github.paopaoyue.mesh.rpc.test;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.api.ITestCaller;
import io.github.paopaoyue.mesh.rpc.proto.RpcTest;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

@ActiveProfiles("test")
@SpringBootTest(properties = "logging.level.io.github.paopaoyue.mesh=DEBUG", classes = RpcAutoConfiguration.class)
public class KeepAliveTimeoutTests {

    private static final Logger logger = LogManager.getLogger(KeepAliveTimeoutTests.class);

    @Autowired
    ITestCaller testCaller;

    @Test
    void testKeepAliveTimeout() {
        RpcAutoConfiguration.getProp().setKeepAliveInterval(2);
        RpcAutoConfiguration.getProp().setKeepAliveTimeout(1);
        RpcTest.EchoResponse resp = testCaller.echo(RpcTest.EchoRequest.newBuilder().setText("hello").build(), new CallOption());
        assertThat(RespBaseUtil.isOK(resp.getBase())).isTrue();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail("exception", e);
        }
        resp = testCaller.echo(RpcTest.EchoRequest.newBuilder().setText("hello").build(), new CallOption());
        assertThat(RespBaseUtil.isOK(resp.getBase())).isTrue();
    }
}
