package com.example.asiochatfrontend.app.di;

import android.content.Context;
import androidx.room.Room;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.data.database.utils.MockDataGenerator;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    private static AppDatabase instance;

    public static AppDatabase initialize(Context context) {
        AppDatabase db = Room.databaseBuilder(
                context,
                AppDatabase.class,
                "asiochat_database"
        ).fallbackToDestructiveMigration().build();

        instance = db;
        return db;
    }

    public static AppDatabase getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppDatabase not initialized. Call initialize() first.");
        }

        return instance;
    }
}
