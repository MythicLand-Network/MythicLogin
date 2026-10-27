package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.framework.security.ChallengeRegistry;
import cn.mythicland.mythiclogin.common.premium.PremiumMessage;
import cn.mythicland.mythiclogin.common.premium.PremiumProtocol;
import cn.mythicland.mythiclogin.common.restriction.AuthStateChannel;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.domain.account.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@InjectComponent
public final class PremiumBridge implements PluginMessageListener {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final AuthSessionManager sessions;
    private final DisplayService displays;
    private final ChallengeRegistry<UUID, Pending> queries = new ChallengeRegistry<>();
    private final ChallengeRegistry<UUID, Pending> bindings = new ChallengeRegistry<>();

    public PremiumBridge(
            JavaPlugin plugin,
            ConfigManager configManager,
            AuthSessionManager sessions,
            DisplayService displays
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.sessions = sessions;
        this.displays = displays;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, PremiumProtocol.CHANNEL);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, AuthStateChannel.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, AuthStateChannel.CHANNEL, this);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, PremiumProtocol.CHANNEL, this);
    }

    public void unregister() {
        Bukkit.getOnlinePlayers().forEach(this::clear);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, PremiumProtocol.CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, PremiumProtocol.CHANNEL);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, AuthStateChannel.CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, AuthStateChannel.CHANNEL, this);
    }

    public CompletableFuture<PremiumMessage> query(Player player) {
        return request(player, PremiumMessage.Type.QUERY, null);
    }

    public void sendAuthState(Player player, boolean authenticated) {
        player.sendPluginMessage(plugin, AuthStateChannel.CHANNEL,
                new byte[]{authenticated ? AuthStateChannel.AUTHENTICATED : AuthStateChannel.UNAUTHENTICATED});
    }

    public void bind(Player player) {
        failBinding(player, configManager.getSettings().premium().enabled()
                ? "请通过代理连接服务器后申请绑定。" : "服务器未开启正版免登录。");
    }

    public void unbind(Player player) {
        displays.showPremiumUnbindFailureTitle(player, configManager.getSettings().premium().enabled()
                ? "请通过代理连接服务器后取消绑定。" : "服务器未开启正版免登录。");
    }

    private void verifyUnbinding(Player player, PremiumMessage proof) {
        AuthSessionManager.SessionContext session = sessions.getSession(player);
        if (!sessions.isAuthenticated(player) || session.getAccount() == null || session.getAccount().getPremiumUuid() == null) {
            displays.showPremiumUnbindFailureTitle(player, "请先通过正版认证登录账号。");
            return;
        }
        int length = Integer.parseInt(proof.detail());
        var restrictions = configManager.getSettings().restrictions();
        if (length < restrictions.getMinPasswordLength() || length > restrictions.getMaxPasswordLength()) {
            displays.showPremiumUnbindFailureTitle(player, "密码长度必须为 " + restrictions.getMinPasswordLength()
                    + " 到 " + restrictions.getMaxPasswordLength() + " 位。");
            return;
        }
        if (bindings.contains(player.getUniqueId())) {
            displays.showPremiumUnbindFailureTitle(player, "正在处理正版绑定操作, 请稍候。");
            return;
        }
        request(player, PremiumMessage.Type.UNBIND_REQUEST, proof).whenComplete((response, failure) -> {
            if (!player.isOnline()) return;
            if (failure != null) {
                displays.showPremiumUnbindFailureTitle(player, "无法取消正版绑定, 请稍后重试。");
            } else if (response.type() == PremiumMessage.Type.UNBOUND) {
                Account account = session.getAccount();
                sessions.replaceAccount(player, account.withPremiumUuid(null).withPassword(response.detail())
                        .withLoginMeta(account.getIp(), account.getLastLogin(), false));
                displays.showPremiumUnboundTitle(player);
            } else {
                displays.showPremiumUnbindFailureTitle(player, response.detail());
            }
        });
    }

    private void verifyBinding(Player player, PremiumMessage proof) {
        if (!sessions.isAuthenticated(player)) {
            failBinding(player, "请先登录账号。");
            return;
        }
        if (bindings.contains(player.getUniqueId())) {
            failBinding(player, "正在申请绑定, 请稍候。");
            return;
        }
        request(player, PremiumMessage.Type.BIND_REQUEST, proof).whenComplete((response, failure) -> {
            if (!player.isOnline()) return;
            if (failure != null) {
                failBinding(player, "无法申请正版绑定, 请稍后重试。");
            } else if (response.type() == PremiumMessage.Type.BIND_ACCEPTED) {
                displays.showPremiumReconnectTitle(player);
            } else {
                failBinding(player, response.detail());
            }
        });
    }

    private CompletableFuture<PremiumMessage> request(Player player, PremiumMessage.Type type, PremiumMessage proof) {
        ChallengeRegistry<UUID, Pending> registry = type == PremiumMessage.Type.QUERY ? queries : bindings;
        Pending pending = new Pending(player, type, new CompletableFuture<>());
        registry.remove(player.getUniqueId()).ifPresent(previous -> previous.future.completeExceptionally(
                new IllegalStateException("Authentication request replaced")));
        UUID nonce = registry.issue(player.getUniqueId(), proof == null ? UUID.randomUUID() : proof.nonce(), pending, Duration.ofSeconds(6));
        PremiumMessage request = new PremiumMessage(type, nonce, player.getName(), player.getUniqueId(), null,
                false, proof == null ? 0 : proof.registerDate(), proof == null ? "" : proof.detail());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && registry.contains(player.getUniqueId())) {
                player.sendPluginMessage(plugin, PremiumProtocol.CHANNEL,
                        PremiumProtocol.encode(request, configManager.getSettings().premium()));
            }
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            registry.consume(player.getUniqueId(), nonce);
            pending.future.completeExceptionally(new IllegalStateException("Authentication request timed out"));
        }, 100L);
        return pending.future;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (AuthStateChannel.CHANNEL.equals(channel)) {
            if (bytes.length == 1 && bytes[0] == AuthStateChannel.QUERY) sendAuthState(player, sessions.isAuthenticated(player));
            return;
        }
        if (!PremiumProtocol.CHANNEL.equals(channel) || !configManager.getSettings().premium().enabled()) return;
        try {
            PremiumMessage response = PremiumProtocol.decode(bytes, configManager.getSettings().premium());
            if (!response.playerName().equals(player.getName()) || !response.gameUuid().equals(player.getUniqueId())) {
                throw new IllegalArgumentException("Authentication identity mismatch");
            }
            if (response.type() == PremiumMessage.Type.BIND_CHECK) {
                verifyBinding(player, response);
                return;
            }
            if (response.type() == PremiumMessage.Type.UNBIND_CHECK) {
                verifyUnbinding(player, response);
                return;
            }
            if (response.type() == PremiumMessage.Type.UNBIND_REJECTED && !bindings.contains(player.getUniqueId())) {
                displays.showPremiumUnbindFailureTitle(player, response.detail());
                return;
            }
            if (response.type() == PremiumMessage.Type.PASSWORD_FAILED) {
                if (sessions.isAuthenticated(player)) displays.showPasswordFailureTitle(player);
                else failBinding(player, "请先登录账号。");
                return;
            }
            if (response.type() == PremiumMessage.Type.COMMAND_FAILED) {
                failBinding(player, response.detail());
                return;
            }
            boolean unbinding = response.type() == PremiumMessage.Type.UNBOUND || response.type() == PremiumMessage.Type.UNBIND_REJECTED;
            boolean binding = unbinding || response.type() == PremiumMessage.Type.BIND_ACCEPTED || response.type() == PremiumMessage.Type.BIND_REJECTED;
            ChallengeRegistry<UUID, Pending> registry = binding ? bindings : queries;
            Pending pending = registry.consume(player.getUniqueId(), response.nonce()).orElseThrow();
            PremiumMessage.Type expected = unbinding ? PremiumMessage.Type.UNBIND_REQUEST : PremiumMessage.Type.BIND_REQUEST;
            if (pending.player != player || (binding && pending.type != expected)
                    || (!binding && response.type() != PremiumMessage.Type.OFFLINE
                    && response.type() != PremiumMessage.Type.NEW_ACCOUNT && response.type() != PremiumMessage.Type.AUTO_LOGIN
                    && response.type() != PremiumMessage.Type.BOUND)) {
                pending.future.completeExceptionally(new IllegalArgumentException("Unexpected authentication response"));
                throw new IllegalArgumentException("Unexpected authentication response");
            }
            pending.future.complete(response);
        } catch (RuntimeException exception) {
            clear(player);
            player.kickPlayer("§c账号认证通信失败, 请重新连接。");
        }
    }

    public void clear(Player player) {
        queries.remove(player.getUniqueId()).ifPresent(pending -> pending.future.completeExceptionally(
                new IllegalStateException("Player disconnected")));
        bindings.remove(player.getUniqueId()).ifPresent(pending -> pending.future.completeExceptionally(
                new IllegalStateException("Player disconnected")));
    }

    private void failBinding(Player player, String reason) {
        displays.showPremiumFailureTitle(player, reason);
    }

    private record Pending(Player player, PremiumMessage.Type type, CompletableFuture<PremiumMessage> future) {
    }
}
