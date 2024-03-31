package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.dictionary_application.proto.DictionaryServiceGrpc;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService

public class GrpcDictionarySrevice extends DictionaryServiceGrpc.DictionaryServiceImplBase {

    private final Map<String, String> dictionary;

    public GrpcDictionarySrevice() {
        dictionary = new ConcurrentHashMap<>();
        dictionary.put("hello", "你好");
        dictionary.put("world", "世界");
    }

    @Override
    public void get(Dictionary.GetRequest request, StreamObserver<Dictionary.GetResponse> responseObserver) {
        String value = dictionary.get(request.getKey());
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        if (value != null) {
            responseObserver.onNext(Dictionary.GetResponse.newBuilder()
                    .setKey(request.getKey())
                    .setValue(value)
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build());
        } else {
            responseObserver.onNext(Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "word not found")).build());
        }
        responseObserver.onCompleted();
    }
}
