package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RpcService(serviceName = "dictionary-application")
public class DictionaryService implements IDictionaryService {

    private final Map<String, String> dictionary;

    public DictionaryService() {
        dictionary = new ConcurrentHashMap<>();
        dictionary.put("hello", "你好");
        dictionary.put("world", "世界");
    }

    @Override
    public Dictionary.GetResponse get(Dictionary.GetRequest request) {
        String value = dictionary.get(request.getKey());
        if (value != null) {
            return Dictionary.GetResponse.newBuilder()
                    .setKey(request.getKey())
                    .setValue(value)
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        }
        return Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "word not found")).build();
    }

    @Override
    public Dictionary.AddResponse add(Dictionary.AddRequest request) {
        String previousValue = dictionary.putIfAbsent(request.getKey(), request.getValue());
        if (previousValue != null) {
            return Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_DUPLICATE_VALUE, "word already exists")).build();
        }
        return Dictionary.AddResponse.newBuilder()
                .setKey(request.getKey())
                .setBase(RespBaseUtil.SuccessRespBase())
                .build();
    }

    @Override
    public Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request) {
        String previousValue = dictionary.remove(request.getKey());
        if (previousValue != null) {
            return Dictionary.RemoveResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        }
        return Dictionary.RemoveResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "word not found")).build();
    }

    @Override
    public Dictionary.UpdateResponse update(Dictionary.UpdateRequest request) {
        String previousValue = dictionary.replace(request.getKey(), request.getValue());
        if (previousValue != null) {
            return Dictionary.UpdateResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        }
        return Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "word not found")).build();
    }
}
