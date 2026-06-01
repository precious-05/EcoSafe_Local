package com.example.eco_front;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREF_NAME = "EcoSafePrefs";
    private static final String KEY_BASE_URL = "backend_base_url";
    
    // Default fallback URL if nothing is saved in SharedPreferences yet
    private static final String DEFAULT_URL = "http://192.168.106.56:8000/";

    /**
     * Retrieves the saved backend base URL from SharedPreferences.
     * Guaranteed to end with a "/" and start with "http://" or "https://".
     */
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_BASE_URL, DEFAULT_URL);
        
        if (url == null || url.trim().isEmpty()) {
            url = DEFAULT_URL;
        }
        
        url = url.trim();
        
        // Auto-prepend http:// if missing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        
        // Auto-append trailing slash
        if (!url.endsWith("/")) {
            url += "/";
        }
        
        return url;
    }

    /**
     * Saves a new backend base URL to SharedPreferences.
     */
    public static void setBaseUrl(Context context, String newUrl) {
        if (newUrl == null || newUrl.trim().isEmpty()) {
            return;
        }
        
        String formattedUrl = newUrl.trim();
        
        // Ensure starting scheme
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://" + formattedUrl;
        }
        
        // Ensure trailing slash
        if (!formattedUrl.endsWith("/")) {
            formattedUrl += "/";
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, formattedUrl).apply();
    }
}
