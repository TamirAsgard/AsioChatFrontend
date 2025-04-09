package com.example.asiochatfrontend.app.di;

import android.content.Context;

import com.example.asiochatfrontend.app.AsioChatFrontendApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    @Provides
    @Singleton
    public Context provideApplicationContext(AsioChatFrontendApplication application) {
        return application.getApplicationContext();
    }
}
