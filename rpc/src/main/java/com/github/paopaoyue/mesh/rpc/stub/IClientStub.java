package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.call.CallOption;
import com.google.protobuf.GeneratedMessage;

public interface IClientStub {
    <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option);
}
