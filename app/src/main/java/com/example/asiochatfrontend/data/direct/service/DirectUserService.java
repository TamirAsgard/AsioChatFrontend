package com.example.asiochatfrontend.data.direct.service;

import android.content.Context;
import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.service.UserService;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.data.direct.network.UserDiscoveryManager;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DirectUserService implements UserService {
    private static final String TAG = "DirectUserService";

    private final UserRepository userRepository;
    private final DirectWebSocketClient directWebSocketClient;
    private ConnectionManager connectionManager;
    private UserDiscoveryManager userDiscoveryManager;
    private final Context context;
    private final List<OnlineUserListener> onlineUserListeners = new CopyOnWriteArrayList<>();

    private String currentUserId;

    @Override
    public void setCurrentUser(String userId) {

    }

    @Override
    public UserDto createUser(UserDto userDto) throws Exception {
        return null;
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updateUserDetailsDto) throws Exception {
        return null;
    }

    @Override
    public UserDto getUserById(String userId) throws Exception {
        return null;
    }

    @Override
    public List<UserDto> getContacts() {
        return Collections.emptyList();
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        return Collections.emptyList();
    }

    @Override
    public void refreshOnlineUsers() {

    }

    @Override
    public List<String> getOnlineUsers() {
        return Collections.emptyList();
    }

    public interface OnlineUserListener {
        void onUserStatusChanged(String userId, boolean isOnline);
    }

    @Inject
    public DirectUserService(
            Context context,
            UserRepository userRepository,
            DirectWebSocketClient directWebSocketClient,
            ConnectionManager connectionManager,
            UserDiscoveryManager userDiscoveryManager
    ) {
        this.context = context;
        this.userRepository = userRepository;
        this.directWebSocketClient = directWebSocketClient;
        this.connectionManager = connectionManager;
        this.userDiscoveryManager = userDiscoveryManager;

        // Set up peer status listener
        setupPeerStatusListener();
    }

    /**
     * Set the ConnectionManager (used to fix circular dependency)
     */
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Set the UserDiscoveryManager (used to fix circular dependency)
     */
    public void setUserDiscoveryManager(UserDiscoveryManager userDiscoveryManager) {
        this.userDiscoveryManager = userDiscoveryManager;
    }

    private void setupPeerStatusListener() {
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                // updateUserOnlineStatus(peerId, true);
                notifyUserStatusChanged(peerId, true);
            }

            @Override
            public void onMessageReceived(MessageDto message) {
                // Update sender's online status
                // updateUserOnlineStatus(message.getSenderId(), true);
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                // updateUserOnlineStatus(peerId, isOnline);
                notifyUserStatusChanged(peerId, isOnline);
            }
        });
    }

    public String getIpForUserId(String userId) {
        return userDiscoveryManager != null ?
                userDiscoveryManager.getIpForUserId(userId) :
                directWebSocketClient.getIpForUserId(userId);
    }

    // Listener management for online user status
    public void addOnlineUserListener(OnlineUserListener listener) {
        if (listener != null) {
            onlineUserListeners.add(listener);
        }
    }

    public void removeOnlineUserListener(OnlineUserListener listener) {
        onlineUserListeners.remove(listener);
    }

    private void notifyUserStatusChanged(String userId, boolean isOnline) {
        for (OnlineUserListener listener : onlineUserListeners) {
            try {
                listener.onUserStatusChanged(userId, isOnline);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener", e);
            }
        }
    }
}