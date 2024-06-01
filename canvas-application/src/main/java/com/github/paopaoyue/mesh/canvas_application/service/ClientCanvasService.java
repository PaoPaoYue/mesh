package com.github.paopaoyue.mesh.canvas_application.service;

import com.github.paopaoyue.mesh.canvas_application.api.ICanvasCaller;
import com.github.paopaoyue.mesh.canvas_application.controller.CanvasController;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.core.server.RpcServer;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import jakarta.annotation.PreDestroy;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ClientCanvasService {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final AtomicBoolean isReset;
    private final Timer syncTimer;
    private final Map<String, CanvasProto.User> userMap;
    private final CanvasController canvasController;
    private final CanvasStorageService storageService;
    private List<CanvasProto.CanvasItem> itemsForReset;
    @Autowired
    private ICanvasCaller canvasCaller;
    @Autowired
    private Properties rpcProp;
    @Autowired
    private com.github.paopaoyue.mesh.canvas_application.config.Properties canvasProp;
    private CanvasProto.User currentUser;
    @Autowired
    private CanvasStorageService canvasStorageService;

    public ClientCanvasService(CanvasController canvasController, CanvasStorageService storageService) {
        this.canvasController = canvasController;
        this.storageService = storageService;
        this.userMap = new HashMap<>();
        this.syncTimer = new Timer();
        isReset = new AtomicBoolean(false);
    }

    public void login(String username) throws Exception {
        var response = canvasCaller.login(CanvasProto.LoginRequest.newBuilder()
                .setUsername(username)
                .setIsHost(isHost())
                .build(), new CallOption().setTimeout(Duration.ofSeconds(canvasProp.getLoginTimeout())));
        if (RespBaseUtil.isOK(response.getBase())) {
            this.currentUser = CanvasProto.User.newBuilder()
                    .setUserId(response.getUserId())
                    .setUsername(response.getUsername())
                    .setIsHost(isHost())
                    .build();
            userMap.put(this.currentUser.getUserId(), this.currentUser);
            startSync();
        } else {
            throw new Exception(response.getBase().getMessage());
        }
    }

    public void logout() {
        syncTimer.cancel();
        var response = canvasCaller.sync(CanvasProto.SyncRequest.newBuilder()
                .setUserId(currentUser.getUserId())
                .setTerminate(true)
                .build(), new CallOption());
        if (!RespBaseUtil.isOK(response.getBase())) {
            logger.error("Failed to sync logout status to server: {}", response.getBase().getMessage());
            return;
        }
        // wait for all client to sync the termination
        if (isHost()) {
            try {
                Thread.sleep(canvasProp.getSyncInterval() * 2L);
            } catch (InterruptedException e) {
                logger.error("Failed to wait for sync completion", e);
            }
        }
    }

    public void kickUser(String userid) {
        if (!isHost()) return;
        var response = canvasCaller.kickUser(CanvasProto.KickUserRequest.newBuilder()
                .setUserId(currentUser.getUserId())
                .setTargetUserId(userid)
                .build(), new CallOption());
        if (!RespBaseUtil.isOK(response.getBase())) {
            logger.error("Failed to kick user: {}", response.getBase().getMessage());
        }
    }

    public void sendTextMessage(String message) {
        var response = canvasCaller.sendTextMessage(CanvasProto.SendTextMessageRequest.newBuilder()
                .setUserId(currentUser.getUserId())
                .setMessage(message)
                .build(), new CallOption());
        if (!RespBaseUtil.isOK(response.getBase())) {
            logger.error("Failed to send text message: {}", response.getBase().getMessage());
        }
    }

    public void addCanvasItemToSync(CanvasProto.CanvasItem item) {
        storageService.addStageItem(item);
    }

    public void saveCanvas(String savePath) {
        if (!isHost()) return;
        canvasStorageService.flushToDisk(savePath);
    }

    public void resetCanvas() {
        try {
            resetCanvas(false, null);
        } catch (IOException e) {
            logger.error("Failed to reset canvas", e);
        }
    }

    public void resetCanvas(boolean load, String loadPath) throws IOException {
        if (!isHost()) return;
        if (isReset.get()) {
            canvasController.runLaterShowAlert(Alert.AlertType.WARNING, "the previous canvas reset is still in sync progress.", false);
            return;
        }
        if (load) {
            itemsForReset = canvasStorageService.loadFromDisk(loadPath);
        }
        isReset.set(true);
    }

    public void startSync() {
        storageService.startSyncStorage();
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<CanvasProto.CanvasItem> items = new ArrayList<>();
                    if (isReset.get()) {
                        if (itemsForReset != null) {
                            items.addAll(itemsForReset);
                            itemsForReset = null;
                        }
                    } else {
                        for (var item = storageService.pollStageItem(); item != null; item = storageService.pollStageItem()) {
                            items.add(item);
                        }
                    }
                    var request = CanvasProto.SyncRequest.newBuilder()
                            .setUserId(currentUser.getUserId())
                            .setSyncId(storageService.getSyncId())
                            .addAllItems(items)
                            .setTerminate(false)
                            .setReset(isReset.get());
                    if (canvasController.getCurrentTool() != null && !canvasController.getCurrentTool().isEmpty()) {
                        request.setTransientItem(canvasController.getCurrentTool().toProto());
                    }
                    var response = canvasCaller.sync(request.build(), new CallOption());
                    if (RespBaseUtil.isOK(response.getBase())) {
                        if (response.getTerminate()) {
                            syncTimer.cancel();
                            canvasController.runLaterShowAlert(Alert.AlertType.CONFIRMATION, "the host has terminated the canvas application.", true);
                            return;
                        }

                        userMap.clear();
                        response.getUsersList().forEach(user -> userMap.put(user.getUserId(), user));

                        if (response.getReset()) {
                            storageService.reset();
                        }

                        storageService.updateTransientItemMap(response.getTransientItemMapMap());
                        for (var item : response.getItemsList()) {
                            storageService.putPersistentItem(item);
                        }

                        canvasController.runLaterSyncData(response);

                        isReset.set(false);
                    } else {
                        switch (response.getBase().getCode()) {
                            case CanvasProto.ServiceStatusCode.USER_BLOCKED_VALUE -> {
                                logger.info("User {} is blocked", currentUser.getUsername());
                                syncTimer.cancel();
                                canvasController.runLaterShowAlert(Alert.AlertType.WARNING, "you are kicked out by the host.", true);
                            }
                            default -> {
                                logger.error("Failed to sync data: {}", response.getBase().getMessage());
                                syncTimer.cancel();
                                canvasController.runLaterShowAlert(Alert.AlertType.WARNING, "the host has aborted the canvas application.", true);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to sync data", e);
                    canvasController.runLaterShowAlert(Alert.AlertType.ERROR, "network connection aborted.", true);
                }
            }
        }, 0, canvasProp.getSyncInterval());
    }

    @PreDestroy
    public void stopSync() {
        syncTimer.cancel();
    }

    public boolean isHost() {
        return rpcProp.isServerEnabled();
    }

    public CanvasProto.User getCurrentUser() {
        return this.currentUser;
    }

    public String getUsername(String userId) {
        return this.userMap.get(userId).getUsername();
    }

}
