package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.common.premium.PremiumMessage;
import cn.mythicland.mythiclogin.common.premium.PremiumSettings;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.config.model.RestrictionSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.manager.DataManager;
import cn.mythicland.mythiclogin.model.AuthState;
import cn.mythicland.mythiclogin.util.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PremiumJoinTest {
    @Test
    void newOfflinePlayerCanEnterRegistrationWithoutPremiumAccount() {
        Fixture fixture = new Fixture();
        when(fixture.repository.findByExactPlayerName("Tester")).thenReturn(Optional.empty());
        PremiumMessage offline = new PremiumMessage(PremiumMessage.Type.OFFLINE, UUID.randomUUID(), "Tester",
                fixture.player.getUniqueId(), null, false, 0, "");
        when(fixture.bridge.query(fixture.player)).thenReturn(CompletableFuture.completedFuture(offline));
        try (MockedStatic<Bukkit> bukkit = fixture.scheduler()) {
            fixture.auth.handleJoin(fixture.player);
            assertEquals(AuthState.UNREGISTERED, fixture.sessions.getSession(fixture.player).getState());
            assertFalse(fixture.sessions.isAuthenticated(fixture.player));
            verify(fixture.messages).send(fixture.player, MessageKey.AUTH_REGISTER_REQUIRED);
            verify(fixture.player, never()).kickPlayer(anyString());
            verifyNoInteractions(fixture.displays);
        }
    }

    @Test
    void waitsForTrustedIdentityAndShowsOnlyBindingSuccess() {
        Fixture fixture = new Fixture();
        CompletableFuture<PremiumMessage> result = new CompletableFuture<>();
        when(fixture.bridge.query(fixture.player)).thenReturn(result);
        try (MockedStatic<Bukkit> bukkit = fixture.scheduler()) {
            fixture.auth.handleJoin(fixture.player);
            assertEquals(AuthState.LOADING, fixture.sessions.getSession(fixture.player).getState());
            verifyNoInteractions(fixture.displays);
            verifyNoInteractions(fixture.messages);
            result.complete(fixture.response(PremiumMessage.Type.BOUND, true));
            assertTrue(fixture.sessions.isAuthenticated(fixture.player));
            verify(fixture.displays).clear(fixture.player);
            verify(fixture.displays).showPremiumBoundTitle(fixture.player);
            verify(fixture.displays, never()).showAutoLoginTitle(any());
            verify(fixture.displays, never()).showSuccessTitle(any(), any());
        }
    }

    @Test
    void crossServerValidationDoesNotRepeatSuccessTitle() {
        Fixture fixture = new Fixture();
        PremiumMessage response = fixture.response(PremiumMessage.Type.AUTO_LOGIN, false);
        when(fixture.bridge.query(fixture.player)).thenReturn(CompletableFuture.completedFuture(response));
        try (MockedStatic<Bukkit> bukkit = fixture.scheduler()) {
            fixture.auth.handleJoin(fixture.player);
            assertTrue(fixture.sessions.isAuthenticated(fixture.player));
            verify(fixture.displays).clear(fixture.player);
            verifyNoMoreInteractions(fixture.displays);
        }
    }

    @Test
    void refusesOfflineResultForPremiumBoundAccount() {
        Fixture fixture = new Fixture();
        PremiumMessage offline = new PremiumMessage(PremiumMessage.Type.OFFLINE, UUID.randomUUID(), "Tester",
                fixture.player.getUniqueId(), null, true, 0, "");
        when(fixture.bridge.query(fixture.player)).thenReturn(CompletableFuture.completedFuture(offline));
        try (MockedStatic<Bukkit> bukkit = fixture.scheduler()) {
            fixture.auth.handleJoin(fixture.player);
            assertFalse(fixture.sessions.isAuthenticated(fixture.player));
            verify(fixture.player).kickPlayer(anyString());
            verifyNoInteractions(fixture.displays);
        }
    }

    private static final class Fixture {
        private final UUID premiumUuid = UUID.randomUUID();
        private final Player player = mock(Player.class);
        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final PremiumBridge bridge = mock(PremiumBridge.class);
        private final DisplayService displays = mock(DisplayService.class);
        private final MessageService messages = mock(MessageService.class);
        private final AuthSessionManager sessions = new AuthSessionManager();
        private final AccountRepository repository = mock(AccountRepository.class);
        private final AuthService auth;

        private Fixture() {
            when(player.getName()).thenReturn("Tester");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            ConfigManager config = mock(ConfigManager.class);
            RestrictionSettings restrictions = mock(RestrictionSettings.class);
            when(config.getSettings()).thenReturn(new PluginSettings(null, new SessionSettings(false, 5), restrictions,
                    DisplaySettings.defaults(), new PremiumSettings(true, "abcdefghijklmnopqrstuvwxyz0123456789")));
            DataManager data = mock(DataManager.class);
            when(data.getAccountRepository()).thenReturn(repository);
            when(repository.findByExactPlayerName("Tester")).thenReturn(Optional.of(
                    new Account("tester", "Tester", "!", null, 0, 1, null, false, false).withPremiumUuid(premiumUuid)));
            ExecutorService executor = mock(ExecutorService.class);
            doAnswer(invocation -> {
                invocation.getArgument(0, Runnable.class).run();
                return null;
            }).when(executor).execute(any(Runnable.class));
            when(data.getExecutorService()).thenReturn(executor);
            auth = new AuthService(plugin, mock(PluginLogger.class), config, data, messages, mock(PasswordService.class),
                    displays, sessions, mock(RestrictionService.class), mock(AccountNotificationService.class), bridge);
        }

        private PremiumMessage response(PremiumMessage.Type type, boolean showTitle) {
            return new PremiumMessage(type, UUID.randomUUID(), "Tester", player.getUniqueId(), premiumUuid, showTitle, 0, "");
        }

        private MockedStatic<Bukkit> scheduler() {
            MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            });
            return bukkit;
        }
    }
}
