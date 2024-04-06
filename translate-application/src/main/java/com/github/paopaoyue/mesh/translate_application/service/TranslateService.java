package com.github.paopaoyue.mesh.translate_application.service;

import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.translate_application.proto.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcService(serviceName = "translate-application")
public class TranslateService implements ITranslateService {

    private static Logger logger = LoggerFactory.getLogger(TranslateService.class);

    @Override
    public Translate.TranslateResponse translate(Translate.TranslateRequest request) {
        logger.info("request context: {}", Context.getContext());
        switch (request.getLocale().toLowerCase()) {
            case "en" -> {
                return Translate.TranslateResponse.newBuilder().setResult("hello").build();
            }
            case "zh" -> {
                return Translate.TranslateResponse.newBuilder().setResult("你好").build();
            }
            case "ja" -> {
                return Translate.TranslateResponse.newBuilder().setResult("こんにちは").build();
            }
            case "ko" -> {
                return Translate.TranslateResponse.newBuilder().setResult("안녕하세요").build();
            }
            default -> {
                return Translate.TranslateResponse.newBuilder().setResult("unknown").build();
            }
        }
    }
}
