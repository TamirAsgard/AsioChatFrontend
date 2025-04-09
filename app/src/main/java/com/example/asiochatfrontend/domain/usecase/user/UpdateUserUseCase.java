package com.example.asiochatfrontend.domain.usecase.user;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;

public class UpdateUserUseCase {
    private final ConnectionManager connectionManager;

    public UpdateUserUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public UserDto execute(String userId, UpdateUserDetailsDto userDetailsDto) throws Exception {
        return connectionManager.updateUser(userId, userDetailsDto);
    }
}