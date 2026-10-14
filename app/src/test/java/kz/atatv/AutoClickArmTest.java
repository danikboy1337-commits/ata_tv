package kz.atatv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutoClickArmTest {

    @Test
    public void notArmedInitially() {
        AutoClickArm arm = new AutoClickArm();
        assertFalse(arm.isArmed(0));
        assertFalse(arm.isArmedFor("free.zona", 0));
    }

    @Test
    public void armedOnlyWithinWindowAndForTargetPackage() {
        AutoClickArm arm = new AutoClickArm();
        arm.arm("free.zona", "ТВ-каналы", 1000, 500);
        assertTrue(arm.isArmed(1000));
        assertTrue(arm.isArmedFor("free.zona", 1499));
        assertFalse(arm.isArmedFor("other.app", 1100));
        assertFalse(arm.isArmedFor(null, 1100));
        assertFalse(arm.isArmed(1500));
        assertEquals("ТВ-каналы", arm.label());
        assertEquals("free.zona", arm.packageName());
    }

    @Test
    public void disarmStopsImmediately() {
        AutoClickArm arm = new AutoClickArm();
        arm.arm("free.zona", "ТВ-каналы", 0, 10_000);
        arm.disarm();
        assertFalse(arm.isArmed(1));
    }
}
