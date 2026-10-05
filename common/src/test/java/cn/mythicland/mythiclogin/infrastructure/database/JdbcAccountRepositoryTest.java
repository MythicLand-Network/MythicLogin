package cn.mythicland.mythiclogin.infrastructure.database;

import cn.mythicland.mythiclogin.domain.account.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(repository.updateEmailIfAvailable("tester", "Tester@Example.com"));
        assertEquals("tester@example.com", repository.findByExactPlayerName("Tester").orElseThrow().getEmail());
        assertEquals("Tester", repository.findByEmailIgnoreCase("TESTER@example.com").orElseThrow().getRealName());
        assertTrue(repository.updateLoggedState("tester", true));
        assertTrue(repository.isLogged("tester"));
        assertTrue(repository.resetPassword("tester", "reset-hash"));
        Account reset = repository.findByExactPlayerName("Tester").orElseThrow();
        assertEquals("reset-hash", reset.getPassword());
        assertFalse(reset.isHasSession());
        assertFalse(reset.isLogged());
        assertTrue(repository.deleteByExactPlayerName("Tester"));
        assertEquals(Optional.empty(), repository.findByExactPlayerName("Tester"));
    }

    @Test
    void shouldRejectEmailAlreadyBoundToAnotherAccount() {
        DatabaseSettings settings = DatabaseSettings.sqlite(
                this.temporaryDirectory.resolve("accounts.db"),
                "accounts"
        );
        this.databaseManager = new DatabaseManager(Logger.getLogger("test"), settings, "MythicLoginTest");
        JdbcAccountRepository repository = new JdbcAccountRepository(this.databaseManager, () -> settings);
        Account first = new Account("first", "First", "hash", null, 0L, 0L, null, false, false);
        Account second = new Account("second", "Second", "hash", null, 0L, 0L, null, false, false);

        assertTrue(repository.create(first));
        assertTrue(repository.create(second));
        assertTrue(repository.updateEmailIfAvailable("first", "shared@example.com"));
        assertFalse(repository.updateEmailIfAvailable("second", "SHARED@example.com"));
        assertNull(repository.findByExactPlayerName("Second").orElseThrow().getEmail());
    }

    @Test
    void bindsOnlyTheUnchangedAccountAndPreservesPremiumIdentityInCopies() {
        DatabaseSettings settings = DatabaseSettings.sqlite(temporaryDirectory.resolve("premium.db"), "accounts");
        databaseManager = new DatabaseManager(Logger.getLogger("test"), settings, "MythicLoginTest");
        JdbcAccountRepository repository = new JdbcAccountRepository(databaseManager, () -> settings);
        Account account = new Account("tester", "Tester", "original-hash", null, 0, 1, null, false, false);
        UUID uuid = UUID.randomUUID();
        assertTrue(repository.create(account));
        assertNull(repository.findByExactPlayerName("Tester").orElseThrow().getPremiumUuid());
        assertFalse(repository.bindPremium(account.withPassword("wrong-hash"), uuid));
        assertTrue(repository.bindPremium(account, uuid));
        assertFalse(repository.bindPremium(account, UUID.randomUUID()));
        Account bound = repository.findByExactPlayerName("Tester").orElseThrow();
        assertEquals(uuid, bound.withPassword("changed").withEmail("email@example.com")
                .withLoginMeta("127.0.0.1", 2, false).withLoggedState(true).getPremiumUuid());
        repository.deleteByExactPlayerName("Tester");
        Account replacement = new Account("tester", "Tester", "original-hash", null, 0, 3, null, false, false);
        assertTrue(repository.create(replacement));
        assertFalse(repository.bindPremium(account, uuid));
    }

    @Test
    void unbindsOnlyTheUnchangedPremiumAccountAndSetsPassword() {
        DatabaseSettings settings = DatabaseSettings.sqlite(temporaryDirectory.resolve("unbind.db"), "accounts");
        databaseManager = new DatabaseManager(Logger.getLogger("test"), settings, "MythicLoginTest");
        JdbcAccountRepository repository = new JdbcAccountRepository(databaseManager, () -> settings);
        UUID uuid = UUID.randomUUID();
        Account account = Account.premium("Tester", uuid, "127.0.0.1", 1);
        assertTrue(repository.create(account));
        assertFalse(repository.unbindPremium(account.withPremiumUuid(UUID.randomUUID()), "new-hash"));
        assertFalse(repository.unbindPremium(account.withPassword("changed"), "new-hash"));
        assertTrue(repository.unbindPremium(account, "new-hash"));
        Account stored = repository.findByExactPlayerName("Tester").orElseThrow();
        assertNull(stored.getPremiumUuid());
        assertEquals("new-hash", stored.getPassword());
        assertFalse(stored.isHasSession());
        assertEquals(account.getRegisterDate(), stored.getRegisterDate());
        assertFalse(repository.unbindPremium(account, "replacement-hash"));
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
