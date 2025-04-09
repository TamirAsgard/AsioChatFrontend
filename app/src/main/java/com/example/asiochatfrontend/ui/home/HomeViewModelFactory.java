package com.example.asiochatfrontend.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final ConnectionManager connectionManager;
    private final String userId;

    public HomeViewModelFactory(ConnectionManager connectionManager, String userId) {
        this.connectionManager = connectionManager;
        this.userId = userId;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(connectionManager, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}