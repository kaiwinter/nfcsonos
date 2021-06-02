package com.github.kaiwinter.nfcsonos.rest;

import android.util.Log;

import com.github.kaiwinter.nfcsonos.storage.SharedPreferencesStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class RestModule {

    private static final String AUTH_ENDPOINT = "https://api.sonos.com";
    private static final String API_ENDPOINT = "https://api.ws.sonos.com";

    @Singleton
    @Provides
    FavoriteService provideFavoriteService(SharedPreferencesStore sharedPreferencesStore) {
        Log.e("TAG", "FavoriteService provideFavoriteService()");
        String token = sharedPreferencesStore.getAccessToken();
        return createRestAdapter(API_ENDPOINT, token).create(FavoriteService.class);
    }

    private static Retrofit createRestAdapter(String endpoint, String token) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder defaultHttpClientBuilder = new OkHttpClient.Builder()
                .addInterceptor(interceptor);

        if (token != null) {
            defaultHttpClientBuilder.addInterceptor(chain -> {
                Request authorisedRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + token).build();
                return chain.proceed(authorisedRequest);
            });
        }

        return new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(defaultHttpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
