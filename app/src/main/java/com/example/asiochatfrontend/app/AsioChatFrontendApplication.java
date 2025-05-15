package com.example.asiochatfrontend.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class AsioChatFrontendApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}