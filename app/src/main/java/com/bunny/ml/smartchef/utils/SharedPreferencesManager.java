package com.bunny.ml.smartchef.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "SmartChefPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_REGISTRATION_COMPLETE = "registration_complete";

    private final SharedPreferences prefs;

    public SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void savePhoneNumber(String phoneNumber) {
        prefs.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply();
    }

    public String getPhoneNumber() {
        return prefs.getString(KEY_PHONE_NUMBER, null);
    }

    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setRegistrationComplete(boolean isComplete) {
        prefs.edit().putBoolean(KEY_REGISTRATION_COMPLETE, isComplete).apply();
    }

    public boolean isRegistrationComplete() {
        return prefs.getBoolean(KEY_REGISTRATION_COMPLETE, false);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
