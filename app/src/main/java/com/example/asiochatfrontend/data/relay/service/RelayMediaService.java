package com.example.asiochatfrontend.data.relay.service;

import android.os.Build;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class RelayMediaService implements MediaService, RelayWebSocketClient.RelayWebSocketListener {

    private static final String TAG = "RelayMediaService";

    private final MediaRepository mediaRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final FileUtils fileUtils;
    private final String currentUserId;
    private final Gson gson;

    private final MutableLiveData<MessageDto> incomingMediaLiveData = new MutableLiveData<>();
    private final MutableLiveData<MessageDto> outgoingMediaLiveData = new MutableLiveData<>();


    @Inject
    public RelayMediaService(
            MediaRepository mediaRepository,
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            FileUtils fileUtils,
            String currentUserId,
            Gson gson
    ) {
        this.mediaRepository = mediaRepository;
        this.messageRepository = messageRepository;
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
            // Generate message ID if not provided
            if (mediaMessageDto.getId() == null) {
                mediaMessageDto.setId(UUID.randomUUID().toString());
            }

            // Set message defaults
            if (mediaMessageDto.getStatus() == null) {
                mediaMessageDto.setStatus(MessageState.PENDING);
            }

            // Offline mode, save message in repo on pending
            if (!ServiceModule.getConnectionManager().isOnline()) {
                // Support both old and new version for timestamp utc now
                Date nowUtcDate;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // take the LOCAL date-time, then reinterpret it as UTC
                    LocalDateTime localNow = LocalDateTime.now();
                    Instant fakeUtc = localNow.toInstant(ZoneOffset.UTC);
                    nowUtcDate = Date.from(fakeUtc);
                } else {
                    // on older devices: get local millis, subtract the offset to zero it
                    Calendar cal = Calendar.getInstance();         // local time
                    long localMillis = cal.getTimeInMillis();
                    int offset     = cal.getTimeZone().getOffset(localMillis);
                    long fakeUtcMs = localMillis - offset;         // now treat that moment as UTC
                    nowUtcDate     = new Date(fakeUtcMs);
                }

                mediaMessageDto.setTimestamp(nowUtcDate);

                // Save in local repository
                mediaRepository.saveMedia(mediaMessageDto);
                return mediaMessageDto;
            }

            // Save in local repository as sent and no timestamp (server will set it)
            mediaMessageDto.setStatus(MessageState.SENT);
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
            List<MediaMessageDto> localMessages = mediaRepository.getMediaForChat(chatId);

            if (!ServiceModule.getConnectionManager().isOnline()) {
                // Offline mode, get messages from local storage
                return localMessages;
            }

            // First try to get from backend
            List<MediaMessageDto> remoteMessages = relayApiClient.getMediaMessagesForChat(chatId);
            List<MediaMessageDto> chatMessages = new ArrayList<>();

            if (remoteMessages != null && !remoteMessages.isEmpty()) {
                // Save to local repository
                for (MediaMessageDto message : remoteMessages) {
                    // Check if the message already exists in the local repository
                    mediaRepository.saveMedia(message);
                    chatMessages.add(message);
                }

                return chatMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages from server, using local cache", e);
        }

        // Fallback to local repository
        return mediaRepository.getMediaForChat(chatId);
    }

    @Override
    public List<MessageDto> sendPendingMessages() {
        List<MediaMessageDto> pendingMessages = mediaRepository.getPendingMessages();
        List<MessageDto> updatedMessages = new ArrayList<>();
        pendingMessages.forEach((pendingMessage) -> {
            try {
                // Send message and add to updated messages list
                updatedMessages.add(createMediaMessage(pendingMessage));
                Log.i(TAG, "Send pending message: " + pendingMessage.getId()
                        + " for chatId: " + pendingMessage.getChatId());
            } catch (Exception e) {
                Log.e(TAG, "Failed to send pending message: " + pendingMessage.getId());
            }
        });

        return updatedMessages;
    }

    public boolean setMessageReadByUser(String messageId, String userId, String readBy) {
        // Send read event to webSocket
        // Create the message read payload
        JsonObject readPayload = new JsonObject();
        readPayload.addProperty("messageId", messageId);
        readPayload.addProperty("sendBy", readBy);
        readPayload.addProperty("readBy", userId);

        // Create the WebSocket event
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.MESSAGE_READ,
                readPayload,
                userId
        );

        // Send the event through the WebSocket client
        webSocketClient.sendEvent(event);
        return true;
    }

    public boolean setMessagesInChatReadByUser(String chatId, String userId) {
        try {
            List<MediaMessageDto> messages = mediaRepository.getMediaForChat(chatId);
            List<MediaMessageDto> remoteMessages = relayApiClient.getMediaMessagesForChat(chatId);

            boolean success = true;

            for (MessageDto message : messages) {
                // Check if message state is 'SENT' but waiting members list is empty
                if (message.getWaitingMemebersList() == null || message.getWaitingMemebersList().isEmpty()) {
                    if(message.getStatus().equals(MessageState.SENT)) {
                        message.setStatus(MessageState.READ);
                        mediaRepository.updateMessage(message);
                    }
                }

                // or waiting members does not contain userId
                if (message.getJid().equals(userId) || !message.getWaitingMemebersList().contains(userId)) {
                    continue;
                } else {
                    MediaMessageDto remoteMessage = remoteMessages.stream()
                            .filter(m -> m.getId().equals(message.getId()))
                            .findFirst()
                            .orElse(null);

                    if (message.getStatus() == MessageState.READ) {
                        // validate message is set on READ in backend
                        if (remoteMessage != null && remoteMessage.getStatus() == MessageState.READ) {
                            continue;
                        }
                    } else {
                        // backend message is set on READ, local message is not
                        if (remoteMessage != null && remoteMessage.getStatus() == MessageState.READ) {
                            message.setStatus(remoteMessage.getStatus());
                            List<String> waitingMembersList = new ArrayList<>(message.getWaitingMemebersList());
                            if (waitingMembersList != null) {
                                waitingMembersList.remove(userId);
                            }
                            message.setWaitingMemebersList(waitingMembersList);
                            mediaRepository.updateMessage(message);
                            continue;
                        }
                    }
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
            incomingMediaLiveData.postValue(message);
            ChatUpdateBus.postLastMessageUpdate(message);

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String chatId = message.getChatId();
                    int textUnread = messageRepository.getUnreadMessagesCount(chatId, currentUserId);
                    int mediaUnread = mediaRepository.getUnreadMessagesCount(chatId, currentUserId);
                    ChatUpdateBus.postUnreadCountUpdate(chatId, textUnread + mediaUnread);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get media unread count", e);
                }
            });
        } catch (Exception e) {
            // Message might be null or invalid for media
        }
    }

    public MutableLiveData<MessageDto> getIncomingMediaLiveData() {
        return incomingMediaLiveData;
    }

    public MutableLiveData<MessageDto> getOutgoingMediaLiveData() {
        return outgoingMediaLiveData;
    }

    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            MediaEntity mediaEntity = mediaRepository.getMediaEntityByMessageId(messageId);
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
            outgoingMediaLiveData.postValue(mediaMessageDto);
            Log.d(TAG, "Message marked as read: " + messageId + " by user: " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking message as read", e);
            return false;
        }
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) {
        return mediaRepository.getUnreadMessagesCount(chatId, userId);
    }

    @Override
    public MessageDto getMediaByMessageId(String lastMessageId) {
        return mediaRepository.getMediaMessageById(lastMessageId);
    }
}
