package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.domain.account.Account;
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

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceLoginLimitTest {
    @Test
    void limitsPasswordChecksBeforeSubmittingToSharedExecutor() {
        DataManager data = mock(DataManager.class);
        ExecutorService executor = mock(ExecutorService.class);
        Queue<Runnable> tasks = new ArrayDeque<>();
        doAnswer(invocation -> { tasks.add(invocation.getArgument(0)); return null; }).when(executor).execute(any());
        when(data.getExecutorService()).thenReturn(executor);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 25565));
        AuthSessionManager sessions = new AuthSessionManager();
        sessions.setPending(player, AuthState.PENDING_LOGIN,
                new Account("player", "Player", "hash", null, 1L, 1L, null, false, false), 60);
        PasswordService passwords = mock(PasswordService.class);
        MessageService messages = new MessageService();
        JavaPlugin plugin = mock(JavaPlugin.class);
        AuthService service = new AuthService(plugin, mock(PluginLogger.class), mock(ConfigManager.class), data,
                messages, passwords, mock(DisplayService.class), sessions, mock(RestrictionService.class),
                mock(AccountNotificationService.class), mock(PremiumBridge.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
            CompletableFuture<String> first = service.login(player, "wrong");
            assertEquals(messages.get(MessageKey.AUTH_LOGIN_RATE_LIMITED), service.login(player, "wrong").join());
            assertEquals(1, tasks.size());
            tasks.remove().run();
            assertEquals(messages.get(MessageKey.AUTH_LOGIN_FAILED), first.join());
            for (int i = 1; i < 5; i++) {
                CompletableFuture<String> result = service.login(player, "wrong");
                tasks.remove().run();
                assertEquals(messages.get(MessageKey.AUTH_LOGIN_FAILED), result.join());
            }
            sessions.setPending(player, AuthState.PENDING_LOGIN, sessions.getSession(player).getAccount(), 60);
            assertEquals(messages.get(MessageKey.AUTH_LOGIN_RATE_LIMITED), service.login(player, "wrong").join());
            verify(executor, times(5)).execute(any());
            verify(passwords, times(5)).verify("wrong", "hash");
        }
    }
}
