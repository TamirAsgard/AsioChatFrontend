package com.example.asiochatfrontend.app.di;

import android.content.Context;
import androidx.room.Room;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.data.database.dao.*;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                "asiochat_database"
        ).fallbackToDestructiveMigration().build();
    }

    @Provides
    public static ChatDao provideChatDao(AppDatabase appDatabase) {
        return appDatabase.chatDao();
    }

    @Provides
    public static MessageDao provideMessageDao(AppDatabase appDatabase) {
        return appDatabase.messageDao();
    }

    @Provides
    public static UserDao provideUserDao(AppDatabase appDatabase) {
        return appDatabase.userDao();
    }

    @Provides
    public static MediaDao provideMediaDao(AppDatabase appDatabase) {
        return appDatabase.mediaDao();
    }

    @Provides
    public static EncryptionKeyDao provideEncryptionKeyDao(AppDatabase appDatabase) {
        return appDatabase.encryptionKeyDao();
    }
}
