package com.example.asiochatfrontend.data.common.repository;

import com.example.asiochatfrontend.core.model.dto.UserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.data.database.dao.UserDao;
import com.example.asiochatfrontend.data.database.entity.UserEntity;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class UserRepositoryImpl implements UserRepository {

    private final UserDao userDao;
    private String currentUserId;

    @Inject
    public UserRepositoryImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDto saveUser(UserDto user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getJid();

        if (user.getUserDetailsDto() != null) {
            entity.firstName = user.getUserDetailsDto().getFirstName();
            entity.lastName = user.getUserDetailsDto().getLastName();
        }

        entity.isOnline = true;
        entity.lastSeen = null;
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = new Date();

        userDao.insertOrUpdateUser(entity);
        return user;
    }

    @Override
    public UserDto getUserById(String userId) {
        UserEntity entity = userDao.getUserById(userId);
        return entity != null ? mapEntityToDto(entity) : null;
    }

    @Override
    public List<UserDto> getUsersInChat(String chatId) {
        return userDao.getUsersInChat(chatId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto updateOnlineStatus(String userId, boolean isOnline) {
        Long lastSeen = isOnline ? null : new Date().getTime();
        userDao.updateUserOnlineStatus(userId, isOnline, lastSeen);
        return getUserById(userId);
    }

    @Override
    public boolean deleteUser(String userId) {
        UserEntity entity = userDao.getUserById(userId);
        userDao.deleteUser(entity);
        return true;
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userDao.getAllUsers()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getUsersByIds(List<String> userIds) {
        return userDao.getUsersByIds(userIds)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getOnlineUserIds() {
        return userDao.getOnlineUserIds();
    }

    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;
    }

    @Override
    public List<String> getOnlineUsers() {
        return userDao.getOnlineUserIds();
    }

    private UserDto mapEntityToDto(UserEntity entity) {
        UserDetailsDto details = new UserDetailsDto(entity.firstName, entity.lastName);
        return new UserDto(
                details,
                entity.id,
                entity.createdAt,
                entity.updatedAt
        );
    }
}
