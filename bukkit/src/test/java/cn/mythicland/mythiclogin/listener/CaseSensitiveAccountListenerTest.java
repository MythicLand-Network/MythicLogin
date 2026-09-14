package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.service.AuthService;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseSensitiveAccountListenerTest {

    @Test
    void shouldDisallowJoinWhenPlayerNameOnlyMatchesIgnoringCase() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.findCaseMismatchRealName("shanguanling")).thenReturn(Optional.of("ShanguanLinG"));
        when(authService.caseMismatchMessage("ShanguanLinG")).thenReturn("\u00A7cuse ShanguanLinG");

        CaseSensitiveAccountListener listener = new CaseSensitiveAccountListener(authService);
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
            "shanguanling",
            InetAddress.getLoopbackAddress(),
            UUID.randomUUID()
        );

        listener.onAsyncPreLogin(event);

        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
        assertEquals("\u00A7cuse ShanguanLinG", event.getKickMessage());
    }

    @Test
    void shouldAllowJoinWhenCaseMatchesExactly() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.findCaseMismatchRealName("ShanguanLinG")).thenReturn(Optional.empty());

        CaseSensitiveAccountListener listener = new CaseSensitiveAccountListener(authService);
        AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
            "ShanguanLinG",
            InetAddress.getLoopbackAddress(),
            UUID.randomUUID()
        );

        listener.onAsyncPreLogin(event);

        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult());
        assertTrue(event.getKickMessage().isEmpty());
    }
}
