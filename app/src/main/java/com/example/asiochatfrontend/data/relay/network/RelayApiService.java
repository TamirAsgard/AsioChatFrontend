package com.example.asiochatfrontend.data.relay.network;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface RelayApiService {
    final String userService = "user-service/api/users/";
    final String authService = "auth-service/api/auth/";
    final String mediaService = "media-service/api/media/";
    final String messageBrokerService = "message-broker/";

    // User operations (User Service)
    @POST(userService)
    Call<UserDto> createUser(@Body UserDto user);

    @PUT(userService + "{userId}")
    Call<UserDto> updateUser(@Path("userId") String userId, @Body UpdateUserDetailsDto user);

    @GET(userService + "{userId}")
    Call<UserDto> getUserById(@Path("userId") String userId);

    // Chat operations (Message Broker Service)
    @POST(messageBrokerService + "chat")
    Call<ChatDto> createChat(@Body ChatDto chat);

    @GET(messageBrokerService + "chat/{chatId}")
    Call<ChatDto> getChatById(@Path("chatId") String chatId);

    @GET(messageBrokerService + "chat/{userId}")
    Call<List<ChatDto>> getChatsForUser(@Path("userId") String userId);

    @PUT(messageBrokerService + "chat/{chatId}")
    Call<ChatDto> updateChat(@Path("chatId") String chatId, @Body ChatDto chat);

    // Recipient operations (Message Broker Service)

    @POST("chats/{chatId}/members/{userId}")
    Call<ChatDto> addMemberToChat(@Path("chatId") String chatId, @Path("userId") String userId);

    @DELETE("chats/{chatId}/members/{userId}")
    Call<ChatDto> removeMemberFromChat(@Path("chatId") String chatId, @Path("userId") String userId);

    // Message operations (Message Service)
    @POST(messageBrokerService)
    Call<MessageDto> sendMessage(@Body MessageDto dto);

    // Media operations
    @Multipart
    @POST("media")
    Call<MessageDto> uploadMedia(
            @Part("id") RequestBody id,
            @Part("chatId") RequestBody chatId,
            @Part("jid") RequestBody jid,
            @Part("timestamp") RequestBody timestamp,
            @Part("status") RequestBody status,
            @Part("waitingMemebersList") RequestBody waitingListJson,
            @Part("payload.fileName") RequestBody fileName,
            @Part("payload.contentType") RequestBody contentType,
            @Part("payload.type") RequestBody mediaType,
            @Part("payload.size") RequestBody size,
            @Part("payload.thumbnailPath") RequestBody thumbnailPath,
            @Part("payload.isProcessed") RequestBody isProcessed,
            @Part("payload.file") MultipartBody.Part file
    );

    @GET(mediaService + "messages/{messageId}")
    Call<MessageDto> getMediaById(@Path("messageId") String messageId);

    @GET(mediaService + "files/{messageId}")
    Call<MediaStreamResultDto> downloadMedia(@Path("messageId") String messageId);
}
