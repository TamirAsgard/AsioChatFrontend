package com.example.asiochatfrontend.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import com.example.asiochatfrontend.domain.usecase.media.GetMediaMessagesUseCase;
import com.example.asiochatfrontend.domain.usecase.message.GetMessagesForChatUseCase;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<List<ChatDto>> chatsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatDto> chatLiveUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsForUserUseCase;
    private final GetMessagesForChatUseCase getMessagesForChatUseCase;
    private final GetMediaMessagesUseCase getMediaMessagesUseCase;
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
        this.getMessagesForChatUseCase = new GetMessagesForChatUseCase(connectionManager);
        this.getMediaMessagesUseCase = new GetMediaMessagesUseCase(connectionManager);
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
        ChatUpdateBus.getUnreadCountUpdates().observeForever(chatUnreadCountUpdate -> {
            if (chatUnreadCountUpdate != null) {
                refresh();
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
            error.postValue("User ID not set");
            return;
        }

        isLoading.postValue(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try
            {
                // Fetch chats from the database
                List<ChatDto> chats = getChatsForUserUseCase.execute(currentUserId);
                allChats = chats;

                //
                allChats.forEach(chat -> {
                    try {
                        getMessagesForChatUseCase.execute(chat.getChatId());
                        getMediaMessagesUseCase.execute(chat.getChatId());
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching messages for chat: " + chat.getChatId(), e);
                        error.postValue("Error fetching messages for chat: " + chat.getChatId());
                    }
                });
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

    public void filterChats() {
        if (showUnreadOnly) {
            Map<String, Integer> unreadCountUpdates = ChatUpdateBus.getUnreadCountUpdates().getValue();
            List<ChatDto> filteredChats = new ArrayList<>();

            for (String chatId : unreadCountUpdates.keySet()) {
                if (unreadCountUpdates.getOrDefault(chatId, 0) > 0) {
                    ChatDto chat = allChats.stream()
                            .filter(chatDto -> chatDto.getChatId().equals(chatId))
                            .findFirst()
                            .orElse(null);
                    if (chat != null) {
                        filteredChats.add(chat);
                    }
                }
            }

            chatsLiveData.postValue(filteredChats);
        } else {
            chatsLiveData.postValue(allChats);
        }
    }

    public void filterChats(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1) Build the unread list if needed
            Map<String, Integer> unreadMap = null;
            List<ChatDto> unreadChats = null;
            if (showUnreadOnly) {
                unreadMap   = ChatUpdateBus.getUnreadCountUpdates().getValue();
                unreadChats = new ArrayList<>();
                if (unreadMap != null) {
                    for (String id : unreadMap.keySet()) {
                        if (unreadMap.getOrDefault(id, 0) > 0) {
                            allChats.stream()
                                    .filter(c -> c.getChatId().equals(id))
                                    .findFirst()
                                    .ifPresent(unreadChats::add);
                        }
                    }
                }
            }

            // 2) No query → just unread or all
            if (query == null || query.trim().isEmpty()) {
                if (showUnreadOnly && unreadChats != null) {
                    chatsLiveData.postValue(unreadChats);
                } else {
                    chatsLiveData.postValue(allChats);
                }
                return;
            }

            // 3) We have a query → always build 'matches' against ALL chats
            String q = query.toLowerCase();
            List<ChatDto> matches = allChats.stream()
                    .filter(chat -> {
                        // pick the display name
                        String name = chat.getGroup()
                                ? chat.getChatName()
                                : chat.getRecipients().stream()
                                .filter(id -> !id.equals(currentUserId))
                                .findFirst().orElse("");
                        return isSubsequence(q, name.toLowerCase());
                    })
                    .collect(Collectors.toList());

            List<ChatDto> result;
            if (showUnreadOnly && unreadChats != null) {
                // union of matches + unreadChats, preserving order and eliminating dupes
                LinkedHashMap<String,ChatDto> map = new LinkedHashMap<>();
                for (ChatDto c: matches)     map.put(c.getChatId(), c);
                for (ChatDto c: unreadChats) map.put(c.getChatId(), c);
                result = new ArrayList<>(map.values());
            } else {
                result = matches;
            }

            chatsLiveData.postValue(result);
        });
    }

    /** helper: is `pattern` a subsequence of `text`? */
    private boolean isSubsequence(String pattern, String text) {
        int i = 0, j = 0;
        while (i < pattern.length() && j < text.length()) {
            if (pattern.charAt(i) == text.charAt(j)) i++;
            j++;
        }
        return i == pattern.length();
    }

    public MessageDto getLastMessageForChat(String chatId) {
        // First, check if there's a value in the ChatUpdateBus that's newer
        Map<String, MessageDto> latestMessages = ChatUpdateBus.getLatestMessagesMap().getValue();
        if (latestMessages != null && latestMessages.containsKey(chatId)) {
            MessageDto latestMessage = latestMessages.get(chatId);
            // Update our local cache with this message
            lastMessageCache.put(chatId, latestMessage);
            return latestMessage;
        }

        MessageDto cached = lastMessageCache.get(chatId);
        if (cached != null) return cached;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<MessageDto> future = executor.submit(() -> {
            try {
                MessageDto textMessage = ServiceModule.getMessageRepository().getLastMessageForChat(chatId);
                MessageDto mediaMessage = ServiceModule.getMediaRepository().getLastMessageForChat(chatId);

                // Pick latest by timestamp
                MessageDto result = null;
                if (textMessage == null) result = mediaMessage;
                else if (mediaMessage == null) result = textMessage;
                else result = textMessage.getTimestamp().after(mediaMessage.getTimestamp()) ?
                            textMessage : mediaMessage;

                // Update cache with this message
                if (result != null) {
                    lastMessageCache.put(chatId, result);
                    // Also update the bus
                    ChatUpdateBus.postLastMessageUpdate(result);
                }
                return result;
            } catch (Exception e) {
                Log.e(TAG, "Error fetching last message from DB", e);
                return null;
            }
        });

        try {
            return future.get(1, TimeUnit.SECONDS); // Add timeout to prevent blocking too long
        } catch (Exception e) {
            Log.e(TAG, "Future failed", e);
            return null;
        } finally {
            executor.shutdown();
        }
    }

    public int getUnreadMessageCountForChat(String chatId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Get text message unread count
            // Add timeout to prevent blocking indefinitely

            return executor.submit(() -> {
                try {
                    // Get text message unread count
                    return connectionManager.getUnreadMessagesCount(chatId, currentUserId);
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching unread message count", e);
                    return 0;
                }
            }).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get unread count: " + e.getMessage());
            return 0;
        } finally {
            executor.shutdownNow();
        }
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

    public void clearLastMessageCache() {
        lastMessageCache.clear();

        // Also reload chat list to ensure it's up to date
        if (currentUserId != null && !currentUserId.isEmpty()) {
            loadChats();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatCache.clear();
        lastMessageCache.clear();
    }
}