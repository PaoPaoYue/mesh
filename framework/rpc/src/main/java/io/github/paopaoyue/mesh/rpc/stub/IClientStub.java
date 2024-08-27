package io.github.paopaoyue.mesh.rpc.stub;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import com.google.protobuf.GeneratedMessage;

public interface IClientStub {
    <RESP extends GeneratedMessage, REQ extends GeneratedMessage> RESP process(Class<RESP> respClass, REQ request, CallOption option);
}
