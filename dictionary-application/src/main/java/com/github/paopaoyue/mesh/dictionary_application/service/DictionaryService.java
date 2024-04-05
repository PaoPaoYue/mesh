package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;

@RpcService(serviceName = "dictionary-application")
public class DictionaryService implements IDictionaryService {

    private LSMTree db;

    public DictionaryService(LSMTree db) {
        this.db = db;
    }

    @Override
    public Dictionary.GetResponse get(Dictionary.GetRequest request) {
        try {
            return Dictionary.GetResponse.newBuilder()
                    .setKey(request.getKey())
                    .setValue(db.query(request.getKey()))
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } catch (IllegalArgumentException e) {
            return Dictionary.GetResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, e.getMessage())).build();
        }
    }

    @Override
    public Dictionary.AddResponse add(Dictionary.AddRequest request) {
        try {
            db.add(request.getKey(), request.getValue());
            return Dictionary.AddResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } catch (IllegalArgumentException e) {
            return Dictionary.AddResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_DUPLICATE_VALUE, e.getMessage())).build();
        }
    }

    @Override
    public Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request) {
        try {
            db.remove(request.getKey());
            return Dictionary.RemoveResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } catch (IllegalArgumentException e) {
            return Dictionary.RemoveResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, e.getMessage())).build();
        }
    }

    @Override
    public Dictionary.UpdateResponse update(Dictionary.UpdateRequest request) {
        try {
            db.update(request.getKey(), request.getValue());
            return Dictionary.UpdateResponse.newBuilder()
                    .setKey(request.getKey())
                    .setBase(RespBaseUtil.SuccessRespBase())
                    .build();
        } catch (IllegalArgumentException e) {
            return Dictionary.UpdateResponse.newBuilder().setBase(RespBaseUtil.ErrorRespBase(Dictionary.ServiceStatusCode.KEY_NOT_FOUND_VALUE, e.getMessage())).build();
        }
    }
}
