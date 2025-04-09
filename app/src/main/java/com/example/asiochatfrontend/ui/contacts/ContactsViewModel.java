package com.example.asiochatfrontend.ui.contacts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.domain.usecase.user.ObserveOnlineUsersUseCase;

import java.util.List;
import javax.inject.Inject;

public class ContactsViewModel extends ViewModel {
    private final MutableLiveData<List<UserDto>> contacts = new MutableLiveData<>();

    @Inject
    public ContactsViewModel(ConnectionManager connectionManager) {
        ObserveOnlineUsersUseCase useCase = new ObserveOnlineUsersUseCase(connectionManager);
        contacts.setValue(useCase.execute());
    }

    public LiveData<List<UserDto>> getContacts() {
        return contacts;
    }
}