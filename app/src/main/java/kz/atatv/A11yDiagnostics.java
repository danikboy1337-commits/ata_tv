package kz.atatv;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

/**
 * Только читает системное состояние спецвозможностей — ничего не меняет.
 * Нужна, чтобы по журналу понять, кто и когда выключает службу на конкретной приставке.
 */
final class A11yDiagnostics {

    private static final int MAX_RAW_LENGTH = 160;
    private static boolean watching;

    private A11yDiagnostics() {
    }

    /** Пишет в журнал каждое изменение настроек спецвозможностей, пока жив процесс ATA TV. */
    static synchronized void startWatching(Context context) {
        if (watching) {
            return;
        }
        watching = true;
        Context app = context.getApplicationContext();
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                DiagLog.add(app, "система изменила спецвозможности: ATA TV "
                        + (TargetApp.isAccessibilityEnabled(app) ? "ВКЛЮЧЁН" : "выключен")
                        + ", accessibility_enabled=" + accessibilityEnabledFlag(app));
            }
        };
        ContentResolver resolver = app.getContentResolver();
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, observer);
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED), false, observer);
    }

    /** Несколько строк для служебного меню. */
    static String describe(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("Служба видна системе: ").append(isInstalledService(context) ? "да" : "НЕТ");
        out.append("\naccessibility_enabled = ").append(accessibilityEnabledFlag(context));
        String raw = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        out.append("\nВключённые службы: ").append(raw == null || raw.isEmpty() ? "(пусто)" : shorten(raw));
        String restricted = restrictedSettingsState(context);
        if (restricted != null) {
            out.append("\nОграниченные настройки: ").append(restricted);
        }
        return out.toString();
    }

    private static int accessibilityEnabledFlag(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, -1);
    }

    private static boolean isInstalledService(Context context) {
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) {
            return false;
        }
        ComponentName mine = new ComponentName(context, TvAccessibilityService.class);
        for (AccessibilityServiceInfo info : manager.getInstalledAccessibilityServiceList()) {
            if (mine.equals(ComponentName.unflattenFromString(info.getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Android 13+ запрещает включать спецвозможности приложениям, поставленным из файла,
     * пока пользователь не разрешит «ограниченные настройки». Здесь только чтение этого флага.
     */
    private static String restrictedSettingsState(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null;
        }
        try {
            AppOpsManager ops = context.getSystemService(AppOpsManager.class);
            int mode = ops.unsafeCheckOpNoThrow("android:access_restricted_settings",
                    Process.myUid(), context.getPackageName());
            switch (mode) {
                case AppOpsManager.MODE_ALLOWED:
                    return "разрешены";
                case AppOpsManager.MODE_DEFAULT:
                    return "по умолчанию (mode=3)";
                default:
                    return "ЗАПРЕЩЕНЫ (mode=" + mode + ")";
            }
        } catch (RuntimeException e) {
            return "не удалось проверить: " + e.getClass().getSimpleName();
        }
    }

    private static String shorten(String value) {
        return value.length() <= MAX_RAW_LENGTH ? value : value.substring(0, MAX_RAW_LENGTH) + "…";
    }
}
