package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import java.util.List;


public interface UserService {
    void setCurrentUser(String userId);

    UserDto createUser(UserDto userDto) throws Exception;

    UserDto updateUser(String userId, UpdateUserDetailsDto updateUserDetailsDto) throws Exception;

    UserDto getUserById(String userId) throws Exception;

    List<UserDto> getContacts();

    List<UserDto> observeOnlineUsers();

    void refreshOnlineUsers();

    List<String> getOnlineUsers();
}