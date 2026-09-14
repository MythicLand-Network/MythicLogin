package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.protocol.ProtocolGatekeeper;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class AuthService {

    private final JavaPlugin plugin;
    private final PluginLogger pluginLogger;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final MessageService messageService;
    private final PasswordService passwordService;
    private final DisplayService displayService;
    private final AuthSessionManager authSessionManager;
    private final RestrictionService restrictionService;
    private final ProtocolGatekeeper protocolGatekeeper;
    private BukkitTask displayTask;

    public AuthService(
        JavaPlugin plugin,
        PluginLogger pluginLogger,
        ConfigManager configManager,
        DataManager dataManager,
        MessageService messageService,
        PasswordService passwordService,
        DisplayService displayService,
        AuthSessionManager authSessionManager,
        RestrictionService restrictionService,
        ProtocolGatekeeper protocolGatekeeper
    ) {
        this.plugin = plugin;
        this.pluginLogger = pluginLogger;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.messageService = messageService;
        this.passwordService = passwordService;
        this.displayService = displayService;
        this.authSessionManager = authSessionManager;
        this.restrictionService = restrictionService;
        this.protocolGatekeeper = protocolGatekeeper;
    }

    public void start() {
        startDisplayTask();
        restrictionService.start();
        protocolGatekeeper.register();
        bootstrapOnlinePlayers();
    }

    public void bootstrapOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            authSessionManager.prepareJoin(player);
            handleJoin(player);
        });
    }

    public void reload() {
        startDisplayTask();
        restrictionService.reload();
        displayService.reload();
    }

    private void startDisplayTask() {
        if (displayTask != null) {
            displayTask.cancel();
        }
        long interval = Math.max(1L, configManager.getSettings().getDisplay().getRefreshIntervalTicks());
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDisplays, interval, interval);
    }

    private void tickDisplays() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
            if (context == null) {
                continue;
            }
            if (context.getState() == AuthState.UNREGISTERED || context.getState() == AuthState.PENDING_LOGIN) {
                if (now >= context.getDeadlineMillis()) {
                    timeout(player);
                    continue;
                }
                displayService.renderCountdown(player, context.getState(), context.getDeadlineMillis());
            }
        }
    }

    public void prepareJoin(Player player) {
        authSessionManager.prepareJoin(player);
    }

    public void handleJoin(Player player) {
        authSessionManager.prepareJoinIfAbsent(player);
        restrictionService.applyRestrictedState(player);
        String playerName = player.getName();
        CompletableFuture
            .supplyAsync(
                    () -> getAccountRepository().findByExactPlayerName(playerName),
                    dataManager.getExecutorService()
            )
            .whenComplete((accountOptional, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    pluginLogger.warning("Failed to load account for " + playerName + ": " + throwable.getMessage());
                    setPending(player, AuthState.UNREGISTERED, null);
                    messageService.send(player, MessageKey.AUTH_REGISTER_REQUIRED);
                    return;
                }
                applyJoinResult(player, accountOptional);
            }));
    }

    private void applyJoinResult(Player player, Optional<Account> accountOptional) {
        if (!accountOptional.isPresent()) {
            setPending(player, AuthState.UNREGISTERED, null);
            messageService.send(player, MessageKey.AUTH_REGISTER_REQUIRED);
            return;
        }

        Account account = accountOptional.get();
        if (shouldAutoLogin(account, player)) {
            authSessionManager.authenticate(player, account);
            restrictionService.clearRestrictedState(player);
            displayService.clear(player);
            displayService.showAutoLoginTitle(player);
            messageService.send(player, MessageKey.AUTH_AUTO_LOGIN_MESSAGE, player.getName());
            updateLoginMetaAsync(account.getUsername(), address(player), System.currentTimeMillis(), true);
            updateLoggedStateAsync(account.getUsername(), true);
            return;
        }

        setPending(player, AuthState.PENDING_LOGIN, account);
        messageService.send(player, MessageKey.AUTH_LOGIN_REQUIRED);
    }

    private void setPending(Player player, AuthState state, Account account) {
        authSessionManager.setPending(player, state, account, configManager.getSettings().getRestrictions().getAuthTimeoutSeconds());
        restrictionService.applyRestrictedState(player);
    }

    private boolean shouldAutoLogin(Account account, Player player) {
        if (!configManager.getSettings().getSession().isEnabled() || !account.isHasSession()) {
            return false;
        }
        String currentIp = address(player);
        if (currentIp == null || account.getIp() == null || !currentIp.equals(account.getIp())) {
            return false;
        }
        long age = System.currentTimeMillis() - account.getLastLogin();
        return age <= configManager.getSettings().getSession().getTimeoutMinutes() * 60_000L;
    }

    public void handleQuit(Player player) {
        AuthSessionManager.SessionContext context = authSessionManager.remove(player);
        restrictionService.clearRestrictedState(player);
        displayService.clear(player);
        if (context != null && context.getState() == AuthState.AUTHENTICATED && context.getAccount() != null) {
            updateLoggedStateAsync(context.getAccount().getUsername(), false);
        }
    }

    public boolean isAuthenticated(Player player) {
        return authSessionManager.isAuthenticated(player);
    }

    public boolean shouldRestrict(Player player) {
        return authSessionManager.shouldRestrict(player);
    }

    public CompletableFuture<String> register(Player player, String password, String confirmation) {
        if (!password.equals(confirmation)) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_PASSWORD_MISMATCH));
        }
        if (!validatePasswordLength(password)) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_PASSWORD_LENGTH, passwordBoundsPlaceholders()));
        }
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String availabilityFailure = availabilityFailureMessage(context);
        if (availabilityFailure != null) {
            return CompletableFuture.completedFuture(availabilityFailure);
        }
        if (context.getState() != AuthState.UNREGISTERED) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_ALREADY_REGISTERED));
        }

        String playerName = player.getName();
        String playerAddress = address(player);
        boolean sessionEnabled = configManager.getSettings().getSession().isEnabled();
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> caseMismatchRealName = findCaseMismatchRealName(playerName);
            if (caseMismatchRealName.isPresent()) {
                return RegisterResult.caseMismatch(caseMismatchRealName.get());
            }
            long now = System.currentTimeMillis();
            Account account = new Account(
                playerName.toLowerCase(Locale.ROOT),
                playerName,
                passwordService.hash(password),
                playerAddress,
                now,
                now,
                playerAddress,
                sessionEnabled,
                false
            );
            return getAccountRepository().create(account)
                ? RegisterResult.success(account)
                : RegisterResult.alreadyRegistered();
        }, dataManager.getExecutorService()).thenApply(result -> {
            if (result.caseMismatchRealName != null) {
                return caseMismatchMessage(result.caseMismatchRealName);
            }
            if (result.account == null) {
                return messageService.get(MessageKey.AUTH_ALREADY_REGISTERED);
            }
            Bukkit.getScheduler().runTask(plugin, () -> completeAuthentication(player, result.account, AuthState.UNREGISTERED));
            return messageService.get(MessageKey.AUTH_REGISTER_SUCCESS);
        });
    }

    public CompletableFuture<String> login(Player player, String password) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String availabilityFailure = availabilityFailureMessage(context);
        if (availabilityFailure != null) {
            return CompletableFuture.completedFuture(availabilityFailure);
        }
        if (context.getAccount() == null) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_NOT_REGISTERED));
        }

        return CompletableFuture.supplyAsync(() -> passwordService.verify(password, context.getAccount().getPassword()), dataManager.getExecutorService())
            .thenApply(valid -> {
                if (!valid) {
                    Bukkit.getScheduler().runTask(plugin, () -> displayService.showLoginFailureTitle(player));
                    return messageService.get(MessageKey.AUTH_LOGIN_FAILED);
                }
                Bukkit.getScheduler().runTask(plugin, () -> completeAuthentication(player, context.getAccount(), AuthState.PENDING_LOGIN));
                return messageService.get(MessageKey.AUTH_LOGIN_SUCCESS);
            });
    }

    public CompletableFuture<String> changePassword(Player player, String newPassword, String confirmation) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String authenticatedFailure = authenticatedFailureMessage(context);
        if (authenticatedFailure != null) {
            return CompletableFuture.completedFuture(authenticatedFailure);
        }
        if (!newPassword.equals(confirmation)) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_PASSWORD_MISMATCH));
        }
        if (!validatePasswordLength(newPassword)) {
            return CompletableFuture.completedFuture(messageService.get(MessageKey.AUTH_PASSWORD_LENGTH, passwordBoundsPlaceholders()));
        }

        Account account = context.getAccount();
        return CompletableFuture.supplyAsync(() -> {
            String passwordHash = passwordService.hash(newPassword);
            boolean updated = getAccountRepository().updatePassword(account.getUsername(), passwordHash);
            return new PasswordUpdateResult(updated, passwordHash);
        }, dataManager.getExecutorService()).thenApply(result -> {
            if (!result.updated()) {
                return messageService.get(MessageKey.AUTH_PASSWORD_CHANGE_FAILED);
            }
            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> syncAuthenticatedPassword(player, account.getUsername(), result.passwordHash())
            );
            return messageService.get(MessageKey.AUTH_PASSWORD_CHANGED);
        });
    }

    public String logout(Player player) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String loadingFailure = loadingFailureMessage(context);
        if (loadingFailure != null) {
            return loadingFailure;
        }
        if (context.getState() != AuthState.AUTHENTICATED || context.getAccount() == null) {
            return messageService.get(MessageKey.AUTH_NOT_AUTHENTICATED);
        }

        setPending(player, AuthState.PENDING_LOGIN, context.getAccount());
        updateLoginMetaAsync(
            context.getAccount().getUsername(),
            context.getAccount().getIp(),
            context.getAccount().getLastLogin(),
            false
        );
        updateLoggedStateAsync(context.getAccount().getUsername(), false);
        return messageService.get(MessageKey.AUTH_LOGOUT_SUCCESS);
    }

    public String authCommandHint(Player player) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String availabilityFailure = availabilityFailureMessage(context);
        if (availabilityFailure != null) {
            return availabilityFailure;
        }
        if (context.getState() == AuthState.UNREGISTERED) {
            return messageService.get(MessageKey.AUTH_REGISTER_REQUIRED);
        }
        return messageService.get(MessageKey.AUTH_LOGIN_REQUIRED);
    }

    public String changePasswordHint(Player player) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        String authenticatedFailure = authenticatedFailureMessage(context);
        if (authenticatedFailure != null) {
            return authenticatedFailure;
        }
        return messageService.get(MessageKey.AUTH_CHANGE_PASSWORD_USAGE);
    }

    public CompletableFuture<Boolean> accountExists(String username) {
        return CompletableFuture.supplyAsync(() -> getAccountRepository().findByExactPlayerName(username).isPresent(), dataManager.getExecutorService());
    }

    public CompletableFuture<Boolean> updatePassword(String username, String newPassword) {
        return CompletableFuture.supplyAsync(() -> getAccountRepository().updatePassword(username, passwordService.hash(newPassword)), dataManager.getExecutorService());
    }

    public CompletableFuture<Boolean> unregisterAccount(String playerName) {
        return CompletableFuture.supplyAsync(
            () -> getAccountRepository().deleteByExactPlayerName(playerName),
            dataManager.getExecutorService()
        ).thenApply(success -> {
            if (!success) {
                return false;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null && player.isOnline()) resetOnlinePlayerAfterUnregister(player);
            });
            return true;
        });
    }

    public void sendMessageWhenComplete(CommandSender sender, CompletableFuture<String> result) {
        result.whenComplete((message, throwable) -> {
            if (throwable != null) {
                pluginLogger.warning("Failed to execute authentication command: " + throwable.getMessage());
                return;
            }
            sendMessage(sender, message);
        });
    }

    private void sendMessage(CommandSender sender, String message) {
        if (Bukkit.isPrimaryThread()) {
            sender.sendMessage(message);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    public boolean isPasswordLengthValid(String password) {
        return validatePasswordLength(password);
    }

    public Object[] passwordBoundsPlaceholders() {
        return new Object[]{
            configManager.getSettings().getRestrictions().getMinPasswordLength(),
            configManager.getSettings().getRestrictions().getMaxPasswordLength()
        };
    }

    public Optional<String> findCaseMismatchRealName(String playerName) {
        Optional<Account> exactAccount = getAccountRepository().findByExactPlayerName(playerName);
        if (exactAccount.isPresent()) {
            return Optional.empty();
        }

        Optional<Account> ignoreCaseAccount = getAccountRepository().findByPlayerNameIgnoreCase(playerName);
        if (!ignoreCaseAccount.isPresent()) {
            return Optional.empty();
        }

        String correctRealName = ignoreCaseAccount.get().getRealName();
        return correctRealName.equals(playerName) ? Optional.empty() : Optional.of(correctRealName);
    }

    public String caseMismatchMessage(String correctPlayerName) {
        return messageService.get(MessageKey.AUTH_CASE_MISMATCH, correctPlayerName);
    }

    private void completeAuthentication(Player player, Account account, AuthState previousState) {
        if (!player.isOnline()) {
            return;
        }
        long now = System.currentTimeMillis();
        Account updated = account.withLoginMeta(address(player), now, configManager.getSettings().getSession().isEnabled()).withLoggedState(true);
        authSessionManager.authenticate(player, updated);
        restrictionService.clearRestrictedState(player);
        displayService.clear(player);
        displayService.showSuccessTitle(player, previousState);
        updateLoginMetaAsync(updated.getUsername(), updated.getIp(), updated.getLastLogin(), updated.isHasSession());
        updateLoggedStateAsync(updated.getUsername(), true);
    }

    private void updateLoginMetaAsync(String username, String ip, long lastLogin, boolean hasSession) {
        CompletableFuture
                .runAsync(
                        () -> getAccountRepository().updateLoginMeta(username, ip, lastLogin, hasSession),
                        dataManager.getExecutorService()
                )
                .exceptionally(throwable -> {
                    pluginLogger.warning("Failed to update login metadata for " + username + ": " + throwable.getMessage());
                    return null;
                });
    }

    private void updateLoggedStateAsync(String username, boolean logged) {
        CompletableFuture
                .runAsync(
                        () -> getAccountRepository().updateLoggedState(username, logged),
                        dataManager.getExecutorService()
                )
                .exceptionally(throwable -> {
                    pluginLogger.warning("Failed to update logged state for " + username + ": " + throwable.getMessage());
                    return null;
                });
    }

    private void resetOnlinePlayerAfterUnregister(Player player) {
        if (!player.isOnline()) {
            return;
        }
        authSessionManager.remove(player);
        displayService.clear(player);
        restrictionService.clearRestrictedState(player);
        authSessionManager.prepareJoin(player);
        handleJoin(player);
    }

    private void timeout(Player player) {
        displayService.clear(player);
        AuthSessionManager.SessionContext context = authSessionManager.remove(player);
        restrictionService.clearRestrictedState(player);
        if (context != null && context.getAccount() != null) {
            updateLoggedStateAsync(context.getAccount().getUsername(), false);
        }
        if (configManager.getSettings().getRestrictions().isKickOnTimeout()) {
            player.kickPlayer(messageService.get(MessageKey.SYSTEM_TIMEOUT_KICK));
        }
    }

    private boolean validatePasswordLength(String password) {
        int length = password.length();
        return length >= configManager.getSettings().getRestrictions().getMinPasswordLength()
            && length <= configManager.getSettings().getRestrictions().getMaxPasswordLength();
    }

    private void syncAuthenticatedPassword(Player player, String username, String passwordHash) {
        AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
        if (context == null || context.getState() != AuthState.AUTHENTICATED || context.getAccount() == null) {
            return;
        }
        if (!context.getAccount().getUsername().equalsIgnoreCase(username)) {
            return;
        }
        authSessionManager.authenticate(player, context.getAccount().withPassword(passwordHash));
    }

    private String availabilityFailureMessage(AuthSessionManager.SessionContext context) {
        String loadingFailure = loadingFailureMessage(context);
        if (loadingFailure != null) {
            return loadingFailure;
        }
        return context.getState() == AuthState.AUTHENTICATED ? messageService.get(MessageKey.AUTH_ALREADY_AUTHENTICATED) : null;
    }

    private String authenticatedFailureMessage(AuthSessionManager.SessionContext context) {
        String loadingFailure = loadingFailureMessage(context);
        if (loadingFailure != null) {
            return loadingFailure;
        }
        return context.getState() != AuthState.AUTHENTICATED || context.getAccount() == null
            ? messageService.get(MessageKey.AUTH_NOT_AUTHENTICATED)
            : null;
    }

    private String loadingFailureMessage(AuthSessionManager.SessionContext context) {
        return context == null || context.getState() == AuthState.LOADING ? messageService.get(MessageKey.SYSTEM_LOADING) : null;
    }

    public String address(Player player) {
        InetSocketAddress socketAddress = player.getAddress();
        return socketAddress == null || socketAddress.getAddress() == null ? null : socketAddress.getAddress().getHostAddress();
    }

    public void shutdown() {
        protocolGatekeeper.unregister();
        if (displayTask != null) {
            displayTask.cancel();
        }
        restrictionService.shutdown();
        Bukkit.getOnlinePlayers().forEach(player -> {
            AuthSessionManager.SessionContext context = authSessionManager.getSession(player);
            if (context != null && context.getState() == AuthState.AUTHENTICATED && context.getAccount() != null) {
                getAccountRepository().updateLoggedState(context.getAccount().getUsername(), false);
            }
            displayService.clear(player);
        });
        authSessionManager.clear();
    }

    private AccountRepository getAccountRepository() {
        return dataManager.getAccountRepository();
    }

    private record PasswordUpdateResult(boolean updated, String passwordHash) {
    }

    private static final class RegisterResult {
        private final Account account;
        private final String caseMismatchRealName;

        private RegisterResult(Account account, String caseMismatchRealName) {
            this.account = account;
            this.caseMismatchRealName = caseMismatchRealName;
        }

        private static RegisterResult success(Account account) {
            return new RegisterResult(account, null);
        }

        private static RegisterResult alreadyRegistered() {
            return new RegisterResult(null, null);
        }

        private static RegisterResult caseMismatch(String caseMismatchRealName) {
            return new RegisterResult(null, caseMismatchRealName);
        }
    }
}
