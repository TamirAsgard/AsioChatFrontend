package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMedia(MediaEntity media);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMediaList(List<MediaEntity> mediaList);

    @Update
    void updateMedia(MediaEntity media);

    @Delete
    void deleteMedia(MediaEntity media);

    @Query("SELECT * FROM media WHERE id = :mediaId")
    MediaEntity getMediaById(String mediaId);

    @Query("SELECT * FROM media WHERE messageId = :messageId")
    MediaEntity getMediaForMessage(String messageId);

    @Query("SELECT * FROM media WHERE chatId = :chatId")
    List<MediaEntity> getAllMediaForChat(String chatId);

    @Query("SELECT * FROM media WHERE chatId = :chatId")
    Flow<List<MediaEntity>> observeMediaForChat(String chatId);

    @Query("UPDATE media SET localUri = :localUri WHERE id = :mediaId")
    int updateLocalUri(String mediaId, String localUri);

    @Query("UPDATE media SET remoteUri = :remoteUri, uploadedAt = :uploadedAt WHERE id = :mediaId")
    int updateRemoteUri(String mediaId, String remoteUri, long uploadedAt);

    @Query("DELETE FROM media WHERE messageId = :messageId")
    void deleteMediaForMessage(String messageId);

    @Query("UPDATE media SET thumbnailUri = :thumbnailUri WHERE id = :mediaId")
    int updateThumbnailUri(String mediaId, String thumbnailUri);

    @Query("SELECT * FROM media WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    MediaEntity getLastMessageForChat(String chatId);

    @Query("SELECT * FROM media WHERE state = 'PENDING'")
    List<MediaEntity> getPendingMessages();
}
