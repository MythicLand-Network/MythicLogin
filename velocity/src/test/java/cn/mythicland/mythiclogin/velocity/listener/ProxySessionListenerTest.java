package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.mythiclogin.common.restriction.AuthStateChannel;
import cn.mythicland.mythiclogin.velocity.config.LoginServerSettings;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProxySessionListenerTest {
    @Test
    void permitsInitialLoginButBlocksEverySubsequentTransferUntilAuthenticated() {
        IProxyRestrictionManager manager = mock(IProxyRestrictionManager.class);
        PluginSettings settings = mock(PluginSettings.class);
        when(settings.getLoginServers()).thenReturn(new LoginServerSettings(false, List.of("Login")));
        ProxySessionListener listener = new ProxySessionListener(manager, settings);
        Player player = mock(Player.class);
        RegisteredServer login = server("Login");
        RegisteredServer lobby = server("Lobby");
        ServerPreConnectEvent initialLogin = new ServerPreConnectEvent(player, login);
        listener.onServerPreConnect(initialLogin);
        assertTrue(initialLogin.getResult().isAllowed());
        ServerPreConnectEvent initialLobby = new ServerPreConnectEvent(player, lobby);
        listener.onServerPreConnect(initialLobby);
        assertFalse(initialLobby.getResult().isAllowed());
        when(player.getCurrentServer()).thenReturn(Optional.of(mock(ServerConnection.class)));
        ServerPreConnectEvent unlogged = new ServerPreConnectEvent(player, lobby);
        listener.onServerPreConnect(unlogged);
        assertFalse(unlogged.getResult().isAllowed());
        when(manager.isTransferAuthenticated(player)).thenReturn(true);
        ServerPreConnectEvent authenticated = new ServerPreConnectEvent(player, lobby);
        listener.onServerPreConnect(authenticated);
        assertTrue(authenticated.getResult().isAllowed());
        when(player.getCurrentServer()).thenReturn(Optional.empty());
        ServerPreConnectEvent fallback = new ServerPreConnectEvent(player, lobby);
        listener.onServerPreConnect(fallback);
        assertTrue(fallback.getResult().isAllowed());
        authenticated.setResult(ServerPreConnectEvent.ServerResult.denied());
        listener.onServerPreConnect(authenticated);
        assertFalse(authenticated.getResult().isAllowed());
    }

    @Test
    void acceptsStateOnlyFromPlayersCurrentLoginServer() {
        IProxyRestrictionManager manager = mock(IProxyRestrictionManager.class);
        PluginSettings settings = mock(PluginSettings.class);
        when(settings.getLoginServers()).thenReturn(new LoginServerSettings(false, List.of("Login")));
        ProxySessionListener listener = new ProxySessionListener(manager, settings);
        Player player = mock(Player.class);
        ServerConnection connection = mock(ServerConnection.class);
        ServerInfo loginInfo = server("Login").getServerInfo();
        ServerInfo lobbyInfo = server("Lobby").getServerInfo();
        when(connection.getPlayer()).thenReturn(player);
        when(connection.getServerInfo()).thenReturn(loginInfo);
        when(player.getCurrentServer()).thenReturn(Optional.of(connection));
        PluginMessageEvent event = mock(PluginMessageEvent.class);
        when(event.getIdentifier()).thenReturn(ProxySessionListener.AUTH_STATE_CHANNEL);
        when(event.getSource()).thenReturn(connection);
        when(event.getTarget()).thenReturn(player);
        when(event.getData()).thenReturn(new byte[]{AuthStateChannel.AUTHENTICATED});
        listener.onAuthState(event);
        verify(manager).setTransferAuthenticated(player, true);
        when(event.getSource()).thenReturn(player);
        listener.onAuthState(event);
        when(event.getSource()).thenReturn(connection);
        when(connection.getServerInfo()).thenReturn(lobbyInfo);
        listener.onAuthState(event);
        verify(manager, times(1)).setTransferAuthenticated(player, true);
        when(connection.getServerInfo()).thenReturn(loginInfo);
        when(event.getData()).thenReturn(new byte[]{AuthStateChannel.UNAUTHENTICATED});
        listener.onAuthState(event);
        verify(manager).setTransferAuthenticated(player, false);
    }

    private static RegisteredServer server(String name) {
        RegisteredServer server = mock(RegisteredServer.class);
        when(server.getServerInfo()).thenReturn(new ServerInfo(name, new InetSocketAddress("127.0.0.1", 25565)));
        return server;
    }
}
