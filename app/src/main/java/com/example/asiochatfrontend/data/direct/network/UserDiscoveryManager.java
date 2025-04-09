package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class UserDiscoveryManager {
    private static final String TAG = "UserDiscoveryManager";
    private static final int BROADCAST_PORT = 8888;
    private static final String BROADCAST_MESSAGE = "ASIO_CHAT_DISCOVERY";

    private final DirectWebSocketClient directWebSocketClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<String>> _onlineUsers = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<String>> onlineUsers = _onlineUsers;
    private boolean isDiscovering = false;

    @Inject
    public UserDiscoveryManager(DirectWebSocketClient directWebSocketClient) {
        this.directWebSocketClient = directWebSocketClient;
        initialize();
    }

    private void initialize() {
        directWebSocketClient.addPresenceListener((userId, isOnline) -> {
            executorService.execute(() -> updateOnlineUsers(userId, isOnline));
        });
        startDiscovery();
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

    public void startDiscovery() {
        if (isDiscovering) return;

        isDiscovering = true;
        executorService.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket(BROADCAST_PORT)) {
                socket.setBroadcast(true);
                byte[] sendData = BROADCAST_MESSAGE.getBytes();

                while (isDiscovering) {
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData,
                            sendData.length,
                            InetAddress.getByName("255.255.255.255"),
                            BROADCAST_PORT
                    );
                    socket.send(sendPacket);

                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    String message = new String(receivePacket.getData()).trim();
                    if (BROADCAST_MESSAGE.equals(message)) {
                        String userId = receivePacket.getAddress().getHostAddress();
                        updateOnlineUsers(userId, true);
                    }

                    Thread.sleep(5000); // Broadcast every 5 seconds
                }
            } catch (Exception e) {
                Log.e(TAG, "Discovery error", e);
            }
        });
    }

    public void stopDiscovery() {
        isDiscovering = false;
    }

    public void refreshOnlineUsers() {
        executorService.execute(() -> {
            List<String> currentUsers = _onlineUsers.getValue();
            if (currentUsers != null) {
                _onlineUsers.postValue(new ArrayList<>(currentUsers));
            }
        });
    }

    public void addContact(String userId) {
        // In P2P, we'll just add to local list as we don't need server-side contact management
        updateOnlineUsers(userId, true);
    }

    public List<String> getOnlineUsers() {
        return onlineUsers.getValue() != null ? new ArrayList<>(onlineUsers.getValue()) : new ArrayList<>();
    }
}