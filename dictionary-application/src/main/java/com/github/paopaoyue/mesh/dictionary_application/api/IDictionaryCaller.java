package com.github.paopaoyue.mesh.dictionary_application.api;

import com.github.paopaoyue.mesh.dictionary_application.proto.Dictionary;
import com.github.paopaoyue.mesh.rpc.api.CallOption;

public interface IDictionaryCaller {
    Dictionary.GetResponse get(Dictionary.GetRequest request, CallOption option);

    Dictionary.AddResponse add(Dictionary.AddRequest request, CallOption option);

    Dictionary.RemoveResponse remove(Dictionary.RemoveRequest request, CallOption option);

    Dictionary.UpdateResponse update(Dictionary.UpdateRequest request, CallOption option);
}
