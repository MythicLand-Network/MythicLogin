package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

public final class ProxySessionListener {

    private final IProxyRestrictionManager proxyRestrictionManager;

    public ProxySessionListener(IProxyRestrictionManager proxyRestrictionManager) {
        this.proxyRestrictionManager = proxyRestrictionManager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        proxyRestrictionManager.refreshAuthentication(event.getPlayer());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        proxyRestrictionManager.refreshAuthentication(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        proxyRestrictionManager.clearAuthentication(event.getPlayer());
    }
}
