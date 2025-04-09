package com.example.asiochatfrontend.ui.contacts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.domain.usecase.chat.CreatePrivateChatUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ContactsViewModel extends ViewModel {
    private final MutableLiveData<List<UserDto>> contacts = new MutableLiveData<>(new ArrayList<>());
    private final ConnectionManager connectionManager;
    private final CreatePrivateChatUseCase createPrivateChatUseCase;

    @Inject
    public ContactsViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.createPrivateChatUseCase = new CreatePrivateChatUseCase(connectionManager);
        loadContacts();
    }

    public LiveData<List<UserDto>> getContacts() {
        return contacts;
    }

    public void loadContacts() {
        try {
            // TODO: Replace with proper contact fetch logic
            List<UserDto> dummyList = new ArrayList<>();
            contacts.setValue(dummyList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPrivateChat(List<String> selectedUserIds) {
        if (selectedUserIds.size() == 1) {
            try {
                createPrivateChatUseCase.execute("current_user_id", selectedUserIds.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}