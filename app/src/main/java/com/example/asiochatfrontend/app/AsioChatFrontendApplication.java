package com.example.asiochatfrontend.app;

import android.app.Application;
import android.content.Context;

import com.example.asiochatfrontend.app.di.DatabaseModule;

public class AsioChatFrontendApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }
}