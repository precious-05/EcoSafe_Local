package com.example.eco_front;

import android.content.Context;
import android.content.SharedPreferences;

public final class PrivacyPrefs {

    private static final String PREFS_NAME = "ecosafe_prefs";
    private static final String KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted";

    private PrivacyPrefs() {
    }

    public static boolean isAccepted(Context context) {
        return getPrefs(context).getBoolean(KEY_PRIVACY_ACCEPTED, false);
    }

    public static void setAccepted(Context context, boolean accepted) {
        getPrefs(context).edit().putBoolean(KEY_PRIVACY_ACCEPTED, accepted).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
