package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.google.protobuf.GeneratedMessageV3;

public interface IClientStub {
    <RESP extends GeneratedMessageV3, REQ extends GeneratedMessageV3> RESP process(Class<RESP> respClass, REQ request, CallOption option);
}
