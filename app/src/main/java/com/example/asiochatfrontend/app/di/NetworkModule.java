package com.example.asiochatfrontend.app.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.database.converter.MessageStateDeserializer;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    @Provides
    @Singleton
    public Moshi provideMoshi() {
        return new Moshi.Builder()
                .add(new KotlinJsonAdapterFactory())
                .build();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Moshi moshi) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(MessageState.class, new MessageStateDeserializer())
                .create();

        return new Retrofit.Builder()
                .baseUrl("http://localhost:8081/") // Replace with your actual API URL
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public RelayApiClient provideRelayApiClient(Retrofit retrofit) {
        return retrofit.create(RelayApiClient.class);
    }
}
