package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public interface OnWSEventCallback {
    void onChatCreateEvent(List<ChatDto> chats); // Fire chat reload on main activity
    void onPendingMessagesSendEvent(List<MessageDto> messages); // Fire pending messages send
    void onRemovedFromChat(String chatId); // Fire chat removal of user
}
