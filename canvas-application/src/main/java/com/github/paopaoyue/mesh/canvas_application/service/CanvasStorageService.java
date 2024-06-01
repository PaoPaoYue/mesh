package com.github.paopaoyue.mesh.canvas_application.service;

import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CanvasStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CanvasStorageService.class);
    private final Timer syncTimer;
    private final Map<String, CanvasProto.CanvasItem> transientItemMap;
    private final Queue<CanvasProto.CanvasItem> stageItemQueue;
    private final List<CanvasProto.CanvasItem> persistentItemList;
    @Autowired
    private Properties rpcProp;
    @Autowired
    private com.github.paopaoyue.mesh.canvas_application.config.Properties canvasProp;

    public CanvasStorageService() {
        this.transientItemMap = new ConcurrentHashMap<>();
        this.stageItemQueue = new ConcurrentLinkedDeque<>();
        this.persistentItemList = new CopyOnWriteArrayList<>();
        this.syncTimer = new Timer();
    }

    public void startSyncStorage() {
        if (!rpcProp.isServerEnabled() || !canvasProp.isAutoSave()) return;
        // Sync storage to disk periodically
        // initial delay to avoid overwriting the auto-save file
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                flushToDisk(canvasProp.getAutoSavePath());
            }
        }, canvasProp.getSyncStorageInterval(), canvasProp.getSyncStorageInterval());
    }

    @PreDestroy
    public void stopSyncStorage() {
        syncTimer.cancel();
    }

    public long getSyncId() {
        return persistentItemList.size();
    }

    public void updateTransientItemMap(Map<String, CanvasProto.CanvasItem> itemMap) {
        transientItemMap.clear();
        transientItemMap.putAll(itemMap);
    }

    public Map<String, CanvasProto.CanvasItem> getTransientItemMap() {
        return transientItemMap;
    }

    public void addStageItem(CanvasProto.CanvasItem item) {
        stageItemQueue.add(item);
    }

    public CanvasProto.CanvasItem pollStageItem() {

        return stageItemQueue.poll();
    }

    public void putPersistentItem(CanvasProto.CanvasItem item) {
        persistentItemList.add(item);
    }

    public List<CanvasProto.CanvasItem> getPersistentItemList() {
        return persistentItemList;
    }

    public synchronized void reset() {
        transientItemMap.clear();
        stageItemQueue.clear();
        persistentItemList.clear();

        if (canvasProp.isAutoSave()) {
            File file = new File(canvasProp.getAutoSavePath());
            if (file.exists()) {
                if (!file.delete()) {
                    logger.error("Failed to delete file: {}", canvasProp.getAutoSavePath());
                }
            }
        }
    }

    // Flush all canvas items if no specific syncId is provided
    public synchronized void flushToDisk(String path) {
        flushToDisk(path, Long.MIN_VALUE);
    }

    // Flush all canvas items with syncId greater than the provided syncId
    public synchronized void flushToDisk(String path, long syncId) {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                logger.error("Failed to create directory: {}", parentDir);
                return;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             DataOutputStream dos = new DataOutputStream(fos)) {
            for (int i = (int) syncId; i < persistentItemList.size(); i++) {
                byte[] bytes = persistentItemList.get(i).toByteArray();
                dos.writeInt(bytes.length);
                dos.write(bytes);
            }
        } catch (IOException e) {
            logger.error("Failed to write to file: {}", path, e);
        }

    }

    public List<CanvasProto.CanvasItem> loadFromDisk(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            logger.warn("File not found: {}", path);
            return null;
        }
        List<CanvasProto.CanvasItem> itemsForReset = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {
            while (dis.available() > 0) {
                int length = dis.readInt();
                byte[] bytes = new byte[length];
                dis.readFully(bytes);
                CanvasProto.CanvasItem item = CanvasProto.CanvasItem.parseFrom(bytes);
                itemsForReset.add(item);
            }
        } catch (IOException e) {
            logger.error("Failed to read from file: {}", path, e);
        }
        return itemsForReset;
    }
}
