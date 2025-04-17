package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.service.ChatService;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class RelayChatService implements ChatService {
    private static final String TAG = "RelayChatService";

    private final ChatRepository chatRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;

    @Inject
    public RelayChatService(
            ChatRepository chatRepository,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            Gson gson
    ) {
        this.chatRepository = chatRepository;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.gson = gson;

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
    }

    @Override
    public ChatDto createPrivateChat(String currentUserId, String otherUserId) {
        Log.d(TAG, "Creating private chat between: " + currentUserId + " and " + otherUserId);

        ChatDto existing = chatRepository.findChatByParticipants(currentUserId, otherUserId);
        if (existing != null) {
            Log.i(TAG, "Private chat already exists: " + existing.getChatId());
            return existing;
        }

        String chatId = UuidGenerator.generateForChat(currentUserId, otherUserId);
        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(otherUserId);

        ChatDto chat = new ChatDto(chatId, false, participants, participants.toString());
        relayApiClient.createPrivateChat(chat);
        chatRepository.createChat(chat);
        broadcastChatUpdate(chat, currentUserId, otherUserId);
        return chat;
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> participants, String creatorId) {
        Log.d(TAG, "Creating group chat: " + name);
        String chatId = UuidGenerator.generate();

        ChatDto chat = new ChatDto(chatId, true, participants, name);
        chatRepository.createChat(chat);
        relayApiClient.createGroupChat(chat);
        broadcastGroupUpdate(chat);
        return chat;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        Log.d(TAG, "Fetching chats for user: " + userId);
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
        if (!participants.contains(userId)) {
            participants.add(userId);
            chat.setRecipients(participants);
            chatRepository.updateChat(chat);
            relayApiClient.addMemberToGroup(chatId, userId);
            broadcastGroupUpdate(chat);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) {
        Log.d(TAG, "Removing user " + userId + " from group: " + chatId);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || !chat.getGroup()) return false;

        List<String> participants = new ArrayList<>(chat.getRecipients());
        if (participants.contains(userId)) {
            participants.remove(userId);
            chat.setRecipients(participants);
            chatRepository.updateChat(chat);
            relayApiClient.removeMemberFromGroup(chatId, userId);
            broadcastGroupUpdate(chat);
            return true;
        }

        return false;
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

    private void broadcastChatUpdate(ChatDto chat, String currentUserId, String otherUserId) {
        JsonElement payload = gson.toJsonTree(chat);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.CHAT,
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
}
