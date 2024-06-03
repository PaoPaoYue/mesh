package ${info.rootPackage}.service;

import ${info.rootPackage}.proto.${info.protoObject};

public interface I${info.serviceClass}Service {

<#list info.methodMap?keys as key>
    ${info.protoObject}.${info.methodMap[key].output.structName} ${info.methodMap[key].methodName}(${info.protoObject}.${info.methodMap[key].input.structName} request);
</#list>
}
