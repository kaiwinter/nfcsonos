package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;
import android.util.Base64;

import androidx.core.util.Consumer;

import com.github.kaiwinter.nfcsonos.R;
import com.github.kaiwinter.nfcsonos.rest.LoginService;
import com.github.kaiwinter.nfcsonos.rest.ServiceFactory;
import com.github.kaiwinter.nfcsonos.rest.model.AccessToken;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessTokenManager {

    private static final long EXPIRE_TOLERANCE_SECONDS = 60;

    private final SharedPreferencesStore sharedPreferencesStore;

    public AccessTokenManager(Context context) {
        sharedPreferencesStore = new SharedPreferencesStore(context);
    }

    /**
     * Checks if a refresh of the token is necessary. A tolerance of {@link #EXPIRE_TOLERANCE_SECONDS} if considered.
     *
     * @return true if a refresh is necessary, false otherwise
     */
    public boolean accessTokenRefreshNeeded() {
        long expiresAt = sharedPreferencesStore.getExpiresAt();
        long currentTimestamp = new Date().getTime();

        return currentTimestamp + EXPIRE_TOLERANCE_SECONDS >= expiresAt;
    }

    /**
     * Refreshes the access token asynchronously.
     *
     * @param context   the context to load string resources
     * @param onSuccess called if the token refresh was successful
     * @param onError   called if the token refresh failed, an error message is passed
     */
    public void refreshAccessToken(Context context, Runnable onSuccess, Consumer<String> onError) {
        LoginService loginService = ServiceFactory.createLoginService();

        String refreshToken = sharedPreferencesStore.getRefreshToken();

        String basic = context.getString(R.string.client_id) + ":" + context.getString(R.string.client_secret);
        String authHeader = "Basic " + Base64.encodeToString(basic.getBytes(), Base64.NO_WRAP);
        Call<AccessToken> call = loginService.refreshToken(authHeader, refreshToken, "refresh_token");
        call.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {

                if (response.code() == 200) {
                    AccessToken body = response.body();
                    long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000;
                    sharedPreferencesStore.setTokens(body.refreshToken, body.accessToken, expiresAt);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    String message = ServiceFactory.handleError(context, response);
                    if (onError != null) {
                        onError.accept(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                String errormessage = context.getString(R.string.error, t.getMessage());
                if (onError != null) {
                    onError.accept(errormessage);
                }
            }
        });
    }
}
