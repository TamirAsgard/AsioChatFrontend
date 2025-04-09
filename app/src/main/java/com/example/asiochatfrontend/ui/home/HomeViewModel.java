package com.example.asiochatfrontend.ui.home;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private final MutableLiveData<List<ChatDto>> chats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsUseCase;
    private final GetChatsForUserUseCase getChatsForUserUseCase;
    private final MutableLiveData<List<ChatDto>> chatsLiveData = new MutableLiveData<>();

    private String currentUserId;
    private boolean showUnreadOnly = false;

    public HomeViewModel(ConnectionManager connectionManager, String userId) {
        this.connectionManager = connectionManager;
        this.getChatsUseCase = new GetChatsForUserUseCase(connectionManager);
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
        loadChats();
    }

    private void loadChats() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User ID not set");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ChatDto> chats = getChatsForUserUseCase.execute(this.currentUserId);
                chatsLiveData.postValue(chats);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}