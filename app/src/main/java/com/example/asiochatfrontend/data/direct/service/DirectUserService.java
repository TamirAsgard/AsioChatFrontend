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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

public class DirectUserService implements UserService {
    private static final String TAG = "DirectUserService";

    private final UserRepository userRepository;
    private final DirectWebSocketClient directWebSocketClient;
    private final ConnectionManager connectionManager;
    private final UserDiscoveryManager userDiscoveryManager;
    private final Context context;
    private final List<OnlineUserListener> onlineUserListeners = new CopyOnWriteArrayList<>();

    private String currentUserId;

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

        // Setup peer connection listener
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                updateUserOnlineStatus(peerId, true);
                notifyUserStatusChanged(peerId, true);
            }

            @Override
            public void onMessageReceived(MessageDto message) {
                // Optional: handle any user-related message processing
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                updateUserOnlineStatus(peerId, isOnline);
                notifyUserStatusChanged(peerId, isOnline);
            }
        });
    }

    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;

        // Notify peers about current user's online status
        try {
            UserDto currentUser = getUserById(userId);
            if (currentUser != null) {
                currentUser.setOnline(true);
                userRepository.saveUser(currentUser);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting current user", e);
        }
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        // Ensure user is marked as online when created
        userDto.setOnline(true);
        return userRepository.saveUser(userDto);
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updateUserDetailsDto) {
        UserDto current = userRepository.getUserById(userId);
        if (current == null) {
            throw new IllegalArgumentException("User not found");
        }

        String updatedName = updateUserDetailsDto.getName() != null ?
                updateUserDetailsDto.getName() : current.getName();
        String updatedPicture = updateUserDetailsDto.getProfilePicture() != null ?
                updateUserDetailsDto.getProfilePicture() : current.getProfilePicture();
        String updatedStatus = "";

        UserDto updatedUser = new UserDto(
                current.getId(),
                updatedName,
                updatedPicture,
                updatedStatus,
                current.isOnline(),
                current.getLastSeen(),
                current.getCreatedAt(),
                new java.util.Date()
        );

        userRepository.saveUser(updatedUser);
        return updatedUser;
    }

    @Override
    public UserDto getUserById(String userId) {
        return userRepository.getUserById(userId);
    }

    @Override
    public List<UserDto> getContacts() {
        return userRepository.getAllUsers();
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        List<String> onlineIds = getOnlineUsers();
        if (onlineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository.getUsersByIds(onlineIds);
    }

    @Override
    public void refreshOnlineUsers() {
        // Trigger peer discovery to refresh online users
        directWebSocketClient.startDiscovery();
    }

    // Utility methods for managing user online status
    private void updateUserOnlineStatus(String userId, boolean isOnline) {
        UserDto user = userRepository.getUserById(userId);
        if (user != null) {
            user.setOnline(isOnline);
            if (!isOnline) {
                user.setLastSeen(new java.util.Date());
            }
            userRepository.saveUser(user);
        }
    }

    public String getIpForUserId(String userId) {
        return userDiscoveryManager.getIpForUserId(userId);
    }

    @Override
    public List<String> getOnlineUsers() {
        return userDiscoveryManager.onlineUsers.getValue();
    }

    // Listener management for online user status
    public void addOnlineUserListener(OnlineUserListener listener) {
        onlineUserListeners.add(listener);
    }

    public void removeOnlineUserListener(OnlineUserListener listener) {
        onlineUserListeners.remove(listener);
    }

    private void notifyUserStatusChanged(String userId, boolean isOnline) {
        for (OnlineUserListener listener : onlineUserListeners) {
            listener.onUserStatusChanged(userId, isOnline);
        }
    }
}