package com.example.eco_front;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Remembers when the user cleared history so old server incidents are not re-imported.
 */
public final class HistoryPrefs {

    private static final String PREFS_NAME = "ecosafe_prefs";
    private static final String KEY_HISTORY_CLEARED_AT_MS = "history_cleared_at_ms";

    private HistoryPrefs() {
    }

    public static void markHistoryCleared(Context context) {
        getPrefs(context).edit()
                .putLong(KEY_HISTORY_CLEARED_AT_MS, System.currentTimeMillis())
                .apply();
    }

    public static long getHistoryClearedAtMs(Context context) {
        return getPrefs(context).getLong(KEY_HISTORY_CLEARED_AT_MS, 0L);
    }

    /** Skip backend rows at or before the last clear-all action. */
    public static boolean shouldSkipServerIncident(Context context, long incidentTimeMs) {
        long clearedAt = getHistoryClearedAtMs(context);
        if (clearedAt <= 0L || incidentTimeMs <= 0L) {
            return false;
        }
        return incidentTimeMs <= clearedAt;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
