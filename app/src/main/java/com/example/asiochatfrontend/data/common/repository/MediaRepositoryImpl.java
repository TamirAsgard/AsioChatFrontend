package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.MediaDao;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MediaRepositoryImpl implements MediaRepository {

    private final MediaDao mediaDao;
    private final MessageRepository messageRepository;
    private final FileUtils fileUtils;

    @Inject
    public MediaRepositoryImpl(MediaDao mediaDao, MessageRepository messageRepository, FileUtils fileUtils) {
        this.mediaDao = mediaDao;
        this.messageRepository = messageRepository;
        this.fileUtils = fileUtils;
    }

    @Override
    public MediaDto saveMedia(MediaMessageDto mediaMessageDto) {
        MediaEntity entity = new MediaEntity();
        MediaDto mediaDto = mediaMessageDto.getPayload();

        // Check if the media already exists in the database
        MediaDto existing = this.getMediaForMessage(mediaMessageDto.getId());
        if (existing != null) {
            MediaEntity existingEntity = getMediaEntityById(existing.getId());
            existingEntity.setWaitingMembersList(mediaMessageDto.getWaitingMemebersList());
            if (mediaMessageDto.getStatus() != null) {
                existingEntity.setState(mediaMessageDto.getStatus());
            }
            if (mediaMessageDto.getTimestamp() != null) {
                existingEntity.setCreatedAt(mediaMessageDto.getTimestamp());
            }
            if (mediaDto.getFile() != null) {
                existingEntity.localUri = mediaDto.getFile().getAbsolutePath();
            }
            if (mediaDto.getFileName() != null) {
                existingEntity.fileName = mediaDto.getFileName();
            }
            if (mediaDto.getSize() != null) {
                existingEntity.fileSize = mediaDto.getSize();
            }
            if (mediaDto.getContentType() != null) {
                existingEntity.mimeType = mediaDto.getContentType();
            }
            if (mediaDto.getType() != null) {
                existingEntity.type = mediaDto.getType();
            }
            mediaDao.updateMedia(existingEntity);
            return mapEntityToDto(existingEntity);
        }

        // Message not in database, so we need to create a new one
        entity.id = mediaDto.getId() != null ? mediaDto.getId() : UuidGenerator.generate();
        entity.type = mediaDto.getType();
        entity.messageId = mediaMessageDto.getId();
        entity.chatId = mediaMessageDto.getChatId();
        entity.senderId = mediaMessageDto.getJid();
        entity.localUri = mediaDto.getFile() != null ? mediaDto.getFile().getAbsolutePath() : null;
        entity.fileName = mediaDto.getFileName();
        entity.fileSize = mediaDto.getSize() != null ? mediaDto.getSize() : 0;
        entity.mimeType = mediaDto.getContentType();
        entity.thumbnailUri = mediaDto.getThumbnailPath();
        entity.isProcessed = mediaDto.getProcessed();
        entity.setState(mediaMessageDto.getStatus());
        entity.setCreatedAt(mediaMessageDto.getTimestamp());

        List<String> participants = mediaMessageDto.getWaitingMemebersList() != null ?
                new ArrayList<>(mediaMessageDto.getWaitingMemebersList()) :
                new ArrayList<>();
        entity.setWaitingMembersList(participants);

        mediaDao.insertMedia(entity);
        return mapEntityToDto(entity);
    }

    @Override
    public MediaDto getMediaById(String mediaId) {
        MediaEntity entity = mediaDao.getMediaById(mediaId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public MediaEntity getMediaEntityById(String mediaId) {
        return mediaDao.getMediaById(mediaId);
    }

    @Override
    public MediaDto getMediaForMessage(String messageId) {
        MediaEntity entity = mediaDao.getMediaForMessage(messageId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public List<MediaMessageDto> getMediaForChat(String chatId) {
        List<MediaEntity> entities = mediaDao.getAllMediaForChat(chatId);
        return mapEntityListToDtoList(entities);
    }

    @Override
    public boolean deleteMedia(String mediaId) {
        MediaEntity entity = mediaDao.getMediaById(mediaId);
        if (entity != null) {
            mediaDao.deleteMedia(entity);
            return true;
        }
        return false;
    }

    @Override
    public MediaDto updateLocalUri(String mediaId, String localUri) {
        mediaDao.updateLocalUri(mediaId, localUri);
        return getMediaById(mediaId);
    }

    @Override
    public MediaDto updateThumbnailUri(String mediaId, String thumbnailUri) {
        mediaDao.updateThumbnailUri(mediaId, thumbnailUri);
        return getMediaById(mediaId);
    }

    @Override
    public MediaMessageDto getLastMessageForChat(String chatId) {
        MediaEntity entity = mediaDao.getLastMessageForChat(chatId);
        return entity != null ? mapEntityToMediaMessageDto(entity) : null;
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String currentUserId) {
        return 0;
    }

    private MediaDto mapEntityToDto(MediaEntity entity) {
        return new MediaDto(
                entity.id,
                entity.fileName,
                entity.localUri != null ? fileUtils.getFileFromPath(entity.localUri) : null,
                entity.mimeType,
                entity.type,
                entity.fileSize,
                entity.thumbnailUri,
                entity.isProcessed
        );
    }

    private MediaMessageDto mapEntityToMediaMessageDto(MediaEntity entity) {
        MediaMessageDto mediaMessageDto = new MediaMessageDto();
        mediaMessageDto.setId(entity.messageId);
        mediaMessageDto.setChatId(entity.chatId);
        mediaMessageDto.setJid(entity.senderId);
        mediaMessageDto.setPayload(mapEntityToDto(entity));
        mediaMessageDto.setTimestamp(entity.getCreatedAt());
        mediaMessageDto.setStatus(entity.getState());
        mediaMessageDto.setWaitingMemebersList(entity.getWaitingMembersList());
        return mediaMessageDto;
    }

    private List<MediaMessageDto> mapEntityListToDtoList(List<MediaEntity> entities) {
        List<MediaMessageDto> list = new ArrayList<>();
        for (MediaEntity entity : entities) {
            MediaMessageDto mediaMessageDto = mapEntityToMediaMessageDto(entity);
            list.add(mediaMessageDto);
        }

        return list;
    }
}
