package com.example.asiochatfrontend.data.direct.service;

import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.service.UserService;
import com.example.asiochatfrontend.data.direct.network.UserDiscoveryManager;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class DirectUserService implements UserService {
    private final UserRepository userRepository;
    private final UserDiscoveryManager userDiscoveryManager;

    @Inject
    public DirectUserService(UserRepository userRepository, UserDiscoveryManager userDiscoveryManager) {
        this.userRepository = userRepository;
        this.userDiscoveryManager = userDiscoveryManager;
    }

    @Override
    public void setCurrentUser(String userId) {
        // Not needed for WebSocket implementation
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        return userRepository.saveUser(userDto);
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto updateUserDetailsDto) {
        UserDto current = userRepository.getUserById(userId);
        if (current == null) {
            throw new IllegalArgumentException("User not found");
        }

        String updatedName = updateUserDetailsDto.getName() != null ? updateUserDetailsDto.getName() : current.getName();
        String updatedPicture = updateUserDetailsDto.getProfilePicture() != null ? updateUserDetailsDto.getProfilePicture() : current.getProfilePicture();

        UserDto updatedUser = new UserDto(
                current.getId(),
                updatedName,
                updatedPicture,
                current.getStatus(),
                current.isOnline(),
                current.getLastSeen(),
                current.getCreatedAt(),
                new java.util.Date()
        );

        userRepository.saveUser(updatedUser);
        return updatedUser;
    }

    @Override
    public UserDto getUserById(String userId) {
        return userRepository.getUserById(userId);
    }

    @Override
    public List<UserDto> getContacts() {
        return userRepository.getAllUsers();
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        List<String> onlineIds = userDiscoveryManager.getOnlineUsers();
        if (onlineIds == null || onlineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository.getUsersByIds(onlineIds);
    }

    @Override
    public void refreshOnlineUsers() {
        userDiscoveryManager.refreshOnlineUsers();
    }

    @Override
    public List<String> getOnlineUsers() {
        List<String> onlineUsers = userDiscoveryManager.getOnlineUsers();
        return onlineUsers != null ? onlineUsers : Collections.emptyList();
    }
}