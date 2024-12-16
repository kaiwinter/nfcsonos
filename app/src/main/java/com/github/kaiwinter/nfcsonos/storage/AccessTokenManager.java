package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;
import com.github.kaiwinter.nfcsonos.rest.model.AccessToken;

public class AccessTokenManager {

    private final SharedPreferencesStore sharedPreferencesStore;

    public AccessTokenManager(Context context) {
        sharedPreferencesStore = new SharedPreferencesStore(context);
    }

    public String getAccessToken() {
        return sharedPreferencesStore.getAccessToken();
    }

    public String getRefreshToken() {
        return sharedPreferencesStore.getRefreshToken();
    }

    public void setToken(AccessToken accessToken) {
        sharedPreferencesStore.setTokens(accessToken.refreshToken, accessToken.accessToken);
    }
}
