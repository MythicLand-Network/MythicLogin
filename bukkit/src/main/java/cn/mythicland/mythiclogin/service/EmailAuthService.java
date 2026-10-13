package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.config.model.EmailSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.email.EmailDeliveryException;
import cn.mythicland.mythiclogin.email.EmailGateway;
import cn.mythicland.mythiclogin.email.VerificationEmail;
import cn.mythicland.mythiclogin.email.VerificationPurpose;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@InjectComponent
public final class EmailAuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final MessageService messageService;
    private final PasswordService passwordService;
    private final AuthSessionManager authSessionManager;
    private final VerificationCodeService verificationCodeService;
    private final EmailGateway emailGateway;
    private final AccountNotificationService accountNotificationService;
    private final Consumer<Runnable> mainThreadExecutor;
    private final Map<SendKey, UUID> sending = new ConcurrentHashMap<>();
    private final Map<UUID, BindingRequest> bindings = new HashMap<>();

    public EmailAuthService(
            PluginLogger pluginLogger,
            ConfigManager configManager,
            DataManager dataManager,
            MessageService messageService,
            PasswordService passwordService,
            AuthSessionManager authSessionManager,
            VerificationCodeService verificationCodeService,
            EmailGateway emailGateway,
            AccountNotificationService accountNotificationService,
            Consumer<Runnable> mainThreadExecutor
    ) {
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.messageService = messageService;
        this.passwordService = passwordService;
        this.authSessionManager = authSessionManager;
        this.verificationCodeService = verificationCodeService;
        this.emailGateway = emailGateway;
        this.accountNotificationService = accountNotificationService;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    public CompletableFuture<String> requestBinding(Player player, String email) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        if (context == null || context.getState() != AuthState.AUTHENTICATED || context.getAccount() == null) {
            return completed(MessageKey.AUTH_NOT_AUTHENTICATED);
        }
        return sendBindingCode(player, context.getAccount(), email);
    }

    public CompletableFuture<String> verifyBinding(Player player, String code) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        if (context == null || context.getState() != AuthState.AUTHENTICATED || context.getAccount() == null) {
            return completed(MessageKey.AUTH_NOT_AUTHENTICATED);
        }
        return completeBinding(player, context.getAccount(), code.trim());
    }

    public CompletableFuture<String> requestPasswordReset(Player player) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        if (context != null && context.getState() == AuthState.AUTHENTICATED) {
            return completed(MessageKey.EMAIL_RESET_ALREADY_AUTHENTICATED);
        }
        if (context == null || context.getState() != AuthState.PENDING_LOGIN || context.getAccount() == null) {
            return completed(MessageKey.EMAIL_RESET_LOGIN_REQUIRED);
        }
        Account account = context.getAccount();
        if (account.getEmail() == null || account.getEmail().isBlank()) {
            return completed(MessageKey.EMAIL_NO_BOUND_ADDRESS);
        }
        return sendChallenge(
                player,
                VerificationPurpose.RESET_PASSWORD,
                account.getEmail(),
                MessageKey.EMAIL_RESET_SENT,
                challenge -> runMainThread(() -> authSessionManager.extendDeadline(player, challenge.expiresAtMillis()))
        ).thenApply(SendResult::message);
    }

    public CompletableFuture<String> resetPassword(
            Player player,
            String code,
            String newPassword,
            String confirmation
    ) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        if (context != null && context.getState() == AuthState.AUTHENTICATED) {
            return completed(MessageKey.EMAIL_RESET_ALREADY_AUTHENTICATED);
        }
        if (context == null || context.getState() != AuthState.PENDING_LOGIN || context.getAccount() == null) {
            return completed(MessageKey.EMAIL_RESET_LOGIN_REQUIRED);
        }
        if (!newPassword.equals(confirmation)) return completed(MessageKey.AUTH_PASSWORD_MISMATCH);
        if (!isPasswordLengthValid(newPassword)) {
            return CompletableFuture.completedFuture(messageService.get(
                    MessageKey.AUTH_PASSWORD_LENGTH,
                    passwordBoundsPlaceholders()
            ));
        }

        VerificationCodeService.VerificationResult verification = verificationCodeService.verify(
                player.getUniqueId(),
                VerificationPurpose.RESET_PASSWORD,
                code
        );
        String failureMessage = verificationFailure(verification.status());
        if (failureMessage != null) return CompletableFuture.completedFuture(failureMessage);

        Account account = context.getAccount();
        if (!verification.targetEmail().equals(account.getEmail())) {
            return completed(MessageKey.EMAIL_CODE_EXPIRED);
        }
        return CompletableFuture.supplyAsync(() -> {
            String passwordHash = passwordService.hash(newPassword);
            boolean updated = dataManager.getAccountRepository().resetPassword(account.getUsername(), passwordHash);
            return new PasswordResetResult(updated, passwordHash);
        }, dataManager.getExecutorService()).thenApply(result -> {
            if (!result.updated()) return messageService.get(MessageKey.EMAIL_PASSWORD_RESET_FAILED);
            Account updatedAccount = account.withPassword(result.passwordHash())
                    .withLoginMeta(account.getIp(), account.getLastLogin(), false)
                    .withLoggedState(false);
            runMainThread(() -> {
                if (!player.isOnline()) return;
                authSessionManager.setPending(
                        player,
                        AuthState.PENDING_LOGIN,
                        updatedAccount,
                        configManager.getSettings().restrictions().getAuthTimeoutSeconds()
                );
            });
            accountNotificationService.notifyPasswordChanged(updatedAccount);
            return messageService.get(MessageKey.EMAIL_PASSWORD_RESET_SUCCESS);
        });
    }

    public void reload() {
        clear();
        String validationError = configManager.getEmailSettings().validationError();
        if (validationError != null) {
            pluginLogger.warning("Email verification is disabled: " + validationError + ".");
        }
    }

    public void clear() {
        synchronized (bindings) {
            verificationCodeService.clear();
            sending.clear();
            bindings.clear();
        }
    }

    private CompletableFuture<String> sendBindingCode(Player player, Account account, String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (!isValidEmail(email)) return completed(MessageKey.EMAIL_ADDRESS_INVALID);
        if (email.equals(account.getEmail())) return completed(MessageKey.EMAIL_ALREADY_BOUND);
        if (!configManager.getEmailSettings().isEnabled()) return completed(MessageKey.EMAIL_SERVICE_UNAVAILABLE);

        return CompletableFuture.supplyAsync(
                () -> dataManager.getAccountRepository().findByEmailIgnoreCase(email),
                dataManager.getExecutorService()
        ).thenCompose(existing -> {
            if (existing.isPresent()) return completed(MessageKey.EMAIL_ALREADY_IN_USE);
            boolean hasEmail = account.getEmail() != null && !account.getEmail().isBlank();
            BindingRequest request = new BindingRequest(
                    account.getEmail(),
                    email,
                    hasEmail ? VerificationPurpose.CHANGE_EMAIL : VerificationPurpose.BIND_EMAIL
            );
            return sendBindingChallenge(player, request, hasEmail ? MessageKey.EMAIL_CHANGE_SENT : MessageKey.EMAIL_BIND_SENT);
        });
    }

    private CompletableFuture<String> sendBindingChallenge(
            Player player,
            BindingRequest request,
            MessageKey successMessage
    ) {
        String recipient = request.purpose() == VerificationPurpose.CHANGE_EMAIL
                ? request.currentEmail()
                : request.targetEmail();
        return sendChallenge(
                player,
                request.purpose(),
                recipient,
                successMessage,
                challenge -> bindings.put(player.getUniqueId(), request)
        ).thenApply(SendResult::message);
    }

    private CompletableFuture<String> completeBinding(Player player, Account account, String code) {
        BindingRequest request;
        synchronized (bindings) {
            request = bindings.get(player.getUniqueId());
            if (request == null) return completed(MessageKey.EMAIL_CODE_MISSING);
            if (!Objects.equals(request.currentEmail(), account.getEmail())) {
                bindings.remove(player.getUniqueId());
                return completed(MessageKey.EMAIL_CODE_EXPIRED);
            }
            VerificationCodeService.VerificationResult verification = verificationCodeService.verify(
                    player.getUniqueId(),
                    request.purpose(),
                    code
            );
            String failureMessage = verificationFailure(verification.status());
            if (failureMessage != null) return CompletableFuture.completedFuture(failureMessage);
            bindings.remove(player.getUniqueId());
            if (request.purpose() == VerificationPurpose.CHANGE_EMAIL) {
                return sendBindingChallenge(
                        player,
                        new BindingRequest(request.currentEmail(), request.targetEmail(), VerificationPurpose.BIND_EMAIL),
                        MessageKey.EMAIL_CHANGE_NEW_SENT
                );
            }
        }

        return CompletableFuture.supplyAsync(
                () -> dataManager.getAccountRepository().updateEmailIfAvailable(
                        account.getUsername(),
                        request.currentEmail(),
                        request.targetEmail()
                ),
                dataManager.getExecutorService()
        ).thenApply(updated -> {
            if (!updated) return messageService.get(MessageKey.EMAIL_BIND_FAILED);
            accountNotificationService.notifyEmailChanged(account);
            runMainThread(() -> authSessionManager.replaceAccount(
                    player,
                    account.withEmail(request.targetEmail())
            ));
            return messageService.get(MessageKey.EMAIL_BIND_SUCCESS);
        });
    }

    private CompletableFuture<SendResult> sendChallenge(
            Player player,
            VerificationPurpose purpose,
            String targetEmail,
            MessageKey successMessage,
            Consumer<VerificationCodeService.Challenge> onActivated
    ) {
        EmailSettings settings = configManager.getEmailSettings();
        if (!settings.isEnabled()) {
            return CompletableFuture.completedFuture(new SendResult(
                    messageService.get(MessageKey.EMAIL_SERVICE_UNAVAILABLE),
                    null
            ));
        }
        long cooldownSeconds = verificationCodeService.remainingCooldownSeconds(
                player.getUniqueId(),
                purpose,
                settings.resendCooldownSeconds()
        );
        if (cooldownSeconds > 0L) {
            return CompletableFuture.completedFuture(new SendResult(
                    messageService.get(MessageKey.EMAIL_COOLDOWN, cooldownSeconds),
                    null
            ));
        }

        SendKey sendKey = new SendKey(player.getUniqueId(), purpose);
        UUID sendId = UUID.randomUUID();
        if (sending.putIfAbsent(sendKey, sendId) != null) {
            return CompletableFuture.completedFuture(new SendResult(
                    messageService.get(MessageKey.EMAIL_COOLDOWN, 1),
                    null
            ));
        }
        VerificationCodeService.Challenge challenge = verificationCodeService.createChallenge(
                player.getUniqueId(),
                purpose,
                targetEmail,
                settings
        );
        VerificationEmail email = new VerificationEmail(
                purpose,
                targetEmail,
                player.getName(),
                challenge.code(),
                settings.expireMinutes(),
                "mythiclogin/" + purpose.name().toLowerCase(Locale.ROOT) + "/" + sendId
        );
        runMainThread(() -> messageService.send(player, MessageKey.EMAIL_SENDING));
        return CompletableFuture.supplyAsync(() -> {
            try {
                emailGateway.send(email);
                synchronized (bindings) {
                    if (!verificationCodeService.activate(challenge)) {
                        return new SendResult(messageService.get(MessageKey.EMAIL_CODE_EXPIRED), null);
                    }
                    onActivated.accept(challenge);
                }
                return new SendResult(messageService.get(successMessage), challenge);
            } catch (EmailDeliveryException exception) {
                pluginLogger.warning("Failed to send a verification email: " + exception.getMessage());
                return new SendResult(messageService.get(MessageKey.EMAIL_SEND_FAILED), null);
            } finally {
                sending.remove(sendKey, sendId);
            }
        }, dataManager.getExecutorService());
    }

    private String verificationFailure(VerificationCodeService.VerificationStatus status) {
        return switch (status) {
            case VALID -> null;
            case MISSING -> messageService.get(MessageKey.EMAIL_CODE_MISSING);
            case EXPIRED -> messageService.get(MessageKey.EMAIL_CODE_EXPIRED);
            case INVALID -> messageService.get(MessageKey.EMAIL_CODE_INVALID);
            case EXHAUSTED -> messageService.get(MessageKey.EMAIL_CODE_EXHAUSTED);
        };
    }

    private boolean isPasswordLengthValid(String password) {
        int length = password.length();
        return length >= configManager.getSettings().restrictions().getMinPasswordLength()
                && length <= configManager.getSettings().restrictions().getMaxPasswordLength();
    }

    private Object[] passwordBoundsPlaceholders() {
        return new Object[]{
                configManager.getSettings().restrictions().getMinPasswordLength(),
                configManager.getSettings().restrictions().getMaxPasswordLength()
        };
    }

    private CompletableFuture<String> completed(MessageKey key) {
        return CompletableFuture.completedFuture(messageService.get(key));
    }

    private void runMainThread(Runnable action) {
        mainThreadExecutor.accept(action);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email.length() <= 320 && EMAIL_PATTERN.matcher(email).matches();
    }

    private record SendKey(UUID playerId, VerificationPurpose purpose) {
    }

    private record BindingRequest(String currentEmail, String targetEmail, VerificationPurpose purpose) {
    }

    private record SendResult(String message, VerificationCodeService.Challenge challenge) {
    }

    private record PasswordResetResult(boolean updated, String passwordHash) {
    }
}
