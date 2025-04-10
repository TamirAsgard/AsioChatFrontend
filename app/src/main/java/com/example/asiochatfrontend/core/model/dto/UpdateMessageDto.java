package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.List;

public class UpdateMessageDto {
    public String payload;
    public List<String> WaitingMemebersList;

    public UpdateMessageDto(String payload, List<String> waitingMemebersList) {
        this.payload = payload;
        WaitingMemebersList = waitingMemebersList;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public List<String> getWaitingMemebersList() {
        return WaitingMemebersList;
    }

    public void setWaitingMemebersList(List<String> waitingMemebersList) {
        WaitingMemebersList = waitingMemebersList;
    }
}