package com.github.kaiwinter.nfcsonos.rest;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.kaiwinter.nfcsonos.BuildConfig;
import com.github.kaiwinter.nfcsonos.rest.model.AccessToken;
import com.github.kaiwinter.nfcsonos.storage.AccessTokenManager;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class RestAuthenticator implements Authenticator {
    private final AccessTokenManager accessTokenManager;
    private final Runnable loadingState;

    public RestAuthenticator(AccessTokenManager accessTokenManager, Runnable loadingState) {
        this.accessTokenManager = accessTokenManager;
        this.loadingState = loadingState;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        synchronized(this) {
            // If there were an authorization header in the request, the token must be invalid now
            if (response.request().header("Authorization") != null) {
                LoginService loginService = ServiceFactory.createLoginService();

                String basic = BuildConfig.CLIENT_ID + ":" + BuildConfig.CLIENT_SECRET;
                String authHeader = "Basic " + Base64.encodeToString(basic.getBytes(), Base64.NO_WRAP);

                if (loadingState != null) {
                    loadingState.run();
                }
                Call<AccessToken> call = loginService.refreshToken(authHeader, accessTokenManager.getRefreshToken(), "refresh_token");
                retrofit2.Response<AccessToken> responseCall = call.execute();
                AccessToken accessToken = responseCall.body();
                if (accessToken != null) {
                    accessTokenManager.setToken(accessToken);
                }
            }
            return response.request()
                    .newBuilder()
                    .header("Authorization", "Bearer " + accessTokenManager.getAccessToken())
                    .build();
        }
    }
}
