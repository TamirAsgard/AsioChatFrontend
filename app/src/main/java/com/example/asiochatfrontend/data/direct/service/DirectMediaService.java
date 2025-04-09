package com.example.asiochatfrontend.data.direct.service;

import android.content.Context;
import android.net.Uri;

import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.domain.repository.MediaRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.inject.Inject;

public class DirectMediaService implements MediaService {

    private final Context context;
    private final MediaRepository mediaRepository;
    private final FileUtils fileUtils;

    @Inject
    public DirectMediaService(Context context, MediaRepository mediaRepository, FileUtils fileUtils) {
        this.context = context;
        this.mediaRepository = mediaRepository;
        this.fileUtils = fileUtils;
    }

    @Override
    public MediaStreamResultDto getMediaStream(String mediaId) {
        MediaDto media = mediaRepository.getMediaById(mediaId);
        if (media == null) {
            return new MediaStreamResultDto(mediaId, null, null);
        }

        try {
            File file = new File(media.getLocalUri());
            if (!file.exists()) {
                return new MediaStreamResultDto(mediaId, null, null);
            }

            Uri uri = fileUtils.getUriForFile(file);
            return new MediaStreamResultDto(mediaId, null, media.getMimeType());
        } catch (Exception e) {
            return new MediaStreamResultDto(mediaId, null, null);
        }
    }

    @Override
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) {
        MediaDto media = mediaRepository.getMediaForMessage(mediaMessageDto.getMessage().getId());
        if (media == null) throw new IllegalArgumentException("Media not found");

        return new MediaMessageDto(mediaMessageDto.getMessage(), media);
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        MediaDto media = mediaRepository.getMediaById(mediaId);
        if (media == null) throw new IllegalArgumentException("Media not found");

        String fileName = UuidGenerator.generate();
        String extension = getExtensionFromMediaType(media.getType());

        File sourceFile = new File(media.getLocalUri());
        if (!sourceFile.exists()) {
            throw new Exception("Media file not found on disk");
        }

        File copiedFile = copyToAppStorage(sourceFile, fileName, extension);
        Uri newUri = fileUtils.getUriForFile(copiedFile);

        MediaDto updatedMedia = new MediaDto(
                media.getId(),
                media.getType(),
                copiedFile.getAbsolutePath(),
                media.getFileName(),
                media.getFileSize(),
                media.getMimeType(),
                media.getDuration(),
                media.getThumbnailUri(),
                media.getCreatedAt(),
                media.getUploadedAt()
        );

        return new MediaMessageDto(null, updatedMedia);
    }

    private File copyToAppStorage(File sourceFile, String fileName, String extension) {
        File targetDir = new File(context.getFilesDir(), "media");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File targetFile = new File(targetDir, fileName + "." + extension);

        try (FileInputStream input = new FileInputStream(sourceFile);
             FileOutputStream output = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sourceFile.delete();
        return targetFile;
    }

    private String getExtensionFromMediaType(MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                return "jpg";
            case VIDEO:
                return "mp4";
            case AUDIO:
                return "m4a";
            case DOCUMENT:
                return "pdf";
            default:
                return "";
        }
    }
}
