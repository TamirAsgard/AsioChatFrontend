package com.example.asiochatfrontend.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<List<ChatDto>> chatsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatDto> chatLiveUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsForUserUseCase;
    private boolean showUnreadOnly = false;
    private String currentUserId;
    private List<ChatDto> allChats = new ArrayList<>();

    // In-memory cache for chat data to avoid frequent database queries
    private final Map<String, ChatDto> chatCache = new HashMap<>();

    // In-memory cache for last messages
    private final Map<String, MessageDto> lastMessageCache = new HashMap<>();

    public HomeViewModel(ConnectionManager connectionManager, String userId) {
        this.connectionManager = connectionManager;
        this.getChatsForUserUseCase = new GetChatsForUserUseCase(connectionManager);
        this.currentUserId = userId;

        // Set up observers for real-time updates
        setupChatUpdateObservers();
    }

    private void setupChatUpdateObservers() {
        // Observe chat updates
        ChatUpdateBus.getChatUpdates().observeForever(chat -> {
            if (chat != null) {
                updateChatInList(chat);
            }
        });

        // Observe last message updates
        ChatUpdateBus.getLastMessageUpdates().observeForever(message -> {
            if (message != null && message.getChatId() != null) {
                updateLastMessageForChat(message);
            }
        });

        // Observe unread count updates
        ChatUpdateBus.getUnreadCountUpdates().observeForever(chatId -> {
            if (chatId != null) {
                refreshChatData(chatId);
            }
        });
    }

    private void updateChatInList(ChatDto updatedChat) {
        if (updatedChat == null || updatedChat.getChatId() == null) return;

        List<ChatDto> currentList = new ArrayList<>(allChats);
        boolean chatFound = false;

        // Update in memory cache
        chatCache.put(updatedChat.getChatId(), updatedChat);

        // Check if the chat is already in our list
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getChatId().equals(updatedChat.getChatId())) {
                // Replace the existing chat with updated data
                currentList.set(i, updatedChat);
                chatFound = true;
                break;
            }
        }

        // If it's a new chat, add it to the list
        if (!chatFound) {
            currentList.add(updatedChat);
        }

        // Update our cached list
        allChats = currentList;

        // Filter if needed
        if (showUnreadOnly) {
            filterChats();
        } else {
            chatsLiveData.postValue(currentList);
        }

        // Also post to the individual chat update
        chatLiveUpdate.postValue(updatedChat);
    }

    public void updateLastMessageForChat(MessageDto message) {
        if (message == null || message.getChatId() == null) return;

        // Update last message cache
        lastMessageCache.put(message.getChatId(), message);

        // Refresh the specific chat
        refreshChatData(message.getChatId());
    }

    private void refreshChatData(String chatId) {
        if (chatId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get the latest chat data from database
                ChatDto updatedChat = connectionManager.getChatById(chatId);
                if (updatedChat != null) {
                    updateChatInList(updatedChat);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing chat data for: " + chatId, e);
            }
        });
    }

    public LiveData<List<ChatDto>> getChats() {
        return chatsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        loadChats();
    }

    public void refresh() {
        loadChats();
    }

    public void loadAllChats() {
        showUnreadOnly = false;
        loadChats();
    }

    public void loadUnreadChats() {
        showUnreadOnly = true;
        filterChats();
    }

    private void loadChats() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User ID not set");
            return;
        }

        isLoading.setValue(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ChatDto> chats = getChatsForUserUseCase.execute(currentUserId);
                allChats = chats;

                if (showUnreadOnly) {
                    filterChats();
                } else {
                    chatsLiveData.postValue(chats);
                }

                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error loading chats", e);
                error.postValue("Error loading chats: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    private void filterChats() {
        if (showUnreadOnly) {
            // TODO implement unread chat filtering logic
//            List<ChatDto> unreadChats = allChats.stream()
//                    .filter(chat -> chat.getUnreadCount() > 0)
//                    .collect(Collectors.toList());
            chatsLiveData.postValue(Collections.emptyList());
        } else {
            chatsLiveData.postValue(allChats);
        }
    }

    public MessageDto getLastMessageForChat(String chatId) {
        // Return cached version immediately (can be null)
        MessageDto cached = lastMessageCache.get(chatId);

        if (cached == null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    MessageDto fromDb = ServiceModule.getMessageRepository().getLastMessageForChat(chatId);
                    if (fromDb != null) {
                        lastMessageCache.put(chatId, fromDb);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching last message from DB for chat: " + chatId, e);
                }
            });
        }

        return cached; // Return immediately (could be null if cache missed)
    }

    public MutableLiveData<List<ChatDto>> getChatsLiveData() {
        return chatsLiveData;
    }

    public void pushChatUpdate(ChatDto chatDto) {
        if (chatDto != null) {
            // Update the chat in the cache
            chatCache.put(chatDto.getChatId(), chatDto);
            chatLiveUpdate.postValue(chatDto);
            updateChatInList(chatDto);

            // Fetch the latest message on a background thread
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    MessageDto lastMessage = lastMessageCache.get(chatDto.getChatId());
                    if (lastMessage == null) {
                        lastMessage = ServiceModule.getMessageRepository().getLastMessageForChat(chatDto.getChatId());
                        if (lastMessage != null) {
                            lastMessageCache.put(chatDto.getChatId(), lastMessage);
                        }
                    }

                    // Post update to ChatUpdateBus
                    if (lastMessage != null) {
                        ChatUpdateBus.postLastMessageUpdate(lastMessage);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Failed to fetch last message", e);
                }
            });
        }
    }

    public MutableLiveData<ChatDto> getChatLiveUpdate() {
        return chatLiveUpdate;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatCache.clear();
        lastMessageCache.clear();
    }
}