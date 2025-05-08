package com.example.asiochatfrontend.domain.repository;

import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for handling media-related data operations.
 */
public interface MediaRepository {

    /**
     * Save media metadata to the repository
     *
     * @param media The media to save
     * @return The saved media with any updated fields
     */
    MediaDto saveMedia(MediaMessageDto media);

    /**
     * Get all media associated with a specific message
     *
     * @param messageId The id of the message
     * @return List of media attached to the message
     */
    MediaDto getMediaForMessage(String messageId);

    /**
     * Get all media associated with a specific chat
     *
     * @param chatId The id of the chat
     * @return List of media shared in the chat
     */
    List<MediaMessageDto> getMediaForChat(String chatId);

    List<MediaMessageDto> getPendingMessages();

    /**
     * Delete media from the repository
     *
     * @param mediaId The id of the media to delete
     * @return true if successful, false otherwise
     */
    boolean deleteMedia(String mediaId);

    MediaDto getMediaById(String mediaId);

    MediaEntity getMediaEntityById(String mediaId);

    MediaEntity getMediaEntityByMessageId(String messageId);

    /**
     * Update the local URI where the media is stored
     *
     * @param mediaId The id of the media
     * @param localUri The local URI where the media is stored
     * @return The updated media
     */
    MediaDto updateLocalUri(String mediaId, String localUri);

    /**
     * Update the thumbnail URI for the media
     *
     * @param mediaId The id of the media
     * @param thumbnailUri The URI of the thumbnail
     * @return The updated media
     */
    MediaDto updateThumbnailUri(String mediaId, String thumbnailUri);

    MessageDto getLastMessageForChat(String chatId);

    int getUnreadMessagesCount(String chatId, String currentUserId);

    void updateMessage(MessageDto message);

    MessageDto getMediaMessageById(String lastMessageId);
}