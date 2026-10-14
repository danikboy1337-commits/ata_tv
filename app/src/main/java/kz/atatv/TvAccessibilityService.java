package kz.atatv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;

/**
 * Один раз после запуска FreeZona нажимает вкладку «ТВ-каналы».
 * Работает только пока {@link AutoClickArm} взведён (до 20 секунд после запуска из ATA TV),
 * события получает только от FreeZona; в остальное время ничего не делает.
 */
public class TvAccessibilityService extends AccessibilityService {

    private static final long EVENT_DEBOUNCE_MS = 250;
    private static final long POLL_INTERVAL_MS = 700;
    /** Ограничение обхода дерева, чтобы не тормозить на тяжёлых экранах. */
    private static final int MAX_NODES = 3000;
    /** Сколько уровней вверх искать нажимаемого родителя у найденной надписи. */
    private static final int MAX_PARENT_DEPTH = 3;

    private static volatile TvAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanRunnable = this::scan;

    /** Вызывается из LauncherActivity сразу после взведения. */
    static void onArmed() {
        TvAccessibilityService service = instance;
        if (service != null) {
            service.restrictToTargetPackage();
            service.scheduleScan(POLL_INTERVAL_MS);
        }
    }

    /** Вызывается из служебного меню: мог поменяться пакет приложения. */
    static void onSettingsChanged() {
        TvAccessibilityService service = instance;
        if (service != null) {
            service.restrictToTargetPackage();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        restrictToTargetPackage();
        if (AutoClickArm.SHARED.isArmed(SystemClock.elapsedRealtime())) {
            scheduleScan(POLL_INTERVAL_MS);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (AutoClickArm.SHARED.isArmedFor(event.getPackageName(), SystemClock.elapsedRealtime())) {
            scheduleScan(EVENT_DEBOUNCE_MS);
        }
    }

    @Override
    public void onInterrupt() {
        // Обратной связи (звук, вибрация) нет — прерывать нечего.
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /** Получать события только от FreeZona (пакет можно поменять в служебном меню). */
    private void restrictToTargetPackage() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            return;
        }
        info.packageNames = new String[] {new Prefs(this).targetPackage()};
        setServiceInfo(info);
    }

    private void scheduleScan(long delayMs) {
        handler.removeCallbacks(scanRunnable);
        handler.postDelayed(scanRunnable, delayMs);
    }

    private void scan() {
        AutoClickArm arm = AutoClickArm.SHARED;
        if (!arm.isArmed(SystemClock.elapsedRealtime())) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && root.getPackageName() != null
                && arm.packageName().contentEquals(root.getPackageName())
                && clickFirstMatch(root, new LabelMatcher(arm.label()))) {
            arm.disarm();
            return;
        }
        // Экран ещё грузится — пробуем снова, пока не истечёт окно.
        scheduleScan(POLL_INTERVAL_MS);
    }

    private boolean clickFirstMatch(AccessibilityNodeInfo root, LabelMatcher matcher) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited < MAX_NODES) {
            AccessibilityNodeInfo node = queue.poll();
            visited++;
            if ((matcher.matches(node.getText()) || matcher.matches(node.getContentDescription()))
                    && click(node)) {
                return true;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return false;
    }

    private boolean click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int depth = 0; current != null && depth <= MAX_PARENT_DEPTH; depth++) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            current = current.getParent();
        }
        return tapCenter(node);
    }

    /** Запасной путь: «тап» по центру надписи (Android 7+). */
    private boolean tapCenter(AccessibilityNodeInfo node) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) {
            return false;
        }
        Path path = new Path();
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        GestureDescription tap = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 50))
                .build();
        return dispatchGesture(tap, null, null);
    }
}
