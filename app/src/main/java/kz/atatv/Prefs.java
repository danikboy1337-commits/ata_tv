package kz.atatv;

import android.content.Context;
import android.content.SharedPreferences;

/** Настройки ATA TV, которые меняются в служебном меню. */
final class Prefs {

    /** Пакет FreeZona (проверено по APK 3.0.75). */
    static final String DEFAULT_PACKAGE = "free.zona";
    /** Подпись вкладки в FreeZona 3.0.75 (строка tv_channels). */
    static final String DEFAULT_TAB_LABEL = "ТВ-каналы";

    private static final String FILE = "ata_tv";
    private static final String KEY_PACKAGE = "target_package";
    private static final String KEY_MODE = "launch_mode";
    private static final String KEY_CHANNEL_ID = "channel_id";
    private static final String KEY_TAB_LABEL = "tab_label";

    private final SharedPreferences prefs;

    Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    String targetPackage() {
        return nonBlank(prefs.getString(KEY_PACKAGE, null), DEFAULT_PACKAGE);
    }

    void setTargetPackage(String value) {
        prefs.edit().putString(KEY_PACKAGE, value.trim()).apply();
    }

    LaunchMode mode() {
        return LaunchMode.fromKey(prefs.getString(KEY_MODE, LaunchMode.DEFAULT.key));
    }

    void setMode(LaunchMode mode) {
        prefs.edit().putString(KEY_MODE, mode.key).apply();
    }

    String channelId() {
        return prefs.getString(KEY_CHANNEL_ID, "").trim();
    }

    void setChannelId(String value) {
        prefs.edit().putString(KEY_CHANNEL_ID, value.trim()).apply();
    }

    String tabLabel() {
        return nonBlank(prefs.getString(KEY_TAB_LABEL, null), DEFAULT_TAB_LABEL);
    }

    void setTabLabel(String value) {
        prefs.edit().putString(KEY_TAB_LABEL, value.trim()).apply();
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
