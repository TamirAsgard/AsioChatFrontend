package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.domain.repository.UserRepository;
import com.example.asiochatfrontend.domain.usecase.user.CreateUserUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;
import com.example.asiochatfrontend.domain.usecase.user.UpdateUserUseCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserDiscoveryManager implements DirectWebSocketClient.PeerConnectionListener {
    private static final String TAG = "UserDiscoveryManager";

    private final DirectWebSocketClient directWebSocketClient;
    private final ExecutorService executorService;
    private final MutableLiveData<List<String>> _onlineUsers = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<String>> onlineUsers = _onlineUsers;
    private final Map<String, String> userIdToIp = new ConcurrentHashMap<>();
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final UserRepository userRepository;

    @Inject
    public UserDiscoveryManager(
            DirectWebSocketClient directWebSocketClient,
            GetUserByIdUseCase getUserByIdUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            UserRepository userRepository
    ) {
        this.directWebSocketClient = directWebSocketClient;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.userRepository = userRepository;
        this.executorService = Executors.newSingleThreadExecutor();

        // Register as listener
        this.directWebSocketClient.addPeerConnectionListener(this);
    }

    public void initialize() {
        // Start discovery
        directWebSocketClient.startDiscovery();

        // Load existing users from database as offline
        executorService.execute(() -> {
            try {
                List<UserDto> allUsers = userRepository.getAllUsers();
                Log.d(TAG, "Loaded " + allUsers.size() + " users from database");

                // Mark all as offline initially
                for (UserDto user : allUsers) {
//                    if (user.isOnline()) {
//                        userRepository.updateOnlineStatus(user.getId(), false);
//                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading existing users", e);
            }
        });
    }

    public void shutdown() {
        directWebSocketClient.removePeerConnectionListener(this);
        directWebSocketClient.stopDiscovery();

        executorService.execute(() -> {
            // Mark current user as offline
            try {
                List<UserDto> allUsers = userRepository.getAllUsers();
                for (UserDto user : allUsers) {
//                    if (_onlineUsers[user.getJid()] != null) {
//                        userRepository.updateOnlineStatus(user.getJid(), false);
//                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating user status on shutdown", e);
            }
        });
    }

    public String getIpForUserId(String userId) {
        return directWebSocketClient.getIpForUserId(userId);
    }

    @Override
    public void onPeerDiscovered(String peerId, String peerIp) {
        userIdToIp.put(peerId, peerIp);

        executorService.execute(() -> {
            try {
                // Check if we already know this user
                UserDto existing = null;
                try {
                    existing = getUserByIdUseCase.execute(peerId);
                } catch (Exception e) {
                    Log.d(TAG, "User not found in local database: " + peerId);
                }

                if (existing != null) {
                    // Update existing user as online
                    Log.d(TAG, "Updating existing user as online: " + peerId);
                    UpdateUserDetailsDto update = new UpdateUserDetailsDto(
                            existing.getUserDetailsDto()
                    );
                    updateUserUseCase.execute(peerId, update);
                } else {
                    // Create new user
                    Log.d(TAG, "Creating new discovered user: " + peerId);
//                    UserDto newUser = new UserDto(
//                            peerId,
//                            "User " + peerId.substring(0, Math.min(8, peerId.length())),
//                            null,  // No profile picture yet
//                            "Available",  // Default status
//                            true,  // Online
//                            new Date(),  // Last seen now
//                            new Date(),  // Created now
//                            new Date()   // Updated now
//                    );
                    //createUserUseCase.execute(newUser);
                }

                // Update online users list
                updateOnlineUsers(peerId, true);

            } catch (Exception e) {
                Log.e(TAG, "Error processing discovered peer", e);
            }
        });
    }

    @Override
    public void onMessageReceived(MessageDto message) {
        // Update the sender as online
        // updateOnlineUsers(message.getSenderId(), true);
    }

    @Override
    public void onPeerStatusChanged(String peerId, boolean isOnline) {
        executorService.execute(() -> {
            try {
                // Update user status in database
                UserDto user = getUserByIdUseCase.execute(peerId);
                if (user != null) {
                    UpdateUserDetailsDto update = new UpdateUserDetailsDto(
                            user.getUserDetailsDto()
                    );
                    updateUserUseCase.execute(peerId, update);
                }

                // Update online users list
                updateOnlineUsers(peerId, isOnline);

            } catch (Exception e) {
                Log.e(TAG, "Error updating peer status", e);
            }
        });
    }

    private synchronized void updateOnlineUsers(String userId, boolean isOnline) {
        List<String> current = new ArrayList<>(_onlineUsers.getValue() != null ?
                _onlineUsers.getValue() : new ArrayList<>());

        boolean changed = false;
        if (isOnline && !current.contains(userId)) {
            current.add(userId);
            changed = true;
            Log.d(TAG, "Added user to online list: " + userId);
        } else if (!isOnline && current.contains(userId)) {
            current.remove(userId);
            changed = true;
            Log.d(TAG, "Removed user from online list: " + userId);
        }

        if (changed) {
            _onlineUsers.postValue(current);
        }
    }

    public void refreshOnlineUsers() {
        executorService.execute(() -> {
            List<String> currentUsers = _onlineUsers.getValue();
            if (currentUsers != null) {
                _onlineUsers.postValue(new ArrayList<>(currentUsers));
            }

            // Re-broadcast presence to trigger discovery
            directWebSocketClient.stopDiscovery();
            directWebSocketClient.startDiscovery();
        });
    }
}