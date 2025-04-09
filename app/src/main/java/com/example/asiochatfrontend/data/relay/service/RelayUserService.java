package com.example.asiochatfrontend.data.relay.service;

import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.service.UserService;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

public class RelayUserService implements UserService {

    private final UserRepository userRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;

    private final CopyOnWriteArrayList<OnlineUserListener> onlineUserListeners = new CopyOnWriteArrayList<>();
    private String currentUserId;

    @Inject
    public RelayUserService(UserRepository userRepository,
                            RelayApiClient relayApiClient,
                            RelayWebSocketClient webSocketClient,
                            Gson gson) {
        this.userRepository = userRepository;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.gson = gson;

        webSocketClient.addListener(event -> {
            if (event.getType() == WebSocketEvent.EventType.USER_PRESENCE) {
                try {
                    JsonObject payload = event.getPayload().getAsJsonObject();
                    String userId = payload.get("userId").getAsString();
                    boolean isOnline = payload.get("isOnline").getAsBoolean();

                    UserDto user = userRepository.getUserById(userId);
                    if (user != null) {
                        user.setOnline(isOnline);
                        if (!isOnline) {
                            user.setLastSeen(new Date());
                        }
                        userRepository.saveUser(user);
                    }

                    notifyOnlineStatusChanged(userId, isOnline);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;

        JsonObject payload = new JsonObject();
        payload.addProperty("userId", userId);
        payload.addProperty("isOnline", true);

        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.USER_PRESENCE,
                payload,
                "presence-" + System.currentTimeMillis(),
                userId
        );

        webSocketClient.sendEvent(event);
    }

    @Override
    public UserDto createUser(UserDto user) {
        userRepository.saveUser(user);

        UserDto created = relayApiClient.createUser(user);
        if (created != null) {
            userRepository.saveUser(created);
            return created;
        }

        return user;
    }

    @Override
    public UserDto getUserById(String userId) {
        UserDto local = userRepository.getUserById(userId);
        if (local != null) return local;

        UserDto remote = relayApiClient.getUserById(userId);
        if (remote != null) {
            userRepository.saveUser(remote);
            return remote;
        }

        return null;
    }

    @Override
    public List<UserDto> getContacts() {
        return Collections.emptyList();
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updatedUser) {
        UserDto current = userRepository.getUserById(userId);
        if (current == null) {
            return null;
        }

        current.setName(updatedUser.getName());
        current.setStatus("Status");

        userRepository.saveUser(current);

        JsonObject payload = new JsonObject();
        payload.addProperty("userId", userId);
        payload.addProperty("name", updatedUser.getName());
        payload.addProperty("status", "Status");

        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.USER_PRESENCE,
                payload,
                "update-" + System.currentTimeMillis(),
                userId
        );

        webSocketClient.sendEvent(event);

        return current;
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        return Collections.emptyList(); // Replace with flow adapter if needed
    }

    @Override
    public void refreshOnlineUsers() {

    }

    @Override
    public List<String> getOnlineUsers() {
        return relayApiClient.getOnlineUsers();
    }

    public void addOnlineUserListener(OnlineUserListener listener) {
        if (listener != null) {
            onlineUserListeners.addIfAbsent(listener);
        }
    }

    public void removeOnlineUserListener(OnlineUserListener listener) {
        onlineUserListeners.remove(listener);
    }

    private void notifyOnlineStatusChanged(String userId, boolean isOnline) {
        for (OnlineUserListener listener : onlineUserListeners) {
            try {
                listener.onStatusChanged(userId, isOnline);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnlineUserListener {
        void onStatusChanged(String userId, boolean isOnline);
    }
}