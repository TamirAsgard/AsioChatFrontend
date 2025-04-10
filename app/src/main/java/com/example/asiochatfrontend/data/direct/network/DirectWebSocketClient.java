package com.example.asiochatfrontend.data.direct.network;

import android.content.Context;
import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DirectWebSocketClient {
    private static final String TAG = "DirectWebSocketClient";
    private static final int DISCOVERY_PORT = 8888;
    private static final int WEBSOCKET_PORT = 8889;

    private final Context context;
    private final String userId;
    private final Gson gson = new Gson();
    private DatagramSocket discoverySocket;
    private WebSocketServer server;
    private final Map<String, WebSocketClient> peerConnections = new ConcurrentHashMap<>();
    private final Map<String, String> userIdToIpMap = new ConcurrentHashMap<>();
    private final List<PeerConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private boolean isRunning = false;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public interface PeerConnectionListener {
        void onPeerDiscovered(String peerId, String peerIp);
        void onMessageReceived(MessageDto message);
        void onPeerStatusChanged(String peerId, boolean isOnline);
    }

    public DirectWebSocketClient(Context context, String userId) {
        this.context = context;
        this.userId = userId;
    }

    public void startDiscovery() {
        if (isRunning) return;
        isRunning = true;

        startDiscoveryListener();
        startWebSocketServer();
        startBroadcastingPresence();
    }

    public void stopDiscovery() {
        isRunning = false;

        // Stop broadcasting presence
        scheduler.shutdown();

        // Close discovery socket
        if (discoverySocket != null && !discoverySocket.isClosed()) {
            discoverySocket.close();
            discoverySocket = null;
        }

        // Stop WebSocket server
        if (server != null) {
            try {
                server.stop();
                server = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping WebSocket server", e);
            }
        }

        // Close all peer connections
        for (WebSocketClient client : peerConnections.values()) {
            if (client != null && client.isOpen()) {
                client.close();
            }
        }
        peerConnections.clear();
        userIdToIpMap.clear();
    }

    private void startDiscoveryListener() {
        Thread discoveryThread = new Thread(() -> {
            try {
                discoverySocket = new DatagramSocket(DISCOVERY_PORT);
                discoverySocket.setBroadcast(true);

                byte[] buffer = new byte[1024];

                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        discoverySocket.receive(packet);

                        String message = new String(packet.getData(), 0, packet.getLength());
                        JsonObject json = gson.fromJson(message, JsonObject.class);

                        if (json != null && json.has("type") && "discovery".equals(json.get("type").getAsString())) {
                            String discoveredUserId = json.get("userId").getAsString();
                            String discoveredIp = json.get("ip").getAsString();

                            // Don't process our own broadcasts
                            if (!userId.equals(discoveredUserId)) {
                                Log.d(TAG, "Discovered peer: " + discoveredUserId + " at " + discoveredIp);
                                userIdToIpMap.put(discoveredUserId, discoveredIp);
                                notifyPeerDiscovered(discoveredUserId, discoveredIp);

                                // Connect to the peer
                                connectToPeer(discoveredUserId, discoveredIp);
                            }
                        }
                    } catch (Exception e) {
                        if (isRunning) {
                            Log.e(TAG, "Error receiving discovery packet", e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating discovery socket", e);
            }
        });
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    private void startBroadcastingPresence() {
        // Schedule regular broadcasts of presence
        scheduler.scheduleAtFixedRate(() -> {
            broadcastPresence();
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void broadcastPresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            // Get all network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

                for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || !(addr instanceof Inet4Address)) continue;

                    // Create discovery message
                    JsonObject discoveryMessage = new JsonObject();
                    discoveryMessage.addProperty("type", "discovery");
                    discoveryMessage.addProperty("userId", userId);
                    discoveryMessage.addProperty("ip", addr.getHostAddress());
                    discoveryMessage.addProperty("port", WEBSOCKET_PORT);

                    String broadcastData = discoveryMessage.toString();
                    byte[] sendData = broadcastData.getBytes();

                    // Broadcast on interface
                    String broadcastAddress = getBroadcastAddress(addr);

                    try {
                        DatagramPacket packet = new DatagramPacket(
                                sendData,
                                sendData.length,
                                InetAddress.getByName(broadcastAddress),
                                DISCOVERY_PORT
                        );
                        socket.send(packet);
                        Log.d(TAG, "Broadcast presence to " + broadcastAddress);
                    } catch (Exception e) {
                        Log.e(TAG, "Error broadcasting to " + broadcastAddress, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in broadcast presence", e);
        }
    }

    private String getBroadcastAddress(InetAddress addr) {
        try {
            byte[] ipBytes = addr.getAddress();
            // Simple broadcast - set last byte to 255
            // For production, you'd want to calculate this based on subnet mask
            ipBytes[3] = (byte) 255;
            return InetAddress.getByAddress(ipBytes).getHostAddress();
        } catch (UnknownHostException e) {
            return "255.255.255.255";  // Fallback to global broadcast
        }
    }

    private void startWebSocketServer() {
        try {
            server = new WebSocketServer(new InetSocketAddress(WEBSOCKET_PORT)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    String peerIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                    Log.d(TAG, "WebSocket connection opened from: " + peerIp);

                    // The peer ID will be sent in the first message
                    // We'll associate this connection with the peer ID then
                }

                @Override
                public void onMessage(WebSocket conn, String message) {
                    try {
                        JsonObject json = gson.fromJson(message, JsonObject.class);
                        String type = json.get("type").getAsString();

                        if ("identification".equals(type)) {
                            // Handle identification message
                            String peerId = json.get("userId").getAsString();
                            String peerIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();

                            userIdToIpMap.put(peerId, peerIp);
                            notifyPeerDiscovered(peerId, peerIp);
                            Log.d(TAG, "Peer identified: " + peerId + " at " + peerIp);
                        } else if ("message".equals(type)) {
                            // Handle chat message
                            MessageDto messageDto = gson.fromJson(json.get("payload"), MessageDto.class);
                            notifyMessageReceived(messageDto);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing message: " + message, e);
                    }
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    String peerIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                    Log.d(TAG, "WebSocket connection closed from: " + peerIp);

                    // Find the user ID associated with this IP
                    for (Map.Entry<String, String> entry : userIdToIpMap.entrySet()) {
                        if (entry.getValue().equals(peerIp)) {
                            notifyPeerStatusChanged(entry.getKey(), false);
                            break;
                        }
                    }
                }

                @Override
                public void onError(WebSocket conn, Exception ex) {
                    Log.e(TAG, "WebSocket server error", ex);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "WebSocket server started on port " + WEBSOCKET_PORT);
                }
            };

            server.setReuseAddr(true);
            server.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting WebSocket server", e);
        }
    }

    private void connectToPeer(String peerId, String peerIp) {
        // Check if already connected
        if (peerConnections.containsKey(peerId)) {
            WebSocketClient existingClient = peerConnections.get(peerId);
            if (existingClient != null && existingClient.isOpen()) {
                return;  // Already connected
            }
        }

        try {
            URI uri = new URI("ws://" + peerIp + ":" + WEBSOCKET_PORT);
            WebSocketClient client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "Connected to peer: " + peerId);

                    // Send identification message
                    JsonObject identMessage = new JsonObject();
                    identMessage.addProperty("type", "identification");
                    identMessage.addProperty("userId", userId);
                    send(identMessage.toString());

                    // Notify listeners
                    notifyPeerStatusChanged(peerId, true);
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonObject json = gson.fromJson(message, JsonObject.class);
                        String type = json.get("type").getAsString();

                        if ("message".equals(type)) {
                            MessageDto messageDto = gson.fromJson(json.get("payload"), MessageDto.class);
                            notifyMessageReceived(messageDto);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing message from peer: " + peerId, e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "Disconnected from peer: " + peerId);
                    peerConnections.remove(peerId);
                    notifyPeerStatusChanged(peerId, false);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "Error in connection to peer: " + peerId, ex);
                }
            };

            client.setConnectionLostTimeout(30);
            client.connect();
            peerConnections.put(peerId, client);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to peer: " + peerId, e);
        }
    }

    public boolean sendMessage(String receiverId, MessageDto message) {
        if (!userIdToIpMap.containsKey(receiverId)) {
            Log.e(TAG, "Unknown peer ID: " + receiverId);
            return false;
        }

        // Get connection to peer
        WebSocketClient client = peerConnections.get(receiverId);
        if (client == null || !client.isOpen()) {
            // Try to connect
            String peerIp = userIdToIpMap.get(receiverId);
            connectToPeer(receiverId, peerIp);
            client = peerConnections.get(receiverId);

            if (client == null || !client.isOpen()) {
                Log.e(TAG, "Failed to connect to peer: " + receiverId);
                return false;
            }
        }

        try {
            // Create message envelope
            JsonObject envelope = new JsonObject();
            envelope.addProperty("type", "message");
            envelope.add("payload", gson.toJsonTree(message));

            // Send message
            client.send(envelope.toString());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to peer: " + receiverId, e);
            return false;
        }
    }

    public void sendMessageToAllPeers(MessageDto message) {
        for (String peerId : peerConnections.keySet()) {
            sendMessage(peerId, message);
        }
    }

    public List<String> getConnectedPeers() {
        return new ArrayList<>(peerConnections.keySet());
    }

    public String getIpForUserId(String userId) {
        return userIdToIpMap.get(userId);
    }

    public void addPeerConnectionListener(PeerConnectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removePeerConnectionListener(PeerConnectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyPeerDiscovered(String peerId, String peerIp) {
        for (PeerConnectionListener listener : listeners) {
            listener.onPeerDiscovered(peerId, peerIp);
        }
    }

    private void notifyMessageReceived(MessageDto message) {
        for (PeerConnectionListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyPeerStatusChanged(String peerId, boolean isOnline) {
        for (PeerConnectionListener listener : listeners) {
            listener.onPeerStatusChanged(peerId, isOnline);
        }
    }
}