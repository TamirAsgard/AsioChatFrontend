package com.example.asiochatfrontend.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.domain.usecase.chat.GetChatsForUserUseCase;
import java.util.List;
import javax.inject.Inject;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<List<ChatDto>> chats = new MutableLiveData<>();

    @Inject
    public HomeViewModel(ConnectionManager connectionManager) {
        GetChatsForUserUseCase getChats = new GetChatsForUserUseCase(connectionManager);
        try {
            chats.setValue(getChats.execute("current_user_id")); // Replace with actual userId retrieval
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<List<ChatDto>> getChats() {
        return chats;
    }
}