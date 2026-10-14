package kz.atatv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LabelMatcherTest {

    private final LabelMatcher matcher = new LabelMatcher("ТВ-каналы");

    @Test
    public void matchesExactLabel() {
        assertTrue(matcher.matches("ТВ-каналы"));
    }

    @Test
    public void ignoresCaseSpacesAndDashKind() {
        assertTrue(matcher.matches("тв-КАНАЛЫ"));
        assertTrue(matcher.matches("  ТВ-каналы \n"));
        assertTrue(matcher.matches("ТВ‑каналы"));
        assertTrue(matcher.matches("ТВ – каналы"));
        assertTrue(matcher.matches("ТВ каналы"));
        assertTrue(matcher.matches("ТВ каналы"));
    }

    @Test
    public void rejectsOtherLabelsContainingTarget() {
        assertFalse(matcher.matches("Избранные ТВ-каналы"));
        assertFalse(matcher.matches("Удалить из избранных ТВ-каналов"));
        assertFalse(matcher.matches("ТВ-канал недоступен"));
        assertFalse(matcher.matches("ТВканалы"));
    }

    @Test
    public void matchesComposeMergedText() {
        assertTrue(matcher.matches("ТВ-каналы, 200"));
        assertTrue(matcher.matches("Новое, ТВ-каналы"));
        assertFalse(matcher.matches("Избранные ТВ-каналы, 5"));
    }

    @Test
    public void nullAndEmptyNeverMatch() {
        assertFalse(matcher.matches(null));
        assertFalse(matcher.matches(""));
        assertFalse(new LabelMatcher("  ").matches(""));
        assertFalse(new LabelMatcher(null).matches("ТВ-каналы"));
    }

    @Test
    public void normalizeCollapsesSeparators() {
        assertEquals("тв каналы", LabelMatcher.normalize(" ТВ -— Каналы "));
        assertEquals("", LabelMatcher.normalize(null));
    }
}
