package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;

import androidx.core.util.Consumer;

import java.util.Date;

public class AccessTokenManager {
    private static final long EXPIRE_TOLERANCE_SECONDS = 60;

    private final SharedPreferencesTokenStore tokenstore;

    public AccessTokenManager(Context context) {
        tokenstore = new SharedPreferencesTokenStore(context);
    }

    public boolean accessTokenRefreshNeeded() {
        long expiresAt = tokenstore.getExpiresAt();
        long currentTimestamp = new Date().getTime() / 1000;

        return currentTimestamp + EXPIRE_TOLERANCE_SECONDS >= expiresAt;
    }

    public void refreshAccessToken(Context context, Runnable onSuccess, Consumer<String> onError) {
//        LoginService loginService = ServiceGenerator.createService(LoginService.class);
//
//        String clientId = context.getString(R.string.client_id);
//        String clientSecret = context.getString(R.string.client_secret);
//        String refreshToken = tokenstore.getRefreshToken();
//        Call<AccessToken> call = loginService.refreshToken(clientId, clientSecret, "refresh_token", refreshToken);
//        call.enqueue(new Callback<AccessToken>() {
//            @Override
//            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
//                Log.e(TAG, "success");
//
//                if (response.code() == 200) {
//                    AccessToken body = response.body();
//                    long expiresAt = System.currentTimeMillis() / 1000 + body.expiresIn;
//                    tokenstore.setTokens(body.refreshToken, body.accessToken, expiresAt);
//                    onSuccess.run();
//                } else {
//                    APIError apiError = ServiceGenerator.parseError(response);
//                    String errormessage = apiError.error.message + " (" + response.code() + ", " + apiError.error.code + ")";
//                    onError.accept(errormessage);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<AccessToken> call, Throwable t) {
//                Log.e(TAG, "fail");
//                String errormessage = context.getString(R.string.main_load_error, t.getMessage());
//                onError.accept(errormessage);
//            }
//        });
    }
}
