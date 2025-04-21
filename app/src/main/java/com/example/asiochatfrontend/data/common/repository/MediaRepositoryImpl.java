package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.MediaDao;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
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

        if (mediaDao.getMediaById(mediaDto.getId()) != null) {
            mediaDao.updateMedia(entity);
            return mapEntityToDto(entity);
        }

        mediaDao.insertMedia(entity);
        MessageDto messageDto = mediaMessageDto;
        messageRepository.saveMessage((TextMessageDto) messageDto);
        return mapEntityToDto(entity);
    }

    @Override
    public MediaDto getMediaById(String mediaId) {
        MediaEntity entity = mediaDao.getMediaById(mediaId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public MediaDto getMediaForMessage(String messageId) {
        MediaEntity entity = mediaDao.getMediaForMessage(messageId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public List<MediaDto> getMediaForChat(String chatId) {
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

    private List<MediaDto> mapEntityListToDtoList(List<MediaEntity> entities) {
        List<MediaDto> list = new ArrayList<>();
        for (MediaEntity entity : entities) {
            list.add(mapEntityToDto(entity));
        }
        return list;
    }
}
