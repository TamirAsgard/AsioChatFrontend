package com.example.asiochatfrontend.app.di;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.security.EncryptionService;
import com.example.asiochatfrontend.core.service.ChatService;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.core.service.UserService;
import com.example.asiochatfrontend.data.direct.service.DirectChatService;
import com.example.asiochatfrontend.data.direct.service.DirectMediaService;
import com.example.asiochatfrontend.data.direct.service.DirectMessageService;
import com.example.asiochatfrontend.data.direct.service.DirectUserService;
import com.example.asiochatfrontend.data.relay.service.RelayChatService;
import com.example.asiochatfrontend.data.relay.service.RelayMediaService;
import com.example.asiochatfrontend.data.relay.service.RelayMessageService;
import com.example.asiochatfrontend.data.relay.service.RelayUserService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class ServiceModule {
    @Provides
    @Singleton
    public EncryptionService provideEncryptionService() {
        return new EncryptionService();
    }

    @Provides
    @Singleton
    public ConnectionManager provideConnectionManager(
            DirectChatService directChatService,
            DirectMessageService directMessageService,
            DirectMediaService directMediaService,
            DirectUserService directUserService,
            RelayChatService relayChatService,
            RelayMessageService relayMessageService,
            RelayMediaService relayMediaService,
            RelayUserService relayUserService
    ) {
        return new ConnectionManager(
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

    @Provides
    @Singleton
    public ChatService provideChatService(ConnectionManager connectionManager) {
        return connectionManager;
    }

    @Provides
    @Singleton
    public MessageService provideMessageService(ConnectionManager connectionManager) {
        return connectionManager;
    }

    @Provides
    @Singleton
    public MediaService provideMediaService(ConnectionManager connectionManager) {
        return connectionManager;
    }

    @Provides
    @Singleton
    public UserService provideUserService(ConnectionManager connectionManager) {
        return connectionManager;
    }
}
