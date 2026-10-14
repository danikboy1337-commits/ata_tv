package kz.atatv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Запасной автозапуск после включения приставки — на случай, если ATA TV
 * ещё не выбран главным экраном. Если выбран, система откроет его сама.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        if (TargetApp.isDefaultHome(context)) {
            return;
        }
        // Android 10+ не даёт открывать окна из фона без разрешения «поверх других окон».
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !TargetApp.canDrawOverlays(context)) {
            return;
        }
        TargetApp.tryStart(context, new Intent(context, LauncherActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
