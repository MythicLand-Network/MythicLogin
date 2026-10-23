package cn.mythicland.mythiclogin.velocity.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.framework.command.CommandLabels;
import cn.mythicland.framework.minecraft.MinecraftProfiles;
import cn.mythicland.framework.security.ChallengeRegistry;
import cn.mythicland.framework.security.PasswordHashes;
import cn.mythicland.mythiclogin.common.premium.PremiumMessage;
import cn.mythicland.mythiclogin.common.premium.PremiumProtocol;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@InjectComponent
public final class PremiumProxyService {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(PremiumProtocol.CHANNEL);
    private final PluginSettings settings;
    private final AccountRepository repository;
    private final ProxyServer proxy;
    private final MinecraftProfiles profiles;
    private final PluginLogger logger;
    private final ChallengeRegistry<Player, Account> credentials = new ChallengeRegistry<>();
    private final ChallengeRegistry<Player, Unbinding> unbindings = new ChallengeRegistry<>();
    private final PasswordHashes passwords = new PasswordHashes();
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Map<String, Binding> bindings = new ConcurrentHashMap<>();
    private final Map<Player, Identity> identities = new ConcurrentHashMap<>();

    public PremiumProxyService(
            PluginSettings settings,
            AccountRepository repository,
            ProxyServer proxy,
            MinecraftProfiles profiles,
            PluginLogger logger
    ) {
        this.settings = settings;
        this.repository = repository;
        this.proxy = proxy;
        this.profiles = profiles;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.LAST)
    public EventTask onPreLogin(PreLoginEvent event) {
        if (!settings.getPremium().enabled() || !event.getResult().isAllowed()) return null;
        return EventTask.async(() -> {
            String name = event.getUsername();
            String key = normalize(name);
            Attempt attempt = new Attempt(event.getConnection(), name);
            try {
                attempts.entrySet().removeIf(entry -> !entry.getValue().connection.isActive()
                        || System.currentTimeMillis() - entry.getValue().startedAt > 30_000L);
                if (proxy.getAllPlayers().stream().anyMatch(player -> player.getUsername().equalsIgnoreCase(name))
                        || attempts.putIfAbsent(key, attempt) != null) {
                    deny(event, "该账号已在线或正在连接。");
                    return;
                }
                Account account = repository.findByPlayerNameIgnoreCase(name).orElse(null);
                if (account != null && !account.getRealName().equals(name)) {
                    throw new IllegalStateException("请使用账号原有的名字大小写。");
                }
                Binding binding = bindings.remove(key);
                if (binding != null && binding.expiresAt > System.currentTimeMillis()) {
                    if (!sameAccount(account, binding.account)) throw new IllegalStateException("账号已变更, 请重新申请绑定。");
                    attempt.binding = binding;
                    attempt.expectedUuid = binding.uuid;
                } else if (account != null) {
                    attempt.expectedUuid = account.getPremiumUuid();
                }
                event.setResult(attempt.expectedUuid == null
                        ? PreLoginEvent.PreLoginComponentResult.forceOfflineMode()
                        : PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            } catch (RuntimeException exception) {
                attempts.remove(key, attempt);
                deny(event, "无法完成账号认证, 请稍后重试。");
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onProfile(GameProfileRequestEvent event) {
        if (!settings.getPremium().enabled()) return;
        Attempt attempt = attempts.get(normalize(event.getUsername()));
        if (attempt == null || attempt.connection != event.getConnection()) return;
        if (event.isOnlineMode()) attempt.verifiedUuid = event.getOriginalProfile().getId();
        event.setGameProfile(event.getGameProfile().withName(attempt.name)
                .withId(MinecraftProfiles.offlineUuid(attempt.name)));
    }

    @Subscribe(order = PostOrder.LAST)
    public EventTask onLogin(LoginEvent event) {
        if (!settings.getPremium().enabled() || !event.getResult().isAllowed()) return null;
        return EventTask.async(() -> {
            Player player = event.getPlayer();
            Attempt attempt = attempts.get(normalize(player.getUsername()));
            try {
                if (attempt == null || !attempt.connection.getRemoteAddress().equals(player.getRemoteAddress())
                        || !player.getUniqueId().equals(MinecraftProfiles.offlineUuid(attempt.name))) {
                    throw new IllegalStateException("Missing connection identity");
                }
                Account current = repository.findByPlayerNameIgnoreCase(attempt.name).orElse(null);
                PremiumMessage.Type type = PremiumMessage.Type.OFFLINE;
                if (attempt.expectedUuid != null) {
                    if (!player.isOnlineMode() || !attempt.expectedUuid.equals(attempt.verifiedUuid)) {
                        throw new IllegalStateException("Premium identity mismatch");
                    }
                    if (attempt.binding != null) {
                        if (attempt.binding.expiresAt <= System.currentTimeMillis()
                                || !repository.bindPremium(attempt.binding.account, attempt.verifiedUuid)) {
                            throw new IllegalStateException("Binding expired or account changed");
                        }
                        type = PremiumMessage.Type.BOUND;
                    } else {
                        if (current == null || !attempt.verifiedUuid.equals(current.getPremiumUuid())) {
                            throw new IllegalStateException("Premium account changed");
                        }
                        type = PremiumMessage.Type.AUTO_LOGIN;
                    }
                } else if (current != null && current.getPremiumUuid() != null) {
                    throw new IllegalStateException("Premium authentication required");
                }
                identities.put(player, new Identity(attempt, type));
            } catch (RuntimeException exception) {
                event.setResult(ResultedEvent.ComponentResult.denied(MessageService.deserialize("&c账号认证失败, 请重新连接。")));
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Identity identity = identities.remove(event.getPlayer());
        credentials.remove(event.getPlayer());
        unbindings.remove(event.getPlayer());
        if (identity != null) attempts.remove(normalize(event.getPlayer().getUsername()), identity.attempt);
    }

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return null;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!settings.getPremium().enabled() || !(event.getSource() instanceof ServerConnection server)) return null;
        Player player = server.getPlayer();
        if (player.getCurrentServer().orElse(null) != server) return null;
        return EventTask.async(() -> {
            try {
                PremiumMessage request = PremiumProtocol.decode(event.getData(), settings.getPremium());
                if (!request.playerName().equals(player.getUsername()) || !request.gameUuid().equals(player.getUniqueId())) {
                    throw new IllegalArgumentException("Message identity mismatch");
                }
                Identity identity = identities.get(player);
                if (identity == null || !identity.claim(request.nonce())) throw new IllegalArgumentException("Unknown or repeated challenge");
                PremiumMessage response;
                if (request.type() == PremiumMessage.Type.QUERY) {
                    boolean showTitle = identity.firstQuery();
                    response = new PremiumMessage(identity.type, request.nonce(), player.getUsername(), player.getUniqueId(),
                            identity.uuid, showTitle, 0, "");
                } else if (request.type() == PremiumMessage.Type.BIND_REQUEST) {
                    response = requestBinding(player, request);
                } else if (request.type() == PremiumMessage.Type.UNBIND_REQUEST) {
                    response = requestUnbinding(player, request);
                } else {
                    throw new IllegalArgumentException("Unexpected premium message type");
                }
                if (player.isActive() && player.getCurrentServer().orElse(null) == server) {
                    server.sendPluginMessage(CHANNEL, PremiumProtocol.encode(response, settings.getPremium()));
                }
            } catch (RuntimeException exception) {
                player.disconnect(MessageService.deserialize("&c账号认证通信失败, 请重新连接。"));
            }
        });
    }

    private PremiumMessage requestBinding(Player player, PremiumMessage request) {
        try {
            Account verified = credentials.consume(player, request.nonce()).orElseThrow(
                    () -> new IllegalStateException("Premium binding password proof missing or expired")
            );
            Account account = repository.findByExactPlayerName(player.getUsername()).orElseThrow(
                    () -> new IllegalStateException("Premium binding account no longer exists")
            );
            if (!sameAccount(account, verified)) return rejectBinding(player, request, "账号已变更或已绑定正版。");
            if (account.getPremiumUuid() != null || !account.getPassword().equals(request.detail())
                    || account.getRegisterDate() != request.registerDate()) {
                return rejectBinding(player, request, "账号已变更或已绑定正版。");
            }
            Optional<MinecraftProfiles.Profile> found = profiles.findByName(player.getUsername());
            if (found.isEmpty()) return rejectBinding(player, request, "当前名字没有对应的正版账号。");
            MinecraftProfiles.Profile profile = found.orElseThrow();
            bindings.entrySet().removeIf(entry -> entry.getValue().expiresAt <= System.currentTimeMillis());
            bindings.put(normalize(player.getUsername()), new Binding(account, profile.uuid(), System.currentTimeMillis() + 60_000L));
            return new PremiumMessage(PremiumMessage.Type.BIND_ACCEPTED, request.nonce(), player.getUsername(),
                    player.getUniqueId(), profile.uuid(), false, 0, "");
        } catch (RuntimeException exception) {
            logger.severe("Failed to request premium binding for " + player.getUsername(), exception);
            return rejectBinding(player, request, "无法申请正版绑定, 请稍后重试。");
        }
    }

    private PremiumMessage rejectBinding(Player player, PremiumMessage request, String reason) {
        return new PremiumMessage(PremiumMessage.Type.BIND_REJECTED, request.nonce(), player.getUsername(),
                player.getUniqueId(), null, false, 0, reason);
    }

    private PremiumMessage requestUnbinding(Player player, PremiumMessage request) {
        try {
            Unbinding pending = unbindings.consume(player, request.nonce()).orElseThrow();
            Identity identity = identities.get(player);
            if (identity == null || identity.uuid == null || !identity.uuid.equals(pending.account.getPremiumUuid())
                    || !repository.unbindPremium(pending.account, pending.passwordHash)) {
                throw new IllegalStateException("Premium binding or account changed");
            }
            Identity offline = new Identity(identity.attempt, PremiumMessage.Type.OFFLINE);
            offline.queried = true;
            identities.put(player, offline);
            bindings.remove(normalize(player.getUsername()));
            credentials.remove(player);
            return new PremiumMessage(PremiumMessage.Type.UNBOUND, request.nonce(), player.getUsername(),
                    player.getUniqueId(), null, false, 0, pending.passwordHash);
        } catch (RuntimeException exception) {
            logger.severe("Failed to remove premium binding for " + player.getUsername(), exception);
            return new PremiumMessage(PremiumMessage.Type.UNBIND_REJECTED, request.nonce(), player.getUsername(),
                    player.getUniqueId(), null, false, 0, "账号已变更或无法取消正版绑定, 请稍后重试。");
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onUnpremiumCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player) || !CommandLabels.matches(event.getCommand(), "unpremium")) return null;
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        return EventTask.async(() -> {
            ServerConnection server = player.getCurrentServer().orElse(null);
            if (server == null) return;
            if (!settings.getPremium().enabled()) {
                player.sendMessage(MessageService.deserialize("&c服务器未开启正版免登录。"));
                return;
            }
            UUID nonce = UUID.randomUUID();
            PremiumMessage.Type type = PremiumMessage.Type.UNBIND_REJECTED;
            String detail = "用法: /unpremium <新密码>";
            String[] parts = event.getCommand().trim().split("\\s+");
            try {
                if (parts.length == 2) {
                    Identity identity = identities.get(player);
                    Account account = repository.findByExactPlayerName(player.getUsername()).orElseThrow();
                    if (identity == null || identity.type == PremiumMessage.Type.OFFLINE || identity.uuid == null
                            || !identity.uuid.equals(account.getPremiumUuid())) {
                        detail = "当前连接未通过该账号的正版认证。";
                    } else {
                        nonce = unbindings.issue(player, new Unbinding(account, passwords.hash(parts[1])), Duration.ofSeconds(60));
                        type = PremiumMessage.Type.UNBIND_CHECK;
                        detail = Integer.toString(parts[1].length());
                    }
                }
            } catch (RuntimeException exception) {
                logger.severe("Failed to prepare premium unbinding for " + player.getUsername(), exception);
                detail = "无法取消正版绑定, 请稍后重试。";
            }
            PremiumMessage response = new PremiumMessage(type, nonce, player.getUsername(), player.getUniqueId(),
                    null, false, 0, detail);
            if (player.isActive() && player.getCurrentServer().orElse(null) == server) {
                server.sendPluginMessage(CHANNEL, PremiumProtocol.encode(response, settings.getPremium()));
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onPremiumCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player) || !CommandLabels.matches(event.getCommand(), "premium")) return null;
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        return EventTask.async(() -> {
            ServerConnection server = player.getCurrentServer().orElse(null);
            if (server == null) return;
            String[] parts = event.getCommand().trim().split("\\s+");
            PremiumMessage.Type type = PremiumMessage.Type.COMMAND_FAILED;
            UUID nonce = UUID.randomUUID();
            long registerDate = 0;
            String detail = "用法: /premium <原密码>";
            try {
                if (!settings.getPremium().enabled()) {
                    player.sendMessage(MessageService.deserialize("&c服务器未开启正版免登录。"));
                    return;
                }
                if (parts.length == 2) {
                    Account account = repository.findByExactPlayerName(player.getUsername()).orElseThrow();
                    if (account.getPremiumUuid() != null) {
                        detail = "该账号已绑定正版。";
                    } else if (!passwords.verify(parts[1], account.getPassword())) {
                        type = PremiumMessage.Type.PASSWORD_FAILED;
                        detail = "";
                    } else {
                        nonce = credentials.issue(player, account, Duration.ofSeconds(60));
                        type = PremiumMessage.Type.BIND_CHECK;
                        registerDate = account.getRegisterDate();
                        detail = account.getPassword();
                    }
                }
            } catch (RuntimeException exception) {
                logger.severe("Failed to verify premium binding password for " + player.getUsername(), exception);
                detail = "无法申请正版绑定, 请稍后重试。";
            }
            PremiumMessage message = new PremiumMessage(type, nonce, player.getUsername(), player.getUniqueId(),
                    null, false, registerDate, detail);
            if (player.isActive() && player.getCurrentServer().orElse(null) == server) {
                server.sendPluginMessage(CHANNEL, PremiumProtocol.encode(message, settings.getPremium()));
            }
        });
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPremiumTabComplete(TabCompleteEvent event) {
        if (isPremiumCommand(event.getPartialMessage())) event.getSuggestions().clear();
    }

    private static boolean isPremiumCommand(String command) {
        return CommandLabels.matches(command, "premium") || CommandLabels.matches(command, "unpremium");
    }

    public boolean isAuthenticated(Player player) {
        Identity identity = identities.get(player);
        return identity != null && identity.type != PremiumMessage.Type.OFFLINE;
    }

    private static boolean sameAccount(Account current, Account expected) {
        return current != null && current.getPremiumUuid() == null
                && current.getRegisterDate() == expected.getRegisterDate()
                && current.getPassword().equals(expected.getPassword())
                && current.getRealName().equals(expected.getRealName());
    }

    private static void deny(PreLoginEvent event, String reason) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(MessageService.deserialize("&c" + reason)));
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static final class Attempt {
        private final InboundConnection connection;
        private final String name;
        private final long startedAt = System.currentTimeMillis();
        private Binding binding;
        private UUID expectedUuid;
        private UUID verifiedUuid;

        private Attempt(InboundConnection connection, String name) {
            this.connection = connection;
            this.name = name;
        }
    }

    private record Binding(Account account, UUID uuid, long expiresAt) {
    }

    private record Unbinding(Account account, String passwordHash) {
    }

    private static final class Identity {
        private final Attempt attempt;
        private final UUID uuid;
        private final PremiumMessage.Type type;
        private final Set<UUID> challenges = new HashSet<>();
        private boolean queried;

        private Identity(Attempt attempt, PremiumMessage.Type type) {
            this.attempt = attempt;
            this.uuid = type == PremiumMessage.Type.OFFLINE ? null : attempt.verifiedUuid;
            this.type = type;
        }

        private synchronized boolean claim(UUID nonce) {
            return challenges.add(nonce);
        }

        private synchronized boolean firstQuery() {
            boolean first = !queried;
            queried = true;
            return first;
        }
    }
}
