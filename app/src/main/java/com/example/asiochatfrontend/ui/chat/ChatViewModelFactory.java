package com.example.asiochatfrontend.ui.chat;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class ChatViewModelFactory implements ViewModelProvider.Factory {
    private final ConnectionManager connectionManager;

    public ChatViewModelFactory(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(connectionManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
