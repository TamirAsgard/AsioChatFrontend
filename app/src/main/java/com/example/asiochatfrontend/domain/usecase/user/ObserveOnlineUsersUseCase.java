package com.example.asiochatfrontend.domain.usecase.user;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;

public class ObserveOnlineUsersUseCase {
    private final ConnectionManager connectionManager;

    public ObserveOnlineUsersUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<UserDto> execute() {
        return connectionManager.observeOnlineUsers();
    }
}
