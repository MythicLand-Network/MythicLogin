package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.model.AuthState;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSessionManagerTest {

    @Test
    void shouldTransitionFromLoadingToPendingToAuthenticatedAndClear() {
        AuthSessionManager sessionManager = new AuthSessionManager();
        Player player = mock(Player.class);
        UUID uniqueId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uniqueId);

        sessionManager.prepareJoin(player);
        assertEquals(AuthState.LOADING, sessionManager.getSession(player).getState());
        assertFalse(sessionManager.isAuthenticated(player));

        sessionManager.setPending(player, AuthState.PENDING_LOGIN, null, 60);
        assertEquals(AuthState.PENDING_LOGIN, sessionManager.getSession(player).getState());
        assertNotNull(sessionManager.getSession(player));
        assertTrue(sessionManager.shouldRestrict(player));

        Account account = new Account("test", "Test", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1", true, false);
        sessionManager.authenticate(player, account);
        assertEquals(AuthState.AUTHENTICATED, sessionManager.getSession(player).getState());
        assertTrue(sessionManager.isAuthenticated(player));

        sessionManager.remove(player);
        assertTrue(sessionManager.shouldRestrict(player));
    }
}
