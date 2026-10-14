package kz.atatv;

import java.nio.charset.StandardCharsets;

/**
 * Ссылки, которые понимает FreeZona 3.0.x (проверено по декомпилированному коду 3.0.75:
 * MainActivity разбирает путь /&lt;тип&gt;/&lt;id&gt;, тип "tv" открывает плеер канала).
 * Путь /tv не входит в intent-filter FreeZona, поэтому ссылку нужно отправлять
 * явным интентом прямо в её активити, а не через браузер.
 */
final class DeepLinks {

    static final String TV_CHANNEL_BASE = "https://mobi.zona/tv/";

    private DeepLinks() {
    }

    /** Ссылка на канал или null, если ID пустой. */
    static String channelUri(String channelId) {
        if (channelId == null) {
            return null;
        }
        String id = channelId.trim();
        if (id.isEmpty()) {
            return null;
        }
        return TV_CHANNEL_BASE + encodePathSegment(id);
    }

    /** Процентное кодирование сегмента пути (RFC 3986, unreserved остаются как есть). */
    static String encodePathSegment(String segment) {
        StringBuilder out = new StringBuilder();
        for (byte b : segment.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xF, 16)));
            }
        }
        return out.toString();
    }
}
