package com.example.asiochatfrontend.data.relay.network;

import android.util.Log;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.database.converter.MediaTypeDeserialize;
import com.example.asiochatfrontend.data.database.converter.MessageStateDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.*;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.InputStream;
import java.util.*;

public class RelayApiClient {
    private static final String TAG = "RelayApiClient";
    private final RelayApiService relayApiService;

    public RelayApiClient(RelayApiService relayApiService) {
        this.relayApiService = relayApiService;
    }

    public static RelayApiClient createInstance(String ip, int port, String userId) {
        String baseUrl = ip + ":" + port + "/";

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(MessageState.class, new MessageStateDeserializer())
                .registerTypeAdapter(MediaType.class, new MediaTypeDeserialize())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return new RelayApiClient(retrofit.create(RelayApiService.class));
    }

    // ==================== AuthService ====================
    // region Public Key
    public boolean registerPublicKey(PublicKeyDto keyDto) {
        try {
            return relayApiService.registerPublicKey(keyDto).execute().isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "registerPublicKey failed", e);
            return false;
        }
    }

    public PublicKeyDto getPublicKeyForTimestamp(String userId, long timestamp) {
        try {
            return relayApiService.getPublicKeyForTimestamp(userId, timestamp).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getPublicKey failed", e);
            return null;
        }
    }

    public List<PublicKeyDto> getAllPublicKeysForUser(String userId) {
        try {
            return relayApiService.getAllPublicKeysForUser(userId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getAllPublicKeysForUser failed", e);
            return Collections.emptyList();
        }
    }
    // endregion

    // region Symmetric Key
    public boolean registerSymmetricKey(SymmetricKeyDto keyDto) {
        try {
            return relayApiService.registerSymmetricKey(keyDto).execute().isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "registerSymmetricKey failed", e);
            return false;
        }
    }

    public SymmetricKeyDto getSymmetricKeyForTimestamp(String chatId, long messageTimestamp) {
        try {
            return relayApiService.getSymmetricKeyForTimestamp(chatId, messageTimestamp).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getSymmetricKeyForTimestamp failed", e);
            return null;
        }
    }
    // endregion

    // ==================== UserService ====================
    // region User
    public void setCurrentUser(String userId) {
        try {
            Object result = relayApiService.createUser(new AuthRequestCredentialsDto(userId)).execute().body();
            if (result != null) {
                Log.i(TAG, "Connection success: user " + userId + " is registered or exists.");
            } else {
                Log.w(TAG, "Connection failed or user not found on server.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection or user creation failed", e);
        }
    }

    public Object createUser(UserDto userDto) {
        try {
            return relayApiService.createUser(new AuthRequestCredentialsDto(userDto.getJid())).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "createUser", e);
            return null;
        }
    }

    public UserDto updateUser(String userId, UpdateUserDetailsDto dto) {
        try {
            return relayApiService.updateUser(userId, dto).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "updateUser", e);
            return null;
        }
    }

    public UserDto getUserById(String userId) {
        try {
            return relayApiService.getUserById(userId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getUserById", e);
            return null;
        }
    }

    public List<UserDto> getContacts() {
        try {
            return relayApiService.getContacts().execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getContacts", e);
            return Collections.emptyList();
        }
    }
    // endregion

    // ==================== ChatService ====================
    // region Chat
    public ChatDto createPrivateChat(ChatDto chatDto) {
        try {
            return relayApiService.createChat(chatDto).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "createPrivateChat", e);
            return null;
        }
    }

    public ChatDto createGroupChat(ChatDto chatDto) {
        try {
            return relayApiService.createChat(chatDto).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "createGroupChat", e);
            return null;
        }
    }

    public List<ChatDto> getChatsForUser(String userId) {
        try {
            return relayApiService.getChatsForUser(userId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getChatsForUser", e);
            return Collections.emptyList();
        }
    }

    public boolean addMemberToGroup(String chatId, String userId) {
        try {
            return relayApiService.addMemberToChat(chatId, userId).execute().isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "addMemberToGroup", e);
            return false;
        }
    }

    public boolean removeMemberFromGroup(String chatId, String userId) {
        try {
            return relayApiService.removeMemberFromChat(chatId, userId).execute().isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "removeMemberFromGroup", e);
            return false;
        }
    }

    public boolean updateGroupName(String chatId, String newName) {
        try {
            ChatDto existing = relayApiService.getChatById(chatId).execute().body();
            if (existing != null) {
                existing.setChatName(newName);
                return relayApiService.updateChat(chatId, existing).execute().isSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateGroupName", e);
        }
        return false;
    }

    public boolean updateGroupRecipients(ChatDto chatDto) {
        try {
            ChatDto existing = relayApiService.getChatById(chatDto.getChatId()).execute().body();
            if (existing != null) {
                existing.setRecipients(new ArrayList<>(chatDto.getRecipients()));
                return relayApiService.updateChat(chatDto.getChatId(), existing).execute().isSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "updateGroupName", e);
        }
        return false;
    }
    // endregion

    // ==================== MessageService ====================
    // region Message
    public MessageDto sendMessage(MessageDto messageDto) {
        try {
            return relayApiService.sendMessage(messageDto).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "sendMessage", e);
            return null;
        }
    }

    public List<TextMessageDto> getMessagesForChat(String chatId) {
        try {
            return relayApiService.getMessagesForChat(chatId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getMessagesForChat", e);
            return Collections.emptyList();
        }
    }

    public List<MessageDto> getOfflineMessages(String userId) {
        try {
            return relayApiService.getOfflineMessages(userId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getOfflineMessages", e);
            return Collections.emptyList();
        }
    }

    public boolean setMessageReadByUser(String messageId, String userId) {
        try {
            return relayApiService.markMessageAsRead(messageId).execute().isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "setMessageReadByUser", e);
            return false;
        }
    }
    // endregion

    // ==================== MediaService ====================
    // region Media
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) {
        try {
            MediaDto payload = mediaMessageDto.getPayload();
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "payload.file",
                    payload.getFileName(),
                    RequestBody.create(FileUtils.readFileToByteArray(payload.getFile()))
            );

            Call<MessageDto> call = relayApiService.uploadMedia(
                    toBody(mediaMessageDto.getId()),
                    toBody(mediaMessageDto.getChatId()),
                    toBody(mediaMessageDto.getJid()),
                    toBody(String.valueOf(mediaMessageDto.getTimestamp().getTime())),
                    toBody(mediaMessageDto.getStatus().name()),
                    toBody(new Gson().toJson(mediaMessageDto.getWaitingMemebersList())),
                    toBody(payload.getFileName()),
                    toBody(payload.getContentType()),
                    toBody(payload.getType().name()),
                    toBody(String.valueOf(payload.getSize())),
                    toBody(payload.getThumbnailPath()),
                    toBody(String.valueOf(payload.getProcessed())),
                    filePart
            );

            MessageDto result = call.execute().body();
            return new MediaMessageDto(
                    result.getId(),
                    result.getWaitingMemebersList(),
                    result.getStatus(),
                    result.getTimestamp(),
                    result.getJid(),
                    result.getChatId(),
                    payload
            );
        } catch (Exception e) {
            Log.e(TAG, "createMediaMessage", e);
            return null;
        }
    }

    public MediaMessageDto getMediaMessage(String mediaId) {
        try {
            MessageDto msg = relayApiService.getMediaById(mediaId).execute().body();
            return new MediaMessageDto(
                    msg.getId(),
                    msg.getWaitingMemebersList(),
                    msg.getStatus(),
                    msg.getTimestamp(),
                    msg.getJid(),
                    msg.getChatId(),
                    null
            );
        } catch (Exception e) {
            Log.e(TAG, "getMediaMessage", e);
            return null;
        }
    }

    public MediaStreamResultDto getMediaStream(String mediaId) {
        try {
            Response<ResponseBody> res = relayApiService.downloadMedia(mediaId).execute();
            if (res.isSuccessful() && res.body() != null) {
                InputStream stream = res.body().byteStream();
                String contentType = res.headers().get("Content-Type");
                String contentDisposition = res.headers().get("Content-Disposition");

                String filename = null;
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    int index = contentDisposition.indexOf("filename=");
                    if (index != -1) {
                        filename = contentDisposition
                                .substring(index + 9)
                                .replace("\"", "");
                    }
                }

                return new MediaStreamResultDto(stream, filename, contentType, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "getMediaStream", e);
        }
        return null;
    }

    public List<MediaMessageDto> getMediaMessagesForChat(String chatId) {
        try {
            return relayApiService.getMediaMessagesForChat(chatId).execute().body();
        } catch (Exception e) {
            Log.e(TAG, "getMediaMessagesForChat", e);
            return Collections.emptyList();
        }
    }
    // endregion

    private RequestBody toBody(String value) {
        return RequestBody.create(value.getBytes());
    }

    public List<String> getOnlineUsers() {
        return Collections.emptyList();
    }
}