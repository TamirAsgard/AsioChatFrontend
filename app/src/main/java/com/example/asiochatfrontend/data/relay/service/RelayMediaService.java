package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import javax.inject.Inject;
import okio.Buffer;
import okio.Okio;
import okio.Source;

public class RelayMediaService implements MediaService {

    private static final String TAG = "RelayMediaService";

    private final MediaRepository mediaRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final FileUtils fileUtils;
    private final Gson gson;

    @Inject
    public RelayMediaService(
            MediaRepository mediaRepository,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            FileUtils fileUtils,
            Gson gson
    ) {
        this.mediaRepository = mediaRepository;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.fileUtils = fileUtils;
        this.gson = gson;

        webSocketClient.addListener(event -> {
            if (event.getType() == WebSocketEvent.EventType.MEDIA_UPLOAD) {
                try {
                    MediaMessageDto mediaMessageDto = gson.fromJson(event.getPayload(), MediaMessageDto.class);
                    mediaRepository.saveMedia(mediaMessageDto);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling MEDIA_UPLOAD event", e);
                }
            }
        });
    }

    @Override
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) {
        try {
            mediaRepository.saveMedia(mediaMessageDto);

            // TODO - Implement the logic to upload media to the server
            /*
            // Start uploading in background
            new Thread(() -> {
                try {

                    File mediaFile = new File(localMedia.getLocalUri());
                    MediaDto uploadedMedia = relayApiClient.uploadMedia(mediaFile, inputMessage.getSenderId(), localMedia.getType());
                    if (uploadedMedia != null) {
                        uploadedMedia.setLocalUri(localMedia.getLocalUri());
                        uploadedMedia.setThumbnailUri(localMedia.getThumbnailUri());

                        mediaRepository.saveMedia(uploadedMedia);

                        JsonElement payload = gson.toJsonTree(new MediaMessageDto(message, uploadedMedia));
                        WebSocketEvent event = new WebSocketEvent(
                                WebSocketEvent.EventType.MEDIA_UPLOAD,
                                payload,
                                "media-upload-" + System.currentTimeMillis(),
                                inputMessage.getSenderId()
                        );
                        webSocketClient.sendEvent(event);
                        Log.d(TAG, "Media uploaded and event sent");
                    } else {
                        Log.e(TAG, "Failed to upload media");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading media", e);
                }
            }).start();
             */

            //return new MediaMessageDto(message, localMedia);
            return mediaMessageDto;
        } catch (Exception e) {
            Log.e(TAG, "Error creating media message", e);
            return null;
        }
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        MediaDto media = mediaRepository.getMediaById(mediaId);
        if (media == null) {
            throw new Exception("Media not found");
        }

        return new MediaMessageDto(null, media);
    }

    @Override
    public MediaStreamResultDto getMediaStream(String mediaId) {
        try {
            MediaDto media = mediaRepository.getMediaById(mediaId);
            if (media == null) {
                throw new Exception("Media not found");
            }

            File mediaFile = new File(media.getLocalUri());
            if (!mediaFile.exists()) {
                throw new Exception("Media file not found");
            }

            Source source = Okio.source(mediaFile);
            Buffer buffer = new Buffer();
            buffer.readFrom((InputStream) source);

            return new MediaStreamResultDto(media.getId(), buffer.readByteArray(), media.getMimeType());
        } catch (Exception e) {
            Log.e(TAG, "Error getting media stream", e);
            return null;
        }
    }
}
