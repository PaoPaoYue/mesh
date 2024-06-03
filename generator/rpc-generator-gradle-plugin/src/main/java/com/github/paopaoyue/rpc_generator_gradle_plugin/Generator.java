package com.github.paopaoyue.rpc_generator_gradle_plugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Generator {

    public void generateIdl(InputInfo inputInfo) {
        System.out.println("Generating idl files...");
        try {
            generateIdlFiles(inputInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Idl files generated successfully.");
    }

    public void generateRpc(InputInfo inputInfo, ParseInfo parseInfo){
        System.out.println("Generating proto files...");
        try {
            generateRpcProto(inputInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Proto files generated successfully.");
        System.out.println("Generating rpc stub code...");
        try {
            generateRpcStub(inputInfo, parseInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Service files generated successfully.");
    }

    private void generateIdlFiles(InputInfo inputInfo) throws IOException {
        if (!inputInfo.protoRepoPath.exists()) {
            System.out.println("IDL repo not exists, creating repo at path: " + inputInfo.protoRepoPath);
            inputInfo.protoRepoPath.mkdirs();
        }

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> info = new HashMap<>();
        root.put("info", info);
        info.put("rootPackage", inputInfo.group + "." + inputInfo.projectName.replace('-', '_'));
        info.put("serviceClass", convertSnakeToCamel(inputInfo.serviceShortAlias));

        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        if (!inputInfo.protoServicePath.exists()) {
            inputInfo.protoServicePath.mkdirs();
        }
        generateFile(cfg.getTemplate("base.proto.ftl"), inputInfo.protoRepoPath.toPath().resolve(
                "base.proto").toFile(), root, false);
        generateFile(cfg.getTemplate("service.proto.ftl"),inputInfo.protoServicePath.toPath().resolve(
                inputInfo.serviceShortAlias + ".proto").toFile(), root, false);
    }

    private void generateRpcProto(InputInfo inputInfo) throws IOException {
        File outputDir = inputInfo.javaPackagePath.toPath().resolve("proto").toFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

       Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "protoc -I=" + inputInfo.protoRepoPath.getAbsolutePath() +
               " --java_out=" + inputInfo.javaSourcePath +" " + String.join(File.separator,inputInfo.protoServicePath.getAbsolutePath(), "*.proto")});
       BufferedReader bre = new BufferedReader(new InputStreamReader(process.getErrorStream()));
       BufferedReader bri = new BufferedReader(new InputStreamReader(process.getInputStream()));
       String line = null;
       boolean success = true;
       while ((line = bre.readLine()) != null) {
           System.out.println(line);
           success = false;
       }
       while ((line = bri.readLine()) != null) {
           System.out.println(line);
       }
       if (!success) {
           throw new RuntimeException("Failed to generate proto files.");
       }

    }

    private void generateRpcStub(InputInfo inputInfo, ParseInfo parseInfo) throws IOException {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> info = new HashMap<>();
        root.put("info", info);
        info.put("rootPackage", inputInfo.group + "." + inputInfo.projectName.replace('-', '_'));
        info.put("service", inputInfo.serviceName);
        info.put("serviceClass", convertSnakeToCamel(inputInfo.serviceShortAlias));
        info.put("methodMap", parseInfo.methodMap);
        info.put("protoObject", parseInfo.methodMap.values().stream().findAny().orElseThrow().objectName);

        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        if (!inputInfo.javaPackagePath.exists()) {
            inputInfo.javaPackagePath.mkdirs();
        }

        generateFile(cfg.getTemplate("ServerStub.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "stub/" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "ServerStub.java").toFile(), root, true);
        generateFile(cfg.getTemplate("ClientStub.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "stub/" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "ClientStub.java").toFile(), root, true);
        generateFile(cfg.getTemplate("IService.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "service/I" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "Service.java").toFile(), root, true);
        generateFile(cfg.getTemplate("Service.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "service/" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "Service.java").toFile(), root, inputInfo.overrideService);
        generateFile(cfg.getTemplate("ICaller.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "api/I" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "Caller.java").toFile(), root, true);
        generateFile(cfg.getTemplate("Caller.ftl"),inputInfo.javaPackagePath.toPath().resolve(
                "api/" + convertSnakeToCamel(inputInfo.serviceShortAlias) + "Caller.java").toFile(), root, inputInfo.overrideCaller);

    }

    private void generateFile(Template template, File file, Object root, boolean override) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists() && !override) {
            System.out.println("File exists: " + file + ", skip writing");
            return;
        }
        Writer out = new OutputStreamWriter(new FileOutputStream(file));
        try {
            template.process(root, out);

        } catch (TemplateException e) {
            System.out.println("Error writing to file: " + file + " while processing FreeMarker template");
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            out.flush();
            out.close();
        }
        System.out.println("File written: " + file);
    }

    private String convertSnakeToCamel(String snakeCase) {
        StringBuilder camelCaseBuilder = new StringBuilder();
        String[] words = snakeCase.split("-");

        for (String word : words) {
            camelCaseBuilder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return camelCaseBuilder.toString();
    }
}
