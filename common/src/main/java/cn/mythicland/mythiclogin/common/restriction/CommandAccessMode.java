package cn.mythicland.mythiclogin.common.restriction;

import java.util.Locale;

public enum CommandAccessMode {
    WHITELIST,
    BLACKLIST;

    public static CommandAccessMode fromString(String value, CommandAccessMode fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return CommandAccessMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
