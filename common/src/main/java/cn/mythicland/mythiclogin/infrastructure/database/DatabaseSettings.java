package cn.mythicland.mythiclogin.infrastructure.database;

import java.nio.file.Path;
import java.util.Objects;

public final class DatabaseSettings {
    public static final String TABLE_NAME = "mythiclogin_accounts";
    private static final int MAXIMUM_POOL_SIZE = 4;

    private final DatabaseMode mode;
    private final Path sqliteFile;
    private final String host;
    private final int port;
    private final String name;
    private final String username;
    private final String password;
    private final String table;

    public static DatabaseSettings sqlite(Path file, String table) {
        return new DatabaseSettings(
                DatabaseMode.SQLITE,
                file,
                "127.0.0.1",
                5432,
                "mythiclogin",
                "postgres",
                "",
                table
        );
    }

    public static DatabaseSettings postgresql(
            String host,
            int port,
            String name,
            String username,
            String password,
            String table
    ) {
        return new DatabaseSettings(
                DatabaseMode.POSTGRESQL,
                Path.of("mythiclogin.db"),
                host,
                port,
                name,
                username,
                password,
                table
        );
    }

    public DatabaseSettings(
            DatabaseMode mode,
            Path sqliteFile,
            String host,
            int port,
            String name,
            String username,
            String password,
            String table
    ) {
        this.mode = Objects.requireNonNull(mode, "database mode");
        this.sqliteFile = Objects.requireNonNull(sqliteFile, "SQLite file").toAbsolutePath().normalize();
        this.host = requireText(host, "PostgreSQL host");
        this.port = port;
        this.name = requireIdentifier(name, "PostgreSQL database name");
        this.username = requireText(username, "PostgreSQL username");
        this.password = Objects.requireNonNull(password, "PostgreSQL password");
        this.table = requireIdentifier(table, "account table");
    }

    public String jdbcUrl() {
        if (this.mode == DatabaseMode.SQLITE) return "jdbc:sqlite:" + this.sqliteFile;
        return "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.name;
    }

    public String driverClassName() {
        return this.mode == DatabaseMode.SQLITE ? "org.sqlite.JDBC" : "org.postgresql.Driver";
    }

    public DatabaseMode mode() {
        return this.mode;
    }

    public Path sqliteFile() {
        return this.sqliteFile;
    }

    public String username() {
        return this.username;
    }

    public String password() {
        return this.password;
    }

    public String table() {
        return this.table;
    }

    public int maximumPoolSize() {
        return this.mode == DatabaseMode.SQLITE ? 1 : MAXIMUM_POOL_SIZE;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " cannot be blank");
        return value.trim();
    }

    private static String requireIdentifier(String value, String name) {
        String identifier = requireText(value, name);
        if (!identifier.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + identifier);
        }
        return identifier;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DatabaseSettings that)) return false;
        return this.port == that.port
                && this.mode == that.mode
                && this.sqliteFile.equals(that.sqliteFile)
                && this.host.equals(that.host)
                && this.name.equals(that.name)
                && this.username.equals(that.username)
                && this.password.equals(that.password)
                && this.table.equals(that.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mode, this.sqliteFile, this.host, this.port, this.name, this.username, this.password, this.table);
    }
}
