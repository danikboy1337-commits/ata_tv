package kz.atatv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LaunchThrottleTest {

    @Test
    public void allowsUpToLimitWithinWindow() {
        LaunchThrottle throttle = new LaunchThrottle(3, 1000);
        assertTrue(throttle.tryAcquire(0));
        assertTrue(throttle.tryAcquire(100));
        assertTrue(throttle.tryAcquire(200));
        assertFalse(throttle.tryAcquire(300));
        assertFalse(throttle.tryAcquire(999));
    }

    @Test
    public void slidingWindowFreesOldLaunches() {
        LaunchThrottle throttle = new LaunchThrottle(2, 1000);
        assertTrue(throttle.tryAcquire(0));
        assertTrue(throttle.tryAcquire(500));
        assertFalse(throttle.tryAcquire(900));
        assertTrue(throttle.tryAcquire(1000)); // запуск в 0 мс вышел из окна
        assertFalse(throttle.tryAcquire(1400));
        assertTrue(throttle.tryAcquire(1500));
    }

    @Test
    public void rejectedAttemptsDoNotExtendTheBlock() {
        LaunchThrottle throttle = new LaunchThrottle(1, 1000);
        assertTrue(throttle.tryAcquire(0));
        assertFalse(throttle.tryAcquire(500));
        assertTrue(throttle.tryAcquire(1000));
    }

    @Test
    public void resetClearsHistory() {
        LaunchThrottle throttle = new LaunchThrottle(1, 1000);
        assertTrue(throttle.tryAcquire(0));
        assertFalse(throttle.tryAcquire(10));
        throttle.reset();
        assertTrue(throttle.tryAcquire(20));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidConfig() {
        new LaunchThrottle(0, 1000);
    }
}
