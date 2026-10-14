package kz.atatv;

import java.util.Locale;

/**
 * Сравнивает подпись элемента интерфейса с искомой («ТВ-каналы»), не обращая внимания
 * на регистр, лишние пробелы и вид дефиса (обычный, неразрывный, тире).
 * Совпадение только полное (целиком или одной из частей через запятую):
 * «Избранные ТВ-каналы» не подходит.
 */
final class LabelMatcher {

    private final String normalizedTarget;

    LabelMatcher(String target) {
        this.normalizedTarget = normalize(target);
    }

    boolean matches(CharSequence text) {
        if (text == null || normalizedTarget.isEmpty()) {
            return false;
        }
        String value = text.toString();
        if (normalize(value).equals(normalizedTarget)) {
            return true;
        }
        // Jetpack Compose (на нём сделана FreeZona 3.x) склеивает тексты дочерних
        // элементов кнопки через ", " — например «ТВ-каналы, 200».
        for (String part : value.split(",")) {
            if (normalize(part).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    /** Нижний регистр, все дефисы/тире/пробелы → один пробел, обрезка по краям. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        boolean pendingSeparator = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isSeparator(c)) {
                pendingSeparator = true;
                continue;
            }
            if (pendingSeparator && out.length() > 0) {
                out.append(' ');
            }
            pendingSeparator = false;
            out.append(c);
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isSeparator(char c) {
        switch (c) {
            case '-':
            case '‐': // hyphen
            case '‑': // non-breaking hyphen
            case '‒': // figure dash
            case '–': // en dash
            case '—': // em dash
            case '−': // minus sign
            case ' ': // no-break space
            case ' ': // narrow no-break space
                return true;
            default:
                return Character.isWhitespace(c);
        }
    }
}
