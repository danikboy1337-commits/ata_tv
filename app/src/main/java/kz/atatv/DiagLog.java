package kz.atatv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Журнал событий для диагностики на месте: служба подключилась/остановилась,
 * запуск FreeZona, нажата ли вкладка, сбои. Виден в служебном меню.
 */
final class DiagLog {

    private static final String FILE = "ata_tv_diag";
    private static final String KEY = "log";
    private static final int MAX_LINES = 25;

    private DiagLog() {
    }

    static void add(Context context, String message) {
        write(context, message, false);
    }

    /** Синхронная запись — для обработчика сбоев, когда процесс сейчас завершится. */
    static void addNow(Context context, String message) {
        write(context, message, true);
    }

    static String read(Context context) {
        return prefs(context).getString(KEY, "");
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY).apply();
    }

    // commit() намеренно: при сбое процесс завершится раньше, чем apply() успеет записать.
    @SuppressLint("ApplySharedPref")
    private static synchronized void write(Context context, String message, boolean sync) {
        String stamp = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.ROOT).format(new Date());
        SharedPreferences prefs = prefs(context);
        String updated = LogRing.append(prefs.getString(KEY, ""), stamp + "  " + message, MAX_LINES);
        SharedPreferences.Editor editor = prefs.edit().putString(KEY, updated);
        if (sync) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
