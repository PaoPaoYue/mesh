package ${info.rootPackage}.service;

import io.github.paopaoyue.mesh.rpc.service.RpcService;
import io.github.paopaoyue.mesh.rpc.util.Context;
import ${info.rootPackage}.proto.${info.protoObject};

@RpcService(serviceName = "${info.service}")
public class ${info.serviceClass}Service implements I${info.serviceClass}Service {

<#list info.methodMap?keys as key>
    @Override
    public ${info.protoObject}.${info.methodMap[key].output.structName} ${info.methodMap[key].methodName}(${info.protoObject}.${info.methodMap[key].input.structName} request) {
         return ${info.protoObject}.${info.methodMap[key].output.structName}.newBuilder().build();
    }
</#list>
}
