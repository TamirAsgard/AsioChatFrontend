package com.example.asiochatfrontend.data.database.utils;

import android.util.Log;

import com.example.asiochatfrontend.data.database.dao.ChatDao;
import com.example.asiochatfrontend.data.database.dao.UserDao;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;
import com.example.asiochatfrontend.data.database.entity.UserEntity;
import com.example.asiochatfrontend.core.model.enums.ChatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MockDataGenerator {

    public static void generateMockUsersAndChats(UserDao userDao, ChatDao chatDao) {
        try {
            Date now = new Date();

            // ✅ Create Users
            List<UserEntity> users = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                UserEntity user = new UserEntity();
                user.id = "user" + i;
                user.name = "User " + i;
                user.phoneNumber = "12345" + i;
                user.status = "Hey there! I'm using Orion";
                user.isOnline = i % 2 == 0;
                user.lastSeen = now;
                user.createdAt = now;
                user.updatedAt = now;
                users.add(user);
            }

            userDao.insertUsers(users);

            // ✅ Create Chats
            List<ChatEntity> chats = new ArrayList<>();

            // One group chat
            ChatEntity groupChat = new ChatEntity();
            groupChat.id = UUID.randomUUID().toString();
            groupChat.name = "Mock Group";
            groupChat.type = ChatType.GROUP;
            groupChat.participants = Arrays.asList("user1", "user2", "tamir@gmail.com");
            groupChat.unreadCount = 0;
            groupChat.createdAt = now;
            groupChat.updatedAt = now;
            chats.add(groupChat);

            // Private chats between user1 and user2, user1 and user4
            ChatEntity privateChat1 = new ChatEntity();
            privateChat1.id = UUID.randomUUID().toString();
            privateChat1.name = null;
            privateChat1.type = ChatType.PRIVATE;
            privateChat1.participants = Arrays.asList("user1", "tamir@gmail.com");
            privateChat1.createdAt = now;
            privateChat1.updatedAt = now;

            ChatEntity privateChat2 = new ChatEntity();
            privateChat2.id = UUID.randomUUID().toString();
            privateChat2.name = null;
            privateChat2.type = ChatType.PRIVATE;
            privateChat2.participants = Arrays.asList("tamir@gmail.com", "user4");
            privateChat2.createdAt = now;
            privateChat2.updatedAt = now;

            chats.add(privateChat1);
            chats.add(privateChat2);

            chatDao.insertChats(chats);

            Log.d("MockData", "Mock users and chats created");

        } catch (Exception e) {
            Log.e("MockData", "Error generating mock data", e);
        }
    }
}
