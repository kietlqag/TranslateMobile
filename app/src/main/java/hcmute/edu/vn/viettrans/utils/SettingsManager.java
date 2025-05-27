package hcmute.edu.vn.viettrans.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_SAVE_HISTORY = "save_history";
    private static final String KEY_SHOW_NOTIFICATIONS = "show_notifications";
    private static final String KEY_FONT_SIZE = "font_size";

    private final SharedPreferences preferences;

    public SettingsManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSaveHistoryEnabled() {
        return preferences.getBoolean(KEY_SAVE_HISTORY, true);
    }

    public void setSaveHistoryEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply();
    }

    public boolean isShowNotificationsEnabled() {
        return preferences.getBoolean(KEY_SHOW_NOTIFICATIONS, true);
    }

    public void setShowNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHOW_NOTIFICATIONS, enabled).apply();
    }

    public int getFontSize() {
        return preferences.getInt(KEY_FONT_SIZE, 16);
    }

    public void setFontSize(int size) {
        preferences.edit().putInt(KEY_FONT_SIZE, size).apply();
    }
} 