package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.ChatDao;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;
import com.example.asiochatfrontend.domain.repository.ChatRepository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class ChatRepositoryImpl implements ChatRepository {
    private final ChatDao chatDao;

    @Inject
    public ChatRepositoryImpl(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    @Override
    public ChatDto createChat(ChatDto chatDto) {
        String chatId = UuidGenerator.generate();
        Date now = new Date();
        ChatEntity chat = new ChatEntity();
        chat.id = chatDto.getChatId() != null ? chatDto.getChatId() : chatId;
        chat.name = chatDto.getChatName() != null ? chatDto.getChatName() : "";
        chat.type = chatDto.isGroup ? ChatType.GROUP : ChatType.PRIVATE;
        chat.participants = chatDto.getRecipients() != null ? chatDto.getRecipients() : List.of();
        chat.createdAt = now;
        chat.updatedAt = now;

        chatDao.insertChat(chat);
        return mapEntityToDto(chat);
    }

    @Override
    public ChatDto getChatById(String chatId) {
        ChatEntity chat = chatDao.getChatById(chatId);
        return chat != null ? mapEntityToDto(chat) : null;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        return chatDao.getChatsForUser(userId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ChatDto updateChat(ChatDto chat) {
        return null;
    }

    @Override
    public boolean deleteChat(String chatId) {
        return false;
    }

    @Override
    public ChatDto findChatByParticipants(String userOneId, String userTwoId) {
        return null;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) {
        return chatDao.updateGroupName(chatId, newName) > 0;
    }

    @Override
    public boolean updateParticipants(String chatId, List<String> participants) {
        return chatDao.updateParticipants(chatId, participants) > 0;
    }

    @Override
    public boolean updateUnreadCount(String chatId, int unreadCount) {
        return chatDao.updateUnreadCount(chatId, unreadCount) > 0;
    }

    @Override
    public List<ChatDto> searchChats(String query) {
        return Collections.emptyList();
    }

    @Override
    public boolean updateLastMessage(String chatId, String lastMessageId) {
        ChatEntity chat = chatDao.getChatById(chatId);
        if (chat == null) return false;
        chat.lastMessageId = lastMessageId;
        chat.updatedAt = new Date();
        chatDao.updateChat(chat);
        return true;
    }

    private ChatDto mapEntityToDto(ChatEntity entity) {
        return new ChatDto(
                entity.id,
                entity.type == ChatType.GROUP,
                entity.participants,
                entity.name
        );
    }
}
