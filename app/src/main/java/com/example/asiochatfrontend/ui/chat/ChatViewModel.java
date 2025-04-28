package com.example.asiochatfrontend.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import com.example.asiochatfrontend.domain.usecase.media.CreateMediaMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.media.GetMediaMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.media.GetMediaMessagesUseCase;
import com.example.asiochatfrontend.domain.usecase.message.CreateMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.message.GetMessagesForChatUseCase;
import com.example.asiochatfrontend.domain.usecase.message.ResendFailedMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

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
    private final GetMediaMessagesUseCase getMediaMessagesUseCase;
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
        this.getChatsUseCase = new GetChatsForUserUseCase(connectionManager);
        this.getUserByIdUseCase = new GetUserByIdUseCase(connectionManager);
        this.getMediaMessagesUseCase = new GetMediaMessagesUseCase(connectionManager);
        this.getMediaMessageUseCase = new GetMediaMessageUseCase(connectionManager);
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

    public LiveData<MessageDto> getIncomingMessageLiveData() {
        return this.connectionManager.relayMessageService.getIncomingMessageLiveData();
    }

    public LiveData<MessageDto> getIncomingMediaLiveData() {
        return this.connectionManager.relayMediaService.getIncomingMediaLiveData();
    }

    public LiveData<MessageDto> getOutgoingMessageLiveData() {
        return this.connectionManager.relayMessageService.getOutgoingMessageLiveData();
    }

    public void addIncomingMessage(MessageDto newMessage) {
        if (chatId == null || !chatId.equals(newMessage.getChatId())) return;

        List<MessageDto> current = messages.getValue();
        if (current == null) current = new ArrayList<>();

        List<MessageDto> updated = new ArrayList<>(current);
        updated.add(newMessage);

        messages.postValue(updated);
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
        TextMessageDto messageDto = new TextMessageDto(
                UuidGenerator.generate(),              // id
                new ArrayList<>(participants),         // WaitingMemebersList
                MessageState.PENDING,                  // Status
                null,                                  // timestamp
                currentUserId,                         // jid
                chatId,                                // chatId
                text                                   // payload
        );

        // Immediately add to UI
        addMessageToList(messageDto);

        // Send in background
        new Thread(() -> {
            try {
                MessageDto sentMessage = createMessageUseCase.execute(messageDto);

                // Update message state in local list if needed
                updateMessageInList(sentMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                error.postValue("Failed to send message: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void sendMediaMessage(Uri mediaUri, MediaType mediaType, String caption, String replyToMessageId) {
        if (mediaUri == null) return;
        isLoading.setValue(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FileUtils fileUtils = ServiceModule.getFileUtils();
                File file = fileUtils.getFileFromUri(mediaUri); // Get local file copy
                if (file == null) {
                    file = new File(Objects.requireNonNull(mediaUri.getPath()));
                    if (!file.exists()) {
                        error.postValue("File not found");
                        return;
                    }
                }

                // Construct MediaDto
                MediaDto mediaDto = new MediaDto(
                        null,                        // id (server may assign)
                        file.getName(),                 // file name
                        file,                           // actual file
                        FileUtils.getMimeType(file),    // content type
                        mediaType,                      // image/video/other
                        file.length(),                  // size
                        null,                           // optional thumbnail path
                        false                           // not yet processed
                );

                // Construct MediaMessageDto
                MediaMessageDto mediaMessageDto = new MediaMessageDto(
                        UuidGenerator.generate(),       // message ID
                        participants,                   // waiting members
                        MessageState.PENDING,           // initial state
                        null,                           // timestamp
                        currentUserId,                  // sender
                        chatId,                         // chat Id
                        mediaDto                        // payload
                );

                // Immediately add to UI
                addMessageToList(mediaMessageDto);

                // Upload via use case
                MediaMessageDto sentMessage = createMediaMessageUseCase.execute(mediaMessageDto);
                if (sentMessage != null) {
                    Log.d("MediaSender", "Media message sent with ID: " + sentMessage.getId());

                    // Update message state in local list if needed
                    updateMessageInList(sentMessage);
                } else {
                    throw new Exception("Failed to send media message");
                }

            } catch (Exception e) {
                Log.e("MediaSender", "Error sending media message", e);
            } finally {
                isLoading.postValue(false);
            }
        });
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

    public void markMessagesAsRead() {
        if (chatId == null || chatId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                // Mark all messages in this chat as read by current user
                boolean success = connectionManager.setMessagesInChatReadByUser(chatId, currentUserId);

                if (success) {
                    Log.d(TAG, "All messages marked as read");
                } else {
                    Log.e(TAG, "Failed to mark all messages as read");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking messages as read", e);
            }
        }).start();
    }

    public void markMessageAsRead(String messageId) {
        if (messageId == null || messageId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                // Mark a specific message as read
                connectionManager.setMessageReadByUser(messageId, currentUserId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking message as read: " + messageId, e);
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
                    if (chat.getChatId().equals(chatId)) {
                        // Store participants for use in messaging
                        participants = new ArrayList<>(chat.getRecipients());

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

        new Thread(() -> {
            try {
                List<TextMessageDto> fetchedTextMessages = getMessagesUseCase.execute(chatId);
                List<MediaMessageDto> fetchedMediaMessages = getMediaMessagesUseCase.execute(chatId);
                List<MessageDto> allMessages = new ArrayList<>();
                allMessages.addAll(fetchedTextMessages);
                allMessages.addAll(fetchedMediaMessages);

                // Sort messages by timestamp
                Collections.sort(allMessages, (a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                    if (a.getTimestamp() == null) return -1;
                    if (b.getTimestamp() == null) return 1;
                    return a.getTimestamp().compareTo(b.getTimestamp());
                });

                messages.postValue(allMessages);
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

    private void addMessageToList(MessageDto message) {
        List<MessageDto> currentList = messages.getValue() != null ? messages.getValue() : new ArrayList<>();
        List<MessageDto> updatedList = new ArrayList<>(currentList); // create a new list

        updatedList.add(message);
        messages.postValue(updatedList); // triggers UI refresh
    }

    public void updateMessageInList(MessageDto updatedMessage) {
        List<MessageDto> currentList = messages.getValue() != null ? messages.getValue() : new ArrayList<>();
        List<MessageDto> updatedList = new ArrayList<>(currentList);

        boolean updated = false;
        for (int i = 0; i < updatedList.size(); i++) {
            if (updatedList.get(i).getId().equals(updatedMessage.getId())) {
                updatedList.set(i, updatedMessage);
                updated = true;
                break;
            }
        }

        if (!updated) {
            updatedList.add(updatedMessage);
        }

        messages.postValue(updatedList); // force UI update
    }
}