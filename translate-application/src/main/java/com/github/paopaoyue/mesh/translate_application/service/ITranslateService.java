package com.github.paopaoyue.mesh.translate_application.service;

import com.github.paopaoyue.mesh.translate_application.proto.Translate;

public interface ITranslateService {
    Translate.TranslateResponse translate(Translate.TranslateRequest request);
}
