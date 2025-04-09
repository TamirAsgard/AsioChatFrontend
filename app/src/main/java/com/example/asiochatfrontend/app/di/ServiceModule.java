package com.example.asiochatfrontend.app.di;

import android.content.Context;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.security.EncryptionService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.direct.network.UserDiscoveryManager;
import com.example.asiochatfrontend.data.direct.service.DirectChatService;
import com.example.asiochatfrontend.data.direct.service.DirectMediaService;
import com.example.asiochatfrontend.data.direct.service.DirectMessageService;
import com.example.asiochatfrontend.data.direct.service.DirectUserService;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayApiService;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.data.relay.service.RelayChatService;
import com.example.asiochatfrontend.data.relay.service.RelayMediaService;
import com.example.asiochatfrontend.data.relay.service.RelayMessageService;
import com.example.asiochatfrontend.data.relay.service.RelayUserService;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;
import com.google.gson.Gson;

public class ServiceModule {
    private static EncryptionService encryptionService;
    private static DirectChatService directChatService;
    private static DirectMessageService directMessageService;
    private static DirectMediaService directMediaService;
    private static DirectUserService directUserService;
    private static RelayApiService relayApiService;
    private static RelayChatService relayChatService;
    private static RelayMessageService relayMessageService;
    private static RelayMediaService relayMediaService;
    private static RelayUserService relayUserService;
    private static ConnectionManager connectionManager;

    public static void initialize(
            Context context,
            ChatRepository chatRepository,
            MessageRepository messageRepository,
            MediaRepository mediaRepository,
            UserRepository userRepository
    ) {
        // Create services with their dependencies
        encryptionService = new EncryptionService();

        // Direct services
        directChatService = new DirectChatService(chatRepository, userRepository);
        directMessageService = new DirectMessageService(messageRepository);
        directMediaService = new DirectMediaService(context, mediaRepository, new FileUtils(context));
        directUserService = new DirectUserService(userRepository, new UserDiscoveryManager());

        // Relay services
        relayChatService = new RelayChatService(chatRepository, new RelayApiClient(), new RelayWebSocketClient(), new Gson());
        relayMessageService = new RelayMessageService(messageRepository, new RelayApiClient(), new RelayWebSocketClient(), new Gson());
        relayMediaService = new RelayMediaService(mediaRepository, new RelayApiClient(), new RelayWebSocketClient(), new FileUtils(context), new Gson());
        relayUserService = new RelayUserService(userRepository, new RelayApiClient(), new RelayWebSocketClient(), new Gson());

        // Create ConnectionManager
        connectionManager = new ConnectionManager(
                directChatService,
                directMessageService,
                directMediaService,
                directUserService,
                relayChatService,
                relayMessageService,
                relayMediaService,
                relayUserService
        );
    }

    public static ConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return connectionManager;
    }

    public static EncryptionService getEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return encryptionService;
    }
}