package com.example.asiochatfrontend.domain.usecase.user;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;

public class CreateUserUseCase {
    private final ConnectionManager connectionManager;

    public CreateUserUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public UserDto execute(UserDto dto) throws Exception {
        return connectionManager.createUser(dto);
    }
}