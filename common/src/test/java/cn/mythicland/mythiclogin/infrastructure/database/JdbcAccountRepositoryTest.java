package cn.mythicland.mythiclogin.infrastructure.database;

import cn.mythicland.mythiclogin.domain.account.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAccountRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;

    @AfterEach
    void closeDatabase() {
        if (this.databaseManager != null) this.databaseManager.shutdown();
    }

    @Test
    void shouldPersistAndUpdateAccountInSqlite() {
        DatabaseSettings settings = DatabaseSettings.sqlite(
                this.temporaryDirectory.resolve("accounts.db"),
                "accounts"
        );
        this.databaseManager = new DatabaseManager(Logger.getLogger("test"), settings, "MythicLoginTest");
        JdbcAccountRepository repository = new JdbcAccountRepository(this.databaseManager, () -> settings);
        Account account = new Account(
                "tester",
                "Tester",
                "hash",
                "127.0.0.1",
                1L,
                1L,
                "127.0.0.1",
                true,
                false
        );

        assertTrue(repository.create(account));
        assertEquals(1, repository.countAccounts());
        assertTrue(repository.findByExactPlayerName("Tester").isPresent());
        assertTrue(repository.findByPlayerNameIgnoreCase("tester").isPresent());
        assertFalse(repository.findByExactPlayerName("tester").isPresent());
        Account stored = repository.findByExactPlayerName("Tester").orElseThrow();
        assertTrue(stored.isHasSession());
        assertFalse(stored.isLogged());
        assertTrue(repository.updatePassword("tester", "new-hash"));
        assertEquals("new-hash", repository.findByExactPlayerName("Tester").orElseThrow().getPassword());
        assertTrue(repository.updateLoggedState("tester", true));
        assertTrue(repository.isLogged("tester"));
        assertTrue(repository.deleteByExactPlayerName("Tester"));
        assertEquals(Optional.empty(), repository.findByExactPlayerName("Tester"));
    }

    @Test
    void failedReloadKeepsCurrentDatabaseAvailable() {
        DatabaseSettings settings = DatabaseSettings.sqlite(
                this.temporaryDirectory.resolve("accounts.db"),
                "accounts"
        );
        this.databaseManager = new DatabaseManager(Logger.getLogger("test"), settings, "MythicLoginTest");
        JdbcAccountRepository repository = new JdbcAccountRepository(this.databaseManager, () -> settings);

        assertThrows(
                RuntimeException.class,
                () -> this.databaseManager.reloadIfNecessary(
                        DatabaseSettings.sqlite(this.temporaryDirectory, "accounts")
                )
        );

        assertEquals(0, repository.countAccounts());
    }
}
