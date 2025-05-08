package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.CreateChatEventDto;
import com.example.asiochatfrontend.core.model.dto.MessageReadByDto;
import com.example.asiochatfrontend.core.model.dto.RemoveFromChatEventDto;
import com.example.asiochatfrontend.core.model.dto.SymmetricKeyDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.service.AuthService;
import com.example.asiochatfrontend.core.service.ChatService;
import com.example.asiochatfrontend.core.service.OnWSEventCallback;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class RelayChatService implements ChatService, RelayWebSocketClient.RelayWebSocketListener {
    private static final String TAG = "RelayChatService";

    private final String currentUserId;
    private final ChatRepository chatRepository;
    private final AuthService authService;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;

    private List<OnWSEventCallback> wsEventCallbacks;

    @Inject
    public RelayChatService(
            String currentUserId,
            ChatRepository chatRepository,
            AuthService authService,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            Gson gson,
            List<OnWSEventCallback> wsEventCallbacks
    ) {
        this.currentUserId = currentUserId;
        this.chatRepository = chatRepository;
        this.authService = authService;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.gson = gson;
        this.wsEventCallbacks = wsEventCallbacks;

        webSocketClient.addListener(event -> {
            try {
                if (event.getType() == WebSocketEvent.EventType.CHAT) {
                    ChatDto chatDto = gson.fromJson(event.getPayload(), ChatDto.class);
                    chatRepository.updateChat(chatDto);
                    Log.d(TAG, "Received chat update via WebSocket: " + chatDto.getChatId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
            }
        });

        webSocketClient.addListener(this);
    }

    @Override
    public ChatDto createPrivateChat(String chatId, String currentUserId, String otherUserId) {
        Log.d(TAG, "Creating private chat between: " + currentUserId + " and " + otherUserId);

        ChatDto existing = chatRepository.findChatByParticipants(currentUserId, otherUserId);
        if (existing != null) {
            Log.i(TAG, "Private chat already exists: " + existing.getChatId());
            return existing;
        }

        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(otherUserId);

        ChatDto chat = new ChatDto(chatId, false, participants, participants.toString());
        chatRepository.createChat(chat);

        // Broadcast chat creation event only if online
        if (ServiceModule.getConnectionManager().isOnline()) {
            relayApiClient.createPrivateChat(chat);
            broadcastChatCreate(chat, currentUserId);
            chatRepository.updateCreatedAt(chatId, new Date());
        }

        return chat;
    }

    @Override
    public ChatDto createGroupChat(String chatId, String name, List<String> participants, String currentUserId) {
        Log.d(TAG, "Creating group chat: " + name);

        ChatDto chat = new ChatDto(chatId, true, participants, name);
        chatRepository.createChat(chat);

        // Register symmetric key for group chat
        if (authService.registerSymmetricKey(chatId)) {
            Log.d(TAG, "Symmetric key registered for group chat: " + chatId);
        } else {
            Log.e(TAG, "Failed to register symmetric key for group chat: " + chatId);
        }

        // Broadcast chat creation event only if online
        if (ServiceModule.getConnectionManager().isOnline()) {
            relayApiClient.createGroupChat(chat);
            broadcastChatCreate(chat, currentUserId);
            chatRepository.updateCreatedAt(chatId, new Date());

            // Check if symmetric key is available for group chat in remote, if not, resend
            SymmetricKeyDto localSymmetricDto = authService.getSymmetricKeyDto(chatId, System.currentTimeMillis());
            SymmetricKeyDto remoteSymmetricDto = relayApiClient.getSymmetricKeyForTimestamp(chatId, System.currentTimeMillis());
            if (remoteSymmetricDto == null) {
                Log.i(TAG, "No symmetric key found for group chat: " + chatId);
                authService.resendSymmetricKey(chatId, localSymmetricDto);
            }
        }

        return chat;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        Log.d(TAG, "Fetching chats for user: " + userId);
        List<ChatDto> localChats = chatRepository.getChatsForUser(userId);

        if (!ServiceModule.getConnectionManager().isOnline()) {
            Log.i(TAG, "Offline mode. Returning local chats.");
            return localChats;
        }

        List<ChatDto> remoteChats = relayApiClient.getChatsForUser(userId);
        if (remoteChats != null && !remoteChats.isEmpty()) {
            for (ChatDto chat : remoteChats) {
                chatRepository.createChat(chat);
            }
            return remoteChats;
        }

        Log.w(TAG, "No server chats found. Returning local data.");
        return chatRepository.getChatsForUser(userId);
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) {
        Log.d(TAG, "Adding user " + userId + " to group: " + chatId);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || !chat.getGroup()) return false;

        List<String> participants = new ArrayList<>(chat.getRecipients());
        if (!participants.contains(userId))
            participants.add(userId);
        chat.setRecipients(participants);
        chatRepository.updateChat(chat);
        relayApiClient.updateGroupRecipients(chat);
        broadcastGroupUpdate(chat);
        return true;
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) {
        Log.d(TAG, "Removing user " + userId + " from group: " + chatId);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || !chat.getGroup()) return false;

        List<String> participants = new ArrayList<>(chat.getRecipients());
        participants.remove(userId);
        chat.setRecipients(participants);
        chatRepository.updateChat(chat);
        relayApiClient.updateGroupRecipients(chat);
        broadcastGroupUpdate(chat);
        broadcastRemoveFromChat(chat, userId);
        return true;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) {
        Log.d(TAG, "Renaming group chat " + chatId + " to " + newName);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || !chat.getGroup()) return false;

        chat.setChatName(newName);
        chatRepository.updateChat(chat);
        relayApiClient.updateGroupName(chatId, newName);
        broadcastGroupUpdate(chat);
        return true;
    }

    @Override
    public ChatDto getChatById(String chatId) {
        return chatRepository.getChatById(chatId);
    }

    @Override
    public List<ChatDto> sendPendingChats() {
        List<ChatDto> pendingChats = chatRepository.getPendingChats();
        if (pendingChats != null && !pendingChats.isEmpty()) {
            for (ChatDto chat : pendingChats) {
                if (chat.getGroup()) {
                    createGroupChat(chat.getChatId(), chat.getChatName(), chat.getRecipients(), currentUserId);
                } else {
                    String otherRecipient = chat.getRecipients().stream()
                            .filter(participant -> !participant.equals(currentUserId))
                            .findFirst()
                            .orElse(null);

                    createPrivateChat(chat.getChatId(), currentUserId, otherRecipient);
                }
            }
        }

        return pendingChats;
    }

    @Override
    public String getChatLastMessage(String chatId) {
        return chatRepository.getChatLastMessage(chatId);
    }

    private void broadcastChatCreate(ChatDto chat, String currentUserId) {
        CreateChatEventDto createChatEventDto = new CreateChatEventDto(
                chat.getRecipients().stream()
                .filter(participant -> !participant.equals(currentUserId))
                .collect(Collectors.toList())
        );

        JsonElement payload = gson.toJsonTree(createChatEventDto);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.CREATE_CHAT,
                payload,
                currentUserId
        );

        webSocketClient.sendEvent(event);
    }

    private void broadcastRemoveFromChat(ChatDto chat, String userIdToRemove) {
        RemoveFromChatEventDto createChatEventDto = new RemoveFromChatEventDto(
                chat.getChatId(),
                userIdToRemove
        );

        JsonElement payload = gson.toJsonTree(createChatEventDto);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.REMOVED_CHAT,
                payload,
                currentUserId
        );

        webSocketClient.sendEvent(event);
    }

    private void broadcastGroupUpdate(ChatDto chat) {
        JsonElement payload = gson.toJsonTree(chat);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.CHAT,
                payload,
                "" // senderId can be added later
        );
        webSocketClient.sendEvent(event);
    }

    @Override
    public void onEvent(WebSocketEvent event) {
        try {
            Log.i("TAG", "WebSocket event received: " + event.toString());
            switch (event.getType()) {
                case CREATE_CHAT:
                    // Fire load all chats event in UI
                    for (OnWSEventCallback onWSEventCallback : wsEventCallbacks) {
                        onWSEventCallback.onChatCreateEvent(null);
                    }
                    break;

                case REMOVED_CHAT:
                    if (event.getPayload() == null) {
                        Log.e(TAG, "Received null payload in WebSocket event");
                        return;
                    }

                    String chatId = gson.fromJson(event.getPayload(), String.class);
                    for (OnWSEventCallback onWSEventCallback : wsEventCallbacks) {
                        onWSEventCallback.onRemovedFromChat(chatId);
                    }
                    break;
                default:
                    Log.d(TAG, "Unhandled WebSocket event type: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
        }
    }

    public void setCallbacks(List<OnWSEventCallback> wsEventCallbacks) {
        this.wsEventCallbacks = wsEventCallbacks;
    }
}