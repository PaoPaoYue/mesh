package com.github.paopaoyue.mesh.rpc_generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Application {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        InputInfo inputInfo = new InputInfo();

//        inputInfo.group = "com.github.paopaoyue";
//        inputInfo.project = "mesh";
//        inputInfo.module = "tran-application";
//        inputInfo.service = "tran-application";
//        inputInfo.serviceAlias = "tran";
//        inputInfo.protoFiles = "idl/tran-application/*.proto";
//        inputInfo.protoIncludePath = "idl";

        inputInfo.group = getInputRequired(scanner, "Enter the group name: ");
        inputInfo.project = getInputRequired(scanner, "Enter the project name: ");
        inputInfo.module = getInput(scanner, "Enter the submodule name (optional): ", false, "");
        inputInfo.service = getInputRequired(scanner, "Enter the service name: ");
        inputInfo.serviceAlias = getInput(scanner, "Enter the service shot alias (optional): ", false, inputInfo.service);
        inputInfo.protoFiles = getInputRequired(scanner, "Enter the proto files: ");
        inputInfo.protoIncludePath = getInputRequired(scanner, "Enter the proto include path: ");
        inputInfo.outputDir = inputInfo.module.isEmpty() ? "src/main/java" : inputInfo.module + "/src/main/java";

        System.out.println("The information you entered is:");
        System.out.println("Group: " + inputInfo.group);
        System.out.println("Project: " + inputInfo.project);
        if (!inputInfo.module.isEmpty()) {
            System.out.println("Module: " + inputInfo.module);
        }
        System.out.println("Service: " + inputInfo.service);
        if (!inputInfo.serviceAlias.isEmpty()) {
            System.out.println("Service alias: " + inputInfo.serviceAlias);
        }
        System.out.println("Proto files: " + inputInfo.protoFiles);
        System.out.println("Proto include path: " + inputInfo.protoIncludePath);

        System.out.println("Do you want to continue? (yes/no)");
        String confirm = scanner.nextLine();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("Cancelled.");
            return;
        }


        System.out.println("Generating proto files...");
        File outputDir = new File(inputInfo.outputDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "protoc -I=" + inputInfo.protoIncludePath + " --java_out=" + inputInfo.outputDir + " " + inputInfo.protoFiles});
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
                System.out.println("Failed to generate proto files.");
                return;
            }
        } catch (IOException e) {
            System.out.println("Failed to generate proto files.");
            e.printStackTrace();
            return;
        }
        System.out.println("Proto files generated successfully.");

        System.out.println("Generating service files...");
        try {
            var parseInfo = new Parser(inputInfo.protoFiles).parse();
            new Generator(inputInfo, parseInfo).generate();
        } catch (IOException e) {
            System.out.println("Failed to generate service files.");
            e.printStackTrace();
            return;
        }
        System.out.println("Service files generated successfully.");


    }

    private static String getInput(Scanner scanner, String prompt, boolean required, String defaultValue) {
        String input = "";
        while (input.isEmpty()) {
            System.out.print(prompt);
            input = scanner.nextLine();
            if (required && input.isEmpty()) {
                System.out.println("This field is required.");
            } else {
                break;
            }
        }
        return input.isEmpty() ? defaultValue : input;
    }

    private static String getInputRequired(Scanner scanner, String prompt) {
        return getInput(scanner, prompt, true, "");
    }
}
