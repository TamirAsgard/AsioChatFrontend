package com.example.asiochatfrontend.ui.contacts;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.domain.usecase.chat.CreateGroupChatUseCase;
import com.example.asiochatfrontend.domain.usecase.chat.CreatePrivateChatUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetAllUsersUseCase;
import com.example.asiochatfrontend.domain.usecase.user.ObserveOnlineUsersUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ContactsViewModel extends ViewModel {
    private static final String TAG = "ContactsViewModel";

    private final MutableLiveData<List<UserDto>> contacts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<UserDto>> filteredContacts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatDto> createdChat = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final ObserveOnlineUsersUseCase observeOnlineUsersUseCase;
    private final GetAllUsersUseCase getAllContactsUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreatePrivateChatUseCase createPrivateChatUseCase;
    private final CreateGroupChatUseCase createGroupChatUseCase;

    private String currentUserId;
    private List<UserDto> allContacts = new ArrayList<>();

    @Inject
    public ContactsViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.observeOnlineUsersUseCase = new ObserveOnlineUsersUseCase(connectionManager);
        this.getUserByIdUseCase = new GetUserByIdUseCase(connectionManager);
        this.createPrivateChatUseCase = new CreatePrivateChatUseCase(connectionManager);
        this.createGroupChatUseCase = new CreateGroupChatUseCase(connectionManager);
        this.getAllContactsUseCase = new GetAllUsersUseCase(connectionManager);

        // Load contacts
        loadContacts();
    }

    public LiveData<List<UserDto>> getContacts() {
        return filteredContacts;
    }

    public LiveData<ChatDto> getCreatedChat() {
        return createdChat;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;

        // Set current user in connection manager
        connectionManager.setCurrentUser(userId);

        // Reload contacts
        loadContacts();
    }

    public void refresh() {
        loadContacts();
    }

    public void filterContacts(String query) {
        if (query == null || query.isEmpty()) {
            // If no query, show all contacts
            filteredContacts.setValue(allContacts);
            return;
        }

        // Filter contacts based on query
        List<UserDto> filtered = allContacts.stream()
                .filter(contact -> contact.getJid() != null &&
                        contact.getJid().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        filteredContacts.setValue(filtered);
    }

    public void createPrivateChat(String otherUserId) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User not logged in");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String chatId = UuidGenerator.generateForChat(currentUserId, otherUserId);
                ChatDto chat = createPrivateChatUseCase.execute(chatId, currentUserId, otherUserId);
                createdChat.postValue(chat);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error creating private chat", e);
                error.postValue("Error creating chat: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void createGroupChat(String name, List<String> memberIds) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User not logged in");
            return;
        }

        // Ensure creator is in the member list
        if (!memberIds.contains(currentUserId)) {
            memberIds.add(currentUserId);
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String chatId = UuidGenerator.generate();
                ChatDto chat = createGroupChatUseCase.execute(chatId, name, memberIds, currentUserId);
                createdChat.postValue(chat);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error creating group chat", e);
                error.postValue("Error creating group chat: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    private void loadContacts() {
        isLoading.setValue(true);

        new Thread(() -> {
            try {
                // In a real app, you'd also get contacts from a local database
                // For now, we'll just use online users as our contacts
                allContacts = getAllContactsUseCase.execute();

                // Remove current user from contacts list
                if (currentUserId != null && !currentUserId.isEmpty()) {
                    allContacts = allContacts.stream()
                            .filter(user -> !user.getJid().equals(currentUserId))
                            .collect(Collectors.toList());
                }

                // Update the contacts list
                contacts.postValue(allContacts);
                filteredContacts.postValue(allContacts);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error loading contacts", e);
                error.postValue("Error loading contacts: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }
}