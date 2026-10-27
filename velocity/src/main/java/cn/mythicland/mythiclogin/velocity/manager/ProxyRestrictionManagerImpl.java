package cn.mythicland.mythiclogin.velocity.manager;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.common.restriction.CommandListMatcher;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.config.key.MessageKey;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import cn.mythicland.mythiclogin.velocity.service.PremiumProxyService;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@InjectComponent
public final class ProxyRestrictionManagerImpl implements IProxyRestrictionManager {

    private final PluginSettings settings;
    private final MessageService messageService;
    private final AccountRepository accountRepository;
    private final PluginLogger pluginLogger;
    private final PremiumProxyService premium;
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<Player> transferAuthenticated = ConcurrentHashMap.newKeySet();
    private final Set<UUID> missingAccountWarnedPlayers = ConcurrentHashMap.newKeySet();

    public ProxyRestrictionManagerImpl(
        PluginSettings settings,
        MessageService messageService,
        AccountRepository accountRepository,
        PluginLogger pluginLogger,
        PremiumProxyService premium
    ) {
        this.settings = settings;
        this.messageService = messageService;
        this.accountRepository = accountRepository;
        this.pluginLogger = pluginLogger;
        this.premium = premium;
    }

    @Override
    public Optional<String> findCaseMismatchKickMessage(String playerName) {
        try {
            if (accountRepository.findByExactPlayerName(playerName).isPresent()) {
                return Optional.empty();
            }

            Optional<Account> ignoreCaseAccount = accountRepository.findByPlayerNameIgnoreCase(playerName);
            if (!ignoreCaseAccount.isPresent()) {
                return Optional.empty();
            }

            String correctRealName = ignoreCaseAccount.get().getRealName();
            if (correctRealName.equals(playerName)) {
                return Optional.empty();
            }
            return Optional.of(messageService.get(MessageKey.PROXY_CASE_MISMATCH_KICK, correctRealName));
        } catch (IllegalStateException exception) {
            pluginLogger.warning("Failed to validate account name for " + playerName + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldBlockCommand(Player player, String command) {
        if (!settings.getProxyCommands().isEnabled()) {
            return false;
        }
        if (!shouldHandleRestrictions(player)) {
            return false;
        }
        AuthDecision decision = evaluateAuthentication(player, "command:" + command);
        return !decision.authenticated
            && new CommandListMatcher(settings.getProxyCommands()).shouldBlock(command);
    }

    @Override
    public List<String> filterSuggestions(Player player, List<String> suggestions) {
        if (!settings.getProxyCommands().isEnabled()) {
            return suggestions;
        }
        if (!shouldHandleRestrictions(player)) {
            return suggestions;
        }
        AuthDecision decision = evaluateAuthentication(player, "tab-complete");
        return decision.authenticated
            ? suggestions
            : new CommandListMatcher(settings.getProxyCommands()).filterSuggestions(suggestions);
    }

    @Override
    public void sendBlockedMessage(Player player) {
        AuthDecision decision = evaluateAuthentication(player, "blocked-message");
        if (decision.status == AuthStatus.CASE_MISMATCH) {
            messageService.send(player, MessageKey.PROXY_CASE_MISMATCH_KICK, decision.correctRealName);
            return;
        }
        if (decision.status == AuthStatus.ACCOUNT_MISSING) {
            messageService.send(player, MessageKey.PROXY_ACCOUNT_DATA_MISSING);
            return;
        }
        messageService.send(player, MessageKey.PROXY_COMMAND_BLOCKED);
    }

    @Override
    public void refreshAuthentication(Player player) {
        evaluateAuthentication(player, "refresh");
    }

    @Override
    public void clearAuthentication(Player player) {
        transferAuthenticated.remove(player);
        authenticatedPlayers.remove(player.getUniqueId());
        missingAccountWarnedPlayers.remove(player.getUniqueId());
    }

    @Override
    public boolean isTransferAuthenticated(Player player) {
        return transferAuthenticated.contains(player);
    }

    @Override
    public void setTransferAuthenticated(Player player, boolean authenticated) {
        if (authenticated) transferAuthenticated.add(player);
        else transferAuthenticated.remove(player);
    }

    @Override
    public void runStartupSanityCheck() {
        try {
            int totalAccounts = accountRepository.countAccounts();
            if (totalAccounts <= 0) {
                pluginLogger.warning("Connected to database successfully, but table `"
                    + settings.getDatabase().table()
                    + "` currently has 0 accounts. If Bukkit already has registered players, check host/port/database/table in MythicLoginVelocity config.");
            } else {
                pluginLogger.info("Database sanity check passed. Found " + totalAccounts + " account(s) in table `"
                    + settings.getDatabase().table() + "`.");
            }
        } catch (IllegalStateException exception) {
            pluginLogger.warning("Database sanity check failed: " + exception.getMessage());
        }
    }

    private AuthDecision evaluateAuthentication(Player player, String source) {
        if (settings.getPremium().enabled() && premium.isAuthenticated(player)) {
            return new AuthDecision(true, AuthStatus.AUTHENTICATED, "premium", null);
        }
        boolean cacheHit = authenticatedPlayers.contains(player.getUniqueId());
        try {
            Optional<Account> accountOptional = accountRepository.findByExactPlayerName(player.getUsername());
            if (!accountOptional.isPresent()) {
                Optional<Account> ignoreCaseAccount = accountRepository.findByPlayerNameIgnoreCase(player.getUsername());
                if (ignoreCaseAccount.isPresent() && !ignoreCaseAccount.get().getRealName().equals(player.getUsername())) {
                    authenticatedPlayers.remove(player.getUniqueId());
                    return new AuthDecision(
                        false,
                        AuthStatus.CASE_MISMATCH,
                        "case-mismatch source=" + source,
                        ignoreCaseAccount.get().getRealName()
                    );
                }
                authenticatedPlayers.remove(player.getUniqueId());
                warnMissingAccount(player, source);
                return new AuthDecision(false, AuthStatus.ACCOUNT_MISSING, "account-missing source=" + source, null);
            }
            Account account = accountOptional.get();
            long now = System.currentTimeMillis();
            long age = now - account.getLastLogin();
            boolean sessionEligible = settings.getSession().isEnabled()
                && account.isHasSession()
                && age <= settings.getSession().getTimeoutMinutes() * 60_000L;
            boolean authenticated = account.isLogged() || sessionEligible;
            if (authenticated) {
                authenticatedPlayers.add(player.getUniqueId());
            } else {
                authenticatedPlayers.remove(player.getUniqueId());
            }
            return new AuthDecision(
                authenticated,
                authenticated ? AuthStatus.AUTHENTICATED : AuthStatus.NOT_AUTHENTICATED,
                "db source=" + source
                    + " logged=" + account.isLogged()
                    + " hasSession=" + account.isHasSession()
                    + " sessionEnabled=" + settings.getSession().isEnabled()
                    + " ageMs=" + age
                    + " timeoutMinutes=" + settings.getSession().getTimeoutMinutes()
                    + " sessionEligible=" + sessionEligible
                    + " cacheHit=" + cacheHit
                    + " cacheStored=" + authenticated,
                null
            );
        } catch (IllegalStateException exception) {
            pluginLogger.warning("Failed to read login state for " + player.getUsername() + ": " + exception.getMessage());
            if (cacheHit) {
                return new AuthDecision(true, AuthStatus.AUTHENTICATED, "db-error-cache-fallback source=" + source, null);
            }
            return new AuthDecision(false, AuthStatus.DB_ERROR, "db-error source=" + source + " message=" + exception.getMessage(), null);
        }
    }

    private void warnMissingAccount(Player player, String source) {
        if (!missingAccountWarnedPlayers.add(player.getUniqueId())) {
            return;
        }
        pluginLogger.warning("Player `" + player.getUsername() + "` was not found in table `"
            + settings.getDatabase().table()
            + "` during " + source
            + ". If this player should already be registered, check MythicLoginVelocity database host/port/name/table settings.");
    }

    private boolean shouldHandleRestrictions(Player player) {
        if (settings.getLoginServers().isAllServersLogin()) {
            return true;
        }
        return settings.getLoginServers().contains(currentServerName(player));
    }

    private String currentServerName(Player player) {
        if (player == null) {
            return null;
        }
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (!serverConnection.isPresent()) {
            return null;
        }
        return serverConnection.get().getServerInfo().getName();
    }

    private static final class AuthDecision {
        private final boolean authenticated;
        private final AuthStatus status;
        private final String reason;
        private final String correctRealName;

        private AuthDecision(boolean authenticated, AuthStatus status, String reason, String correctRealName) {
            this.authenticated = authenticated;
            this.status = status;
            this.reason = reason;
            this.correctRealName = correctRealName;
        }
    }

    private enum AuthStatus {
        AUTHENTICATED,
        NOT_AUTHENTICATED,
        CASE_MISMATCH,
        ACCOUNT_MISSING,
        DB_ERROR
    }
}
