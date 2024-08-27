package io.github.paopaoyue.mesh.rpc.test;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.api.ITestCaller;
import io.github.paopaoyue.mesh.rpc.proto.RpcTest;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Fail.fail;

@ActiveProfiles("test")
@SpringBootTest(classes = RpcAutoConfiguration.class)
class StressTests {

    private static final Logger logger = LoggerFactory.getLogger(StressTests.class);

    @Autowired
    ITestCaller testCaller;

    @Test
    void smokeTest() {
        testRequest(1, 100, false);
    }

    @Test
    void stressTest() {
        testRequest(10, 10000, false);
    }

    @Test
    void stressTestWithDedicatedConnection() {
        testRequest(10, 10000, true);
    }

    private void testRequest(int threadNum, int requestNum, boolean dedicatedConnection) {
        CountDownLatch latch = new CountDownLatch(threadNum);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicInteger success = new AtomicInteger();
        for (int i = 0; i < threadNum; i++) {
            String threadName = "thread-" + i;
            new Thread(() -> {
                for (int j = 0; j < requestNum; j++) {
                    var callOption = new CallOption();
                    if (dedicatedConnection) {
                        callOption.setConnectionTag(threadName);
                    }
                    var resp = testCaller.echo(RpcTest.EchoRequest.newBuilder().setText("hello").build(), callOption);
                    if (RespBaseUtil.isOK(resp.getBase())) {
                        success.addAndGet(1);
                    }
                }
                latch.countDown();
            }, threadName).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("exception", e);
        }
        stopWatch.stop();
        assertThat(success.get() == requestNum * threadNum).isTrue();
        logger.info("test consumed time: {}", stopWatch.getTotalTimeMillis() / 1000.0);
    }

}
