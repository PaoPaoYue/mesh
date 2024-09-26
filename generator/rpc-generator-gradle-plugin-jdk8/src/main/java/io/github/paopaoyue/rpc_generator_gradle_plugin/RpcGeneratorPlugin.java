package io.github.paopaoyue.rpc_generator_gradle_plugin;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.plugins.JavaPluginExtension;

import java.nio.file.Paths;

public class RpcGeneratorPlugin implements Plugin<Project> {

    private InputInfo inputInfo;

    public void apply(Project project) {
        project.getDependencies().add("implementation", "com.google.protobuf:protobuf-java:4.26.0");
        project.getDependencies().add("implementation", "io.github.paopaoyue:ypp-rpc:0.1.0-jdk8");

        RpcGeneratorPluginExtension extension = project.getExtensions()
                .create("rpcGenerator", RpcGeneratorPluginExtension.class);

        project.getTasks().register("verifyConfig", task -> {
            task.setGroup("rpc-generator");
            task.doLast(s -> {
                inputInfo = generateInputInfo(project);
            });
        });
        project.getTasks().register("generateIdl", task -> {
            task.setGroup("rpc-generator");
            task.dependsOn("verifyConfig");
            task.doLast(s -> {
                new Generator().generateIdl(inputInfo);
            });
        });
        project.getTasks().register("generateRpc", task -> {
            task.setGroup("rpc-generator");
            task.dependsOn("verifyConfig");
            task.doLast(s -> {
                ParseInfo parseInfo = new Parser().parse(inputInfo.protoServicePath);
                new Generator().generateRpc(inputInfo, parseInfo);
            });
        });
    }

    private InputInfo generateInputInfo(Project project) {
        InputInfo inputInfo = new InputInfo();

        inputInfo.projectName = project.getName();

        RpcGeneratorPluginExtension extension = project.getExtensions().findByType(RpcGeneratorPluginExtension.class);
        if (extension == null) {
            throw new IllegalArgumentException("rpcGenerator plugin extensions not found");
        }

        if (extension.serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name is required in rpcGenerator plugin");
        } else {
            inputInfo.serviceName = extension.serviceName;
        }

        if (extension.serviceShortAlias.isEmpty()) {
            System.out.println("Service short alias is not set in rpcGenerator plugin, using service name as default");
            inputInfo.serviceShortAlias = extension.serviceName;
        } else {
            inputInfo.serviceShortAlias = extension.serviceShortAlias;
        }

        inputInfo.overrideCaller = extension.overrideCaller;
        inputInfo.overrideService = extension.overrideService;

        if (extension.protoRepoPath.isEmpty()) {
            throw new IllegalArgumentException("Proto repo path is required in rpcGenerator plugin");
        } else {
            inputInfo.protoRepoPath = project.getProjectDir().getAbsoluteFile().toPath().resolve(extension.protoRepoPath).toFile();
            inputInfo.protoServicePath = Paths.get(inputInfo.protoRepoPath.getAbsolutePath(), inputInfo.serviceName).toFile();
        }

        if (project.getGroup().toString().isEmpty()) {
            throw new IllegalArgumentException("Group is required in project");
        } else {
            inputInfo.group = project.getGroup().toString();
        }

        if (project.getPlugins().hasPlugin("java")) {
            JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
            if (javaPluginExtension != null) {
                javaPluginExtension.getSourceSets().getByName("main").getJava().getSrcDirs().stream().findAny().ifPresent(sourcePath -> {
                    inputInfo.javaSourcePath = sourcePath.getAbsoluteFile();
                    inputInfo.javaPackagePath = Paths.get(sourcePath.getAbsolutePath(),
                            inputInfo.group.replace('.', '/'),
                            inputInfo.projectName.replace('-', '_'))
                            .toFile();
                });
            } else {
                throw new IllegalArgumentException("Plugin 'java' extensions not found");
            }
        } else {
            throw new IllegalArgumentException("Plugin 'java' not found");
        }

        return inputInfo;
    }
}
