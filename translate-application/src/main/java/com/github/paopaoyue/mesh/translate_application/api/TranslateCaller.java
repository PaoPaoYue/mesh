package com.github.paopaoyue.mesh.translate_application.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.api.RpcCaller;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.translate_application.proto.Translate;

@RpcCaller(serviceName = "translate-application")
public class TranslateCaller implements ITranslateCaller {

    IClientStub clientStub;

    @Override
    public Translate.TranslateResponse translate(Translate.TranslateRequest request, CallOption option) {
        return clientStub.process(Translate.TranslateResponse.class, request, option);
    }
}
