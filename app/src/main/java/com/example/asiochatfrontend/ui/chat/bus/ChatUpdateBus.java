package com.example.asiochatfrontend.ui.chat.bus;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

/**
 * Enhanced event bus for chat-related updates across the app
 */
@Singleton
public class ChatUpdateBus {
    // Chat updates for when a chat is created, modified, or needs UI refresh
    private static final MutableLiveData<ChatDto> chatUpdateLiveData = new MutableLiveData<>();

    // Last message updates to trigger chat list refresh
    private static final MutableLiveData<MessageDto> lastMessageUpdateLiveData = new MutableLiveData<>();

    // Unread count updates mapped by chatId
    private static final MutableLiveData<Map<String, Integer>> unreadCountsLiveData = new MutableLiveData<>(new HashMap<>());

    /**
     * Post a chat update that will be broadcast to all observers
     */
    public static void postChatUpdate(ChatDto chatDto) {
        chatUpdateLiveData.postValue(chatDto);
    }

    /**
     * Post a message that should update the last message for a chat
     */
    public static void postLastMessageUpdate(MessageDto messageDto) {
        lastMessageUpdateLiveData.postValue(messageDto);
    }

    /**
     * Post a notification that unread counts have changed for a specific chat
     */
    public static void postUnreadCountUpdate(String chatId, int unreadCount) {
        Map<String, Integer> current = unreadCountsLiveData.getValue();
        if (current == null) current = new HashMap<>();
        current.put(chatId, unreadCount);
        unreadCountsLiveData.postValue(current);    }

    /**
     * Get the LiveData for observing chat updates
     */
    public static LiveData<ChatDto> getChatUpdates() {
        return chatUpdateLiveData;
    }

    /**
     * Get the LiveData for observing last message updates
     */
    public static LiveData<MessageDto> getLastMessageUpdates() {
        return lastMessageUpdateLiveData;
    }

    /**
     * Get the LiveData for observing unread count updates
     */
    public static LiveData<Map<String, Integer>> getUnreadCountUpdates() {
        return unreadCountsLiveData;
    }
}