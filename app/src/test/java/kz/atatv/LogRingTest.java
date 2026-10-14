package kz.atatv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LogRingTest {

    @Test
    public void appendsToEmpty() {
        assertEquals("a", LogRing.append("", "a", 3));
        assertEquals("a", LogRing.append(null, "a", 3));
    }

    @Test
    public void keepsOnlyLastLines() {
        String log = "";
        for (String line : new String[] {"1", "2", "3", "4", "5"}) {
            log = LogRing.append(log, line, 3);
        }
        assertEquals("3\n4\n5", log);
    }

    @Test
    public void newlinesInsideMessageDoNotBreakRing() {
        assertEquals("a\nb c", LogRing.append("a", "b\nc", 3));
    }
}
