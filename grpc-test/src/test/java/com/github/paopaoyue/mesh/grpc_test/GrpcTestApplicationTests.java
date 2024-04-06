package com.github.paopaoyue.mesh.grpc_test;

import com.github.paopaoyue.mesh.grpc_test.proto.Base;
import com.github.paopaoyue.mesh.grpc_test.proto.Dictionary;
import com.github.paopaoyue.mesh.grpc_test.proto.DictionaryServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
class GrpcTestApplicationTests {

    private static Logger logger = LoggerFactory.getLogger(GrpcTestApplicationTests.class.getName());

    @GrpcClient("dictionary-application")
    DictionaryServiceGrpc.DictionaryServiceBlockingStub dictionaryCaller;

    @Test
    void benchmark() {
        testRequest(10, 10000);
    }

    private void testRequest(int threadNum, int requestNum) {
        Dictionary.GetRequest request = Dictionary.GetRequest.newBuilder().setKey("hello world").build();
        CountDownLatch latch = new CountDownLatch(threadNum);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                int success = 0;
                for (int j = 0; j < requestNum; j++) {
                    var resp = dictionaryCaller.get(request);
                    if (resp.getBase().getCode() == 0) {
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
        @GrpcService()
        public static class GrpcDictionaryService extends DictionaryServiceGrpc.DictionaryServiceImplBase {
            @Override
            public void get(Dictionary.GetRequest request, StreamObserver<Dictionary.GetResponse> responseObserver) {
                responseObserver.onNext(Dictionary.GetResponse.newBuilder()
                        .setKey(request.getKey())
                        .setValue("hello world")
                        .setBase(Base.RespBase.newBuilder().build())
                        .build());
                responseObserver.onCompleted();
            }
        }
    }

}
