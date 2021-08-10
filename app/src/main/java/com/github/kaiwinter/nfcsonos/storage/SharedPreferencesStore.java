package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferencesStore {

    private static final String KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN";
    private static final String KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN";
    private static final String KEY_EXPIRES_AT = "KEY_EXPIRES_AT";

    private static final String HOUSEHOLD_ID = "HOUSEHOLD_ID";
    private static final String GROUP_ID = "GROUP_ID";
    private static final String GROUP_COORDINATOR_ID = "GROUP_COORDINATOR_ID";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesStore(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setHouseholdAndGroup(String householdId, String groupId, String groupCoordinatorId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HOUSEHOLD_ID, householdId);
        editor.putString(GROUP_ID, groupId);
        editor.putString(GROUP_COORDINATOR_ID, groupCoordinatorId);
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

    public String getGroupCoordinatorId() {
        return sharedPreferences.getString(GROUP_COORDINATOR_ID, null);
    }
}
