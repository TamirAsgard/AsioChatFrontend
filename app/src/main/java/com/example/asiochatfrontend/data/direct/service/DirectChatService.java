package com.example.asiochatfrontend.data.direct.service;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.service.ChatService;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class DirectChatService implements ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final DirectWebSocketClient webSocketClient;

    @Inject
    public DirectChatService(ChatRepository chatRepository, UserRepository userRepository, DirectWebSocketClient webSocketClient) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.webSocketClient = webSocketClient;
    }

    @Override
    public ChatDto createPrivateChat(String currentUserId, String otherUserId) {
        ChatDto existingChat = chatRepository.findChatByParticipants(currentUserId, otherUserId);
        if (existingChat != null) {
            return existingChat;
        }

        String chatId = UuidGenerator.generateForChat(currentUserId, otherUserId);

        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(otherUserId);

        ChatDto chat = new ChatDto(
                chatId,
                "",
                ChatType.PRIVATE,
                participants,
                null,
                0,
                new Date(),
                new Date()
        );

        chatRepository.createChat(chat);
        return chat;
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> participants, String creatorId) {
        String chatId = UuidGenerator.generate();
        long now = System.currentTimeMillis();

        ChatDto chat = new ChatDto(
                chatId,
                name,
                ChatType.GROUP,
                participants,
                null,
                0,
                new Date(),
                new Date()
        );

        chatRepository.createChat(chat);
        return chat;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        List<ChatDto> chatDtos = chatRepository.getChatsForUser(userId);
        return chatDtos;
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) {
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) return false;

        List<String> updatedParticipants = new ArrayList<>(chat.getParticipants());
        if (!updatedParticipants.contains(userId)) {
            updatedParticipants.add(userId);
            chat.setParticipants(updatedParticipants); // Update participants
            chatRepository.updateChat(chat);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) {
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) return false;

        List<String> updatedParticipants = new ArrayList<>(chat.getParticipants());
        if (updatedParticipants.contains(userId)) {
            updatedParticipants.remove(userId);
            chat.setParticipants(updatedParticipants); // Update participants
            chatRepository.updateChat(chat);
            return true;
        }

        return false;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) {
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getType() != ChatType.GROUP) return false;

        chat.setName(newName); // Update name
        chatRepository.updateChat(chat);
        return true;
    }
}