package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceCaseMismatchTest {

    @Test
    void shouldRecommendEmailBindingAfterLoginWhenAccountHasNoEmail() {
        MessageService messageService = new MessageService();
        AuthService authService = new AuthService(
                null,
                mock(PluginLogger.class),
                mock(ConfigManager.class),
                mock(DataManager.class),
                messageService,
                mock(PasswordService.class),
                mock(DisplayService.class),
                new AuthSessionManager(),
                mock(RestrictionService.class),
                mock(AccountNotificationService.class)
        );
        Account account = new Account(
                "player", "Player", "hash", null, 1L, 1L, null, false, false
        );

        assertEquals(
                messageService.get(MessageKey.AUTH_LOGIN_SUCCESS) + "\n"
                        + messageService.get(MessageKey.AUTH_EMAIL_BIND_RECOMMENDED),
                authService.loginSuccessMessage(account)
        );
    }

    @Test
    void shouldRejectRegisterWhenOnlyCaseInsensitiveAccountExists() {
        PluginLogger pluginLogger = new PluginLogger(Logger.getLogger("AuthServiceCaseMismatchTest"), "Test");
        ConfigManager configManager = mock(ConfigManager.class);
        DataManager dataManager = mock(DataManager.class);
        MessageService messageService = mock(MessageService.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        AuthSessionManager authSessionManager = new AuthSessionManager();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            when(configManager.getSettings()).thenReturn(pluginSettings());
            when(dataManager.getExecutorService()).thenReturn(executorService);
            when(dataManager.getAccountRepository()).thenReturn(accountRepository);
            when(messageService.get(MessageKey.AUTH_CASE_MISMATCH, "ShanguanLinG")).thenReturn("\u00A7cuse ShanguanLinG");

            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getName()).thenReturn("shanguanling");

            authSessionManager.prepareJoin(player);
            authSessionManager.setPending(player, AuthState.UNREGISTERED, null, 60);

            when(accountRepository.findByExactPlayerName("shanguanling")).thenReturn(Optional.empty());
            when(accountRepository.findByPlayerNameIgnoreCase("shanguanling")).thenReturn(Optional.of(
                new Account("shanguanling", "ShanguanLinG", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1", false, false)
            ));

            AuthService authService = new AuthService(
                null,
                pluginLogger,
                configManager,
                dataManager,
                messageService,
                new PasswordService(),
                null,
                authSessionManager,
                null,
                mock(AccountNotificationService.class)
            );

            String result = authService.register(player, "secret123", "secret123").join();

            assertEquals("\u00A7cuse ShanguanLinG", result);
        } finally {
            executorService.shutdownNow();
        }
    }

    private PluginSettings pluginSettings() {
        return new PluginSettings(
            DatabaseSettings.postgresql("127.0.0.1", 5432, "mythiclogin", "postgres", "change-me", "accounts"),
            new SessionSettings(true, 30),
            new RestrictionSettings(Collections.singletonList("/login"), false, false, true, 60, 6, 32, true),
            DisplaySettings.defaults()
        );
    }
}
