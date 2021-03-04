package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.rest.login.LoginService;
import com.github.kaiwinter.nfcsonos.rest.favorite.FavoriteService;
import com.github.kaiwinter.nfcsonos.rest.playbackmetadata.PlaybackMetadataService;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    private static final String AUTH_ENDPOINT = "https://api.sonos.com";
    private static final String API_ENDPOINT = "https://api.ws.sonos.com";

    public static LoginService createLoginService() {
        return createRestAdapter(AUTH_ENDPOINT, null).create(LoginService.class);
    }

    public static FavoriteService createFavoriteService(String token) {
        return createRestAdapter(API_ENDPOINT, token).create(FavoriteService.class);
    }

    public static PlaybackMetadataService createPlaybackMetadataService(String token) {
        return createRestAdapter(API_ENDPOINT, token).create(PlaybackMetadataService.class);
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

    public static APIError parseError(retrofit2.Response<?> response) {
        Gson gson = new Gson();
        return gson.fromJson(response.errorBody().charStream(), APIError.class);
    }
}
