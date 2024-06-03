package com.github.paopaoyue.rpc_generator_gradle_plugin;

import org.gradle.api.provider.Property;

import static org.apache.tools.ant.Project.getProject;

public abstract class RpcGeneratorPluginExtension {

    public String serviceName = "";
    public String serviceShortAlias = "";
    public boolean overrideService = false;
    public boolean overrideCaller = true;
    public String protoRepoPath = "idl";

}
