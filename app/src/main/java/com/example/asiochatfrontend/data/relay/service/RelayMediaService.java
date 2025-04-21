package com.example.asiochatfrontend.data.relay.service;

import android.os.Build;
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
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;

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
            if (event.getType() == WebSocketEvent.EventType.CHAT) {
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

            File mediaFile = mediaMessageDto.getPayload().getFile();
            if (mediaFile == null || !mediaFile.exists()) {
                Log.e(TAG, "Media file is missing");
                return null;
            }

            byte[] fileBytes = FileUtils.readFileToByteArray(mediaFile);
            String base64Data = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                base64Data = Base64.getEncoder().encodeToString(fileBytes);
            }

            JsonObject rootPayload = new JsonObject();
            rootPayload.addProperty("id", mediaMessageDto.getId());
            rootPayload.addProperty("jid", mediaMessageDto.getJid());
            rootPayload.addProperty("chatId", mediaMessageDto.getChatId());

            if (mediaMessageDto.getTimestamp() != null) {
                rootPayload.addProperty("timestamp", mediaMessageDto.getTimestamp().getTime());
            }

            if (mediaMessageDto.getWaitingMemebersList() != null) {
                rootPayload.add("waitingMemebersList", gson.toJsonTree(mediaMessageDto.getWaitingMemebersList()));
            }

            // Media Payload (matches .NET MediaDto expected structure)
            JsonObject mediaPayload = new JsonObject();
            mediaPayload.addProperty("name", mediaMessageDto.getPayload().getFileName());
            mediaPayload.addProperty("type", mediaMessageDto.getPayload().getContentType());
            mediaPayload.addProperty("data", base64Data);

            // This entire payload will go under 'payload'
            rootPayload.add("payload", mediaPayload);

            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.CHAT,
                    rootPayload,
                    mediaMessageDto.getJid()
            );

            webSocketClient.sendEvent(event);
            Log.i(TAG, "📎 Media message sent: " + mediaMessageDto.getId());

            mediaMessageDto.setStatus(MessageState.SENT);
            mediaRepository.saveMedia(mediaMessageDto);

            ChatUpdateBus.postLastMessageUpdate(mediaMessageDto);
            return mediaMessageDto;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending media message", e);
            return null;
        }
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        MediaDto media = mediaRepository.getMediaById(mediaId);
        if (media == null) {
            throw new Exception("Media not found");
        }

        return null;
    }

    @Override
    public MediaStreamResultDto getMediaStream(String messageId) {
        try {
            MediaDto media = mediaRepository.getMediaById(messageId);
            if (media == null) {
                throw new Exception("Media not found");
            }

            File mediaFile = new File(media.getFileName());
            if (!mediaFile.exists()) {
                throw new Exception("Media file not found");
            }

            Source source = Okio.source(mediaFile);
            Buffer buffer = new Buffer();
            buffer.readFrom((InputStream) source);

            InputStream stream = new FileInputStream(mediaFile);
            // TODO Implement
            return new MediaStreamResultDto();
        } catch (Exception e) {
            Log.e(TAG, "Error getting media stream", e);
            return null;
        }
    }
}
