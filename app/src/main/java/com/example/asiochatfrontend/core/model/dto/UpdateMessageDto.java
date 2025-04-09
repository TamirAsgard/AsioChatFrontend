package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.List;

public class UpdateMessageDto {
    public MessageState state;
    public List<String> waitingMembersList;

    public UpdateMessageDto(MessageState state, List<String> readBy) {
        this.state = state;
        this.waitingMembersList = readBy;
    }

    public MessageState getState() {
        return state;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public List<String> getWaitingMembersList() {
        return waitingMembersList;
    }

    public void setWaitingMembersList(List<String> waitingMembersList) {
        this.waitingMembersList = waitingMembersList;
    }
}