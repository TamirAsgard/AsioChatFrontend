package com.example.asiochatfrontend.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<List<ChatDto>> chats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsUseCase;

    private String currentUserId;
    private boolean showUnreadOnly = false;

    @Inject
    public HomeViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.getChatsUseCase = new GetChatsForUserUseCase(connectionManager);

        // Get the current user ID from the connection manager
        // In a real implementation, this should be retrieved from a user repository or session manager
        this.currentUserId = "current_user_id"; // This will be replaced by the actual user ID
    }

    public LiveData<List<ChatDto>> getChats() {
        return chats;
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
        loadChats();
    }

    private void loadChats() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User ID not set");
            return;
        }

        isLoading.setValue(true);
        new Thread(() -> {
            try {
                List<ChatDto> fetchedChats = getChatsUseCase.execute(currentUserId);

                // Filter for unread chats if necessary
                if (showUnreadOnly) {
                    fetchedChats = fetchedChats.stream()
                            .filter(chat -> chat.getUnreadCount() > 0)
                            .collect(Collectors.toList());
                }

                // Sort chats by last message time (most recent first)
                fetchedChats.sort((a, b) -> {
                    if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                    if (a.getUpdatedAt() == null) return 1;
                    if (b.getUpdatedAt() == null) return -1;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                });

                chats.postValue(fetchedChats);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error loading chats", e);
                error.postValue("Failed to load chats: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }
}