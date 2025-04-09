package com.example.asiochatfrontend.data.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class UuidGenerator {
    private static final Random random = new Random();

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String generateTimeBased() {
        long timeMs = System.currentTimeMillis();
        int randomNum = random.nextInt(1000);
        return timeMs + "-" + randomNum;
    }

    public static String generateForMessage(String senderId, String receiverId) {
        long timeMs = System.currentTimeMillis();
        return senderId + "-" + receiverId + "-" + timeMs;
    }

    public static String generateForChat(String userIdA, String userIdB) {
        List<String> sorted = Arrays.asList(userIdA, userIdB);
        Collections.sort(sorted);
        return sorted.get(0) + "-" + sorted.get(1);
    }
}
