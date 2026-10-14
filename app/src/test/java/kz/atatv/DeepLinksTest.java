package kz.atatv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DeepLinksTest {

    @Test
    public void buildsChannelLink() {
        assertEquals("https://mobi.zona/tv/12345", DeepLinks.channelUri("12345"));
        assertEquals("https://mobi.zona/tv/pervyy-kanal", DeepLinks.channelUri("  pervyy-kanal "));
    }

    @Test
    public void blankIdGivesNoLink() {
        assertNull(DeepLinks.channelUri(null));
        assertNull(DeepLinks.channelUri(""));
        assertNull(DeepLinks.channelUri("   "));
    }

    @Test
    public void encodesUnsafeCharacters() {
        assertEquals("a%2Fb%20c", DeepLinks.encodePathSegment("a/b c"));
        assertEquals("%D0%A5%D0%B0%D0%B1%D0%B0%D1%80", DeepLinks.encodePathSegment("Хабар"));
        assertEquals("A-z_0.9~", DeepLinks.encodePathSegment("A-z_0.9~"));
    }
}
