package com.example.asiochatfrontend.data.relay.network;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Singleton
public class RelayApiClient {
    private static final String TAG = "RelayApiClient";
    private final RelayApiService relayApiService;
    private String authToken;
    private String refreshToken;

    @Inject
    public RelayApiClient(RelayApiService relayApiService) {
        this.relayApiService = relayApiService;
    }

    public static RelayApiClient createInstance(String ip, int port) {
        String baseUrl = ip + ":" + port + "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RelayApiService relayApiService = retrofit.create(RelayApiService.class);
        return new RelayApiClient(relayApiService);
    }
    public boolean login(String username, String password) {
        try {
            Response<Map<String, String>> response = relayApiService.login(username, password).execute();
            if (response.isSuccessful() && response.body() != null) {
                Map<String, String> tokens = response.body();
                authToken = tokens.get("token");
                refreshToken = tokens.get("refreshToken");
                Log.d(TAG, "Login successful. Token received.");
                return true;
            } else {
                Log.w(TAG, "Login failed: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Login error: ", e);
        }
        return false;
    }

    public boolean refreshAuthToken() {
        if (refreshToken != null) {
            try {
                Response<Map<String, String>> response = relayApiService.refreshToken("Bearer " + refreshToken).execute();
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> tokens = response.body();
                    authToken = tokens.get("token");
                    refreshToken = tokens.get("refreshToken");
                    Log.d(TAG, "Token refreshed successfully.");
                    return true;
                } else {
                    Log.w(TAG, "Token refresh failed: " + response.code() + " - " + response.message());
                }
            } catch (Exception e) {
                Log.e(TAG, "Token refresh error: ", e);
            }
        }
        return false;
    }

    public UserDto createUser(UserDto user) {
        try {
            Response<UserDto> response = relayApiService.createUser(user).execute();
            return handleResponse(response, "createUser");
        } catch (Exception e) {
            Log.e(TAG, "createUser error: ", e);
            return null;
        }
    }

    public UserDto updateUser(String userId, UserDto user) {
        try {
            Response<UserDto> response = relayApiService.updateUser(userId, user).execute();
            return handleResponse(response, "updateUser");
        } catch (Exception e) {
            Log.e(TAG, "updateUser error: ", e);
            return null;
        }
    }

    public UserDto getUserById(String userId) {
        try {
            Response<UserDto> response = relayApiService.getUserById(userId).execute();
            return handleResponse(response, "getUserById");
        } catch (Exception e) {
            Log.e(TAG, "getUserById error: ", e);
            return null;
        }
    }

    public List<UserDto> searchUsers(String query) {
        try {
            Response<List<UserDto>> response = relayApiService.searchUsers(query).execute();
            return handleListResponse(response, "searchUsers");
        } catch (Exception e) {
            Log.e(TAG, "searchUsers error: ", e);
            return Collections.emptyList();
        }
    }

    public List<String> getOnlineUsers() {
        try {
            Response<List<String>> response = relayApiService.getOnlineUsers().execute();
            return handleListResponse(response, "getOnlineUsers");
        } catch (Exception e) {
            Log.e(TAG, "getOnlineUsers error: ", e);
            return Collections.emptyList();
        }
    }

    public ChatDto createChat(ChatDto chat) {
        try {
            Response<ChatDto> response = relayApiService.createChat(chat).execute();
            return handleResponse(response, "createChat");
        } catch (Exception e) {
            Log.e(TAG, "createChat error: ", e);
            return null;
        }
    }

    public ChatDto getChatById(String chatId) {
        try {
            Response<ChatDto> response = relayApiService.getChatById(chatId).execute();
            return handleResponse(response, "getChatById");
        } catch (Exception e) {
            Log.e(TAG, "getChatById error: ", e);
            return null;
        }
    }

    public List<ChatDto> getChatsForUser(String userId) {
        try {
            Response<List<ChatDto>> response = relayApiService.getChatsForUser(userId).execute();
            return handleListResponse(response, "getChatsForUser");
        } catch (Exception e) {
            Log.e(TAG, "getChatsForUser error: ", e);
            return Collections.emptyList();
        }
    }

    public ChatDto updateChat(String chatId, ChatDto chat) {
        try {
            Response<ChatDto> response = relayApiService.updateChat(chatId, chat).execute();
            return handleResponse(response, "updateChat");
        } catch (Exception e) {
            Log.e(TAG, "updateChat error: ", e);
            return null;
        }
    }

    public ChatDto addMemberToChat(String chatId, String userId) {
        try {
            Response<ChatDto> response = relayApiService.addMemberToChat(chatId, userId).execute();
            return handleResponse(response, "addMemberToChat");
        } catch (Exception e) {
            Log.e(TAG, "addMemberToChat error: ", e);
            return null;
        }
    }

    public ChatDto removeMemberFromChat(String chatId, String userId) {
        try {
            Response<ChatDto> response = relayApiService.removeMemberFromChat(chatId, userId).execute();
            return handleResponse(response, "removeMemberFromChat");
        } catch (Exception e) {
            Log.e(TAG, "removeMemberFromChat error: ", e);
            return null;
        }
    }

    public MessageDto sendMessage(MessageDto message) {
        try {
            Response<MessageDto> response = relayApiService.sendMessage(message).execute();
            return handleResponse(response, "sendMessage");
        } catch (Exception e) {
            Log.e(TAG, "sendMessage error: ", e);
            return null;
        }
    }

    public List<MessageDto> getMessagesForChat(String chatId) {
        try {
            Response<List<MessageDto>> response = relayApiService.getMessagesForChat(chatId).execute();
            return handleListResponse(response, "getMessagesForChat");
        } catch (Exception e) {
            Log.e(TAG, "getMessagesForChat error: ", e);
            return Collections.emptyList();
        }
    }

    public List<MessageDto> getOfflineMessages(String userId) {
        try {
            Response<List<MessageDto>> response = relayApiService.getOfflineMessages(userId).execute();
            return handleListResponse(response, "getOfflineMessages");
        } catch (Exception e) {
            Log.e(TAG, "getOfflineMessages error: ", e);
            return Collections.emptyList();
        }
    }

    public boolean markMessageAsRead(String messageId) {
        try {
            Response<Void> response = relayApiService.markMessageAsRead(messageId).execute();
            if (!response.isSuccessful()) {
                Log.w(TAG, "markMessageAsRead failed: " + response.code() + " - " + response.message());
            }
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "markMessageAsRead error: ", e);
            return false;
        }
    }

    public List<MessageDto> searchMessages(String query) {
        try {
            Response<List<MessageDto>> response = relayApiService.searchMessages(query).execute();
            return handleListResponse(response, "searchMessages");
        } catch (Exception e) {
            Log.e(TAG, "searchMessages error: ", e);
            return Collections.emptyList();
        }
    }

    public MediaDto uploadMedia(File file, String uploaderId, MediaType mediaType) {
        try {
            RequestBody requestFile = RequestBody.create(file, okhttp3.MediaType.parse("multipart/form-data"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            Response<MediaDto> response = relayApiService.uploadMedia(filePart, uploaderId, mediaType.name()).execute();
            return handleResponse(response, "uploadMedia");
        } catch (Exception e) {
            Log.e(TAG, "uploadMedia error: ", e);
            return null;
        }
    }

    public MediaDto getMediaById(String mediaId) {
        try {
            Response<MediaDto> response = relayApiService.getMediaById(mediaId).execute();
            return handleResponse(response, "getMediaById");
        } catch (Exception e) {
            Log.e(TAG, "getMediaById error: ", e);
            return null;
        }
    }

    public InputStream downloadMedia(String mediaId) {
        try {
            Response<ResponseBody> response = relayApiService.downloadMedia(mediaId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().byteStream();
            } else {
                Log.w(TAG, "downloadMedia failed: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "downloadMedia error: ", e);
        }
        return null;
    }

    // Helper method for logging and returning data
    private <T> T handleResponse(Response<T> response, String methodName) {
        if (response.isSuccessful()) {
            return response.body();
        } else {
            Log.w(TAG, methodName + " failed: " + response.code() + " - " + response.message());
            return null;
        }
    }

    private <T> List<T> handleListResponse(Response<List<T>> response, String methodName) {
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        } else {
            Log.w(TAG, methodName + " failed: " + response.code() + " - " + response.message());
            return Collections.emptyList();
        }
    }
}
