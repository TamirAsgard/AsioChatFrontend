package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.MessageDao;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class MessageRepositoryImpl implements MessageRepository {
    private final MessageDao messageDao;

    @Inject
    public MessageRepositoryImpl(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    @Override
    public TextMessageDto saveMessage(TextMessageDto messageDto) {
        MessageEntity entity = new MessageEntity();

        entity.id = messageDto.getId() != null ? messageDto.getId() : UuidGenerator.generate();
        entity.chatId = messageDto.getChatId();
        entity.senderId = messageDto.getJid();
        entity.content = messageDto.getPayload();
        entity.mediaId = null; // Handle this if media is present
        entity.replyToMessageId = null; // Handle this if reply ID is present
        entity.state = messageDto.getStatus() != null ? messageDto.getStatus() : MessageState.UNKNOWN;
        entity.waitingMembersList = messageDto.getWaitingMemebersList();
        entity.createdAt = messageDto.getTimestamp() != null ? messageDto.getTimestamp() : new Date();
        entity.deliveredAt = null;
        entity.readAt = null;

        messageDao.insertMessage(entity);
        return mapEntityToDto(entity);
    }

    @Override
    public TextMessageDto getMessageById(String messageId) {
        MessageEntity entity = messageDao.getMessageById(messageId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) {
        return messageDao.getMessagesForChat(chatId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId, int offset, int limit) {
        return messageDao.getMessagesForChat(chatId) // Replace with pagination logic if supported
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextMessageDto> getPendingMessages() {
        return messageDao.getPendingMessages()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TextMessageDto> getFailedMessages() {
        return messageDao.getFailedMessages()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateMessage(TextMessageDto message) {
        MessageEntity existing = messageDao.getMessageById(message.getId());

        existing.state = message.getStatus();
        existing.waitingMembersList = message.getWaitingMemebersList();
        existing.createdAt = message.getTimestamp();

        messageDao.updateMessage(existing);
        return true;
    }

    @Override
    public boolean updateMessageState(String messageId, MessageState state) {
        messageDao.updateMessageState(messageId, state.name());
        return true;
    }

    @Override
    public boolean updateMessageDeliveredAt(String messageId, Date deliveredAt) {
        return messageDao.updateMessageDeliveredAt(messageId, deliveredAt.getTime()) > 0;
    }

    @Override
    public boolean updateMessageReadAt(String messageId, Date readAt) {
        return messageDao.updateMessageReadAt(messageId, readAt.getTime()) > 0;
    }

    @Override
    public TextMessageDto getLastMessageForChat(String chatId) {
        MessageEntity entity = messageDao.getLastMessageForChat(chatId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) {
        List<MessageEntity> messageEntities = messageDao.getUnreadMessagesCount(chatId);
        if (messageEntities == null || messageEntities.isEmpty()) {
            return 0;
        }

        return (int) messageEntities
                .stream()
                .filter((messageEntity -> messageEntity.waitingMembersList.contains(userId)))
                .count();
    }

    private TextMessageDto mapEntityToDto(MessageEntity entity) {
        return new TextMessageDto(
                entity.id,
                entity.waitingMembersList,
                entity.state,
                entity.createdAt,
                entity.senderId,
                entity.chatId,
                entity.content
        );
    }
}
