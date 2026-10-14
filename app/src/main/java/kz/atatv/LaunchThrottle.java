package kz.atatv;

import java.util.ArrayDeque;

/**
 * Защита от бесконечного цикла «главный экран → FreeZona → вылет → главный экран».
 * Разрешает не больше {@code maxLaunches} запусков за {@code windowMs} миллисекунд.
 */
final class LaunchThrottle {

    private final int maxLaunches;
    private final long windowMs;
    private final ArrayDeque<Long> launches = new ArrayDeque<>();

    LaunchThrottle(int maxLaunches, long windowMs) {
        if (maxLaunches < 1 || windowMs < 1) {
            throw new IllegalArgumentException("maxLaunches and windowMs must be positive");
        }
        this.maxLaunches = maxLaunches;
        this.windowMs = windowMs;
    }

    /** Записывает запуск и возвращает true, если лимит ещё не исчерпан. */
    synchronized boolean tryAcquire(long nowMs) {
        while (!launches.isEmpty() && nowMs - launches.peekFirst() >= windowMs) {
            launches.pollFirst();
        }
        if (launches.size() >= maxLaunches) {
            return false;
        }
        launches.addLast(nowMs);
        return true;
    }

    synchronized void reset() {
        launches.clear();
    }
}
