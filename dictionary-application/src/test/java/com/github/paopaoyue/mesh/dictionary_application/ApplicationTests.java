package com.github.paopaoyue.mesh.dictionary_application;

import com.github.paopaoyue.mesh.dictionary_application.api.IDictionaryCaller;
import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;


@SpringBootTest
class ApplicationTests {

    Logger logger = LoggerFactory.getLogger(ApplicationTests.class.getName());

    @Autowired
    ApplicationContext context;

    @Autowired
    IDictionaryCaller dictionaryCaller;

    @Test
    void contextLoads() {
        testRequest(10, 100);
        try {
            Thread.sleep(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void testRequest(int threadNum, int requestNum) {
        Dictionary.GetRequest request = Dictionary.GetRequest.newBuilder().setKey("hello").build();
        CallOption option = new CallOption();
//        Mockito.when(dictionaryCaller.get(request, option))
//                .thenReturn(Dictionary.GetResponse.newBuilder().setValue("world").build());
        CountDownLatch latch = new CountDownLatch(threadNum);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                int success = 0;
                for (int j = 0; j < requestNum; j++) {
                    var resp = dictionaryCaller.get(request, option);
                    if (RespBaseUtil.isOK(resp.getBase())) {
//                        logger.info(new String(resp.getValue().getBytes(), StandardCharsets.UTF_8));
                        success += 1;
                    } else {
                        logger.error("error: {}", resp.getBase().getMessage());
                    }
                }
                logger.info("success: {}", success);
                latch.countDown();
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopWatch.stop();
        logger.info("time: {}", stopWatch.getTotalTimeMillis() / 1000.0);
    }

}
