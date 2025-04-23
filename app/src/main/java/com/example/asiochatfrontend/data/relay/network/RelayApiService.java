package com.example.asiochatfrontend.data.relay.network;

import com.example.asiochatfrontend.core.model.dto.AuthRequestCredentialsDto;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.core.model.dto.SymmetricKeyDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.UpdateUserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface RelayApiService {
    // ================== Service Base URLs ==================
    String userService = "user-service/api/users/";
    String authService = "auth-service/api/auth/";
    String mediaService = "media-service/api/media/";
    String messageBrokerService = "message-broker/";
    String messageService = "message-service/api/messages/";

    // ================== Auth Operations ==================
    // region Auth - Registration & Login
    @POST(authService + "register")
    Call<UserDto> createUser(@Body AuthRequestCredentialsDto credentials);
    // endregion

    // region Auth - Public Key Operations
    @POST(authService + "keys/public")
    Call<Boolean> registerPublicKey(@Body PublicKeyDto keyDto);

    @GET(authService + "keys/public/{userId}/timestamp/{timestamp}")
    Call<PublicKeyDto> getPublicKeyForTimestamp(@Path("userId") String userId, @Path("timestamp") long timestamp);

    @GET(authService + "keys/public/all/{userId}")
    Call<List<PublicKeyDto>> getAllPublicKeysForUser(@Path("userId") String userId);
    // endregion

    // region Auth - Symmetric Key Operations
    @POST(authService + "keys/symmetric")
    Call<Boolean> registerSymmetricKey(@Body SymmetricKeyDto keyDto);

    @GET(authService + "keys/symmetric/{chatId}/timestamp/{timestamp}")
    Call<SymmetricKeyDto> getSymmetricKeyForTimestamp(@Path("chatId") String chatId, @Path("timestamp") long timestamp);

    @GET(authService + "keys/symmetric/all/{userId}")
    Call<List<SymmetricKeyDto>> getAllSymmetricKeysForChat(@Path("userId") String userId);
    // endregion

    // ================== User Operations ==================
    // region User
    @PUT(userService + "{userId}")
    Call<UserDto> updateUser(@Path("userId") String userId, @Body UpdateUserDetailsDto user);

    @GET(userService + "{userId}")
    Call<UserDto> getUserById(@Path("userId") String userId);

    @GET(userService)
    Call<List<UserDto>> getContacts();
    // endregion

    // ================== Chat Operations ==================
    // region Chat (via Message Broker)
    @POST(messageBrokerService + "chat")
    Call<ChatDto> createChat(@Body ChatDto chat);

    @GET(messageBrokerService + "chat/{chatId}")
    Call<ChatDto> getChatById(@Path("chatId") String chatId);

    @GET(messageBrokerService + "chat/user/{userId}")
    Call<List<ChatDto>> getChatsForUser(@Path("userId") String userId);

    @PUT(messageBrokerService + "chat/{chatId}")
    Call<ChatDto> updateChat(@Path("chatId") String chatId, @Body ChatDto chat);

    @POST("chats/{chatId}/members/{userId}")
    Call<ChatDto> addMemberToChat(@Path("chatId") String chatId, @Path("userId") String userId);

    @DELETE("chats/{chatId}/members/{userId}")
    Call<ChatDto> removeMemberFromChat(@Path("chatId") String chatId, @Path("userId") String userId);
    // endregion

    // ================== Message Operations ==================
    // region Messaging (via Message Service)
    @POST(messageBrokerService)
    Call<MessageDto> sendMessage(@Body MessageDto dto);

    @GET(messageService + "chat/{chatId}")
    Call<List<TextMessageDto>> getMessagesForChat(@Path("chatId") String chatId);

    Call<List<MessageDto>> getOfflineMessages(String userId);

    Call<Boolean> markMessageAsRead(String messageId);
    // endregion

    // ================== Media Operations ==================
    // region Media Upload/Download
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

    @GET(mediaService + "messages/chat/{chatId}")
    Call<List<MediaMessageDto>> getMediaMessagesForChat(@Path("chatId") String chatId);

    @GET(mediaService + "messages/{messageId}")
    Call<MessageDto> getMediaById(@Path("messageId") String messageId);

    @GET(mediaService + "files/{messageId}")
    Call<ResponseBody> downloadMedia(@Path("messageId") String messageId);
    // endregion
}