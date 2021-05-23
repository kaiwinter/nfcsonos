package com.github.kaiwinter.nfcsonos.rest;

import android.content.Context;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.rest.model.APIError;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
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

    public static DiscoverService createDiscoverService(String token) {
        return createRestAdapter(API_ENDPOINT, token).create(DiscoverService.class);
    }

    public static GroupVolumeService createGroupVolumeService(String token) {
        return createRestAdapter(API_ENDPOINT, token).create(GroupVolumeService.class);
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

    /**
     * Parses the errorBody of a {@link Response} into an {@link APIError}.
     *
     * @param response the Retrofit response
     * @return an {@link APIError} object
     */
    public static APIError parseError(retrofit2.Response<?> response) {
        Gson gson = new Gson();
        if (response.errorBody() == null) {
            APIError apiError = new APIError();
            apiError.errorCode = "";
            apiError.reason = "Error body was empty";
            return apiError;
        }
        return gson.fromJson(response.errorBody().charStream(), APIError.class);
    }

    /**
     * Parses the response of a unsuccessful REST service call and tries to figure out an error message which will be returned.
     *
     * @param context  Activity context
     * @param response unsuccessful REST call response
     * @return the error message which should be shown to tbe user.
     */
    public static String handleError(Context context, Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return context.getString(R.string.failed_response_error, 1);
        }
        MediaType mediaType = errorBody.contentType();
        if ("text".equals(mediaType.type()) && "plain".equals(mediaType.subtype())) {
            try {
                return errorBody.string();
            } catch (IOException e) {
                return context.getString(R.string.failed_response_error, 2);
            }
        } else if ("application".equals(mediaType.type()) && "json".equals(mediaType.subtype())) {
            APIError apiError = ServiceFactory.parseError(response);
            return context.getString(R.string.error_long, response.code(), apiError.errorCode, apiError.reason);
        } else {
            try {
                return errorBody.string();
            } catch (IOException e) {
                return context.getString(R.string.failed_response_error, 3);
            }
        }
    }
}
