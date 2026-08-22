package com.github.kaiwinter.nfcsonos.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class SharedPreferencesStore {

    private static final String KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN";
    private static final String KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN";

    private static final String HOUSEHOLD_ID = "HOUSEHOLD_ID";
    private static final String GROUP_ID = "GROUP_ID";
    private static final String GROUP_COORDINATOR_ID = "GROUP_COORDINATOR_ID";
    private static final String HOUSEHOLD_NAME = "HOUSEHOLD_NAME"; // Used for display only
    private static final String GROUP_NAME = "GROUP_NAME"; // Used for display only

    private static final String SHUFFLE_PLAYBACK = "SHUFFLE_PLAYBACK";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesStore(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setHouseholdAndGroup(String householdId, String groupId, String groupCoordinatorId, String householdName, String groupName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HOUSEHOLD_ID, householdId);
        editor.putString(GROUP_ID, groupId);
        editor.putString(GROUP_COORDINATOR_ID, groupCoordinatorId);
        editor.putString(HOUSEHOLD_NAME, householdName);
        editor.putString(GROUP_NAME, groupName);
        editor.apply();
    }

    public void setTokens(String refreshToken, String accessToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getHouseholdId() {
        return sharedPreferences.getString(HOUSEHOLD_ID, null);
    }

    public String getGroupId() {
        return sharedPreferences.getString(GROUP_ID, null);
    }

    public String getHouseholdName() {
        return sharedPreferences.getString(HOUSEHOLD_NAME, "");
    }

    public String getGroupName() {
        return sharedPreferences.getString(GROUP_NAME, "");
    }

    /**
     * Returns the setting if the playback should be shuffled.
     * @return true when the playback should be shuffled, false otherwise
     */
    public boolean getShufflePlayback() {
        return sharedPreferences.getBoolean(SHUFFLE_PLAYBACK, false);
    }

    public String getGroupCoordinatorId() {
        return sharedPreferences.getString(GROUP_COORDINATOR_ID, null);
    }
}
