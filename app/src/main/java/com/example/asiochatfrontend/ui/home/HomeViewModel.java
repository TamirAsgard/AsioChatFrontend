package com.example.asiochatfrontend.ui.home;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<List<ChatDto>> chatsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsForUserUseCase;
    private boolean showUnreadOnly = false;
    private String currentUserId;
    private List<ChatDto> allChats = new ArrayList<>();

    public HomeViewModel(ConnectionManager connectionManager, String userId) {
        this.connectionManager = connectionManager;
        this.getChatsForUserUseCase = new GetChatsForUserUseCase(connectionManager);
        this.currentUserId = userId;
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
}