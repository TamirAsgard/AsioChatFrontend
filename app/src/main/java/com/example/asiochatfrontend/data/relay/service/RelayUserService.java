package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.AuthRequestCredentialsDto;
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
            if (event.getType() == WebSocketEvent.EventType.CONNECTION) {
                try {
                    JsonObject payload = event.getPayload().getAsJsonObject();
                    System.out.println("Connected to server: " + payload);
                    String userId = payload.get("jid").getAsString();

                    UserDto user = userRepository.getUserById(userId);
                    if (user != null) {
                        userRepository.saveUser(user);
                    }

                    notifyOnlineStatusChanged(userId, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;

        if (ServiceModule.getConnectionManager().isOnline()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("jid", userId);
            payload.addProperty("isAlive", true);

            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.CONNECTION,
                    payload,
                    userId
            );

            webSocketClient.sendEvent(event);
        }
    }

    @Override
    public UserDto createUser(UserDto user) {
        if (ServiceModule.getConnectionManager().isOnline()) {
            Object created = relayApiClient.createUser(user);
            if (created != null) {
                userRepository.saveUser(user);
                return user;
            }
        }

        return null;
    }

    @Override
    public UserDto getUserById(String userId) {
        UserDto local = userRepository.getUserById(userId);
        if (local != null) return local;

        if (ServiceModule.getConnectionManager().isOnline()) {
            UserDto remote = relayApiClient.getUserById(userId);
            if (remote != null) {
                userRepository.saveUser(remote);
                return remote;
            }
        }

        return null;
    }

    @Override
    public List<UserDto> getContacts() {
        List<UserDto> localContacts = userRepository.getAllUsers();

        if (!ServiceModule.getConnectionManager().isOnline()) {
            Log.i("RelayUserService", "Offline, fetch contacts from local storage");
            return localContacts;
        }

        List<UserDto> remoteContacts = relayApiClient.getContacts();
        for (UserDto remoteContact : remoteContacts) {
            if (!localContacts.contains(remoteContact)) {
                userRepository.saveUser(remoteContact);
            }
        }

        return remoteContacts;
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updatedUser) {
        // TODO Implement update user logic
        return null;
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

    private void notifyOnlineStatusChanged(String jid, boolean isAlive) {
        for (OnlineUserListener listener : onlineUserListeners) {
            try {
                listener.onStatusChanged(jid, isAlive);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnlineUserListener {
        void onStatusChanged(String userId, boolean isOnline);
    }
}