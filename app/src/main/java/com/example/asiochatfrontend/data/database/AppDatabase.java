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
        version = 7,
        exportSchema = false
)
@TypeConverters({DateTimeConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();
    public abstract MessageDao messageDao();
    public abstract UserDao userDao();
    public abstract MediaDao mediaDao();
    public abstract EncryptionKeyDao encryptionKeyDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AppDatabase.class, "asiochat_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
