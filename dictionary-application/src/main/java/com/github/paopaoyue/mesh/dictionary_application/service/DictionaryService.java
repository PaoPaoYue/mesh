package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.proto.Base;
import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;

@RpcService(serviceName = "dictionary-application")
public class DictionaryService implements IDictionaryService {

    private static final int MAX_KEY_LENGTH = 64;
    private static final int MAX_VALUE_LENGTH = 1024;

    private LSMTree db;

    public DictionaryService(LSMTree db) {
        this.db = db;
    }

    @Override
    public Dictionary.GetResponse get(Dictionary.GetRequest request) {
        if (request.getKey().isEmpty() || request.getKey().length() > MAX_KEY_LENGTH) {
            return Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid key")).build();
        }
        String value = db.query(request.getKey());
        if (value == null) {
            return Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "key not found")).build();
        } else {
            return Dictionary.GetResponse.newBuilder()
                    .setKey(request.getKey())
                    .setValue(value)
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        }
    }

    @Override
    public Dictionary.AddResponse add(Dictionary.AddRequest request) {
        if (request.getKey().isEmpty() || request.getKey().length() > MAX_KEY_LENGTH) {
            return Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid key")).build();
        }
        if (request.getValue().isEmpty() || request.getValue().length() > MAX_VALUE_LENGTH) {
            return Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid value")).build();
        }
        if (db.add(request.getKey(), request.getValue())) {
            return Dictionary.AddResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } else {
            return Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_DUPLICATE_VALUE, "key already exists")).build();
        }
    }

    @Override
    public Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request) {
        if (request.getKey().isEmpty() || request.getKey().length() > MAX_KEY_LENGTH) {
            return Dictionary.RemoveResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid key")).build();
        }
        if (db.remove(request.getKey())) {
            return Dictionary.RemoveResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } else {
            return Dictionary.RemoveResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "key not found")).build();
        }
    }

    @Override
    public Dictionary.UpdateResponse update(Dictionary.UpdateRequest request) {
        if (request.getKey().isEmpty() || request.getKey().length() > MAX_KEY_LENGTH) {
            return Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid key")).build();
        }
        if (request.getValue().isEmpty() || request.getValue().length() > MAX_VALUE_LENGTH) {
            return Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Base.StatusCode.INVALID_PARAM_ERROR, "invalid value")).build();
        }
        if (db.update(request.getKey(), request.getValue())) {
            return Dictionary.UpdateResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } else {
            return Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, "key not found")).build();
        }
    }
}
