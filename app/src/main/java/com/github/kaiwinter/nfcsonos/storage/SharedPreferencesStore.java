package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SharedPreferencesStore {

    private static final String KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN";
    private static final String KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN";
    private static final String KEY_EXPIRES_AT = "KEY_EXPIRES_AT";

    private static final String HOUSEHOLD_ID = "HOUSEHOLD_ID";
    private static final String GROUP_ID = "GROUP_ID";

    private SharedPreferences sharedPreferences;

    @Inject
    public SharedPreferencesStore(@ApplicationContext Context context) {
        Log.e("TAG", "SharedPreferencesStore(@ApplicationContext Context context)");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setHouseholdAndGroup(String householdId, String groupId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HOUSEHOLD_ID, householdId);
        editor.putString(GROUP_ID, groupId);
        editor.apply();
    }

    public void setTokens(String refreshToken, String accessToken, long expiresAt) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAt);
        editor.apply();
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public long getExpiresAt() {
        return sharedPreferences.getLong(KEY_EXPIRES_AT, 0);
    }

    public String getHouseholdId() {
        return sharedPreferences.getString(HOUSEHOLD_ID, null);
    }

    public String getGroupId() {
        return sharedPreferences.getString(GROUP_ID, null);
    }
}
