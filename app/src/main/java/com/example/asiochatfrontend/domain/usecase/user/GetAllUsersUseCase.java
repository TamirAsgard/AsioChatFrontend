package com.example.asiochatfrontend.domain.usecase.user;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;

public class GetAllUsersUseCase {
    private final ConnectionManager connectionManager;

    public GetAllUsersUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<UserDto> execute() throws Exception {
        return connectionManager.getContacts();
    }
}
