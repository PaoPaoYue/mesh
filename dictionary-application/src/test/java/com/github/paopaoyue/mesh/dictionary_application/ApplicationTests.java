package com.github.paopaoyue.mesh.dictionary_application;

import com.github.paopaoyue.mesh.dictionary_application.api.IDictionaryCaller;
import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.dictionary_application.service.DictionaryService;
import com.github.paopaoyue.mesh.dictionary_application.service.IDictionaryService;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.service.MockRpcService;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class ApplicationTests {

    Logger logger = LoggerFactory.getLogger(ApplicationTests.class.getName());

    @Autowired
    ApplicationContext context;

    @Autowired
    IDictionaryCaller dictionaryCaller;

    @Test
    void benchmark() {
        testRequest(10, 10000);
    }

    private void testRequest(int threadNum, int requestNum) {
        Dictionary.GetRequest request = Dictionary.GetRequest.newBuilder().setKey("hello").build();
        CountDownLatch latch = new CountDownLatch(threadNum);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < threadNum; i++) {
            int finalI = i;
            new Thread(() -> {
                int success = 0;
                for (int j = 0; j < requestNum; j++) {
                    var resp = dictionaryCaller.get(request, new CallOption().setConnectionTag(String.valueOf(finalI)));
                    if (RespBaseUtil.isOK(resp.getBase())) {
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

    @TestConfiguration
    public static class TestConfig {
        @MockRpcService(serviceName = "dictionary-application")
        public IDictionaryService mockDictionaryService() {
            IDictionaryService service = mock(DictionaryService.class);
            when(service.get(any())).thenAnswer(invocation -> {
                Dictionary.GetRequest request = invocation.getArgument(0);
                return Dictionary.GetResponse.newBuilder().setValue(request.getKey()).setValue("hello world").build();
            });
            return service;
        }
    }

}
