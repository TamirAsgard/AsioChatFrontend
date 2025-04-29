package com.example.asiochatfrontend.ui.group;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.domain.usecase.chat.AddMemberToGroupUseCase;
import com.example.asiochatfrontend.domain.usecase.chat.CreatePrivateChatUseCase;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import com.example.asiochatfrontend.domain.usecase.chat.RemoveMemberFromGroupUseCase;
import com.example.asiochatfrontend.domain.usecase.chat.UpdateGroupNameUseCase;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class GroupViewModel extends ViewModel {
    private static final String TAG = "GroupViewModel";

    private final MutableLiveData<ChatDto> groupData = new MutableLiveData<>();
    private final MutableLiveData<List<UserDto>> groupMembers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<ChatDto> createdPrivateChat = new MutableLiveData<>();

    private final ConnectionManager connectionManager;
    private final GetChatsForUserUseCase getChatsUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateGroupNameUseCase updateGroupNameUseCase;
    private final AddMemberToGroupUseCase addMemberToGroupUseCase;
    private final RemoveMemberFromGroupUseCase removeMemberFromGroupUseCase;
    private final CreatePrivateChatUseCase createPrivateChatUseCase;

    private String chatId;
    private String currentUserId;

    @Inject
    public GroupViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.getChatsUseCase = new GetChatsForUserUseCase(connectionManager);
        this.getUserByIdUseCase = new GetUserByIdUseCase(connectionManager);
        this.updateGroupNameUseCase = new UpdateGroupNameUseCase(connectionManager);
        this.addMemberToGroupUseCase = new AddMemberToGroupUseCase(connectionManager);
        this.removeMemberFromGroupUseCase = new RemoveMemberFromGroupUseCase(connectionManager);
        this.createPrivateChatUseCase = new CreatePrivateChatUseCase(connectionManager);
    }

    public void initialize(String chatId, String currentUserId) {
        this.chatId = chatId;
        this.currentUserId = currentUserId;

        // Set current user in the connection manager
        connectionManager.setCurrentUser(currentUserId);

        // Load group data and members
        loadGroupData();
    }

    public LiveData<ChatDto> getGroupData() {
        return groupData;
    }

    public LiveData<List<UserDto>> getGroupMembers() {
        return groupMembers;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<ChatDto> getCreatedPrivateChat() {
        return createdPrivateChat;
    }

    public void refresh() {
        loadGroupData();
    }

    public void updateGroupName(String newName) {
        if (chatId == null || chatId.isEmpty()) {
            error.setValue("Invalid group ID");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                boolean success = updateGroupNameUseCase.execute(chatId, newName);
                if (success) {
                    // Reload group data to get updated name
                    loadGroupData();
                } else {
                    error.postValue("Failed to update group name");
                    isLoading.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating group name", e);
                error.postValue("Error updating group name: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void addMemberToGroup(String userId) {
        if (chatId == null || chatId.isEmpty()) {
            error.setValue("Invalid group ID");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                boolean success = addMemberToGroupUseCase.execute(chatId, userId);
                if (success) {
                    // Reload group data to get updated members
                    loadGroupData();
                } else {
                    error.postValue("Failed to add member to group");
                    isLoading.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding member to group", e);
                error.postValue("Error adding member to group: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void removeMemberFromGroup(String userId) {
        if (chatId == null || chatId.isEmpty()) {
            error.setValue("Invalid group ID");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                boolean success = removeMemberFromGroupUseCase.execute(chatId, userId);
                if (success) {
                    // Reload group data to get updated members
                    loadGroupData();
                } else {
                    error.postValue("Failed to remove member from group");
                    isLoading.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing member from group", e);
                error.postValue("Error removing member from group: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    public void startPrivateChat(String otherUserId) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            error.setValue("User not logged in");
            return;
        }

        isLoading.setValue(true);

        new Thread(() -> {
            try {
                String chatId = UuidGenerator.generateForChat(currentUserId, otherUserId);
                ChatDto chat = createPrivateChatUseCase.execute(chatId, currentUserId, otherUserId);
                createdPrivateChat.postValue(chat);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error creating private chat", e);
                error.postValue("Error creating private chat: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }

    private void loadGroupData() {
        if (chatId == null || chatId.isEmpty()) {
            error.postValue("Invalid group ID");
            return;
        }

        isLoading.postValue(true);

        new Thread(() -> {
            try {
                // Get the chat data
                List<ChatDto> chats = getChatsUseCase.execute(currentUserId);
                ChatDto targetChat = null;

                for (ChatDto chat : chats) {
                    if (chat.getChatId().equals(chatId)) {
                        targetChat = chat;
                        break;
                    }
                }

                if (targetChat == null) {
                    error.postValue("Group not found");
                    isLoading.postValue(false);
                    return;
                }

                // Post the chat data
                groupData.postValue(targetChat);

                // Load user data for each member
                List<UserDto> members = new ArrayList<>();
                for (String memberId : targetChat.getRecipients()) {
                    try {
                        UserDto member = getUserByIdUseCase.execute(memberId);
                        if (member != null) {
                            members.add(member);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading member data for " + memberId, e);
                    }
                }

                groupMembers.postValue(members);
                isLoading.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error loading group data", e);
                error.postValue("Error loading group data: " + e.getMessage());
                isLoading.postValue(false);
            }
        }).start();
    }
}