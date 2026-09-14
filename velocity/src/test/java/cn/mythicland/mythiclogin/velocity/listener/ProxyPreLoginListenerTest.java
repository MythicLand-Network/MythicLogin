package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyPreLoginListenerTest {

    @Test
    void shouldDenyPreLoginWhenPlayerNameOnlyMatchesIgnoringCase() {
        IProxyRestrictionManager proxyRestrictionManager = mock(IProxyRestrictionManager.class);
        InboundConnection connection = mock(InboundConnection.class);
        when(proxyRestrictionManager.findCaseMismatchKickMessage("shanguanling")).thenReturn(Optional.of("\u00A7cuse ShanguanLinG"));

        ProxyPreLoginListener listener = new ProxyPreLoginListener(proxyRestrictionManager);
        PreLoginEvent event = new PreLoginEvent(connection, "shanguanling");

        listener.onPreLogin(event);

        assertFalse(event.getResult().isAllowed());
        assertTrue(event.getResult().getReasonComponent().isPresent());
        String reason = PlainTextComponentSerializer.plainText().serialize(event.getResult().getReasonComponent().get());
        assertTrue(reason.contains("use ShanguanLinG"));
    }
}
