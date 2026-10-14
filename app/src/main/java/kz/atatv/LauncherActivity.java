package kz.atatv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Главный экран приставки. Показывает заставку «Включаю телевидение…» и открывает FreeZona.
 * Сюда система возвращает при загрузке, по кнопке «Домой» и при выходе из FreeZona,
 * поэтому дедушка всегда снова попадает в FreeZona.
 * Служебное меню: три нажатия «вверх» во время заставки.
 */
public class LauncherActivity extends Activity {

    private static final long SPLASH_MS = 1500;
    /** Сколько ждать появления вкладки: холодный старт FreeZona с сетью бывает долгим. */
    private static final long AUTO_CLICK_WINDOW_MS = 20_000;
    private static final int SECRET_UP_PRESSES = 3;
    /** Больше 5 запусков за 30 секунд — вероятно, FreeZona вылетает; останавливаемся. */
    private static final LaunchThrottle THROTTLE = new LaunchThrottle(5, 30_000);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable launchRunnable = this::launchTarget;

    private Prefs prefs;
    private TextView title;
    private TextView problem;
    private View problemButtons;
    private Button retryButton;
    private boolean splashShown;
    private boolean stoppedByThrottle;
    private int upPresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        prefs = new Prefs(this);
        title = findViewById(R.id.title);
        problem = findViewById(R.id.problem);
        problemButtons = findViewById(R.id.problem_buttons);
        retryButton = findViewById(R.id.retry);
        retryButton.setOnClickListener(v -> {
            THROTTLE.reset();
            stoppedByThrottle = false;
            startSplash();
        });
        findViewById(R.id.service_menu).setOnClickListener(v -> openServiceMenu());
    }

    /**
     * Новый интент — это явное действие человека (кнопка «Домой» на приставке или
     * «Запустить телевидение» в служебном меню), поэтому снимаем блокировку и пробуем снова.
     * Обычный выход из FreeZona сюда не попадает: система просто возвращает экран (onResume).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        THROTTLE.reset();
        stoppedByThrottle = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stoppedByThrottle) {
            return;
        }
        startSplash();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(launchRunnable);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            // Главный экран никуда не уходит по «Назад».
            return true;
        }
        if (splashShown && code == KeyEvent.KEYCODE_DPAD_UP
                && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            upPresses++;
            if (upPresses >= SECRET_UP_PRESSES) {
                openServiceMenu();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void startSplash() {
        upPresses = 0;
        splashShown = true;
        title.setText(R.string.splash_title);
        problem.setVisibility(View.GONE);
        problemButtons.setVisibility(View.GONE);
        handler.removeCallbacks(launchRunnable);
        handler.postDelayed(launchRunnable, SPLASH_MS);
    }

    private void showProblem(String text) {
        splashShown = false;
        title.setText(R.string.problem_title);
        problem.setText(text);
        problem.setVisibility(View.VISIBLE);
        problemButtons.setVisibility(View.VISIBLE);
        retryButton.requestFocus();
    }

    private void launchTarget() {
        String pkg = prefs.targetPackage();
        Intent intent = TargetApp.buildLaunchIntent(this, prefs);
        if (intent == null) {
            DiagLog.add(this, pkg + " не установлена");
            showProblem(getString(R.string.problem_not_installed, pkg));
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (!THROTTLE.tryAcquire(now)) {
            stoppedByThrottle = true;
            DiagLog.add(this, "остановка: слишком частые перезапуски " + pkg);
            showProblem(getString(R.string.problem_too_many_launches));
            return;
        }
        DiagLog.add(this, "запуск " + pkg + ", режим " + prefs.mode().key
                + (TvAccessibilityService.isRunning() ? "" : " (служба автонажатия не запущена)"));
        if (prefs.mode() == LaunchMode.TV_CHANNELS) {
            AutoClickArm.SHARED.arm(pkg, prefs.tabLabel(), now, AUTO_CLICK_WINDOW_MS);
            TvAccessibilityService.onArmed();
        } else {
            AutoClickArm.SHARED.disarm();
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            AutoClickArm.SHARED.disarm();
            DiagLog.add(this, "не удалось открыть " + pkg + ": " + e);
            showProblem(getString(R.string.problem_cannot_start, pkg));
        }
    }

    private void openServiceMenu() {
        handler.removeCallbacks(launchRunnable);
        splashShown = false;
        startActivity(new Intent(this, ServiceMenuActivity.class));
    }
}
