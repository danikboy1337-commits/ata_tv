package kz.atatv;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** Всё, что касается запускаемого приложения и состояния системы. */
final class TargetApp {

    private TargetApp() {
    }

    /** Интент для запуска FreeZona в выбранном режиме или null, если она не установлена. */
    static Intent buildLaunchIntent(Context context, Prefs prefs) {
        PackageManager pm = context.getPackageManager();
        String pkg = prefs.targetPackage();
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch == null) {
            launch = pm.getLaunchIntentForPackage(pkg);
        }
        if (launch == null) {
            return null;
        }
        if (prefs.mode() == LaunchMode.CHANNEL) {
            String uri = DeepLinks.channelUri(prefs.channelId());
            if (uri != null) {
                // Явный интент: путь /tv не объявлен в intent-filter FreeZona.
                Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                view.setComponent(launch.getComponent());
                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return view;
            }
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return launch;
    }

    /** Версия установленного приложения или null, если его нет. */
    static String installedVersion(Context context, String pkg) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
            return info.versionName == null ? "?" : info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Пакет текущего главного экрана по умолчанию или null, если он не выбран. */
    static String defaultHomePackage(Context context) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager()
                .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) {
            return null;
        }
        String pkg = info.activityInfo.packageName;
        // Если умолчание не выбрано, система возвращает свой экран выбора («android»).
        return "android".equals(pkg) ? null : pkg;
    }

    static boolean isDefaultHome(Context context) {
        return context.getPackageName().equals(defaultHomePackage(context));
    }

    static boolean isAccessibilityEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) {
            return false;
        }
        ComponentName mine = new ComponentName(context, TvAccessibilityService.class);
        for (String entry : enabled.split(":")) {
            if (mine.equals(ComponentName.unflattenFromString(entry))) {
                return true;
            }
        }
        return false;
    }

    /** Разрешение «поверх других окон»; на Android 5 его не существует — считаем, что есть. */
    static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    /** Открывает экран системы, если он есть на этой прошивке. */
    static boolean tryStart(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
