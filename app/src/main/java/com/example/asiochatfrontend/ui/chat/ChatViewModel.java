package com.example.asiochatfrontend.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.domain.usecase.message.CreateMessageUseCase;
import com.example.asiochatfrontend.domain.usecase.message.GetMessagesForChatUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ChatViewModel extends ViewModel {
    private final MutableLiveData<List<MessageDto>> messages = new MutableLiveData<>(new ArrayList<>());
    private final ConnectionManager connectionManager;
    private final CreateMessageUseCase createMessageUseCase;
    private final GetMessagesForChatUseCase getMessagesUseCase;

    @Inject
    public ChatViewModel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.createMessageUseCase = new CreateMessageUseCase(connectionManager);
        this.getMessagesUseCase = new GetMessagesForChatUseCase(connectionManager);

        loadMessages();
    }

    public LiveData<List<MessageDto>> getMessages() {
        return messages;
    }

    public void loadMessages() {
        try {
            messages.setValue(getMessagesUseCase.execute("chat_id")); // Replace with actual chat ID
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String content) {
        MessageDto message = new MessageDto();
        message.chatId = "chat_id"; // Replace with actual chat ID
        message.senderId = "current_user_id";
        message.content = content;

        try {
            MessageDto sentMessage = createMessageUseCase.execute(message);
            List<MessageDto> updated = new ArrayList<>(messages.getValue());
            updated.add(sentMessage);
            messages.setValue(updated);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
