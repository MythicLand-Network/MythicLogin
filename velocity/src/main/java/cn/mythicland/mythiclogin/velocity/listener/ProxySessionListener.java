package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.common.restriction.AuthStateChannel;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

@InjectComponent
public final class ProxySessionListener {
    public static final MinecraftChannelIdentifier AUTH_STATE_CHANNEL = MinecraftChannelIdentifier.from(AuthStateChannel.CHANNEL);

    private final IProxyRestrictionManager proxyRestrictionManager;
    private final PluginSettings settings;

    public ProxySessionListener(IProxyRestrictionManager proxyRestrictionManager, PluginSettings settings) {
        this.proxyRestrictionManager = proxyRestrictionManager;
        this.settings = settings;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        proxyRestrictionManager.refreshAuthentication(event.getPlayer());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (isLoginServer(event.getServer().getServerInfo().getName())) {
            proxyRestrictionManager.setTransferAuthenticated(event.getPlayer(), false);
        }
        proxyRestrictionManager.refreshAuthentication(event.getPlayer());
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(connection -> {
            if (isLoginServer(connection.getServerInfo().getName())) {
                connection.sendPluginMessage(AUTH_STATE_CHANNEL, new byte[]{AuthStateChannel.QUERY});
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onAuthState(PluginMessageEvent event) {
        if (!AUTH_STATE_CHANNEL.equals(event.getIdentifier())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection connection)) return;
        Player player = connection.getPlayer();
        if (event.getTarget() != player || player.getCurrentServer().orElse(null) != connection) return;
        String server = connection.getServerInfo().getName();
        if (!isLoginServer(server)) return;
        byte[] data = event.getData();
        if (data.length != 1 || (data[0] != AuthStateChannel.AUTHENTICATED && data[0] != AuthStateChannel.UNAUTHENTICATED)) return;
        proxyRestrictionManager.setTransferAuthenticated(player, data[0] == AuthStateChannel.AUTHENTICATED);
    }

    @Subscribe(order = PostOrder.LATE)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!event.getResult().isAllowed()) return;
        Player player = event.getPlayer();
        if (proxyRestrictionManager.isTransferAuthenticated(player)) return;
        if (player.getCurrentServer().isEmpty()) {
            String target = event.getResult().getServer().orElseThrow().getServerInfo().getName();
            if (isLoginServer(target)) return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
    }

    private boolean isLoginServer(String server) {
        return settings.getLoginServers().isAllServersLogin() || settings.getLoginServers().contains(server);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        proxyRestrictionManager.clearAuthentication(event.getPlayer());
    }
}
