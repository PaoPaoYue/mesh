package io.github.paopaoyue.rpc_generator_gradle_plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static final Pattern PACKAGE_REGEX = Pattern.compile("option\\s+java_package\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern OBJECT_REGEX = Pattern.compile("option\\s+java_outer_classname\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern STRUCT_REGEX = Pattern.compile("message\\s+([^\\s]+)\\s*");
    private static final Pattern METHOD_REGEX = Pattern.compile("rpc\\s+([^\\s]+)\\s*\\(([^\\s]+)\\)\\s*returns\\s*\\(([^\\s]+)\\)");

    public ParseInfo parse(File protoServicePath) {
        ParseInfo info = new ParseInfo();
        String protoFilesPath = String.join(File.separator,protoServicePath.getAbsolutePath(), "*.proto");
        int index = protoFilesPath.lastIndexOf(File.separator);
        Path directory = Paths.get(protoFilesPath.substring(0, index));
        String filePattern = protoFilesPath.substring(index + 1);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, filePattern)) {
            for (Path entry : stream) {
                BufferedReader br = new BufferedReader(new FileReader(entry.toFile()));
                parseStruct(br, info);
                br.close();
                br = new BufferedReader(new FileReader(entry.toFile()));
                parseMethod(br, info);
                br.close();
            }
        } catch (IOException | DirectoryIteratorException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    private void parseStruct(BufferedReader br, ParseInfo info) throws IOException {
        String packageName = null;
        String objectName = null;

        String line;
        while ((line = br.readLine()) != null) {
            if (packageName == null) {
                Matcher matcher = PACKAGE_REGEX.matcher(line);
                if (matcher.find()) {
                    packageName = matcher.group(1);
                }
            }
            if (objectName == null) {
                Matcher matcher = OBJECT_REGEX.matcher(line);
                if (matcher.find()) {
                    objectName = matcher.group(1);
                }
            }
            Matcher matcher = STRUCT_REGEX.matcher(line);
            if (matcher.find()) {
                ParseInfo.ProtoStruct struct = new ParseInfo.ProtoStruct(packageName, objectName, matcher.group(1));
                info.structMap.put(struct.structName, struct);
            }
        }
    }

    private void parseMethod(BufferedReader br, ParseInfo info) throws IOException {
        String packageName = null;
        String objectName = null;

        String line;
        while ((line = br.readLine()) != null) {
            if (packageName == null) {
                Matcher matcher = PACKAGE_REGEX.matcher(line);
                if (matcher.find()) {
                    packageName = matcher.group(1);
                }
            }
            if (objectName == null) {
                Matcher matcher = OBJECT_REGEX.matcher(line);
                if (matcher.find()) {
                    objectName = matcher.group(1);
                }
            }
            Matcher matcher = METHOD_REGEX.matcher(line);
            if (matcher.find()) {
                ParseInfo.ProtoStruct input = info.structMap.get(matcher.group(2));
                ParseInfo.ProtoStruct output = info.structMap.get(matcher.group(3));
                ParseInfo.ProtoMethod method = new ParseInfo.ProtoMethod(packageName, objectName, lowerFirst(matcher.group(1)), input, output);
                info.methodMap.put(method.methodName, method);
            }

        }
    }

    private String lowerFirst(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

}
