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
                if (event.getType() == WebSocketEvent.EventType.CHAT_UPDATE ||
                        event.getType() == WebSocketEvent.EventType.GROUP_UPDATE) {
                    ChatDto chatDto = gson.fromJson(event.getPayload(), ChatDto.class);
                    chatRepository.updateChat(chatDto);
                    Log.d(TAG, "Received chat update from WebSocket: " + chatDto.getId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to handle WebSocket event: " + event.getType(), e);
            }
        });
    }

    @Override
    public ChatDto createPrivateChat(String currentUserId, String otherUserId) {
        Log.d(TAG, "Creating private chat between " + currentUserId + " and " + otherUserId);

        ChatDto existing = chatRepository.findChatByParticipants(currentUserId, otherUserId);
        if (existing != null) {
            Log.i(TAG, "Private chat already exists: " + existing.getId());
            return existing;
        }

        String chatId = UuidGenerator.generateForChat(currentUserId, otherUserId);
        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(otherUserId);

        ChatDto chat = new ChatDto(chatId, "", ChatType.PRIVATE, participants, null, 0, new Date(), new Date());
        chatRepository.createChat(chat);
        relayApiClient.createChat(chat);
        broadcastChatUpdate(chat);
        Log.d(TAG, "Private chat created: " + chatId);

        return chat;
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> participants, String creatorId) {
        Log.d(TAG, "Creating group chat with name: " + name);
        String chatId = UuidGenerator.generate();

        ChatDto chat = new ChatDto(chatId, name, ChatType.GROUP, participants, null, 0, new Date(), new Date());
        chatRepository.createChat(chat);
        relayApiClient.createChat(chat);
        broadcastGroupUpdate(chat);
        Log.d(TAG, "Group chat created: " + chatId);

        return chat;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        Log.d(TAG, "Fetching chats for user: " + userId);
        List<ChatDto> serverChats = relayApiClient.getChatsForUser(userId);
        if (serverChats != null && !serverChats.isEmpty()) {
            for (ChatDto chat : serverChats) {
                chatRepository.createChat(chat);
            }
            Log.d(TAG, "Fetched " + serverChats.size() + " chats from server.");
            return serverChats;
        }

        Log.w(TAG, "No server chats found. Using local DB fallback.");
        return chatRepository.getChatsForUser(userId);
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) {
        Log.d(TAG, "Adding member " + userId + " to group chat " + chatId);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) {
            Log.w(TAG, "Chat not found or not a group chat: " + chatId);
            return false;
        }

        List<String> participants = new ArrayList<>(chat.getParticipants());
        if (!participants.contains(userId)) {
            participants.add(userId);
            chat.setParticipants(participants);
            chatRepository.updateChat(chat);
            relayApiClient.addMemberToChat(chatId, userId);
            broadcastGroupUpdate(chat);
            Log.d(TAG, "Member " + userId + " added to group " + chatId);
            return true;
        }

        Log.i(TAG, "Member " + userId + " already in group " + chatId);
        return false;
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) {
        Log.d(TAG, "Removing member " + userId + " from group chat " + chatId);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) {
            Log.w(TAG, "Chat not found or not a group chat: " + chatId);
            return false;
        }

        List<String> participants = new ArrayList<>(chat.getParticipants());
        if (participants.contains(userId)) {
            participants.remove(userId);
            chat.setParticipants(participants);
            chatRepository.updateChat(chat);
            relayApiClient.removeMemberFromChat(chatId, userId);
            broadcastGroupUpdate(chat);
            Log.d(TAG, "Member " + userId + " removed from group " + chatId);
            return true;
        }

        Log.i(TAG, "Member " + userId + " not in group " + chatId);
        return false;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) {
        Log.d(TAG, "Updating group chat name: " + chatId + " -> " + newName);
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) {
            Log.w(TAG, "Chat not found or not a group chat: " + chatId);
            return false;
        }

        chat.setName(newName);
        chatRepository.updateChat(chat);
        relayApiClient.updateChat(chatId, chat);
        broadcastGroupUpdate(chat);
        Log.d(TAG, "Group chat name updated: " + chatId);

        return true;
    }

    private void broadcastChatUpdate(ChatDto chat) {
        JsonElement payload = gson.toJsonTree(chat);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.CHAT_UPDATE,
                payload,
                "chat-update-" + System.currentTimeMillis(),
                "" // senderId should be set appropriately
        );
        webSocketClient.sendEvent(event);
        Log.d(TAG, "Broadcasted CHAT_UPDATE for chat: " + chat.getId());
    }

    private void broadcastGroupUpdate(ChatDto chat) {
        JsonElement payload = gson.toJsonTree(chat);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.GROUP_UPDATE,
                payload,
                "group-update-" + System.currentTimeMillis(),
                "" // senderId should be set appropriately
        );
        webSocketClient.sendEvent(event);
        Log.d(TAG, "Broadcasted GROUP_UPDATE for chat: " + chat.getId());
    }
}
