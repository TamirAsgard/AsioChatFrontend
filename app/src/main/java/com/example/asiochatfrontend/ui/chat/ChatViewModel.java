package com.example.asiochatfrontend.ui.chat;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import com.example.asiochatfrontend.domain.usecase.media.CreateMediaMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.media.GetMediaMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.message.CreateMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.message.GetMessagesForChatUseCase;
import com.example.asiochatfrontend.domain.usecase.message.ResendFailedMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatViewModel extends ViewModel {
    private static final String TAG = "ChatViewModel";

    private final MutableLiveData<List<MessageDto>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatDto> chatData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<MediaDto> selectedMedia = new MutableLiveData<>();
    private final MutableLiveData<List<UserDto>> chatMembers = new MutableLiveData<>(new ArrayList<>());

    private final ConnectionManager connectionManager;
    private final CreateMessageUseCase createMessageUseCase;
    private final GetMessagesForChatUseCase getMessagesUseCase;
    private final ResendFailedMessageUseCase resendFailedMessageUseCase;
    private final CreateMediaMessageUseCase createMediaMessageUseCase;
    private final GetMediaMessageUseCase getMediaMessageUseCase;
    private final GetChatsForUserUseCase getChatsUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    private String chatId;
    private String currentUserId;
    private List<String> participants = new ArrayList<>();

    @Inject
    public ChatViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.createMessageUseCase = new CreateMessageUseCase(connectionManager);
        this.getMessagesUseCase = new GetMessagesForChatUseCase(connectionManager);
        this.resendFailedMessageUseCase = new ResendFailedMessageUseCase(connectionManager);
        this.createMediaMessageUseCase = new CreateMediaMessageUseCase(connectionManager);
        this.getMediaMessageUseCase = new GetMediaMessageUseCase(connectionManager);
        this.getChatsUseCase = new GetChatsForUserUseCase(connectionManager);
        this.getUserByIdUseCase = new GetUserByIdUseCase(connectionManager);
    }

    public void initialize(String chatId, String currentUserId) {
        this.chatId = chatId;
        this.currentUserId = currentUserId;

        // Set current user in the connection manager
        connectionManager.setCurrentUser(currentUserId);

        // Load chat data and messages
        loadChatData();
        loadMessages();
    }

    public LiveData<List<MessageDto>> getMessages() {
        return messages;
    }

    public LiveData<ChatDto> getChatData() {
        return chatData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<MediaDto> getSelectedMedia() {
        return selectedMedia;
    }

    public LiveData<List<UserDto>> getChatMembers() {
        return chatMembers;
    }

    public void refresh() {
        loadMessages();
        loadChatMembers();
    }

    public void switchConnectionMode(boolean directMode) {
        ConnectionMode newMode = directMode ? ConnectionMode.DIRECT : ConnectionMode.RELAY;
        connectionManager.setConnectionMode(newMode);
        refresh();
    }

    public void sendTextMessage(String text, String replyToMessageId) {
        if (text.isEmpty()) {
            return;
        }

        isLoading.setValue(true);

        // Create message DTO
        MessageDto messageDto = new MessageDto(
                UuidGenerator.generate(),
                chatId,
                currentUserId,
                text,
                null, // No media
                replyToMessageId,
                MessageState.PENDING,
                new ArrayList<>(participants), // Include all participants as waiting members
                new Date(),
                null,
                null
        );

        // Send in background
        new Thread(() -> {
            try {
                MessageDto sentMessage = createMessageUseCase.execute(messageDto);
                loadMessages(); // Refresh to show the new message
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                error.postValue("Failed to send message: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    // TODO Implement media message send
    public void sendMediaMessage(Uri mediaUri, MediaType mediaType, String caption, String replyToMessageId) {
        if (mediaUri == null) {
            return;
        }

        isLoading.setValue(true);
    }

    public void resendMessage(String messageId) {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                boolean success = resendFailedMessageUseCase.execute(messageId);
                loadMessages(); // Refresh to update message status
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error resending message", e);
                error.postValue("Error resending message: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void openMedia(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) {
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                MediaMessageDto mediaMessage = getMediaMessageUseCase.execute(mediaId);
                if (mediaMessage != null && mediaMessage.getMedia() != null) {
                    selectedMedia.postValue(mediaMessage.getMedia());
                } else {
                    error.postValue("Media not found");
                }
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error opening media", e);
                error.postValue("Error opening media: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void markMessagesAsRead() {
        if (chatId == null || chatId.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                connectionManager.setMessagesInChatReadByUser(chatId, currentUserId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking messages as read", e);
            }
        }).start();
    }

    private void loadChatData() {
        if (chatId == null || chatId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("Invalid chat or user ID");
            return;
        }

        new Thread(() -> {
            try {
                List<ChatDto> chats = getChatsUseCase.execute(currentUserId);
                for (ChatDto chat : chats) {
                    if (chat.getId().equals(chatId)) {
                        // Store participants for use in messaging
                        participants = new ArrayList<>(chat.getParticipants());

                        // Remove current user from participants when sending messages
                        participants.remove(currentUserId);

                        chatData.postValue(chat);

                        // Load chat members
                        loadChatMembers();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading chat data", e);
                error.postValue("Error loading chat data: " + e.getMessage());
            }
        }).start();
    }

    private void loadChatMembers() {
        if (participants == null || participants.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                List<UserDto> members = new ArrayList<>();
                for (String userId : participants) {
                    try {
                        UserDto user = getUserByIdUseCase.execute(userId);
                        if (user != null) {
                            members.add(user);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading user " + userId, e);
                    }
                }
                chatMembers.postValue(members);
            } catch (Exception e) {
                Log.e(TAG, "Error loading chat members", e);
            }
        }).start();
    }

    private void loadMessages() {
        if (chatId == null || chatId.isEmpty()) {
            error.setValue("Invalid chat ID");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                List<MessageDto> fetchedMessages = getMessagesUseCase.execute(chatId);

                // Sort messages by timestamp
                Collections.sort(fetchedMessages, (a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return -1;
                    if (b.getCreatedAt() == null) return 1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                });

                messages.postValue(fetchedMessages);
                isLoading.postValue(false);

                // Mark messages as read
                markMessagesAsRead();
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages", e);
                error.postValue("Error loading messages: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }
}