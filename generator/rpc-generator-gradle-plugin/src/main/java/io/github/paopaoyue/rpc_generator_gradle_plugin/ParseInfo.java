package io.github.paopaoyue.rpc_generator_gradle_plugin;

import java.util.HashMap;
import java.util.Map;

public class ParseInfo {

    public Map<String, ProtoStruct> structMap;
    public Map<String, ProtoMethod> methodMap;

    public ParseInfo() {
        this.structMap = new HashMap<>();
        this.methodMap = new HashMap<>();
    }

    public static class ProtoStruct {
        public String packageName;
        public String objectName;
        public String structName;

        public ProtoStruct(String packageName, String objectName, String structName) {
            this.packageName = packageName;
            this.objectName = objectName;
            this.structName = structName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public String getStructName() {
            return structName;
        }

        public void setStructName(String structName) {
            this.structName = structName;
        }
    }

    public static class ProtoMethod {
        public String packageName;
        public String objectName;
        public String methodName;
        public ProtoStruct input;
        public ProtoStruct output;

        public ProtoMethod(String packageName, String objectName, String methodName, ProtoStruct input, ProtoStruct output) {
            this.packageName = packageName;
            this.objectName = objectName;
            this.methodName = methodName;
            this.input = input;
            this.output = output;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public ProtoStruct getInput() {
            return input;
        }

        public void setInput(ProtoStruct input) {
            this.input = input;
        }

        public ProtoStruct getOutput() {
            return output;
        }

        public void setOutput(ProtoStruct output) {
            this.output = output;
        }
    }

    @Override
    public String toString() {
        return "ParseInfo{" +
                "structMap=" + structMap +
                ", methodMap=" + methodMap +
                '}';
    }
}
