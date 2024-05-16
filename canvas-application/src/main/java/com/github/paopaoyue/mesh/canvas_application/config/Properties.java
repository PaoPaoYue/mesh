package com.github.paopaoyue.mesh.canvas_application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesh.canvas-application")
public class Properties {
    private int loginTimeout = 10;
    private int syncInterval = 200;
    private int syncStorageInterval = 1000;
    private int maxMessageLength = 100;
    private int maxUsernameLength = 30;
    private int maxCanvasTextLength = 100;
    private boolean needConfirmToJoin = false;

    private String autoSaveFolder = "canvas-auto-save";
    private String defaultSaveFolder = "canvas-save";
    private String defaultSaveFileName = "canvas-save.bak";

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public int getMaxUsernameLength() {
        return maxUsernameLength;
    }

    public void setMaxUsernameLength(int maxUsernameLength) {
        this.maxUsernameLength = maxUsernameLength;
    }

    public int getMaxCanvasTextLength() {
        return maxCanvasTextLength;
    }

    public void setMaxCanvasTextLength(int maxCanvasTextLength) {
        this.maxCanvasTextLength = maxCanvasTextLength;
    }

    public int getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(int syncInterval) {
        this.syncInterval = syncInterval;
    }

    public int getSyncStorageInterval() {
        return syncStorageInterval;
    }

    public void setSyncStorageInterval(int syncStorageInterval) {
        this.syncStorageInterval = syncStorageInterval;
    }

    public boolean isNeedConfirmToJoin() {
        return needConfirmToJoin;
    }

    public void setNeedConfirmToJoin(boolean needConfirmToJoin) {
        this.needConfirmToJoin = needConfirmToJoin;
    }

    public String getAutoSaveFolder() {
        return autoSaveFolder;
    }

    public void setAutoSaveFolder(String autoSaveFolder) {
        this.autoSaveFolder = autoSaveFolder;
    }

    public String getDefaultSaveFolder() {
        return defaultSaveFolder;
    }

    public void setDefaultSaveFolder(String defaultSaveFolder) {
        this.defaultSaveFolder = defaultSaveFolder;
    }

    public String getAutoSavePath() {
        return autoSaveFolder + "/" + defaultSaveFileName;
    }

    public String getDefaultSavePath() {
        return defaultSaveFolder + "/" + defaultSaveFileName;
    }

    public String getDefaultSaveFileName() {
        return defaultSaveFileName;
    }

    public void setDefaultSaveFileName(String defaultSaveFileName) {
        this.defaultSaveFileName = defaultSaveFileName;
    }

    @Override
    public String toString() {
        return "Properties{" +
                "loginTimeout=" + loginTimeout +
                ", syncInterval=" + syncInterval +
                ", syncStorageInterval=" + syncStorageInterval +
                ", maxMessageLength=" + maxMessageLength +
                ", maxUsernameLength=" + maxUsernameLength +
                ", maxCanvasTextLength=" + maxCanvasTextLength +
                ", needConfirmToJoin=" + needConfirmToJoin +
                ", autoSaveFolder='" + autoSaveFolder + '\'' +
                ", defaultSaveFolder='" + defaultSaveFolder + '\'' +
                ", defaultSaveFileName='" + defaultSaveFileName +
                '}';
    }
}