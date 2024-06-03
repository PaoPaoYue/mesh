package com.github.paopaoyue.rpc_generator_gradle_plugin;

import java.io.File;

public class InputInfo {
    String projectName;
    String serviceName;
    String serviceShortAlias;
    boolean overrideCaller = true;
    boolean overrideService;
    String group;
    File protoRepoPath;
    File protoServicePath;
    File javaSourcePath;
    File javaPackagePath;

    @Override
    public String toString() {
        return "InputInfo{" +
                "projectName='" + projectName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", serviceShortAlias='" + serviceShortAlias + '\'' +
                ", overrideCaller=" + overrideCaller +
                ", overrideService=" + overrideService +
                ", group='" + group + '\'' +
                ", protoRepoPath=" + protoRepoPath +
                ", protoServicePath=" + protoServicePath +
                ", javaSourcePath=" + javaSourcePath +
                ", javaPackagePath=" + javaPackagePath +
                '}';
    }
}
