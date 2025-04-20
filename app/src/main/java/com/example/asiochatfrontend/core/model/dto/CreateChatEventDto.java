package com.example.asiochatfrontend.core.model.dto;

import java.util.List;

public class CreateChatEventDto {
    List<String> participants;

    public CreateChatEventDto(List<String> participants) {
        this.participants = participants;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}
