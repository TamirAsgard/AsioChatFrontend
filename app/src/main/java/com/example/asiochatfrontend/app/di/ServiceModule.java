package com.example.asiochatfrontend.app.di;

import android.content.Context;
import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.security.EncryptionManager;
import com.example.asiochatfrontend.core.security.EncryptionService;
import com.example.asiochatfrontend.core.service.OnWSEventCallback;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.data.direct.network.UserDiscoveryManager;
import com.example.asiochatfrontend.data.direct.service.DirectChatService;
import com.example.asiochatfrontend.data.direct.service.DirectMediaService;
import com.example.asiochatfrontend.data.direct.service.DirectMessageService;
import com.example.asiochatfrontend.data.direct.service.DirectUserService;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.data.relay.network.WebSocketHealthMonitor;
import com.example.asiochatfrontend.data.relay.service.RelayAuthService;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {

    //==============================
    // Static singleton fields
    //==============================
    private static EncryptionService      encryptionService;
    private static EncryptionManager      encryptionManager;

    private static DirectWebSocketClient  directWebSocketClient;
    private static DirectChatService      directChatService;
    private static DirectMessageService   directMessageService;
    private static DirectMediaService     directMediaService;
    private static DirectUserService      directUserService;

    private static RelayApiClient         relayApiClient;
    private static RelayChatService       relayChatService;
    private static RelayMessageService    relayMessageService;
    private static RelayMediaService      relayMediaService;
    private static RelayUserService       relayUserService;
    private static RelayAuthService       relayAuthService;

    private static UserDiscoveryManager   userDiscoveryManager;
    private static RelayWebSocketClient    relayWebSocketClient;
    private static WebSocketHealthMonitor  webSocketHealthMonitor;
    private static ConnectionManager       connectionManager;
    private static Gson                    gson;

    private static ChatRepository          chatRepository;
    private static MessageRepository       messageRepository;
    private static MediaRepository         mediaRepository;
    private static UserRepository          userRepository;

    private static FileUtils               fileUtils;
    private static final List<OnWSEventCallback> wsEventCallbacks = new CopyOnWriteArrayList<>();

    //==============================
    // Public initialization API
    //==============================
    /**
     * Initialize all services and wiring. Must be called once at app startup.
     */
    public static synchronized void initialize(
            Context context,
            ChatRepository chatRepo,
            MessageRepository msgRepo,
            MediaRepository mediaRepo,
            UserRepository userRepo,
            String userId,
            String relayIp,
            int port,
            AppDatabase db
    ) {
        // — Repositories —
        chatRepository    = chatRepo;
        messageRepository = msgRepo;
        mediaRepository   = mediaRepo;
        userRepository    = userRepo;

        // — Gson for MessageState deserialization —
        gson = new GsonBuilder()
                .registerTypeAdapter(MessageState.class, new MessageStateDeserializer())
                .create();

        // — File utilities —
        fileUtils = new FileUtils(context);

        // — Direct WebSocket client & services —
        directWebSocketClient = new DirectWebSocketClient(context, userId);
        directMediaService    = new DirectMediaService(context, mediaRepository, fileUtils);
        directChatService     = new DirectChatService(chatRepository, userRepository, directWebSocketClient);

        // Temporary ConnectionManager to break circular dep
        ConnectionManager tempConnMgr = new ConnectionManager(
                null, null, null, null, null, null, null, null, null
        );
        directUserService    = new DirectUserService(context, userRepository, directWebSocketClient, tempConnMgr, null);
        directMessageService = new DirectMessageService(messageRepository, chatRepository, directWebSocketClient, tempConnMgr);

        // — Encryption setup —
        encryptionService  = new EncryptionService();
        encryptionManager  = new EncryptionManager(encryptionService, db.encryptionKeyDao(), userId);

        // — Relay API & WebSocket client —
        String protocol      = relayIp.startsWith("http") ? "" : "http://";
        relayApiClient       = RelayApiClient.createInstance(relayIp, port, userId);
        String baseUrl       = protocol + relayIp + ":" + port;
        relayWebSocketClient = new RelayWebSocketClient(baseUrl, userId);

        // — Relay services —
        relayAuthService    = new RelayAuthService(relayApiClient, encryptionManager, userId);
        relayChatService    = new RelayChatService(userId, chatRepository, relayAuthService, relayApiClient, relayWebSocketClient, gson, wsEventCallbacks);
        relayMessageService = new RelayMessageService(messageRepository, mediaRepository, chatRepository, relayAuthService, relayApiClient, relayWebSocketClient, gson, userId);
        relayMediaService   = new RelayMediaService(mediaRepository, messageRepository, chatRepository, relayApiClient, relayWebSocketClient, fileUtils, userId, gson, wsEventCallbacks);
        relayUserService    = new RelayUserService(userRepository, relayApiClient, relayWebSocketClient, gson);

        // — Final ConnectionManager wiring —
        connectionManager = new ConnectionManager(
                directChatService,
                directMessageService,
                directMediaService,
                directUserService,
                relayChatService,
                relayMessageService,
                relayMediaService,
                relayUserService,
                relayAuthService
        );
        directUserService.setConnectionManager(connectionManager);
        directMessageService.setConnectionManager(connectionManager);

        // — P2P user discovery —
        userDiscoveryManager = new UserDiscoveryManager(
                directWebSocketClient,
                new GetUserByIdUseCase(connectionManager),
                new CreateUserUseCase(connectionManager),
                new UpdateUserUseCase(connectionManager),
                userRepository
        );
        directUserService.setUserDiscoveryManager(userDiscoveryManager);

        // — Health monitoring for Relay TCP ping loop —
        ExecutorService healthExecutor = Executors.newSingleThreadExecutor();
        webSocketHealthMonitor = new WebSocketHealthMonitor(
                relayIp.replace("http://", "").replace("https://", ""),
                port,
                new WebSocketHealthMonitor.HealthObserver() {
                    @Override public void onConnectionLost() {
                        healthExecutor.execute(() -> connectionManager.updateOnlineStatus(false));
                    }
                    @Override public void onConnectionRestored() {
                        healthExecutor.execute(() -> {
                            connectionManager.updateOnlineStatus(true);
                            relayWebSocketClient.scheduleReconnect();
                            if (Boolean.TRUE.equals(connectionManager.getOnlineStatus().getValue()))
                            {
                                try {
                                    List<MessageDto> messageDtoList = connectionManager.sendPendingMessages();
                                    List<ChatDto> chatDtoList = connectionManager.sendPendingChats();
                                    // Fire the callback to notify that pending messages have been sent in UI
                                    for (OnWSEventCallback onWSEventCallback : wsEventCallbacks) {
                                        onWSEventCallback.onPendingMessagesSendEvent(messageDtoList);
                                        onWSEventCallback.onChatCreateEvent(chatDtoList);
                                    }
                                } catch (Exception e) {
                                    // Failed to send pending messages
                                }
                            }
                        });
                    }
                }
        );

        webSocketHealthMonitor.start();
    }

    //==============================
    // Expose singletons to Dagger/Hilt
    //==============================
    /** register a listener (Activity) **/
    public static void addWSEventCallback(OnWSEventCallback cb) {
        if (cb != null) wsEventCallbacks.add(cb);
    }
    /** unregister when done **/
    public static void removeWSEventCallback(OnWSEventCallback cb) {
        wsEventCallbacks.remove(cb);
    }

    public static void startUserDiscovery() {
        if (userDiscoveryManager == null) {
            throw new IllegalStateException("ServiceModule not initialized. Call initialize() first.");
        }
        userDiscoveryManager.initialize();
    }

    public static void stopUserDiscovery() {
        if (userDiscoveryManager != null) {
            userDiscoveryManager.shutdown();
        }
    }

    @Singleton @Provides
    public static ConnectionManager provideConnectionManager() {
        if (connectionManager == null) {
            throw new IllegalStateException("ServiceModule not initialized.");
        }
        return connectionManager;
    }

    @Singleton @Provides
    public static EncryptionService provideEncryptionService() {
        if (encryptionService == null) {
            throw new IllegalStateException("ServiceModule not initialized.");
        }
        return encryptionService;
    }

    public static DirectWebSocketClient getDirectWebSocketClient() {
        return directWebSocketClient;
    }

    public static RelayWebSocketClient getRelayWebSocketClient() {
        return relayWebSocketClient;
    }

    public static void shutdownRelayServices() {
        if (relayWebSocketClient != null) {
            Log.i("ServiceModule", "Shutting down relay WebSocket");
            relayWebSocketClient.shutdown();
        }
    }

    public static ChatRepository getChatRepository() {
        return chatRepository;
    }

    public static MessageRepository getMessageRepository() {
        return messageRepository;
    }

    public static MediaRepository getMediaRepository() {
        return mediaRepository;
    }

    public static UserRepository getUserRepository() {
        return userRepository;
    }

    public static RelayApiClient getRelayApiClient() {
        return relayApiClient;
    }

    public static EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public static FileUtils getFileUtils() {
        return fileUtils;
    }

    public static Gson getGson() {
        return gson;
    }

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public static WebSocketHealthMonitor getWebSocketHealthMonitor() {
        return webSocketHealthMonitor;
    }

    public static UserDiscoveryManager getUserDiscoveryManager() {
        return userDiscoveryManager;
    }

    public static RelayAuthService getRelayAuthService() {
        return relayAuthService;
    }

    public static RelayUserService getRelayUserService() {
        return relayUserService;
    }

    public static RelayMediaService getRelayMediaService() {
        return relayMediaService;
    }

    public static RelayMessageService getRelayMessageService() {
        return relayMessageService;
    }

    public static RelayChatService getRelayChatService() {
        return relayChatService;
    }

    public static DirectMediaService getDirectMediaService() {
        return directMediaService;
    }

    public static DirectUserService getDirectUserService() {
        return directUserService;
    }

    public static DirectMessageService getDirectMessageService() {
        return directMessageService;
    }

    public static EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public static DirectChatService getDirectChatService() {
        return directChatService;
    }
}