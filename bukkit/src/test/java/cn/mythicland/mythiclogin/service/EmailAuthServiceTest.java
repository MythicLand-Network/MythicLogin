package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.email.AccountNotificationEmail;
import cn.mythicland.mythiclogin.email.VerificationEmail;
import cn.mythicland.mythiclogin.email.VerificationPurpose;
import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailAuthServiceTest {

    @Test
    void shouldDirectAuthenticatedPlayerToChangePassword() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Account account = new Account(
                "player",
                "Player",
                "hash",
                "127.0.0.1",
                1L,
                1L,
                "127.0.0.1",
                "player@example.com",
                true,
                true
        );
        AuthSessionManager sessionManager = new AuthSessionManager();
        sessionManager.authenticate(player, account);
        MessageService messageService = new MessageService();
        EmailAuthService service = new EmailAuthService(
                new PluginLogger(Logger.getLogger("EmailAuthServiceTest"), "Test"),
                mock(ConfigManager.class),
                mock(DataManager.class),
                messageService,
                new PasswordService(),
                sessionManager,
                new VerificationCodeService(),
                new CapturingEmailGateway(null),
                mock(AccountNotificationService.class),
                Runnable::run
        );

        assertEquals(
                messageService.get(MessageKey.EMAIL_RESET_ALREADY_AUTHENTICATED),
                service.requestPasswordReset(player).join()
        );
        assertEquals(
                messageService.get(MessageKey.EMAIL_RESET_ALREADY_AUTHENTICATED),
                service.resetPassword(player, "123456", "new-secret", "new-secret").join()
        );
    }

    @Test
    void shouldBindNormalizedAvailableEmailAfterVerification() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            UUID playerId = UUID.randomUUID();
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getName()).thenReturn("Player");

            Account account = new Account(
                    "player",
                    "Player",
                    "hash",
                    "127.0.0.1",
                    1L,
                    1L,
                    "127.0.0.1",
                    true,
                    false
            );
            AuthSessionManager sessionManager = new AuthSessionManager();
            sessionManager.authenticate(player, account);

            ConfigManager configManager = mock(ConfigManager.class);
            when(configManager.getEmailSettings()).thenReturn(emailSettings());
            when(configManager.getSettings()).thenReturn(pluginSettings());
            AccountRepository repository = mock(AccountRepository.class);
            when(repository.findByEmailIgnoreCase("player@example.com")).thenReturn(Optional.empty());
            when(repository.updateEmailIfAvailable("player", null, "player@example.com")).thenReturn(true);
            DataManager dataManager = mock(DataManager.class);
            when(dataManager.getExecutorService()).thenReturn(executorService);
            when(dataManager.getAccountRepository()).thenReturn(repository);

            AtomicReference<VerificationEmail> deliveredEmail = new AtomicReference<>();
            AccountNotificationService notificationService = mock(AccountNotificationService.class);
            EmailAuthService service = emailAuthService(
                    configManager,
                    dataManager,
                    sessionManager,
                    deliveredEmail,
                    new PasswordService(),
                    notificationService
            );

            service.requestBinding(player, " Player@Example.com ").join();
            service.verifyBinding(player, deliveredEmail.get().code()).join();

            verify(repository).updateEmailIfAvailable("player", null, "player@example.com");
            verify(notificationService).notifyEmailChanged(account);
            verify(player).sendMessage("§e正在发送验证码邮件, 请稍候……");
            assertEquals("player@example.com", sessionManager.getSession(player).getAccount().getEmail());
        } finally {
            executorService.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldVerifyOldEmailBeforeNewEmailAndOnlyThenUpdateBinding(boolean clearBeforeCompletion) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getName()).thenReturn("Player");
            Account account = new Account(
                    "player", "Player", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1",
                    "old@example.com", true, true
            );
            AuthSessionManager sessionManager = new AuthSessionManager();
            sessionManager.authenticate(player, account);
            ConfigManager configManager = mock(ConfigManager.class);
            when(configManager.getEmailSettings()).thenReturn(emailSettings());
            AccountRepository repository = mock(AccountRepository.class);
            when(repository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
            when(repository.updateEmailIfAvailable("player", "old@example.com", "new@example.com")).thenReturn(true);
            DataManager dataManager = mock(DataManager.class);
            when(dataManager.getExecutorService()).thenReturn(executorService);
            when(dataManager.getAccountRepository()).thenReturn(repository);
            AtomicReference<VerificationEmail> deliveredEmail = new AtomicReference<>();
            AccountNotificationService notificationService = mock(AccountNotificationService.class);
            EmailAuthService service = emailAuthService(
                    configManager, dataManager, sessionManager, deliveredEmail, new PasswordService(), notificationService
            );
            MessageService messages = new MessageService();

            assertEquals(messages.get(MessageKey.EMAIL_CHANGE_SENT), service.requestBinding(player, "new@example.com").join());
            VerificationEmail oldEmail = deliveredEmail.get();
            assertEquals("old@example.com", oldEmail.recipient());
            assertEquals(VerificationPurpose.CHANGE_EMAIL, oldEmail.purpose());
            assertEquals(messages.get(MessageKey.EMAIL_CODE_INVALID), service.verifyBinding(player, "wrong").join());
            assertEquals(oldEmail, deliveredEmail.get());
            verify(repository, never()).updateEmailIfAvailable(anyString(), anyString(), anyString());

            assertEquals(messages.get(MessageKey.EMAIL_CHANGE_NEW_SENT), service.verifyBinding(player, oldEmail.code()).join());
            VerificationEmail newEmail = deliveredEmail.get();
            assertEquals("new@example.com", newEmail.recipient());
            assertEquals(VerificationPurpose.BIND_EMAIL, newEmail.purpose());
            assertEquals(messages.get(MessageKey.EMAIL_CODE_INVALID), service.verifyBinding(player, "wrong").join());
            verify(repository, never()).updateEmailIfAvailable(anyString(), anyString(), anyString());
            verify(notificationService, never()).notifyEmailChanged(any(Account.class));
            assertEquals("old@example.com", sessionManager.getSession(player).getAccount().getEmail());

            if (clearBeforeCompletion) {
                service.clear();
                assertEquals(messages.get(MessageKey.EMAIL_CODE_MISSING), service.verifyBinding(player, newEmail.code()).join());
                verify(repository, never()).updateEmailIfAvailable(anyString(), anyString(), anyString());
                assertEquals("old@example.com", sessionManager.getSession(player).getAccount().getEmail());
            } else {
                assertEquals(messages.get(MessageKey.EMAIL_BIND_SUCCESS), service.verifyBinding(player, newEmail.code()).join());
                verify(repository).updateEmailIfAvailable("player", "old@example.com", "new@example.com");
                verify(notificationService).notifyEmailChanged(account);
                assertEquals("new@example.com", sessionManager.getSession(player).getAccount().getEmail());
                assertEquals(messages.get(MessageKey.EMAIL_CODE_MISSING), service.verifyBinding(player, newEmail.code()).join());
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldResetPasswordWithoutDeletingAccount() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            UUID playerId = UUID.randomUUID();
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getName()).thenReturn("Player");
            when(player.isOnline()).thenReturn(true);

            Account account = new Account(
                    "player",
                    "Player",
                    "old-hash",
                    "127.0.0.1",
                    1L,
                    1L,
                    "127.0.0.1",
                    "player@example.com",
                    true,
                    false
            );
            AuthSessionManager sessionManager = new AuthSessionManager();
            sessionManager.setPending(player, AuthState.PENDING_LOGIN, account, 60);

            ConfigManager configManager = mock(ConfigManager.class);
            when(configManager.getEmailSettings()).thenReturn(emailSettings());
            when(configManager.getSettings()).thenReturn(pluginSettings());

            AccountRepository repository = mock(AccountRepository.class);
            when(repository.resetPassword(eq("player"), anyString())).thenReturn(true);
            DataManager dataManager = mock(DataManager.class);
            when(dataManager.getExecutorService()).thenReturn(executorService);
            when(dataManager.getAccountRepository()).thenReturn(repository);

            PasswordService passwordService = new PasswordService();
            AtomicReference<VerificationEmail> deliveredEmail = new AtomicReference<>();
            AccountNotificationService notificationService = mock(AccountNotificationService.class);
            EmailAuthService service = emailAuthService(
                    configManager,
                    dataManager,
                    sessionManager,
                    deliveredEmail,
                    passwordService,
                    notificationService
            );

            service.requestPasswordReset(player).join();
            service.resetPassword(
                    player,
                    deliveredEmail.get().code(),
                    "new-secret",
                    "new-secret"
            ).join();

            verify(repository).resetPassword(eq("player"), anyString());
            verify(repository, never()).deleteByExactPlayerName("Player");
            verify(notificationService).notifyPasswordChanged(any(Account.class));
            Account updated = sessionManager.getSession(player).getAccount();
            assertTrue(passwordService.verify("new-secret", updated.getPassword()));
            assertFalse(updated.isHasSession());
        } finally {
            executorService.shutdownNow();
        }
    }

    private EmailAuthService emailAuthService(
            ConfigManager configManager,
            DataManager dataManager,
            AuthSessionManager sessionManager,
            AtomicReference<VerificationEmail> deliveredEmail,
            PasswordService passwordService,
            AccountNotificationService notificationService
    ) {
        EmailGateway emailGateway = new CapturingEmailGateway(deliveredEmail);
        return new EmailAuthService(
                new PluginLogger(Logger.getLogger("EmailAuthServiceTest"), "Test"),
                configManager,
                dataManager,
                new MessageService(),
                passwordService,
                sessionManager,
                new VerificationCodeService(),
                emailGateway,
                notificationService,
                Runnable::run
        );
    }

    private EmailSettings emailSettings() {
        return new EmailSettings(
                "re_test",
                "MythicLogin <login@example.com>",
                6,
                5,
                60,
                5
        );
    }

    private PluginSettings pluginSettings() {
        return new PluginSettings(
                DatabaseSettings.sqlite(Path.of("accounts.db"), "accounts"),
                new SessionSettings(true, 5),
                new RestrictionSettings(
                        Collections.singletonList("/login"),
                        true,
                        true,
                        true,
                        60,
                        4,
                        26,
                        true
                ),
                DisplaySettings.defaults()
        );
    }

    private static final class CapturingEmailGateway implements EmailGateway {
        private final AtomicReference<VerificationEmail> deliveredEmail;

        private CapturingEmailGateway(AtomicReference<VerificationEmail> deliveredEmail) {
            this.deliveredEmail = deliveredEmail;
        }

        @Override
        public void send(VerificationEmail email) {
            if (deliveredEmail != null) deliveredEmail.set(email);
        }

        @Override
        public void send(AccountNotificationEmail email) {
        }
    }
}
