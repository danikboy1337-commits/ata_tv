package kz.atatv;

/** Что ATA TV делает, когда открывает приложение с каналами. */
public enum LaunchMode {
    /** Открыть FreeZona и нажать вкладку «ТВ-каналы» через спецвозможности. */
    TV_CHANNELS("tv_channels"),
    /** Просто открыть FreeZona. */
    OPEN_APP("open_app"),
    /** Сразу включить канал по ID через ссылку https://mobi.zona/tv/&lt;ID&gt;. */
    CHANNEL("channel");

    public static final LaunchMode DEFAULT = TV_CHANNELS;

    /** Ключ для хранения в настройках; не меняется при переименовании констант. */
    public final String key;

    LaunchMode(String key) {
        this.key = key;
    }

    public static LaunchMode fromKey(String key) {
        for (LaunchMode mode : values()) {
            if (mode.key.equals(key)) {
                return mode;
            }
        }
        return DEFAULT;
    }

    /** Следующий режим по кругу — для переключения одной кнопкой в служебном меню. */
    public LaunchMode next() {
        LaunchMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
