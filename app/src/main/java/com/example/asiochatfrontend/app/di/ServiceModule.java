package com.example.asiochatfrontend.app.di;

import android.content.Context;
import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.security.EncryptionService;
import com.example.asiochatfrontend.core.service.OnWSEventCallback;
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
import com.example.asiochatfrontend.data.database.converter.MessageStateDeserializer;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    // Static references for singleton instances
    private static EncryptionService encryptionService;
    private static DirectWebSocketClient directWebSocketClient;
    private static DirectChatService directChatService;
    private static DirectMessageService directMessageService;
    private static DirectMediaService directMediaService;
    private static DirectUserService directUserService;
    private static RelayApiClient relayApiClient;
    private static RelayChatService relayChatService;
    private static RelayMessageService relayMessageService;
    private static RelayMediaService relayMediaService;
    private static RelayUserService relayUserService;
    private static UserDiscoveryManager userDiscoveryManager;
    private static RelayWebSocketClient relayWebSocketClient;
    private static ConnectionManager connectionManager;
    private static Gson gson;

    private static ChatRepository chatRepository;
    private static MessageRepository messageRepository;
    private static MediaRepository mediaRepository;
    private static UserRepository userRepository;

    private static FileUtils fileUtils;

    /**
     * Initialize all services
     */
    public static synchronized void initialize(
            Context context,
            ChatRepository chatRepository,
            MessageRepository messageRepository,
            MediaRepository mediaRepository,
            UserRepository userRepository,
            String userId,
            String relayIp,
            int port,
            OnWSEventCallback onWSEventCallback
    ) {
        if (connectionManager != null) {
            // Already initialized
            return;
        }

        // Initialize repositories
        ServiceModule.chatRepository = chatRepository;
        ServiceModule.messageRepository = messageRepository;
        ServiceModule.mediaRepository = mediaRepository;
        ServiceModule.userRepository = userRepository;

        // Create Gson instance
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(MessageState.class, new MessageStateDeserializer())
                .create();

        // Initialize encryption service
        encryptionService = new EncryptionService();

        // Initialize direct mode services
        fileUtils = new FileUtils(context);

        // Initialize direct WebSocket client
        directWebSocketClient = new DirectWebSocketClient(context, userId);

        // Initialize relay API client and WebSocket
        String protocol = relayIp.startsWith("http") ? "" : "http://";
        relayApiClient = RelayApiClient.createInstance(relayIp, port, userId);
        String baseUrl = protocol + relayIp + ":" + port;
        relayWebSocketClient = new RelayWebSocketClient(baseUrl, userId);

        // Initialize direct services
        directMediaService = new DirectMediaService(context, mediaRepository, fileUtils);
        directChatService = new DirectChatService(chatRepository, userRepository, directWebSocketClient);

        // Create temporary ConnectionManager to resolve circular dependency
        ConnectionManager tempConnectionManager = new ConnectionManager(
                null, null, null, null, null, null, null, null
        );

        directUserService = new DirectUserService(
                context,
                userRepository,
                directWebSocketClient,
                tempConnectionManager,
                null  // UserDiscoveryManager will be set later
        );

        directMessageService = new DirectMessageService(
                messageRepository,
                chatRepository,
                directWebSocketClient,
                tempConnectionManager
        );

        // Initialize relay services
        relayChatService = new RelayChatService(chatRepository, relayApiClient, relayWebSocketClient, gson, onWSEventCallback);
        relayMessageService = new RelayMessageService(messageRepository, chatRepository ,relayApiClient, relayWebSocketClient, gson, userId);
        relayMediaService = new RelayMediaService(mediaRepository, relayApiClient, relayWebSocketClient, fileUtils, gson);
        relayUserService = new RelayUserService(userRepository, relayApiClient, relayWebSocketClient, gson);

        // Create the real ConnectionManager
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

        // Set the real ConnectionManager in services that needed it
        directUserService.setConnectionManager(connectionManager);
        directMessageService.setConnectionManager(connectionManager);

        // Create UserDiscoveryManager
        userDiscoveryManager = new UserDiscoveryManager(
                directWebSocketClient,
                new GetUserByIdUseCase(connectionManager),
                new CreateUserUseCase(connectionManager),
                new UpdateUserUseCase(connectionManager),
                userRepository
        );

        // Set UserDiscoveryManager in DirectUserService
        directUserService.setUserDiscoveryManager(userDiscoveryManager);
    }

    /**
     * Get the ConnectionManager instance
     */
    public static ConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return connectionManager;
    }

    /**
     * Get the EncryptionService instance
     */
    public static EncryptionService getEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return encryptionService;
    }

    /**
     * Start user discovery for P2P mode
     */
    public static void startUserDiscovery() {
        if (userDiscoveryManager == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }

        userDiscoveryManager.initialize();
    }

    /**
     * Stop user discovery
     */
    public static void stopUserDiscovery() {
        if (userDiscoveryManager != null) {
            userDiscoveryManager.shutdown();
        }
    }

    // Dagger provider methods

    @Provides
    @Singleton
    public static ConnectionManager provideConnectionManager() {
        if (connectionManager == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return connectionManager;
    }

    @Provides
    @Singleton
    public static EncryptionService provideEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        return encryptionService;
    }

    public static DirectWebSocketClient getDirectWebSocketClient() {
        return directWebSocketClient;
    }

    public static void setDirectWebSocketClient(DirectWebSocketClient directWebSocketClient) {
        ServiceModule.directWebSocketClient = directWebSocketClient;
    }

    public static RelayWebSocketClient getRelayWebSocketClient() {
        return relayWebSocketClient;
    }

    public static void setRelayWebSocketClient(RelayWebSocketClient relayWebSocketClient) {
        ServiceModule.relayWebSocketClient = relayWebSocketClient;
    }

    public static void shutdownRelayServices() {
        if (relayWebSocketClient != null) {
            Log.i("ServiceModule", "Shutting down relay WebSocket client from ServiceModule");
            relayWebSocketClient.shutdown();
        }
    }

    public static void setCurrentUser(String userId) {
        if (relayApiClient != null) {
            relayApiClient.setCurrentUser(userId);
        }
    }

    public static ChatRepository getChatRepository() {
        if (chatRepository == null)
            throw new IllegalStateException("ChatRepository not initialized");
        return chatRepository;
    }

    public static MessageRepository getMessageRepository() {
        if (messageRepository == null)
            throw new IllegalStateException("MessageRepository not initialized");
        return messageRepository;
    }

    public static MediaRepository getMediaRepository() {
        if (mediaRepository == null)
            throw new IllegalStateException("MediaRepository not initialized");
        return mediaRepository;
    }

    public static UserRepository getUserRepository() {
        if (userRepository == null)
            throw new IllegalStateException("UserRepository not initialized");
        return userRepository;
    }

    public static void setCallbacks(OnWSEventCallback onWSEventCallback) {
        if (relayChatService != null) {
            relayChatService.setCallbacks(onWSEventCallback);
        }
    }

    public static FileUtils getFileUtils() {
        return fileUtils;
    }
}