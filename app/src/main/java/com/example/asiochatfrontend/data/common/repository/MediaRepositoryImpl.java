package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.MediaDao;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import com.example.asiochatfrontend.domain.repository.MediaRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class MediaRepositoryImpl implements MediaRepository {

    private final MediaDao mediaDao;
    private final FileUtils fileUtils;

    @Inject
    public MediaRepositoryImpl(MediaDao mediaDao, FileUtils fileUtils) {
        this.mediaDao = mediaDao;
        this.fileUtils = fileUtils;
    }

    @Override
    public MediaDto saveMedia(MediaMessageDto mediaMessageDto) {
        MediaEntity entity = new MediaEntity();
        MediaDto media = mediaMessageDto.getMedia();
        entity.id = media.getId() != null ? media.getId() : UuidGenerator.generate();
        entity.type = media.getType();
        entity.localUri = media.getLocalUri();
        entity.fileName = media.getFileName();
        entity.fileSize = media.getFileSize();
        entity.mimeType = media.getMimeType();
        entity.duration = media.getDuration();
        entity.thumbnailUri = media.getThumbnailUri();
        entity.createdAt = media.getCreatedAt() != null ? media.getCreatedAt() : new Date();
        entity.uploadedAt = media.getUploadedAt();

        mediaDao.insertMedia(entity);
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
        return mapEntityToDto(entity);
    }

    @Override
    public List<MediaDto> getMediaForChat(String chatId) {
        List<MediaEntity> entities = mediaDao.getAllMediaForChat(chatId);
        return mapEntityListToDtoList(entities);
    }

    @Override
    public boolean deleteMedia(String mediaId) {
        MediaEntity entity = mediaDao.getMediaById(mediaId);
        mediaDao.deleteMedia(entity);
        return true;
    }

    @Override
    public MediaDto updateLocalUri(String mediaId, String localUri) {
        mediaDao.updateLocalUri(mediaId, localUri);
        return getMediaById(mediaId);
    }

    @Override
    public MediaDto updateThumbnailUri(String mediaId, String thumbnailUri) {
        return getMediaById(mediaId);
    }

    private MediaDto mapEntityToDto(MediaEntity entity) {
        return new MediaDto(
                entity.id,
                entity.type,
                entity.localUri,
                entity.fileName,
                entity.fileSize,
                entity.mimeType,
                entity.duration,
                entity.thumbnailUri,
                entity.createdAt,
                entity.uploadedAt
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
