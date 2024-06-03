package com.github.paopaoyue.mesh.canvas_application.service;

import com.github.paopaoyue.mesh.canvas_application.config.Properties;
import com.github.paopaoyue.mesh.canvas_application.controller.CanvasController;
import com.github.paopaoyue.mesh.canvas_application.proto.CanvasProto;
import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RpcService(serviceName = "canvas-application")
public class CanvasService implements ICanvasService {

    @Autowired
    private Properties prop;
    private String hostId;
    private Map<String, CanvasProto.User> userMap;
    private Map<String, Long> userTimeoutMap;
    private List<CanvasProto.TextMessage> messageList;
    private Set<String> blacklist;
    private Set<String> resetNotifySet;
    private boolean isTerminating;
    private CanvasController canvasController;
    private Timer checkActiveTimer;
    private Lock lock;

    private Map<String, CanvasProto.CanvasItem> transientItemMap;
    private List<CanvasProto.CanvasItem> persistentItemList;

    public CanvasService(CanvasController canvasController) {
        this.canvasController = canvasController;
        this.userMap = new ConcurrentHashMap<>();
        this.userTimeoutMap = new ConcurrentHashMap<>();
        this.messageList = new CopyOnWriteArrayList<>();
        this.blacklist = new ConcurrentSkipListSet<>();
        this.resetNotifySet = new HashSet<>();
        this.checkActiveTimer = new Timer();
        this.lock = new ReentrantLock();

        this.transientItemMap = new ConcurrentHashMap<>();
        this.persistentItemList = new CopyOnWriteArrayList<>();
    }

    @PostConstruct
    public void startCheckActive() {
        checkActiveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<String> userToRemove = new ArrayList<>();
                userMap.forEach((userId, user) -> {
                    if (user.getIsHost()) {
                        return;
                    }
                    long currentMilli = System.currentTimeMillis();
                    if (currentMilli > userTimeoutMap.getOrDefault(userId, currentMilli)) {
                        userToRemove.add(userId);
                    }
                });
                userToRemove.forEach(userId -> {
                    userMap.remove(userId);
                    transientItemMap.remove(userId);
                    userTimeoutMap.remove(userId);
                });
            }
        }, 0, prop.getSyncInterval() / 2);
    }


    @Override
    public CanvasProto.KickUserResponse kickUser(CanvasProto.KickUserRequest request) {
        if (!userMap.containsKey(request.getUserId())) {
            return CanvasProto.KickUserResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NOT_FOUND_VALUE, "user not found"))
                    .build();
        }
        blacklist.add(request.getTargetUserId());
        userMap.remove(request.getTargetUserId());
        transientItemMap.remove(request.getTargetUserId());
        return CanvasProto.KickUserResponse.newBuilder().setUserId(request.getUserId()).build();
    }

    @Override
    public CanvasProto.SendTextMessageResponse sendTextMessage(CanvasProto.SendTextMessageRequest request) {
        if (blacklist.contains(request.getUserId())) {
            return CanvasProto.SendTextMessageResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_BLOCKED_VALUE, "this user is blocked"))
                    .build();
        }
        if (!userMap.containsKey(request.getUserId())) {
            return CanvasProto.SendTextMessageResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NOT_FOUND_VALUE, "user not found"))
                    .build();
        }
        var message = CanvasProto.TextMessage.newBuilder()
                .setUserId(request.getUserId())
                .setMessage(request.getMessage())
                .build();
        this.messageList.add(message);
        return CanvasProto.SendTextMessageResponse.newBuilder()
                .setUserId(request.getUserId())
                .build();
    }

    @Override
    public CanvasProto.LoginResponse login(CanvasProto.LoginRequest request) {
        if (userMap.values().stream().anyMatch(user -> user.getUsername().equals(request.getUsername()))) {
            return CanvasProto.LoginResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NAME_DUPLICATE_VALUE, "this user already exists"))
                    .build();
        }
        if (request.getIsHost() && userMap.values().stream().anyMatch(CanvasProto.User::getIsHost)) {
            return CanvasProto.LoginResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NO_PERMISSION_VALUE, "host already exists"))
                    .build();
        }
        String userId = UUID.randomUUID().toString();
        var user = CanvasProto.User.newBuilder()
                .setUserId(userId)
                .setUsername(request.getUsername())
                .setIsHost(request.getIsHost())
                .build();
        if (request.getIsHost()) {
            this.hostId = userId;
        } else {
            boolean confirm = !prop.isNeedConfirmToJoin() || canvasController.runLaterJoinConfirm(user);
            if (!confirm) {
                return CanvasProto.LoginResponse.newBuilder()
                        .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NO_PERMISSION_VALUE, "host rejected"))
                        .build();
            }
        }
        this.userMap.put(userId, user);
        return CanvasProto.LoginResponse.newBuilder()
                .setUserId(userId)
                .setUsername(request.getUsername())
                .build();
    }

    @Override
    public CanvasProto.SyncResponse sync(CanvasProto.SyncRequest request) {
        if (blacklist.contains(request.getUserId())) {
            return CanvasProto.SyncResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_BLOCKED_VALUE, "this user is blocked"))
                    .build();
        }
        if (!userMap.containsKey(request.getUserId())) {
            return CanvasProto.SyncResponse.newBuilder()
                    .setBase(RespBaseUtil.ErrorRespBase(CanvasProto.ServiceStatusCode.USER_NOT_FOUND_VALUE, "user not found"))
                    .build();
        }

        // update server data
        long currentMilli = System.currentTimeMillis();
        userTimeoutMap.put(request.getUserId(), currentMilli + prop.getSyncInterval() * 3L / 2);

        if (request.getTerminate() && request.getUserId().equals(hostId)) {
            this.isTerminating = true;
        }

        // synchronize the process when updating server canvas data
        boolean isNotifyReset = false;
        try {
            lock.lock();

            if (request.getReset() && request.getUserId().equals(hostId)) {
                resetNotifySet.addAll(userMap.keySet());
                transientItemMap.clear();
                persistentItemList.clear();
            }

            if (resetNotifySet.contains(request.getUserId())) {
                resetNotifySet.remove(request.getUserId());
                isNotifyReset = true;
            }
            if (request.hasTransientItem()) {
                transientItemMap.put(request.getUserId(), request.getTransientItem());
            } else {
                transientItemMap.remove(request.getUserId());
            }
            persistentItemList.addAll(request.getItemsList());


        } finally {
            lock.unlock();
        }

        // handle return data
        // return all items has not been sync to the client
        var syncId = isNotifyReset ? 0 : request.getSyncId();
        List<CanvasProto.CanvasItem> items = new ArrayList<>();
        for (long i = syncId; i < persistentItemList.size(); i++) {
            items.add(persistentItemList.get((int) i));
        }
        return CanvasProto.SyncResponse.newBuilder()
                .setUserId(request.getUserId())
                .setSyncId(persistentItemList.size())
                .addAllUsers(userMap.values())
                .addAllTextMessages(messageList)
                .putAllTransientItemMap(transientItemMap)
                .addAllItems(items)
                .setTerminate(isTerminating)
                .setReset(isNotifyReset)
                .build();
    }


}
