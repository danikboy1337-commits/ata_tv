package kz.atatv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LaunchModeTest {

    @Test
    public void parsesKnownKeys() {
        for (LaunchMode mode : LaunchMode.values()) {
            assertEquals(mode, LaunchMode.fromKey(mode.key));
        }
    }

    @Test
    public void unknownOrMissingKeyFallsBackToDefault() {
        assertEquals(LaunchMode.TV_CHANNELS, LaunchMode.fromKey(null));
        assertEquals(LaunchMode.TV_CHANNELS, LaunchMode.fromKey("garbage"));
    }

    @Test
    public void nextCyclesThroughAllModes() {
        assertEquals(LaunchMode.OPEN_APP, LaunchMode.TV_CHANNELS.next());
        assertEquals(LaunchMode.CHANNEL, LaunchMode.OPEN_APP.next());
        assertEquals(LaunchMode.TV_CHANNELS, LaunchMode.CHANNEL.next());
    }
}
