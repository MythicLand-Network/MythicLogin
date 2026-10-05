package cn.mythicland.mythiclogin.infrastructure.database;

import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
    public Optional<Account> findByEmailIgnoreCase(String email) {
        return this.find("LOWER(email) = ?", normalize(email));
    }

    @Override
    public boolean create(Account account) {
        String sql = "INSERT INTO " + this.table()
                + " (username, realname, password, ip, lastlogin, regdate, regip, email, is_logged, has_session, premium_uuid)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return this.update(sql, statement -> {
            statement.setString(1, normalize(account.getUsername()));
            statement.setString(2, account.getRealName());
            statement.setString(3, account.getPassword());
            statement.setString(4, account.getIp());
            statement.setLong(5, account.getLastLogin());
            statement.setLong(6, account.getRegisterDate());
            statement.setString(7, account.getRegisterIp());
            statement.setString(8, account.getEmail());
            statement.setBoolean(9, account.isLogged());
            statement.setBoolean(10, account.isHasSession());
            statement.setString(11, account.getPremiumUuid() == null ? null : account.getPremiumUuid().toString());
        });
    }

    @Override
    public boolean bindPremium(Account expected, UUID premiumUuid) {
        String sql = "UPDATE " + table() + " SET premium_uuid = ? WHERE username = ? AND realname = ?"
                + " AND password = ? AND regdate = ? AND premium_uuid IS NULL";
        return update(sql, statement -> {
            statement.setString(1, premiumUuid.toString());
            statement.setString(2, expected.getUsername());
            statement.setString(3, expected.getRealName());
            statement.setString(4, expected.getPassword());
            statement.setLong(5, expected.getRegisterDate());
        });
    }

    @Override
    public boolean unbindPremium(Account expected, String passwordHash) {
        String sql = "UPDATE " + table() + " SET premium_uuid = NULL, password = ?, has_session = ?"
                + " WHERE username = ? AND realname = ? AND password = ? AND regdate = ? AND premium_uuid = ?";
        return update(sql, statement -> {
            statement.setString(1, passwordHash);
            statement.setBoolean(2, false);
            statement.setString(3, expected.getUsername());
            statement.setString(4, expected.getRealName());
            statement.setString(5, expected.getPassword());
            statement.setLong(6, expected.getRegisterDate());
            statement.setString(7, expected.getPremiumUuid().toString());
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
    public boolean resetPassword(String username, String passwordHash) {
        String sql = "UPDATE " + this.table() + " SET password = ?, has_session = ?, is_logged = ? WHERE username = ?";
        return this.update(sql, statement -> {
            statement.setString(1, passwordHash);
            statement.setBoolean(2, false);
            statement.setBoolean(3, false);
            statement.setString(4, normalize(username));
        });
    }

    @Override
    public boolean updateEmailIfAvailable(String username, String email) {
        String sql = "UPDATE " + this.table() + " SET email = ? WHERE username = ? AND NOT EXISTS "
                + "(SELECT 1 FROM " + this.table() + " WHERE LOWER(email) = ? AND username <> ?)";
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String normalizedUsername = normalize(username);
            statement.setString(1, normalize(email));
            statement.setString(2, normalizedUsername);
            statement.setString(3, normalize(email));
            statement.setString(4, normalizedUsername);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            if (isConstraintViolation(exception)) return false;
            throw new IllegalStateException("Failed to update account email", exception);
        }
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
        String sql = "SELECT username, realname, password, ip, lastlogin, regdate, regip, email, has_session, is_logged, premium_uuid FROM "
                + this.table() + " WHERE " + predicate + " LIMIT 1";
        try (Connection connection = this.databaseManager.dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                String premiumUuid = result.getString("premium_uuid");
                Account account = new Account(
                        result.getString("username"), result.getString("realname"), result.getString("password"),
                        result.getString("ip"), result.getLong("lastlogin"), result.getLong("regdate"),
                        result.getString("regip"), result.getString("email"),
                        result.getBoolean("has_session"), result.getBoolean("is_logged")
                );
                return Optional.of(account.withPremiumUuid(premiumUuid == null ? null : UUID.fromString(premiumUuid)));
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

    private static boolean isConstraintViolation(SQLException exception) {
        String sqlState = exception.getSQLState();
        return exception.getErrorCode() == 19 || (sqlState != null && sqlState.startsWith("23"));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
