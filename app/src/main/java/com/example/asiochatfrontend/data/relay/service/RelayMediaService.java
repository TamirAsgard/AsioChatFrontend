package com.example.asiochatfrontend.data.relay.service;

import android.os.Build;
import android.util.Log;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class RelayMediaService implements MediaService, RelayWebSocketClient.RelayWebSocketListener {

    private static final String TAG = "RelayMediaService";

    private final MediaRepository mediaRepository;
    private final ChatRepository chatRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final FileUtils fileUtils;
    private final String currentUserId;
    private final Gson gson;

    @Inject
    public RelayMediaService(
            MediaRepository mediaRepository,
            ChatRepository chatRepository,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            FileUtils fileUtils,
            String currentUserId,
            Gson gson
    ) {
        this.mediaRepository = mediaRepository;
        this.chatRepository = chatRepository;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.fileUtils = fileUtils;
        this.currentUserId = currentUserId;
        this.gson = gson;

        webSocketClient.addListener(this);
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
                JsonArray waitingMembers = new JsonArray();
                for (String member : mediaMessageDto.getWaitingMemebersList()) {
                    waitingMembers.add(member);
                }
                rootPayload.add("waitingMemebersList", waitingMembers);
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
            MediaDto media = mediaRepository.getMediaForMessage(messageId);

            if (media == null) {
                Log.e("RelayMediaService", "Media not found for message ID: " + messageId);
                return null;
            }

            MediaEntity extraDetailsEntity = mediaRepository.getMediaEntityById(media.getId());
            String mediaId = media.getId();
            if (extraDetailsEntity == null) {
                Log.e("RelayMediaService", "MediaEntity not found for media ID: " + mediaId);
                return null;
            }

            // Check if the file exists in the app storage
            String localUri = extraDetailsEntity.getLocalUri();
            if (localUri != null) {
                File localFile = new File(localUri);
                if (localFile.exists()) {
                    try {
                        InputStream stream = new FileInputStream(localFile);
                        return new MediaStreamResultDto(
                                stream,
                                localFile.getName(),
                                media.getContentType() != null ? media.getContentType() : "application/octet-stream",
                                localFile.getAbsolutePath()
                        );
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Local file found but could not open InputStream", e);
                    }
                }
            }

            // File not found in app storage, try to download it from server with messageId
            MediaStreamResultDto mediaStreamResultDto =
                    relayApiClient.getMediaStream(extraDetailsEntity.getMessageId());

            if (mediaStreamResultDto == null) {
                Log.e(TAG, "Failed to get media stream from server");
                return null;
            }

            // Save the media stream to app storage
            String fileName = mediaStreamResultDto.getFileName();
            String extension = fileUtils.getExtensionFromFileName(fileName);
            File targetFile = fileUtils.copyToAppStorage(mediaStreamResultDto.getStream(), fileName);

            if (targetFile != null) {
                // Update the local URI in the MediaEntity
                extraDetailsEntity.setLocalUri(targetFile.getAbsolutePath());

                MediaMessageDto mediaMessageDto = new MediaMessageDto();
                mediaMessageDto.setId(messageId);
                mediaMessageDto.setPayload(new MediaDto());
                mediaMessageDto.getPayload().setId(mediaId);
                mediaMessageDto.getPayload().setFileName(targetFile.getName());
                mediaMessageDto.getPayload().setFile(targetFile);
                mediaRepository.saveMedia(mediaMessageDto);

                // Return the media stream result with the new local file
                return new MediaStreamResultDto(
                        new FileInputStream(targetFile),
                        targetFile.getName(),
                        media.getContentType() != null ? media.getContentType() : "application/octet-stream",
                        targetFile.getAbsolutePath()
                );
            } else {
                Log.e(TAG, "Failed to save media stream to app storage");
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting media stream", e);
            return null;
        }
    }

    @Override
    public List<MediaMessageDto> getMediaMessagesForChat(String chatId) {
        try {
            // First try to get from backend
            List<MediaMessageDto> remoteMessages = relayApiClient.getMediaMessagesForChat(chatId);
            if (remoteMessages != null && !remoteMessages.isEmpty()) {
                // Save to local repository
                for (MediaMessageDto message : remoteMessages) {
                    // Check if the message already exists in the local repository
                    mediaRepository.saveMedia(message);
                }

                return remoteMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages from server, using local cache", e);
        }

        // Fallback to local repository
        return mediaRepository.getMediaForChat(chatId);
    }

    public boolean setMessageReadByUser(String messageId, String userId) {
        MediaEntity mediaEntity = mediaRepository.getMediaEntityById(messageId);

        // Send read event to webSocket
        // Create the message read payload
        JsonObject readPayload = new JsonObject();
        readPayload.addProperty("messageId", messageId);
        readPayload.addProperty("sendBy", mediaEntity.getSenderId());
        readPayload.addProperty("readBy", userId);

        // Create the WebSocket event
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.MESSAGE_READ,
                readPayload,
                userId
        );

        // Send the event through the WebSocket client
        webSocketClient.sendEvent(event);
        Log.d(TAG, "Media read event sent: message " + messageId +
                " sent by " + mediaEntity.getSenderId() + " was read by " + userId);

        return true;
    }

    public boolean setMessagesInChatReadByUser(String chatId, String userId) {
        try {
            List<MediaMessageDto> messages = mediaRepository.getMediaForChat(chatId);
            boolean success = true;

            for (MessageDto message : messages) {
                // Skip messages from the current user or already read
                // or waiting members does not contain userId
                if (message.getJid().equals(userId)
                        || message.getStatus() == MessageState.READ
                        || !message.getWaitingMemebersList().contains(userId)) {
                    continue;
                }

                // Send read event to webSocket
                // Create the message read payload
                JsonObject readPayload = new JsonObject();
                readPayload.addProperty("messageId", message.getId());
                readPayload.addProperty("sendBy", message.getJid());
                readPayload.addProperty("readBy", userId);

                // Create the WebSocket event
                WebSocketEvent event = new WebSocketEvent(
                        WebSocketEvent.EventType.MESSAGE_READ,
                        readPayload,
                        userId
                );

                // Send the event through the WebSocket client
                webSocketClient.sendEvent(event);
                Log.d(TAG, "Message read event sent: message " + message.getId() +
                        " sent by " + message.getJid() + " was read by " + userId);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error marking all messages as read", e);
            return false;
        }
    }

    @Override
    public void onEvent(WebSocketEvent event) {
        try {
            Log.i("TAG", "WebSocket event received: " + event.toString());
            switch (event.getType()) {
                case INCOMING:
                    handleIncomingMedia(event);
                    break;
                case MESSAGE_READ:
                    // Handle message read event
                    MessageReadByDto readByDto = gson.fromJson(event.getPayload(), MessageReadByDto.class);
                    markMessageAsRead(readByDto.getMessageId(), readByDto.getReadBy());
                    break;
                default:
                    Log.d(TAG, "Unhandled WebSocket event type: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
        }
    }

    private void handleIncomingMedia(WebSocketEvent event) {
        try {
            if (event.getPayload() == null) {
                Log.e(TAG, "Received null payload in WebSocket event");
                return;
            }
            MediaMessageDto message = gson.fromJson(event.getPayload(), MediaMessageDto.class);
            if (message == null) return;

            // Skip messages from self
            if (currentUserId != null && currentUserId.equals(message.getJid())) {
                return;
            }

            Log.d(TAG, "Received media via WebSocket: " + message.getId() + " for chat: " + message.getChatId());

            // Process message
            message.setStatus(message.getStatus());
            if (message.getTimestamp() == null) {
                message.setTimestamp(new Date());
            }

            // Save to repository
            mediaRepository.saveMedia((MediaMessageDto) message);

            // Update chat's last message
            if (message.getChatId() != null) {
                chatRepository.updateLastMessage(message.getChatId(), message.getId());
            }

            // Add message to LiveData for real-time display
            // incomingMessageLiveData.postValue(message);
            ChatUpdateBus.postLastMessageUpdate(message);

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    int unreadMessagesCount = mediaRepository.getUnreadMessagesCount(message.getChatId(), currentUserId);
                    int currentUnreadCountsForChat = unreadMessagesCount;
                    if (!Objects.isNull(ChatUpdateBus.getUnreadCountUpdates())) {
                        currentUnreadCountsForChat +=
                                ChatUpdateBus
                                .getUnreadCountUpdates()
                                .getValue()
                                .getOrDefault(message.getChatId(), 0);
                        }

                    ChatUpdateBus.postUnreadCountUpdate(message.getChatId(), currentUnreadCountsForChat);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get unread count", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming message", e);
        }
    }

    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            MediaEntity mediaEntity = mediaRepository.getMediaEntityById(messageId);
            MediaMessageDto mediaMessageDto = new MediaMessageDto();

            if (mediaEntity == null) return false;

            // Skip if this is your own message
            if (currentUserId != null && currentUserId.equals(userId)) {
                return true;
            }

            List<String> waitingMembersList = new ArrayList<>(mediaEntity.getWaitingMembersList());
            waitingMembersList.remove(userId);
            mediaEntity.setWaitingMembersList(waitingMembersList);

            // Update local status
            if (mediaEntity.getWaitingMembersList().isEmpty()) {
                mediaEntity.setState(MessageState.READ);
            }

            mediaMessageDto.setId(messageId);
            mediaMessageDto.setJid(mediaEntity.getSenderId());
            mediaMessageDto.setChatId(mediaEntity.getChatId());
            mediaMessageDto.setStatus(mediaEntity.getState());
            mediaMessageDto.setWaitingMemebersList(mediaEntity.getWaitingMembersList());
            mediaMessageDto.setTimestamp(mediaEntity.getCreatedAt());
            mediaRepository.saveMedia(mediaMessageDto);

            // Update LiveData
            ChatUpdateBus.postLastMessageUpdate(mediaMessageDto);
            Log.d(TAG, "Message marked as read: " + messageId + " by user: " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking message as read", e);
            return false;
        }
    }
}
