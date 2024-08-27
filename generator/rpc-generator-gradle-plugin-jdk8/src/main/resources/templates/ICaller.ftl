package ${info.rootPackage}.api;

import io.github.paopaoyue.mesh.rpc.api.CallOption;
import ${info.rootPackage}.proto.${info.protoObject};

public interface I${info.serviceClass}Caller {

<#list info.methodMap?keys as key>
    ${info.protoObject}.${info.methodMap[key].output.structName} ${info.methodMap[key].methodName}(${info.protoObject}.${info.methodMap[key].input.structName} request, CallOption option);
</#list>
}
