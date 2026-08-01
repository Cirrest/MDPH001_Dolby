package com.mdph.dolbycontrol;

import java.util.Locale;

final class UiText {
    enum Key {
        APP_TITLE,
        THEME,
        THEME_LIGHT,
        THEME_DARK,
        THEME_SYSTEM,
        CONNECTING,
        GLOBAL_PROCESSING,
        MODE,
        MODE_DYNAMIC,
        MODE_MOVIE,
        MODE_MUSIC,
        MODE_CUSTOM,
        OUTPUT,
        TUNING,
        NO_OUTPUT,
        VOLUME,
        INTELLIGENT_EQUALIZER,
        OFF,
        IEQ_BALANCED,
        IEQ_WARM,
        IEQ_DETAILED,
        DIALOG_ENHANCEMENT,
        SOUNDSTAGE,
        VOLUME_LEVELER,
        HEADPHONE_VIRTUALIZER,
        SPEAKER_VIRTUALIZER,
        GRAPHIC_EQUALIZER_20,
        RESET,
        ENABLE_CUSTOM_EQUALIZER,
        MAINTENANCE,
        RESTART_AUDIO_SERVICE,
        RESTART_REQUESTED,
        CONNECTED_ENABLED,
        CONNECTED_DISABLED,
        ROUTE_UNKNOWN,
        ROUTE_HEADPHONE,
        ROUTE_BLUETOOTH,
        ROUTE_USB,
        ROUTE_HDMI,
        ROUTE_SPEAKER,
        ROUTE_EARPIECE,
        NOT_CONNECTED,
        PROFILES,
        EFFECT,
        CHANNEL_NAME,
        CHANNEL_DESCRIPTION,
        NOTIFICATION_ACTIVE
    }

    private final boolean chinese;

    private UiText(boolean chinese) {
        this.chinese = chinese;
    }

    static UiText forLanguageTag(String languageTag) {
        String normalized = languageTag == null
                ? ""
                : languageTag.trim().toLowerCase(Locale.US);
        return new UiText(
                normalized.equals("zh")
                        || normalized.startsWith("zh-")
                        || normalized.startsWith("zh_"));
    }

    String get(Key key) {
        return chinese ? getChinese(key) : getEnglish(key);
    }

    private static String getEnglish(Key key) {
        switch (key) {
            case APP_TITLE: return "MIAD01 Dolby Atoms";
            case THEME: return "Theme";
            case THEME_LIGHT: return "Light";
            case THEME_DARK: return "Dark";
            case THEME_SYSTEM: return "Follow system";
            case CONNECTING: return "Connecting";
            case GLOBAL_PROCESSING: return "Global Dolby processing";
            case MODE: return "Mode";
            case MODE_DYNAMIC: return "Dynamic";
            case MODE_MOVIE: return "Movie";
            case MODE_MUSIC: return "Music";
            case MODE_CUSTOM: return "Custom";
            case OUTPUT: return "Output";
            case TUNING: return "Tuning";
            case NO_OUTPUT: return "No output";
            case VOLUME: return "Volume";
            case INTELLIGENT_EQUALIZER: return "Intelligent equalizer";
            case OFF: return "Off";
            case IEQ_BALANCED: return "Balanced";
            case IEQ_WARM: return "Warm";
            case IEQ_DETAILED: return "Detailed";
            case DIALOG_ENHANCEMENT: return "Dialogue enhancement";
            case SOUNDSTAGE: return "Soundstage";
            case VOLUME_LEVELER: return "Volume leveler";
            case HEADPHONE_VIRTUALIZER: return "Headphone virtualizer";
            case SPEAKER_VIRTUALIZER: return "Speaker virtualizer";
            case GRAPHIC_EQUALIZER_20: return "20-band graphic equalizer";
            case RESET: return "Reset";
            case ENABLE_CUSTOM_EQUALIZER: return "Enable custom equalizer";
            case MAINTENANCE: return "Maintenance";
            case RESTART_AUDIO_SERVICE: return "Restart audio service";
            case RESTART_REQUESTED: return "Audio service restart requested";
            case CONNECTED_ENABLED: return "Connected, processing enabled";
            case CONNECTED_DISABLED: return "Connected, processing disabled";
            case ROUTE_UNKNOWN: return "Unknown";
            case ROUTE_HEADPHONE: return "Headphones";
            case ROUTE_BLUETOOTH: return "Bluetooth";
            case ROUTE_USB: return "USB";
            case ROUTE_HDMI: return "HDMI";
            case ROUTE_SPEAKER: return "Speaker";
            case ROUTE_EARPIECE: return "Earpiece";
            case NOT_CONNECTED: return "Not connected";
            case PROFILES: return "profiles";
            case EFFECT: return "effect";
            case CHANNEL_NAME: return "Dolby control";
            case CHANNEL_DESCRIPTION: return "Keeps the global Dolby DAP effect active";
            case NOTIFICATION_ACTIVE: return "Global DAP controller is active";
            default: throw new IllegalArgumentException("Unknown text key: " + key);
        }
    }

    private static String getChinese(Key key) {
        switch (key) {
            case APP_TITLE: return "MIAD01 Dolby Atoms";
            case THEME: return "\u4e3b\u9898";
            case THEME_LIGHT: return "\u6d45\u8272";
            case THEME_DARK: return "\u6df1\u8272";
            case THEME_SYSTEM: return "\u8ddf\u968f\u7cfb\u7edf";
            case CONNECTING: return "\u6b63\u5728\u8fde\u63a5";
            case GLOBAL_PROCESSING: return "\u5168\u5c40 Dolby \u5904\u7406";
            case MODE: return "\u6a21\u5f0f";
            case MODE_DYNAMIC: return "\u52a8\u6001";
            case MODE_MOVIE: return "\u7535\u5f71";
            case MODE_MUSIC: return "\u97f3\u4e50";
            case MODE_CUSTOM: return "\u81ea\u5b9a\u4e49";
            case OUTPUT: return "\u8f93\u51fa";
            case TUNING: return "\u8c03\u97f3";
            case NO_OUTPUT: return "\u65e0\u8f93\u51fa";
            case VOLUME: return "\u97f3\u91cf";
            case INTELLIGENT_EQUALIZER: return "\u667a\u80fd\u5747\u8861";
            case OFF: return "\u5173";
            case IEQ_BALANCED: return "\u5e73\u8861";
            case IEQ_WARM: return "\u6e29\u6696";
            case IEQ_DETAILED: return "\u7ec6\u8282";
            case DIALOG_ENHANCEMENT: return "\u5bf9\u8bdd\u589e\u5f3a";
            case SOUNDSTAGE: return "\u97f3\u573a";
            case VOLUME_LEVELER: return "\u97f3\u91cf\u5747\u8861";
            case HEADPHONE_VIRTUALIZER: return "\u8033\u673a\u865a\u62df\u5316";
            case SPEAKER_VIRTUALIZER: return "\u626c\u58f0\u5668\u865a\u62df\u5316";
            case GRAPHIC_EQUALIZER_20: return "20 \u6bb5\u56fe\u5f62\u5747\u8861";
            case RESET: return "\u6e05\u96f6";
            case ENABLE_CUSTOM_EQUALIZER: return "\u542f\u7528\u81ea\u5b9a\u4e49\u5747\u8861";
            case MAINTENANCE: return "\u7ef4\u62a4";
            case RESTART_AUDIO_SERVICE: return "\u91cd\u542f\u97f3\u9891\u670d\u52a1";
            case RESTART_REQUESTED: return "\u5df2\u8bf7\u6c42\u91cd\u542f\u97f3\u9891\u670d\u52a1";
            case CONNECTED_ENABLED: return "\u5df2\u8fde\u63a5\uff0c\u5904\u7406\u5f00\u542f";
            case CONNECTED_DISABLED: return "\u5df2\u8fde\u63a5\uff0c\u5904\u7406\u5173\u95ed";
            case ROUTE_UNKNOWN: return "\u672a\u77e5";
            case ROUTE_HEADPHONE: return "\u8033\u673a";
            case ROUTE_BLUETOOTH: return "\u84dd\u7259";
            case ROUTE_USB: return "USB";
            case ROUTE_HDMI: return "HDMI";
            case ROUTE_SPEAKER: return "\u626c\u58f0\u5668";
            case ROUTE_EARPIECE: return "\u542c\u7b52";
            case NOT_CONNECTED: return "\u672a\u8fde\u63a5";
            case PROFILES: return "\u4e2a\u914d\u7f6e";
            case EFFECT: return "\u6548\u679c";
            case CHANNEL_NAME: return "Dolby \u63a7\u5236";
            case CHANNEL_DESCRIPTION: return "\u4fdd\u6301\u5168\u5c40 Dolby DAP \u6548\u679c\u6d3b\u8dc3";
            case NOTIFICATION_ACTIVE: return "\u5168\u5c40 DAP \u63a7\u5236\u5668\u5df2\u542f\u7528";
            default: throw new IllegalArgumentException("Unknown text key: " + key);
        }
    }
}
