package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class FallbackRestrictionListenerTest {
    @Test
    void movementRestrictionDoesNotBlockTeleportBeforeAuthentication() {
        AuthService auth = mock(AuthService.class);
        FallbackRestrictionListener listener = new FallbackRestrictionListener(auth, mock(MessageService.class));
        Player player = mock(Player.class);
        PlayerTeleportEvent event = mock(PlayerTeleportEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(auth.shouldRestrict(player)).thenReturn(true);
        listener.onMove(event);
        verify(event, never()).setCancelled(true);
        verify(event, never()).setTo(any());
    }
}
