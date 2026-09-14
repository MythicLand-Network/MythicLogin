package cn.mythicland.mythiclogin.velocity.manager;

import cn.mythicland.mythiclogin.infrastructure.database.DatabaseSettings;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.common.restriction.CommandAccessMode;
import cn.mythicland.mythiclogin.common.restriction.ProxyCommandSettings;
import cn.mythicland.mythiclogin.velocity.config.LoginServerSettings;
import cn.mythicland.mythiclogin.velocity.config.MessageConfig;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.domain.session.SessionSettings;
import cn.mythicland.mythiclogin.velocity.service.MessageService;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyRestrictionManagerImplTest {

    @Test
    void shouldHandleAuthenticationAndWhitelistFiltering(@TempDir Path tempDir) throws IOException {
        PluginSettings settings = new PluginSettings(
            DatabaseSettings.postgresql("127.0.0.1", 5432, "mythiclogin", "postgres", "change-me", "mythiclogin"),
            new SessionSettings(true, 5),
            new LoginServerSettings(true, Arrays.asList("auth")),
            new ProxyCommandSettings(true, CommandAccessMode.WHITELIST, Arrays.asList("/login", "/register"), Arrays.asList("/server"))
        );
        MessageService messageService = new MessageService(createMessageConfig(tempDir));
        AccountRepository accountRepository = mock(AccountRepository.class);
        PluginLogger pluginLogger = new PluginLogger(mock(Logger.class), "Test");
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUsername()).thenReturn("Tester");
        mockCurrentServer(player, "auth");

        ProxyRestrictionManagerImpl manager = new ProxyRestrictionManagerImpl(settings, messageService, accountRepository, pluginLogger);

        when(accountRepository.findByExactPlayerName("Tester")).thenReturn(Optional.empty());
        when(accountRepository.findByPlayerNameIgnoreCase("Tester")).thenReturn(Optional.empty());
        assertTrue(manager.shouldBlockCommand(player, "/server lobby"));

        Account logged = new Account("tester", "Tester", "hash", "127.0.0.1", System.currentTimeMillis(), 1L, "127.0.0.1", false, true);
        when(accountRepository.findByExactPlayerName("Tester")).thenReturn(Optional.of(logged));
        assertFalse(manager.shouldBlockCommand(player, "/server lobby"));

        Account sessionEligible = new Account("tester", "Tester", "hash", "127.0.0.1", System.currentTimeMillis(), 1L, "127.0.0.1", true, false);
        when(accountRepository.findByExactPlayerName("Tester")).thenReturn(Optional.of(sessionEligible));
        assertFalse(manager.shouldBlockCommand(player, "/server lobby"));

        Account unauthenticated = new Account("tester", "Tester", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1", false, false);
        when(accountRepository.findByExactPlayerName("Tester")).thenReturn(Optional.of(unauthenticated));
        List<String> filtered = manager.filterSuggestions(player, Arrays.asList("/login", "/server", "/register", "/glist"));
        assertEquals(Arrays.asList("/login", "/register"), filtered);
    }

    @Test
    void shouldOnlyBlockCommandsOnLoginServers(@TempDir Path tempDir) throws IOException {
        PluginSettings settings = new PluginSettings(
            DatabaseSettings.postgresql("127.0.0.1", 5432, "mythiclogin", "postgres", "change-me", "mythiclogin"),
            new SessionSettings(true, 5),
            new LoginServerSettings(false, Arrays.asList("auth", "login-2")),
            new ProxyCommandSettings(true, CommandAccessMode.WHITELIST, Arrays.asList("/login", "/register"), Arrays.asList("/server"))
        );
        MessageService messageService = new MessageService(createMessageConfig(tempDir));
        AccountRepository accountRepository = mock(AccountRepository.class);
        PluginLogger pluginLogger = new PluginLogger(mock(Logger.class), "Test");
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUsername()).thenReturn("Tester");

        Account unauthenticated = new Account("tester", "Tester", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1", false, false);
        when(accountRepository.findByExactPlayerName("Tester")).thenReturn(Optional.of(unauthenticated));

        ProxyRestrictionManagerImpl manager = new ProxyRestrictionManagerImpl(settings, messageService, accountRepository, pluginLogger);

        mockCurrentServer(player, "survival");
        assertFalse(manager.shouldBlockCommand(player, "/hub"));
        assertEquals(Arrays.asList("/hub", "/server"), manager.filterSuggestions(player, Arrays.asList("/hub", "/server")));

        mockCurrentServer(player, "auth");
        assertTrue(manager.shouldBlockCommand(player, "/hub"));
        assertEquals(Collections.emptyList(), manager.filterSuggestions(player, Arrays.asList("/hub", "/server")));
    }

    @Test
    void shouldBuildCaseMismatchKickMessage(@TempDir Path tempDir) throws IOException {
        PluginSettings settings = new PluginSettings(
            DatabaseSettings.postgresql("127.0.0.1", 5432, "mythiclogin", "postgres", "change-me", "mythiclogin"),
            new SessionSettings(true, 5),
            new LoginServerSettings(true, Arrays.asList("auth")),
            new ProxyCommandSettings(true, CommandAccessMode.WHITELIST, Arrays.asList("/login", "/register"), Arrays.asList("/server"))
        );
        MessageService messageService = new MessageService(createMessageConfig(tempDir));
        AccountRepository accountRepository = mock(AccountRepository.class);
        PluginLogger pluginLogger = new PluginLogger(mock(Logger.class), "Test");

        when(accountRepository.findByExactPlayerName("shanguanling")).thenReturn(Optional.empty());
        when(accountRepository.findByPlayerNameIgnoreCase("shanguanling")).thenReturn(Optional.of(
            new Account("shanguanling", "ShanguanLinG", "hash", "127.0.0.1", 1L, 1L, "127.0.0.1", false, false)
        ));

        ProxyRestrictionManagerImpl manager = new ProxyRestrictionManagerImpl(settings, messageService, accountRepository, pluginLogger);

        assertEquals(
            Optional.of("\u00A7c检测到已有账号, 请使用正确的游戏账户 \u00A7fShanguanLinG \u00A7c登录。"),
            manager.findCaseMismatchKickMessage("shanguanling")
        );
    }

    private void mockCurrentServer(Player player, String serverName) {
        ServerConnection serverConnection = mock(ServerConnection.class);
        ServerInfo serverInfo = mock(ServerInfo.class);
        when(serverConnection.getServerInfo()).thenReturn(serverInfo);
        when(serverInfo.getName()).thenReturn(serverName);
        when(player.getCurrentServer()).thenReturn(Optional.of(serverConnection));
    }

    private MessageConfig createMessageConfig(Path tempDir) throws IOException {
        Files.writeString(
            tempDir.resolve("messages.yml"),
            "prefix: '&7[&dMythicLoginVelocity&7]&r'\nproxy:\n  case-mismatch-kick: '&c检测到已有账号, 请使用正确的游戏账户 &f{0} &c登录。'\n  command-blocked: '&cblocked'\n  account-data-missing: '&cmissing'\n",
            StandardCharsets.UTF_8
        );
        return new MessageConfig(tempDir, getClass().getClassLoader());
    }
}
