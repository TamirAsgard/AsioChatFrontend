package com.example.asiochatfrontend.domain.repository;

import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;

/**
 * Repository interface for handling user-related data operations.
 */
public interface UserRepository {

    /**
     * Save a user to the repository
     *
     * @param user The user to save
     * @return The saved user with any updated fields
     */
    UserDto saveUser(UserDto user);

    /**
     * Get a user by their unique identifier
     *
     * @param userId The id of the user to retrieve
     * @return The user if found, null otherwise
     */
    UserDto getUserById(String userId);

    /**
     * Get all users in a specific chat
     *
     * @param chatId The id of the chat
     * @return List of users in the chat
     */
    List<UserDto> getUsersInChat(String chatId);

    /**
     * Update the online status of a user
     *
     * @param userId The id of the user
     * @param isOnline The new online status
     * @return The updated user
     */
    UserDto updateOnlineStatus(String userId, boolean isOnline);

    /**
     * Delete a user
     *
     * @param userId The id of the user to delete
     * @return true if successful, false otherwise
     */
    boolean deleteUser(String userId);

    /**
     * Get all users in the repository
     *
     * @return List of all users
     */
    List<UserDto> getAllUsers();

    /**
     * Get all users matching the given list of user IDs.
     *
     * @param userIds The list of user IDs to look up
     * @return List of matching UserDto
     */
    List<UserDto> getUsersByIds(List<String> userIds);

    /**
     * Get all currently online users.
     *
     * @return List of online user IDs
     */
    List<String> getOnlineUserIds();

    void setCurrentUser(String userId);

    List<String> getOnlineUsers();
}
