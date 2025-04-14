package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.MessageDao;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

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
    public MessageDto saveMessage(MessageDto messageDto) {
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
    public MessageDto getMessageById(String messageId) {
        MessageEntity entity = messageDao.getMessageById(messageId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) {
        return messageDao.getMessagesForChat(chatId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId, int offset, int limit) {
        return messageDao.getMessagesForChat(chatId) // Replace with pagination logic if supported
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDto> getFailedMessages() {
        return messageDao.getFailedMessages()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateMessage(MessageDto message) {
        MessageEntity entity = new MessageEntity();
        entity.id = message.getId();
        entity.chatId = message.getChatId();
        entity.senderId = message.getJid();
        entity.content = message.getPayload();
        entity.mediaId = null;
        entity.replyToMessageId = null;
        entity.state = message.getStatus();
        entity.waitingMembersList = message.getWaitingMemebersList();
        entity.createdAt = message.getTimestamp();
        entity.deliveredAt = null;
        entity.readAt = null;

        messageDao.updateMessage(entity);
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

    private MessageDto mapEntityToDto(MessageEntity entity) {
        return new MessageDto(
                entity.id,
                entity.waitingMembersList,
                entity.state,
                entity.createdAt,
                entity.content,
                entity.senderId,
                entity.chatId
        );
    }
}
