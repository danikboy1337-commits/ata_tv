package kz.atatv;

import android.app.Application;
import android.util.Log;

/** Записывает сбои приложения в журнал диагностики, чтобы их было видно без компьютера. */
public class AtaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Повторная строка после включения тумблера = процесс ATA TV убивали (например, force-stop).
        DiagLog.add(this, "процесс ATA TV запущен");
        A11yDiagnostics.startWatching(this);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StackTraceElement[] stack = error.getStackTrace();
                String where = stack.length > 0 ? " @ " + stack[0] : "";
                DiagLog.addNow(this, "СБОЙ: " + error + where);
            } catch (RuntimeException e) {
                Log.w("AtaApp", "cannot record crash", e);
            }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }
}
