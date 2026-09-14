package cn.mythicland.mythiclogin.infrastructure.database;

import java.util.Locale;

public enum DatabaseMode {
    SQLITE,
    POSTGRESQL;

    public static DatabaseMode parse(String value) {
        if (value == null || value.isBlank()) return SQLITE;
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "SQLITE" -> SQLITE;
            case "POSTGRES", "POSTGRESQL" -> POSTGRESQL;
            default -> throw new IllegalArgumentException("Unsupported database mode: " + value);
        };
    }
}
