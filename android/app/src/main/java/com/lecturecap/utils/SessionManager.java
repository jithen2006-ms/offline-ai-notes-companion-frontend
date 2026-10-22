package com.lecturecap.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "lecture_capture_pref";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_BACKEND_URL = "backend_url";
    private static final String KEY_AUTO_RECORD = "auto_record_enabled";

    public static final String DEFAULT_BACKEND_URL = "http://10.24.99.3:8000/api/lectures/upload";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn, String email, String name) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "student@college.edu");
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Student");
    }

    public String getBackendUrl() {
        String url = pref.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL);
        if (url == null || url.contains("10.0.2.2") || url.contains(":5000") || url.contains("10.168.154.3") || url.contains("10.114.249.3")) {
            url = DEFAULT_BACKEND_URL;
            setBackendUrl(url);
        }
        return url;
    }

    public void setBackendUrl(String url) {
        editor.putString(KEY_BACKEND_URL, url);
        editor.apply();
    }

    public boolean isAutoRecordEnabled() {
        return pref.getBoolean(KEY_AUTO_RECORD, true);
    }

    public void setAutoRecordEnabled(boolean enabled) {
        editor.putBoolean(KEY_AUTO_RECORD, enabled);
        editor.apply();
    }

    public void logout() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
}
