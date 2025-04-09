package com.example.asiochatfrontend.app.di;

import android.content.Context;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.security.EncryptionService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.data.direct.network.UserDiscoveryManager;
import com.example.asiochatfrontend.data.direct.service.DirectChatService;
import com.example.asiochatfrontend.data.direct.service.DirectMediaService;
import com.example.asiochatfrontend.data.direct.service.DirectMessageService;
import com.example.asiochatfrontend.data.direct.service.DirectUserService;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.data.relay.service.RelayChatService;
import com.example.asiochatfrontend.data.relay.service.RelayMediaService;
import com.example.asiochatfrontend.data.relay.service.RelayMessageService;
import com.example.asiochatfrontend.data.relay.service.RelayUserService;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;
import com.example.asiochatfrontend.domain.usecase.user.CreateUserUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;
import com.example.asiochatfrontend.domain.usecase.user.UpdateUserUseCase;
import com.google.gson.Gson;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    private static EncryptionService encryptionService;
    private static DirectChatService directChatService;
    private static DirectMessageService directMessageService;
    private static DirectMediaService directMediaService;
    private static DirectUserService directUserService;
    private static RelayApiClient relayApiClient;
    private static RelayChatService relayChatService;
    private static RelayMessageService relayMessageService;
    private static RelayMediaService relayMediaService;
    private static RelayUserService relayUserService;

    private static DirectWebSocketClient directWebSocketClient;
    private static UserDiscoveryManager userDiscoveryManager;
    private static RelayWebSocketClient relayWebSocketClient;
    private static ConnectionManager connectionManager;

    public static void initialize(
            Context context,
            ChatRepository chatRepository,
            MessageRepository messageRepository,
            MediaRepository mediaRepository,
            UserRepository userRepository,
            String userId,
            String relayIp,
            int port
    ) {
        // Create services with their dependencies
        encryptionService = new EncryptionService();

        directWebSocketClient = new DirectWebSocketClient(context, userId);
        relayWebSocketClient = new RelayWebSocketClient(relayIp, userId);
        relayApiClient = RelayApiClient.createInstance(relayIp, port);

        // Direct services
        directChatService = new DirectChatService(chatRepository, userRepository, directWebSocketClient);
        directMessageService =
                new DirectMessageService(messageRepository, chatRepository, directWebSocketClient, connectionManager);
        directMediaService = new DirectMediaService(context, mediaRepository, new FileUtils(context));
        directUserService =
                new DirectUserService(context, userRepository, directWebSocketClient, connectionManager, userDiscoveryManager);

        // Relay services
        relayChatService = new RelayChatService(chatRepository, relayApiClient, relayWebSocketClient, new Gson());
        relayMessageService = new RelayMessageService(messageRepository, relayApiClient, relayWebSocketClient, new Gson());
        relayMediaService = new RelayMediaService(mediaRepository, relayApiClient, relayWebSocketClient, new FileUtils(context), new Gson());
        relayUserService = new RelayUserService(userRepository, relayApiClient, relayWebSocketClient, new Gson());

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

    public static void startUserDiscovery(ConnectionManager connectionManager) {
        if (userDiscoveryManager != null) {
            throw new IllegalStateException("UserDiscoveryManager already started.");
        }

        userDiscoveryManager = new UserDiscoveryManager(
                directWebSocketClient,
                new GetUserByIdUseCase(connectionManager),
                new CreateUserUseCase(connectionManager),
                new UpdateUserUseCase(connectionManager)
        );
    }
}