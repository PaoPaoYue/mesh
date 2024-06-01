package com.github.paopaoyue.mesh.rpc_generator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Generator {
    public InputInfo inputInfo;
    public ParseInfo parseInfo;

    public Generator(InputInfo inputInfo, ParseInfo parseInfo) {
        this.inputInfo = inputInfo;
        this.parseInfo = parseInfo;
    }

    public void generate() throws IOException {
        var root = prepareRoot();

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(this.getClass().getClassLoader().getResource("templates").getFile()));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        generateFile(cfg.getTemplate("build.gradle.ftl"), Path.of(getRootDirectory() + "build.gradle"), root, false);
        if (inputInfo.module.isEmpty())
            generateFile(cfg.getTemplate("settings.gradle.ftl"), Path.of(getRootDirectory() + "settings.gradle"), root, false);
        generateFile(cfg.getTemplate("Application.ftl"), Path.of(getRootCodeDirectory() + convertSnakeToCamel(inputInfo.serviceAlias) + "Application.java"), root, false);
        generateFile(cfg.getTemplate("ApplicationTests.ftl"), Path.of(getRootTestDirectory() + convertSnakeToCamel(inputInfo.serviceAlias) + "ApplicationTests.java"), root, false);
        generateFile(cfg.getTemplate("application.properties.ftl"), Path.of(getResourceDirectory() + "application.properties"), root, false);
        generateFile(cfg.getTemplate("AutoConfiguration.ftl"), Path.of(getRootCodeDirectory() + "config/" + "AutoConfiguration.java"), root, false);
        generateFile(cfg.getTemplate("Properties.ftl"), Path.of(getRootCodeDirectory() + "config/" + "Properties.java"), root, false);
        generateFile(cfg.getTemplate("Service.ftl"), Path.of(getRootCodeDirectory() + "service/" + convertSnakeToCamel(inputInfo.serviceAlias) + "Service.java"), root, false);
        generateFile(cfg.getTemplate("IService.ftl"), Path.of(getRootCodeDirectory() + "service/I" + convertSnakeToCamel(inputInfo.serviceAlias) + "Service.java"), root, true);
        generateFile(cfg.getTemplate("Caller.ftl"), Path.of(getRootCodeDirectory() + "api/" + convertSnakeToCamel(inputInfo.serviceAlias) + "Caller.java"), root, true);
        generateFile(cfg.getTemplate("ICaller.ftl"), Path.of(getRootCodeDirectory() + "api/I" + convertSnakeToCamel(inputInfo.serviceAlias) + "Caller.java"), root, true);
        generateFile(cfg.getTemplate("ServerStub.ftl"), Path.of(getRootCodeDirectory() + "stub/" + convertSnakeToCamel(inputInfo.serviceAlias) + "ServerStub.java"), root, true);
        generateFile(cfg.getTemplate("ClientStub.ftl"), Path.of(getRootCodeDirectory() + "stub/" + convertSnakeToCamel(inputInfo.serviceAlias) + "ClientStub.java"), root, true);

        generateImportFile(Path.of(getResourceDirectory() + "/MEAT_INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
    }

    private Map<String, Object> prepareRoot() {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> info = new HashMap<>();
        root.put("info", info);
        info.put("rootPackage", inputInfo.module.isEmpty() ? inputInfo.group + "." + convertModuleName(inputInfo.project) : inputInfo.group + "." + convertModuleName(inputInfo.project) + "." + convertModuleName(inputInfo.module));
        info.put("service", inputInfo.service);
        info.put("serviceClass", convertSnakeToCamel(inputInfo.serviceAlias));
        info.put("configPrefix", inputInfo.module.isEmpty() ? inputInfo.project : inputInfo.project + "." + inputInfo.module);
        info.put("methodMap", parseInfo.methodMap);
        info.put("protoObject", parseInfo.methodMap.values().stream().findAny().orElseThrow().objectName);
        return root;
    }

    private void injectModule(Path projectSettingPath) throws IOException {
        if (inputInfo.module.isEmpty()) {
            return;
        }
        if (!projectSettingPath.getParent().toFile().exists()) {
            projectSettingPath.getParent().toFile().mkdirs();
        }
        Writer out;
        if (projectSettingPath.toFile().exists()) {
            out = new OutputStreamWriter(new FileOutputStream(projectSettingPath.toFile(), true));
        } else {
            out = new OutputStreamWriter(new FileOutputStream(projectSettingPath.toFile()));
        }
        out.write("include '" + inputInfo.module + "'\n");
        out.flush();
        out.close();
    }

    private void generateImportFile(Path path) throws IOException {
        if (!path.getParent().toFile().exists()) {
            path.getParent().toFile().mkdirs();
        }
        if (path.toFile().exists()) {
            System.out.println("File exists: " + path + ", skip writing");
            return;
        }
        Writer out = new OutputStreamWriter(new FileOutputStream(path.toFile()));
        out.write(inputInfo.module.isEmpty() ? inputInfo.group + "." + convertModuleName(inputInfo.project) : inputInfo.group + "." + convertModuleName(inputInfo.project) + "." + convertModuleName(inputInfo.module) + ".config.AutoConfiguration\n");
        out.flush();
        out.close();
        System.out.println("File written: " + path);
    }

    private void generateFile(Template template, Path path, Object root, boolean override) throws IOException {
        if (!path.getParent().toFile().exists()) {
            path.getParent().toFile().mkdirs();
        }
        if (path.toFile().exists() && !override) {
            System.out.println("File exists: " + path + ", skip writing");
            return;
        }
        Writer out = new OutputStreamWriter(new FileOutputStream(path.toFile()));
        try {
            template.process(root, out);

        } catch (TemplateException e) {
            System.out.println("Error writing to file: " + path + " while processing FreeMarker template");
            e.printStackTrace();
        } finally {
            out.flush();
            out.close();
        }
        System.out.println("File written: " + path);
    }

    private String convertPackageToPath(String packageName) {
        return packageName.replace('.', '/');
    }

    private String convertModuleName(String moduleName) {
        return moduleName.replace('-', '_');
    }

    private String getRootDirectory() {
        return inputInfo.module.isEmpty() ? "./" : "./" + inputInfo.module + "/";
    }

    private String getRootCodeDirectory() {
        return inputInfo.module.isEmpty() ?
                "./src/main/java/" + convertPackageToPath(inputInfo.group) + "/" + convertModuleName(inputInfo.project) + "/" :
                "./" + inputInfo.module + "/src/main/java/" + convertPackageToPath(inputInfo.group) + "/" + convertModuleName(inputInfo.project) + "/" + convertModuleName(inputInfo.module) + "/";
    }

    private String getResourceDirectory() {
        return inputInfo.module.isEmpty() ?
                "./src/main/resources/" :
                "./" + inputInfo.module + "/src/main/resources/";
    }

    private String getRootTestDirectory() {
        return inputInfo.module.isEmpty() ?
                "./src/test/java/" + convertPackageToPath(inputInfo.group) + "/" + convertModuleName(inputInfo.project) + "/" :
                "./" + inputInfo.module + "/src/test/java/" + convertPackageToPath(inputInfo.group) + "/" + convertModuleName(inputInfo.project) + "/" + convertModuleName(inputInfo.module) + "/";
    }

    private String convertSnakeToCamel(String snakeCase) {
        StringBuilder camelCaseBuilder = new StringBuilder();
        String[] words = snakeCase.split("_");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            camelCaseBuilder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return camelCaseBuilder.toString();
    }
}
