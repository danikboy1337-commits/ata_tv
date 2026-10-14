package kz.atatv;

/**
 * «Взведённое» автонажатие: после запуска FreeZona служба спецвозможностей
 * в течение ограниченного времени ищет вкладку и нажимает её один раз.
 * Вне этого окна служба ничего не трогает, чтобы не мешать дедушке.
 */
final class AutoClickArm {

    static final AutoClickArm SHARED = new AutoClickArm();

    private String packageName;
    private String label;
    private long armedUntilMs;

    synchronized void arm(String packageName, String label, long nowMs, long durationMs) {
        this.packageName = packageName;
        this.label = label;
        this.armedUntilMs = nowMs + durationMs;
    }

    synchronized void disarm() {
        armedUntilMs = 0;
    }

    synchronized boolean isArmed(long nowMs) {
        return packageName != null && nowMs < armedUntilMs;
    }

    synchronized boolean isArmedFor(CharSequence eventPackage, long nowMs) {
        return isArmed(nowMs) && eventPackage != null && packageName.contentEquals(eventPackage);
    }

    synchronized String packageName() {
        return packageName;
    }

    synchronized String label() {
        return label;
    }
}
