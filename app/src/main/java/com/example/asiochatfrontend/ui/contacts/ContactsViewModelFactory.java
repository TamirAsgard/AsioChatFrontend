package com.example.asiochatfrontend.ui.contacts;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class ContactsViewModelFactory implements ViewModelProvider.Factory {
    private final ConnectionManager connectionManager;

    public ContactsViewModelFactory(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ContactsViewModel.class)) {
            return (T) new ContactsViewModel(connectionManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}