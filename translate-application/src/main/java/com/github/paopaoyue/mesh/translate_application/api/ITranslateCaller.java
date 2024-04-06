package com.github.paopaoyue.mesh.translate_application.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.translate_application.proto.Translate;

public interface ITranslateCaller {
    Translate.TranslateResponse translate(Translate.TranslateRequest request, CallOption option);
}
