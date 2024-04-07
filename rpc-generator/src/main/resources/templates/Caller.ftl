package ${info.rootPackage}.api;

import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.api.RpcCaller;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import ${info.rootPackage}.proto.${info.protoObject};

@RpcCaller(serviceName = "${info.service}")
public class ${info.serviceClass}Caller implements I${info.serviceClass}Caller {

    IClientStub clientStub;

<#list info.methodMap?keys as key>
    @Override
    public ${info.protoObject}.${info.methodMap[key].output.structName} ${info.methodMap[key].methodName}(${info.protoObject}.${info.methodMap[key].input.structName} request, CallOption option) {
        return clientStub.process(${info.protoObject}.${info.methodMap[key].output.structName}.class, request, option);
    }
</#list>
}