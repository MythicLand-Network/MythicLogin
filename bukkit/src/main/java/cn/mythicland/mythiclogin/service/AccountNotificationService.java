package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.email.AccountNotificationEmail;
import cn.mythicland.mythiclogin.email.AccountNotificationType;
import cn.mythicland.mythiclogin.email.EmailDeliveryException;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.util.PluginLogger;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@InjectComponent
public final class AccountNotificationService {
    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final EmailGateway emailGateway;

    public AccountNotificationService(
            PluginLogger pluginLogger,
            ConfigManager configManager,
            DataManager dataManager,
            EmailGateway emailGateway
    ) {
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.emailGateway = emailGateway;
    }

    public void notifyPasswordChanged(Account account) {
        send(AccountNotificationType.PASSWORD_CHANGED, account.getEmail(), account.getRealName());
    }

    public void notifyEmailChanged(Account account) {
        send(AccountNotificationType.EMAIL_CHANGED, account.getEmail(), account.getRealName());
    }

    private void send(AccountNotificationType type, String recipient, String playerName) {
        EmailSettings settings = configManager.getEmailSettings();
        if (recipient == null || recipient.isBlank() || !settings.isEnabled()) return;

        AccountNotificationEmail email = new AccountNotificationEmail(
                type,
                recipient,
                playerName,
                "mythiclogin/" + type.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID()
        );
        CompletableFuture.runAsync(() -> {
            try {
                emailGateway.send(email);
            } catch (EmailDeliveryException exception) {
                pluginLogger.warning("Failed to send an account security notification: " + exception.getMessage());
            }
        }, dataManager.getExecutorService());
    }
}
