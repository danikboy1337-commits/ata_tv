package kz.atatv;

/** Кольцевой журнал из последних N строк, хранится одной строкой с переводами строк. */
final class LogRing {

    private LogRing() {
    }

    /** Добавляет строку в конец и оставляет не больше {@code maxLines} последних строк. */
    static String append(String existing, String line, int maxLines) {
        String clean = line == null ? "" : line.replace('\n', ' ').replace('\r', ' ');
        String combined = existing == null || existing.isEmpty() ? clean : existing + "\n" + clean;
        String[] lines = combined.split("\n", -1);
        if (lines.length <= maxLines) {
            return combined;
        }
        StringBuilder out = new StringBuilder();
        for (int i = lines.length - maxLines; i < lines.length; i++) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(lines[i]);
        }
        return out.toString();
    }
}
