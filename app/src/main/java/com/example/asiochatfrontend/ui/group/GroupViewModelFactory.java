package com.example.asiochatfrontend.ui.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class GroupViewModelFactory implements ViewModelProvider.Factory {
    private final ConnectionManager cm;

    public GroupViewModelFactory(ConnectionManager cm) {
        this.cm = cm;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(GroupViewModel.class)) {
            //noinspection unchecked
            return (T) new GroupViewModel(cm);
        }
        throw new IllegalArgumentException("unknown VM");
    }
}
