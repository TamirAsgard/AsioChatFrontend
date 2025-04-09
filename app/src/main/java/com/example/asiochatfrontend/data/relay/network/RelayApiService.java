package com.example.asiochatfrontend.data.relay.network;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface RelayApiService {

    // User operations
    @POST("users")
    Call<UserDto> createUser(@Body UserDto user);

    @PUT("users/{userId}")
    Call<UserDto> updateUser(@Path("userId") String userId, @Body UserDto user);

    @GET("users/{userId}")
    Call<UserDto> getUserById(@Path("userId") String userId);

    @GET("users/search")
    Call<List<UserDto>> searchUsers(@Query("query") String query);

    @GET("users/online")
    Call<List<String>> getOnlineUsers();

    // Chat operations
    @POST("chats")
    Call<ChatDto> createChat(@Body ChatDto chat);

    @GET("chats/{chatId}")
    Call<ChatDto> getChatById(@Path("chatId") String chatId);

    @GET("users/{userId}/chats")
    Call<List<ChatDto>> getChatsForUser(@Path("userId") String userId);

    @PUT("chats/{chatId}")
    Call<ChatDto> updateChat(@Path("chatId") String chatId, @Body ChatDto chat);

    @POST("chats/{chatId}/members/{userId}")
    Call<ChatDto> addMemberToChat(@Path("chatId") String chatId, @Path("userId") String userId);

    @DELETE("chats/{chatId}/members/{userId}")
    Call<ChatDto> removeMemberFromChat(@Path("chatId") String chatId, @Path("userId") String userId);

    // Message operations
    @POST("messages")
    Call<MessageDto> sendMessage(@Body MessageDto message);

    @GET("chats/{chatId}/messages")
    Call<List<MessageDto>> getMessagesForChat(@Path("chatId") String chatId);

    @GET("users/{userId}/messages/offline")
    Call<List<MessageDto>> getOfflineMessages(@Path("userId") String userId);

    @PUT("messages/{messageId}/read")
    Call<Void> markMessageAsRead(@Path("messageId") String messageId);

    @GET("messages/search")
    Call<List<MessageDto>> searchMessages(@Query("query") String query);

    // Media operations
    @Multipart
    @POST("media")
    Call<MediaDto> uploadMedia(
            @Part MultipartBody.Part file,
            @Part("uploaderId") String uploaderId,
            @Part("mediaType") String mediaType
    );

    @GET("media/{mediaId}")
    Call<MediaDto> getMediaById(@Path("mediaId") String mediaId);

    @Streaming
    @GET("media/{mediaId}/content")
    Call<ResponseBody> downloadMedia(@Path("mediaId") String mediaId);

    // Authentication
    @POST("auth/login")
    Call<Map<String, String>> login(
            @Query("username") String username,
            @Query("password") String password
    );

    @POST("auth/refresh")
    Call<Map<String, String>> refreshToken(@Header("Authorization") String refreshToken);
}
