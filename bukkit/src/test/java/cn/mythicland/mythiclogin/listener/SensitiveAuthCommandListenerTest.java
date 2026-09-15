package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class SensitiveAuthCommandListenerTest {
    @Test
    void leavesAuthenticatedCommandsToTheFrameworkRegistrar() {
        AuthService authService = mock(AuthService.class);
        PlayerCommandService commandService = mock(PlayerCommandService.class);
        Player player = mock(Player.class);
        Server server = mock(Server.class);
        when(player.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptyList());
        when(authService.shouldRestrict(player)).thenReturn(false);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/login secret");
        new SensitiveAuthCommandListener(authService, commandService).onCommand(event);

        assertFalse(event.isCancelled());
        verifyNoInteractions(commandService);
    }

    @Test
    void handlesRestrictedCommandsBeforeBukkitCommandDispatch() {
        AuthService authService = mock(AuthService.class);
        PlayerCommandService commandService = mock(PlayerCommandService.class);
        Player player = mock(Player.class);
        Server server = mock(Server.class);
        when(player.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptyList());
        when(authService.shouldRestrict(player)).thenReturn(true);
        when(commandService.handle(player, "/login secret")).thenReturn(true);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/login secret");
        new SensitiveAuthCommandListener(authService, commandService).onCommand(event);

        assertTrue(event.isCancelled());
        verify(commandService).handle(player, "/login secret");
    }
}
