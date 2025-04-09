package com.example.asiochatfrontend.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.asiochatfrontend.data.database.converter.DateTimeConverter;
import com.example.asiochatfrontend.data.database.converter.ListConverter;
import com.example.asiochatfrontend.data.database.dao.ChatDao;
import com.example.asiochatfrontend.data.database.dao.EncryptionKeyDao;
import com.example.asiochatfrontend.data.database.dao.MediaDao;
import com.example.asiochatfrontend.data.database.dao.MessageDao;
import com.example.asiochatfrontend.data.database.dao.UserDao;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;
import com.example.asiochatfrontend.data.database.entity.MediaEntity;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import com.example.asiochatfrontend.data.database.entity.UserEntity;

@Database(
        entities = {
                ChatEntity.class,
                MessageEntity.class,
                UserEntity.class,
                MediaEntity.class,
                EncryptionKeyEntity.class
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({DateTimeConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract UserDao userDao();
    public abstract MediaDao mediaDao();
    public abstract EncryptionKeyDao encryptionKeyDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "asiochat_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
