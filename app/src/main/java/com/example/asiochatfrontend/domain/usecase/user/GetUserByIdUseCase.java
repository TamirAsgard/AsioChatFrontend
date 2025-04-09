package com.example.asiochatfrontend.domain.usecase.user;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;

public class GetUserByIdUseCase {
    private final ConnectionManager connectionManager;

    public GetUserByIdUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public UserDto execute(String userId) throws Exception {
        return connectionManager.getUserById(userId);
    }
}