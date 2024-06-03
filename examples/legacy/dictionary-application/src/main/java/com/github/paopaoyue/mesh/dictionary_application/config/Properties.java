package com.github.paopaoyue.mesh.dictionary_application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesh.dictionary-application")
public class Properties {

    private int logfileRotationSize = 1000;

    private String dataFolderPath = "data";
    private String logFileBaseName = "log";
    private String diskFileBaseName = "disk";

    public int getLogfileRotationSize() {
        return logfileRotationSize;
    }

    public void setLogfileRotationSize(int logfileRotationSize) {
        this.logfileRotationSize = logfileRotationSize;
    }

    public String getDataFolderPath() {
        return dataFolderPath;
    }

    public void setDataFolderPath(String dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
    }

    public String getLogFileBaseName() {
        return logFileBaseName;
    }

    public void setLogFileBaseName(String logFileBaseName) {
        this.logFileBaseName = logFileBaseName;
    }

    public String getDiskFileBaseName() {
        return diskFileBaseName;
    }

    public void setDiskFileBaseName(String diskFileBaseName) {
        this.diskFileBaseName = diskFileBaseName;
    }

}
