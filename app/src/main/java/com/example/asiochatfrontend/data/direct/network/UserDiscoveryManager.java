package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.domain.usecase.user.CreateUserUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;
import com.example.asiochatfrontend.domain.usecase.user.UpdateUserUseCase;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class UserDiscoveryManager {
    private static final String TAG = "UserDiscoveryManager";

    private final DirectWebSocketClient directWebSocketClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<String>> _onlineUsers = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<String>> onlineUsers = _onlineUsers;
    private final Map<String, String> userIdToIp = new ConcurrentHashMap<>();
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;


    @Inject
    public UserDiscoveryManager(
            DirectWebSocketClient directWebSocketClient,
            GetUserByIdUseCase getUserByIdUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase
    ) {
        this.directWebSocketClient = directWebSocketClient;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        initialize();
    }

    public String getIpForUserId(String userId) {
        return userIdToIp.get(userId);
    }

    private void initialize() {
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                userIdToIp.put(peerId, peerIp);
                executorService.execute(() -> {
                    try {
                        UserDto existing = getUserByIdUseCase.execute(peerId);

                        if (existing != null) {
                            existing.setOnline(true);
                            updateUserUseCase.execute(peerId, new UpdateUserDetailsDto(existing.getId(), existing.getProfilePicture(), true));
                            Log.d(TAG, "Updated existing peer as online: " + peerId);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Creating new discovered peer: " + peerId);

                        UserDto newUser = new UserDto(
                                peerId,
                                "Discovered " + peerId,
                                null,
                                "Available",
                                true,
                                new Date(), // lastSeen
                                new Date(), // createdAt
                                new Date()  // updatedAt
                        );

                        try {
                            createUserUseCase.execute(newUser);
                        } catch (Exception ex) {
                            Log.e(TAG, "Failed to create peer user", ex);
                        }
                    }

                    updateOnlineUsers(peerId, true);
                });
            }

            @Override
            public void onMessageReceived(MessageDto message) {
                // Handle received messages if needed
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                updateOnlineUsers(peerId, isOnline);
            }
        });

        // Start discovery
        directWebSocketClient.startDiscovery();
    }

    private synchronized void updateOnlineUsers(String userId, boolean isOnline) {
        List<String> current = new ArrayList<>(_onlineUsers.getValue() != null ? _onlineUsers.getValue() : new ArrayList<>());

        if (isOnline && !current.contains(userId)) {
            current.add(userId);
            Log.d(TAG, "User online: " + userId);
        } else if (!isOnline) {
            current.remove(userId);
            Log.d(TAG, "User offline: " + userId);
        }

        _onlineUsers.postValue(current);
    }

    public void refreshOnlineUsers() {
        executorService.execute(() -> {
            List<String> currentUsers = _onlineUsers.getValue();
            if (currentUsers != null) {
                _onlineUsers.postValue(new ArrayList<>(currentUsers));
            }
        });
    }
}