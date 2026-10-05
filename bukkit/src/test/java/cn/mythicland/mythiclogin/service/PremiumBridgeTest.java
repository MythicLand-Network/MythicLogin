package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.common.premium.PremiumMessage;
import cn.mythicland.mythiclogin.common.premium.PremiumProtocol;
import cn.mythicland.mythiclogin.common.premium.PremiumSettings;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PremiumBridgeTest {
    @Test
    void onlyAcceptsMatchingSignedChallengeOnce() {
        Fixture fixture = new Fixture();
        try (MockedStatic<Bukkit> ignored = fixture.scheduler()) {
            CompletableFuture<PremiumMessage> result = fixture.bridge.query(fixture.player);
            assertFalse(result.isDone());
            PremiumMessage query = fixture.outgoing();
            PremiumMessage response = new PremiumMessage(PremiumMessage.Type.OFFLINE, query.nonce(), query.playerName(),
                    query.gameUuid(), null, true, 0, "");
            byte[] bytes = PremiumProtocol.encode(response, fixture.settings);
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player, bytes);
            assertEquals(response, result.join());
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player, bytes);
            verify(fixture.player).kickPlayer(anyString());
        }
    }

    @Test
    void tamperingCannotReleaseAnUnauthenticatedPlayer() {
        Fixture fixture = new Fixture();
        try (MockedStatic<Bukkit> ignored = fixture.scheduler()) {
            CompletableFuture<PremiumMessage> result = fixture.bridge.query(fixture.player);
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player, new byte[]{1});
            assertTrue(result.isCompletedExceptionally());
            verify(fixture.player).kickPlayer(anyString());
        }
    }

    @Test
    void unbindingValidatesPasswordPolicyAndKeepsAuthenticatedSession() {
        Fixture fixture = new Fixture();
        Account account = Account.premium("Tester", UUID.randomUUID(), "127.0.0.1", 1);
        fixture.sessions.authenticate(fixture.player, account);
        try (MockedStatic<Bukkit> ignored = fixture.scheduler()) {
            PremiumMessage shortPassword = new PremiumMessage(PremiumMessage.Type.UNBIND_CHECK, UUID.randomUUID(),
                    "Tester", fixture.player.getUniqueId(), null, false, 0, "3");
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player,
                    PremiumProtocol.encode(shortPassword, fixture.settings));
            verify(fixture.displays).showPremiumUnbindFailureTitle(eq(fixture.player), contains("4 到 26"));
            verify(fixture.player, never()).sendPluginMessage(any(), anyString(), any(byte[].class));

            PremiumMessage check = new PremiumMessage(PremiumMessage.Type.UNBIND_CHECK, UUID.randomUUID(),
                    "Tester", fixture.player.getUniqueId(), null, false, 0, "12");
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player,
                    PremiumProtocol.encode(check, fixture.settings));
            PremiumMessage request = fixture.outgoing();
            assertEquals(PremiumMessage.Type.UNBIND_REQUEST, request.type());
            assertEquals(check.nonce(), request.nonce());
            PremiumMessage response = new PremiumMessage(PremiumMessage.Type.UNBOUND, request.nonce(),
                    "Tester", fixture.player.getUniqueId(), null, false, 0, "new-password-hash");
            fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player,
                    PremiumProtocol.encode(response, fixture.settings));

            assertTrue(fixture.sessions.isAuthenticated(fixture.player));
            Account updated = fixture.sessions.getSession(fixture.player).getAccount();
            assertNull(updated.getPremiumUuid());
            assertEquals("new-password-hash", updated.getPassword());
            verify(fixture.displays).showPremiumUnboundTitle(fixture.player);
        }
    }

    @Test
    void unbindingCannotRunBeforeAuthentication() {
        Fixture fixture = new Fixture();
        PremiumMessage check = new PremiumMessage(PremiumMessage.Type.UNBIND_CHECK, UUID.randomUUID(),
                "Tester", fixture.player.getUniqueId(), null, false, 0, "12");
        fixture.bridge.onPluginMessageReceived(PremiumProtocol.CHANNEL, fixture.player,
                PremiumProtocol.encode(check, fixture.settings));
        verify(fixture.displays).showPremiumUnbindFailureTitle(fixture.player, "请先通过正版认证登录账号。");
        verify(fixture.player, never()).sendPluginMessage(any(), anyString(), any(byte[].class));
    }

    @Test
    void timeoutCompletesRequestWithoutAnyProxyResponse() {
        Fixture fixture = new Fixture();
        try (MockedStatic<Bukkit> ignored = fixture.scheduler()) {
            CompletableFuture<PremiumMessage> result = fixture.bridge.query(fixture.player);
            fixture.tasks.get(100L).run();
            assertTrue(result.isCompletedExceptionally());
        }
    }

    private static final class Fixture {
        private final PremiumSettings settings = new PremiumSettings(true, "abcdefghijklmnopqrstuvwxyz0123456789");
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final Player player = mock(Player.class);
        private final Map<Long, Runnable> tasks = new HashMap<>();
        private final AuthSessionManager sessions = new AuthSessionManager();
        private final DisplayService displays = mock(DisplayService.class);
        private final PremiumBridge bridge;

        private Fixture() {
            ConfigManager config = mock(ConfigManager.class);
            RestrictionSettings restrictions = mock(RestrictionSettings.class);
            when(restrictions.getMinPasswordLength()).thenReturn(4);
            when(restrictions.getMaxPasswordLength()).thenReturn(26);
            when(config.getSettings()).thenReturn(new PluginSettings(null, null, restrictions, DisplaySettings.defaults(), settings));
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.getName()).thenReturn("Tester");
            when(player.isOnline()).thenReturn(true);
            bridge = new PremiumBridge(plugin, config, sessions, displays);
        }

        private MockedStatic<Bukkit> scheduler() {
            MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
                tasks.put(invocation.getArgument(2), invocation.getArgument(1));
                return null;
            });
            return bukkit;
        }

        private PremiumMessage outgoing() {
            tasks.get(1L).run();
            ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
            verify(player).sendPluginMessage(eq(plugin), eq(PremiumProtocol.CHANNEL), bytes.capture());
            return PremiumProtocol.decode(bytes.getValue(), settings);
        }
    }
}
