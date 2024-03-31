package com.github.paopaoyue.mesh.dictionary_application.service;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;

public interface IDictionaryService {
    Dictionary.GetResponse get(Dictionary.GetRequest request);

    Dictionary.AddResponse add(Dictionary.AddRequest request);

    Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request);

    Dictionary.UpdateResponse update(Dictionary.UpdateRequest request);
}
