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
                updateUserOnlineStatus(peerId, true);
                notifyUserStatusChanged(peerId, true);
            }

            @Override
            public void onMessageReceived(MessageDto message) {
                // Update sender's online status
                updateUserOnlineStatus(message.getSenderId(), true);
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

        // Update user in repository
        try {
            UserDto user = userRepository.getUserById(userId);
            if (user != null) {
                // Mark as online
                user.setOnline(true);
                userRepository.saveUser(user);
            } else {
                // Create new user if not found
                user = new UserDto(
                        userId,
                        "User " + userId.substring(0, Math.min(8, userId.length())),
                        null, // No profile picture
                        "Available", // Default status
                        true, // Online
                        new Date(), // Last seen now
                        new Date(), // Created now
                        new Date() // Updated now
                );
                userRepository.saveUser(user);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting current user", e);
        }
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        // Ensure user is marked as online when created in direct mode
        userDto.setOnline(true);
        return userRepository.saveUser(userDto);
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updateUserDetailsDto) throws Exception {
        UserDto current = userRepository.getUserById(userId);
        if (current == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        // Update fields that were provided
        String updatedName = updateUserDetailsDto.getName() != null ?
                updateUserDetailsDto.getName() : current.getName();
        String updatedPicture = updateUserDetailsDto.getProfilePicture() != null ?
                updateUserDetailsDto.getProfilePicture() : current.getProfilePicture();
        boolean isOnline = updateUserDetailsDto.isOnline();

        UserDto updatedUser = new UserDto(
                current.getId(),
                updatedName,
                updatedPicture,
                current.getStatus(), // Keep existing status
                isOnline,
                isOnline ? current.getLastSeen() : new Date(), // Update last seen if going offline
                current.getCreatedAt(),
                new Date() // Updated now
        );

        userRepository.saveUser(updatedUser);
        return updatedUser;
    }

    @Override
    public UserDto getUserById(String userId) throws Exception {
        UserDto user = userRepository.getUserById(userId);

        // If user not found locally but we're connected to them, create a placeholder
        if (user == null && directWebSocketClient.getIpForUserId(userId) != null) {
            user = new UserDto(
                    userId,
                    "User " + userId.substring(0, Math.min(8, userId.length())),
                    null, // No profile picture
                    "Available", // Default status
                    true, // Online since we're connected
                    new Date(), // Last seen now
                    new Date(), // Created now
                    new Date() // Updated now
            );
            userRepository.saveUser(user);
        }

        return user;
    }

    @Override
    public List<UserDto> getContacts() {
        // In direct mode, contacts are all known users
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
        if (userDiscoveryManager != null) {
            userDiscoveryManager.refreshOnlineUsers();
        } else {
            directWebSocketClient.startDiscovery();
        }
    }

    @Override
    public List<String> getOnlineUsers() {
        if (userDiscoveryManager != null) {
            List<String> online = userDiscoveryManager.onlineUsers.getValue();
            return online != null ? online : new ArrayList<>();
        } else {
            // Fallback to WebSocket client's connected peers
            return directWebSocketClient.getConnectedPeers();
        }
    }

    // Utility methods for managing user online status
    private void updateUserOnlineStatus(String userId, boolean isOnline) {
        try {
            UserDto user = userRepository.getUserById(userId);
            if (user != null) {
                user.setOnline(isOnline);
                if (!isOnline) {
                    user.setLastSeen(new Date());
                }
                userRepository.saveUser(user);
            } else if (isOnline) {
                // If user is online but not in our database, create a placeholder
                UserDto newUser = new UserDto(
                        userId,
                        "User " + userId.substring(0, Math.min(8, userId.length())),
                        null, // No profile picture
                        "Available", // Default status
                        true, // Online
                        new Date(), // Last seen now
                        new Date(), // Created now
                        new Date() // Updated now
                );
                userRepository.saveUser(newUser);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating user online status", e);
        }
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