package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.email.AccountNotificationEmail;
import cn.mythicland.mythiclogin.email.AccountNotificationType;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.email.VerificationEmail;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountNotificationServiceTest {

    @Test
    void shouldSilentlyQueuePasswordAndOldEmailNotifications() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ConfigManager configManager = mock(ConfigManager.class);
            when(configManager.getEmailSettings()).thenReturn(new EmailSettings(
                    "re_test", "MythicLogin <login@example.com>", 6, 5, 60, 5
            ));
            DataManager dataManager = mock(DataManager.class);
            when(dataManager.getExecutorService()).thenReturn(executorService);
            List<AccountNotificationEmail> delivered = new ArrayList<>();
            EmailGateway gateway = new EmailGateway() {
                @Override
                public void send(VerificationEmail email) {
                }

                @Override
                public void send(AccountNotificationEmail email) {
                    delivered.add(email);
                }
            };
            AccountNotificationService service = new AccountNotificationService(
                    new PluginLogger(Logger.getLogger("AccountNotificationServiceTest"), "Test"),
                    configManager,
                    dataManager,
                    gateway
            );
            Account account = new Account(
                    "player", "Player", "hash", null, 1L, 1L, null,
                    "old@example.com", false, false
            );

            service.notifyPasswordChanged(account);
            service.notifyEmailChanged(account);
            executorService.submit(() -> { }).get();

            assertEquals(2, delivered.size());
            assertEquals(AccountNotificationType.PASSWORD_CHANGED, delivered.get(0).type());
            assertEquals(AccountNotificationType.EMAIL_CHANGED, delivered.get(1).type());
            assertEquals("old@example.com", delivered.get(1).recipient());
            assertTrue(delivered.get(0).idempotencyKey().startsWith("mythiclogin/password_changed/"));
        } finally {
            executorService.shutdownNow();
        }
    }
}
