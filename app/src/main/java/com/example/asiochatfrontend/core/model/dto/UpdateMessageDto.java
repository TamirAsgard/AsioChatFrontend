package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.List;

public class UpdateMessageDto {
    public MessageState state;
    public List<String> readBy;

    public UpdateMessageDto(MessageState state, List<String> readBy) {
        this.state = state;
        this.readBy = readBy;
    }

    public MessageState getState() {
        return state;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public List<String> getReadBy() {
        return readBy;
    }

    public void setReadBy(List<String> readBy) {
        this.readBy = readBy;
    }
}