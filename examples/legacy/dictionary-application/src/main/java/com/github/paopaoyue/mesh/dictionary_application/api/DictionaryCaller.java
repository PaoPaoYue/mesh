package com.github.paopaoyue.mesh.dictionary_application.api;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.api.RpcCaller;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;

@RpcCaller(serviceName = "dictionary-application")
public class DictionaryCaller implements IDictionaryCaller {

    private IClientStub clientStub;

    @Override
    public Dictionary.GetResponse get(Dictionary.GetRequest request, CallOption option) {
        return clientStub.process(Dictionary.GetResponse.class, request, option);
    }

    @Override
    public Dictionary.AddResponse add(Dictionary.AddRequest request, CallOption option) {
        return clientStub.process(Dictionary.AddResponse.class, request, option);
    }

    @Override
    public Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request, CallOption option) {
        return clientStub.process(Dictionary.RemoveResponse.class, request, option);
    }

    @Override
    public Dictionary.UpdateResponse update(Dictionary.UpdateRequest request, CallOption option) {
        return clientStub.process(Dictionary.UpdateResponse.class, request, option);
    }
}
