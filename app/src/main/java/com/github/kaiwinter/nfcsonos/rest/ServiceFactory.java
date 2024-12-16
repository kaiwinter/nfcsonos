package com.github.kaiwinter.nfcsonos.rest;

import com.github.kaiwinter.nfcsonos.BuildConfig;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceFactory {

    private static final String AUTH_ENDPOINT = "https://api.sonos.com";
    public static final String API_ENDPOINT = "https://api.ws.sonos.com";

    private final String apiEndpoint;
    private final AccessTokenManager accessTokenManager;

    public ServiceFactory(String apiEndpoint, AccessTokenManager accessTokenManager) {
        this.apiEndpoint = apiEndpoint;
        this.accessTokenManager = accessTokenManager;
    }

    public static LoginService createLoginService() {
        return createRestAdapter(AUTH_ENDPOINT, null, null).create(LoginService.class);
    }

    public FavoriteService createFavoriteService(Runnable loadingState) {
        return createRestAdapter(apiEndpoint, accessTokenManager, loadingState).create(FavoriteService.class);
    }

    public PlaybackMetadataService createPlaybackMetadataService(Runnable loadingState) {
        return createRestAdapter(apiEndpoint, accessTokenManager, loadingState).create(PlaybackMetadataService.class);
    }

    public DiscoverService createDiscoverService(Runnable loadingState) {
        return createRestAdapter(apiEndpoint, accessTokenManager, loadingState).create(DiscoverService.class);
    }

    public GroupVolumeService createGroupVolumeService(Runnable loadingState) {
        return createRestAdapter(apiEndpoint, accessTokenManager, loadingState).create(GroupVolumeService.class);
    }

    public PlaybackService createPlaybackService(Runnable loadingState) {
        return createRestAdapter(apiEndpoint, accessTokenManager, loadingState).create(PlaybackService.class);
    }

    private static Retrofit createRestAdapter(String endpoint, AccessTokenManager accessTokenManager, Runnable loadingState) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

        if (accessTokenManager != null) {
            okHttpClientBuilder.authenticator(new RestAuthenticator(accessTokenManager, loadingState));
        }
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.level(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(interceptor);
        }
        if (accessTokenManager != null && accessTokenManager.getAccessToken() != null) {
            okHttpClientBuilder.addInterceptor(chain -> {
                Request authorisedRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + accessTokenManager.getAccessToken()).build();
                return chain.proceed(authorisedRequest);
            });
        }

        return new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(okHttpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Parses the errorBody of a {@link Response} into an {@link APIError}.
     *
     * @param response the Retrofit response
     * @return an {@link APIError} object
     */
    public static APIError parseError(retrofit2.Response<?> response) {
        try (var responseErrorBody = response.errorBody()) {
            if (responseErrorBody == null) {
                return new APIError(response.code(), "", "Error body was empty");
            }

            MediaType mediaType = responseErrorBody.contentType();
            if (mediaType != null && "text".equals(mediaType.type()) && "plain".equals(mediaType.subtype())) {
                try {
                    return new APIError(response.code(), "", responseErrorBody.string());
                } catch (IOException e) {
                    return new APIError(response.code(), "", "Couldn't read error body");
                }
            } else if (mediaType != null && "application".equals(mediaType.type()) && "json".equals(mediaType.subtype())) {
                Gson gson = new Gson();
                APIError apiError = gson.fromJson(responseErrorBody.charStream(), APIError.class);
                apiError.httpCode = response.code();
                return apiError;
            }

            return new APIError(response.code(), "", "Couldn't read error response");
        }
    }
}
