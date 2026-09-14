package cn.mythicland.mythiclogin.infrastructure.database;

import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public final class JdbcAccountRepository implements AccountRepository {
    private final DatabaseManager databaseManager;
    private final Supplier<DatabaseSettings> settingsSupplier;

    public JdbcAccountRepository(DatabaseManager databaseManager, Supplier<DatabaseSettings> settingsSupplier) {
        this.databaseManager = databaseManager;
        this.settingsSupplier = settingsSupplier;
    }

    @Override
    public Optional<Account> findByExactPlayerName(String playerName) {
        return this.find("realname = ?", playerName);
    }

    @Override
    public Optional<Account> findByPlayerNameIgnoreCase(String playerName) {
        return this.find("LOWER(realname) = ?", normalize(playerName));
    }

    @Override
    public boolean create(Account account) {
        String sql = "INSERT INTO " + this.table() + " (username, realname, password, ip, lastlogin, regdate, regip, is_logged, has_session) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return this.update(sql, statement -> {
            statement.setString(1, normalize(account.getUsername()));
            statement.setString(2, account.getRealName());
            statement.setString(3, account.getPassword());
            statement.setString(4, account.getIp());
            statement.setLong(5, account.getLastLogin());
            statement.setLong(6, account.getRegisterDate());
            statement.setString(7, account.getRegisterIp());
            statement.setBoolean(8, account.isLogged());
            statement.setBoolean(9, account.isHasSession());
        });
    }

    @Override
    public boolean updatePassword(String username, String passwordHash) {
        return this.update("UPDATE " + this.table() + " SET password = ? WHERE username = ?", statement -> {
            statement.setString(1, passwordHash);
            statement.setString(2, normalize(username));
        });
    }

    @Override
    public boolean updateLoginMeta(String username, String ip, long lastLogin, boolean hasSession) {
        return this.update("UPDATE " + this.table() + " SET ip = ?, lastlogin = ?, has_session = ? WHERE username = ?", statement -> {
            statement.setString(1, ip);
            statement.setLong(2, lastLogin);
            statement.setBoolean(3, hasSession);
            statement.setString(4, normalize(username));
        });
    }

    @Override
    public boolean updateLoggedState(String username, boolean logged) {
        return this.update("UPDATE " + this.table() + " SET is_logged = ? WHERE username = ?", statement -> {
            statement.setBoolean(1, logged);
            statement.setString(2, normalize(username));
        });
    }

    @Override
    public boolean isLogged(String username) {
        String sql = "SELECT is_logged FROM " + this.table() + " WHERE username = ?";
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalize(username));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read logged state for " + username, exception);
        }
    }

    @Override
    public boolean deleteByExactPlayerName(String playerName) {
        return this.update("DELETE FROM " + this.table() + " WHERE realname = ?", statement -> statement.setString(1, playerName));
    }

    @Override
    public int countAccounts() {
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + this.table());
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count accounts", exception);
        }
    }

    private Optional<Account> find(String predicate, String value) {
        String sql = "SELECT username, realname, password, ip, lastlogin, regdate, regip, has_session, is_logged FROM " + this.table() + " WHERE " + predicate + " LIMIT 1";
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new Account(
                        result.getString("username"), result.getString("realname"), result.getString("password"),
                        result.getString("ip"), result.getLong("lastlogin"), result.getLong("regdate"),
                        result.getString("regip"), result.getBoolean("has_session"), result.getBoolean("is_logged")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load account " + value, exception);
        }
    }

    private boolean update(String sql, StatementBinder binder) {
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update account storage", exception);
        }
    }

    private String table() {
        return '"' + this.settingsSupplier.get().table() + '"';
    }

    private static String normalize(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
