package cn.mythicland.mythiclogin.velocity.service;

import cn.mythicland.framework.minecraft.MinecraftProfiles;
import cn.mythicland.framework.security.PasswordHashes;
import cn.mythicland.mythiclogin.common.premium.PremiumMessage;
import cn.mythicland.mythiclogin.common.premium.PremiumProtocol;
import cn.mythicland.mythiclogin.common.premium.PremiumSettings;
import cn.mythicland.mythiclogin.domain.account.Account;
import cn.mythicland.mythiclogin.domain.account.AccountRepository;
import cn.mythicland.mythiclogin.velocity.config.PluginSettings;
import cn.mythicland.mythiclogin.velocity.util.PluginLogger;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.util.GameProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PremiumProxyServiceTest {
    private static final String NAME = "Tester";
    private final UUID officialUuid = UUID.randomUUID();
    private final PremiumSettings premium = new PremiumSettings(true, "abcdefghijklmnopqrstuvwxyz0123456789");
    private final AccountRepository repository = mock(AccountRepository.class);
    private final MinecraftProfiles profiles = mock(MinecraftProfiles.class);
    private final ProxyServer proxy = mock(ProxyServer.class);
    private final InboundConnection connection = mock(InboundConnection.class);
    private final Player player = mock(Player.class);
    private final ServerConnection server = mock(ServerConnection.class);
    private final PluginLogger logger = mock(PluginLogger.class);
    private PremiumProxyService service;

    @BeforeEach
    void setup() {
        PluginSettings settings = new PluginSettings(null, null, null, null, premium);
        service = new PremiumProxyService(settings, repository, proxy, profiles, logger);
        when(proxy.getAllPlayers()).thenReturn(List.of());
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 25565);
        when(connection.isActive()).thenReturn(true);
        when(connection.getRemoteAddress()).thenReturn(address);
        when(player.getRemoteAddress()).thenReturn(address);
        when(player.getUsername()).thenReturn(NAME);
        when(player.getUniqueId()).thenReturn(MinecraftProfiles.offlineUuid(NAME));
        when(player.getCurrentServer()).thenReturn(Optional.of(server));
        when(player.isActive()).thenReturn(true);
        when(server.getPlayer()).thenReturn(player);
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.empty());
        when(repository.findByExactPlayerName(NAME)).thenReturn(Optional.empty());
    }

    @Test
    void existingOfflineAccountCannotBeTakenByMatchingPremiumName() {
        Account offline = account();
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(offline));
        PreLoginEvent event = preLogin();
        assertTrue(event.getResult().isForceOfflineMode());
        verifyNoInteractions(profiles);
        LoginEvent login = finish(false, MinecraftProfiles.offlineUuid(NAME));
        assertTrue(login.getResult().isAllowed());
        assertFalse(service.isAuthenticated(player));
        verify(repository, never()).bindPremium(any(), any());
        verify(repository, never()).create(any());
    }

    @Test
    void newOfflineNameCanContinueToRegistrationWithoutCreatingPremiumAccount() {
        assertTrue(preLogin().getResult().isForceOfflineMode());
        assertTrue(finish(false, MinecraftProfiles.offlineUuid(NAME)).getResult().isAllowed());
        assertFalse(service.isAuthenticated(player));
        assertEquals(PremiumMessage.Type.OFFLINE, query(UUID.randomUUID()).type());
        verify(repository, never()).create(any());
        verify(repository, never()).bindPremium(any(), any());
        verifyNoInteractions(profiles);
    }

    @Test
    void boundPremiumIdentityIsVerifiedWhileGameUuidRemainsOffline() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account().withPremiumUuid(officialUuid)));
        assertTrue(preLogin().getResult().isOnlineModeAllowed());
        LoginEvent login = finish(true, officialUuid);
        assertTrue(login.getResult().isAllowed());
        assertTrue(service.isAuthenticated(player));
        verify(repository, never()).create(any());
        verifyNoInteractions(profiles);
        PremiumMessage first = query(UUID.randomUUID());
        assertEquals(PremiumMessage.Type.AUTO_LOGIN, first.type());
        assertTrue(first.showTitle());
        assertFalse(query(UUID.randomUUID()).showTitle());
    }

    @Test
    void profileIdentityIsNormalizedBeforeSkinListenersAndPreservesTextures() throws Exception {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()
                .withPremiumUuid(officialUuid)));
        preLogin();
        var textures = List.of(new GameProfile.Property("textures", "selected-skin", "signature"));
        var event = new GameProfileRequestEvent(connection, new GameProfile(officialUuid, NAME, textures), true);
        Subscribe subscription = PremiumProxyService.class.getMethod("onProfile", GameProfileRequestEvent.class)
                .getAnnotation(Subscribe.class);
        PostOrder skinListenerOrder = (PostOrder) Subscribe.class.getMethod("order").getDefaultValue();

        assertTrue(subscription.order().ordinal() < skinListenerOrder.ordinal());
        service.onProfile(event);

        assertEquals(MinecraftProfiles.offlineUuid(NAME), event.getGameProfile().getId());
        assertEquals(officialUuid, event.getOriginalProfile().getId());
        assertEquals(textures, event.getGameProfile().getProperties());
        when(player.isOnlineMode()).thenReturn(true);
        LoginEvent login = new LoginEvent(player);
        run(service.onLogin(login));
        assertTrue(login.getResult().isAllowed());
        assertTrue(service.isAuthenticated(player));
    }

    @Test
    void rejectsOfflineAndWrongOfficialIdentityForBoundAccount() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account().withPremiumUuid(officialUuid)));
        preLogin();
        assertFalse(finish(false, MinecraftProfiles.offlineUuid(NAME)).getResult().isAllowed());
        service = new PremiumProxyService(new PluginSettings(null, null, null, null, premium), repository, proxy, profiles, logger);
        preLogin();
        assertFalse(finish(true, UUID.randomUUID()).getResult().isAllowed());
    }

    @Test
    void refusesPremiumBindingChangedDuringHandshake() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account().withPremiumUuid(officialUuid)));
        preLogin();
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()));
        assertFalse(finish(true, officialUuid).getResult().isAllowed());
        verify(repository, never()).create(any());
        verify(repository, never()).bindPremium(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newPlayerAlwaysUsesOfflineModeRegardlessOfOfficialProfile(boolean profileUnavailable) {
        if (profileUnavailable) {
            when(profiles.findByName(NAME)).thenThrow(new IllegalStateException("Unavailable"));
        } else {
            when(profiles.findByName(NAME)).thenReturn(Optional.of(new MinecraftProfiles.Profile(officialUuid, NAME)));
        }
        assertTrue(preLogin().getResult().isForceOfflineMode());
        assertTrue(finish(false, MinecraftProfiles.offlineUuid(NAME)).getResult().isAllowed());
        assertFalse(service.isAuthenticated(player));
        verifyNoInteractions(profiles);
        verify(repository, never()).create(any());
    }

    @Test
    void refusesDuplicateNameWithoutDisconnectingExistingPlayer() {
        when(proxy.getAllPlayers()).thenReturn(List.of(player));
        assertFalse(preLogin().getResult().isAllowed());
        verify(player, never()).disconnect(any());
    }

    @Test
    void rejectsReplayedQueryAndHandlesClientChannelWithoutForwarding() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()));
        preLogin();
        finish(false, MinecraftProfiles.offlineUuid(NAME));
        UUID nonce = UUID.randomUUID();
        query(nonce);
        query(nonce);
        verify(player).disconnect(any());
        PluginMessageEvent spoof = new PluginMessageEvent(player, server, PremiumProxyService.CHANNEL, new byte[]{1});
        assertNull(service.onMessage(spoof));
        assertFalse(spoof.getResult().isAllowed());
    }

    @Test
    void rejectsBackendQuerySignedWithDifferentSecret() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()));
        preLogin();
        finish(false, MinecraftProfiles.offlineUuid(NAME));
        PremiumSettings backend = new PremiumSettings(true, "0123456789abcdefghijklmnopqrstuvwxyz");
        PremiumMessage request = new PremiumMessage(
                PremiumMessage.Type.QUERY, UUID.randomUUID(), NAME, player.getUniqueId(), null, false, 0, ""
        );
        PluginMessageEvent event = new PluginMessageEvent(
                server, player, PremiumProxyService.CHANNEL, PremiumProtocol.encode(request, backend)
        );

        run(service.onMessage(event));

        verify(player).disconnect(MessageService.deserialize("&c账号认证通信失败, 请重新连接。"));
        verify(server, never()).sendPluginMessage(any(), any(byte[].class));
    }

    @Test
    void unbindingRequiresBackendApprovalAndChangesFutureConnectionsToPasswordLogin() {
        Account bound = account().withPremiumUuid(officialUuid);
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(bound));
        when(repository.findByExactPlayerName(NAME)).thenReturn(Optional.of(bound));
        when(repository.unbindPremium(eq(bound), anyString())).thenReturn(true);
        preLogin();
        finish(true, officialUuid);

        CommandExecuteEvent command = new CommandExecuteEvent(player, "unpremium new-password");
        assertNull(service.onPremiumCommand(command));
        run(service.onUnpremiumCommand(command));
        assertFalse(command.getResult().isAllowed());
        PremiumMessage check = lastResponse();
        assertEquals(PremiumMessage.Type.UNBIND_CHECK, check.type());
        assertEquals(Integer.toString("new-password".length()), check.detail());
        verify(repository, never()).unbindPremium(any(), anyString());

        approveUnbinding(check);

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(repository).unbindPremium(eq(bound), passwordHash.capture());
        assertTrue(new PasswordHashes().verify("new-password", passwordHash.getValue()));
        assertEquals(PremiumMessage.Type.UNBOUND, lastResponse().type());
        assertFalse(service.isAuthenticated(player));
        PremiumMessage crossServer = query(UUID.randomUUID());
        assertEquals(PremiumMessage.Type.OFFLINE, crossServer.type());
        assertNull(crossServer.premiumUuid());
        assertFalse(crossServer.showTitle());
    }

    @Test
    void offlineConnectionCannotRemovePremiumBinding() {
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()));
        when(repository.findByExactPlayerName(NAME)).thenReturn(Optional.of(account().withPremiumUuid(officialUuid)));
        preLogin();
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(account()));
        finish(false, MinecraftProfiles.offlineUuid(NAME));
        run(service.onUnpremiumCommand(new CommandExecuteEvent(player, "unpremium new-password")));
        assertEquals(PremiumMessage.Type.UNBIND_REJECTED, lastResponse().type());
        verify(repository, never()).unbindPremium(any(), anyString());
    }

    @Test
    void unbindingFailureKeepsPremiumAuthentication() {
        Account bound = account().withPremiumUuid(officialUuid);
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(bound));
        when(repository.findByExactPlayerName(NAME)).thenReturn(Optional.of(bound));
        preLogin();
        finish(true, officialUuid);
        run(service.onUnpremiumCommand(new CommandExecuteEvent(player, "unpremium new-password")));
        approveUnbinding(lastResponse());
        assertEquals(PremiumMessage.Type.UNBIND_REJECTED, lastResponse().type());
        assertTrue(service.isAuthenticated(player));
    }

    private void approveUnbinding(PremiumMessage check) {
        PremiumMessage request = new PremiumMessage(
                PremiumMessage.Type.UNBIND_REQUEST, check.nonce(), NAME, player.getUniqueId(), null, false, 0, check.detail()
        );
        run(service.onMessage(new PluginMessageEvent(
                server, player, PremiumProxyService.CHANNEL, PremiumProtocol.encode(request, premium)
        )));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bindingRequiresPasswordProofThenOnlineHandshakeAndUnchangedAccount(boolean accountUnchanged) {
        Account offline = account();
        when(repository.findByPlayerNameIgnoreCase(NAME)).thenReturn(Optional.of(offline));
        when(repository.findByExactPlayerName(NAME)).thenReturn(Optional.of(offline));
        when(profiles.findByName(NAME)).thenReturn(Optional.of(new MinecraftProfiles.Profile(officialUuid, NAME)));
        preLogin();
        finish(false, MinecraftProfiles.offlineUuid(NAME));
        CommandExecuteEvent command = new CommandExecuteEvent(player, "premium original-password");
        run(service.onPremiumCommand(command));
        assertFalse(command.getResult().isAllowed());
        PremiumMessage proof = lastResponse();
        assertEquals(PremiumMessage.Type.BIND_CHECK, proof.type());
        PremiumMessage request = new PremiumMessage(PremiumMessage.Type.BIND_REQUEST, proof.nonce(), NAME,
                player.getUniqueId(), null, false, proof.registerDate(), proof.detail());
        run(service.onMessage(new PluginMessageEvent(server, player, PremiumProxyService.CHANNEL, PremiumProtocol.encode(request, premium))));
        assertEquals(PremiumMessage.Type.BIND_ACCEPTED, lastResponse().type());
        verify(repository, never()).bindPremium(any(), any());
        when(connection.isActive()).thenReturn(false);
        InboundConnection reconnect = mock(InboundConnection.class);
        when(reconnect.isActive()).thenReturn(true);
        InetSocketAddress address = player.getRemoteAddress();
        when(reconnect.getRemoteAddress()).thenReturn(address);
        PreLoginEvent event = new PreLoginEvent(reconnect, NAME);
        run(service.onPreLogin(event));
        assertTrue(event.getResult().isOnlineModeAllowed());
        GameProfileRequestEvent profile = new GameProfileRequestEvent(reconnect, new GameProfile(officialUuid, NAME, List.of()), true);
        service.onProfile(profile);
        when(player.isOnlineMode()).thenReturn(true);
        when(repository.bindPremium(offline, officialUuid)).thenReturn(accountUnchanged);
        LoginEvent login = new LoginEvent(player);
        run(service.onLogin(login));
        assertEquals(accountUnchanged, login.getResult().isAllowed());
        verify(repository).bindPremium(offline, officialUuid);
        verify(profiles).findByName(NAME);
        if (accountUnchanged) {
            assertTrue(service.isAuthenticated(player));
            assertEquals(PremiumMessage.Type.BOUND, query(UUID.randomUUID()).type());
        }
    }

    private Account account() {
        return new Account("tester", NAME, new PasswordHashes().hashWithSalt("original-password", "0123456789abcdef"),
                null, 0, 1, null, false, false);
    }

    private PreLoginEvent preLogin() {
        PreLoginEvent event = new PreLoginEvent(connection, NAME);
        run(service.onPreLogin(event));
        return event;
    }

    private LoginEvent finish(boolean online, UUID uuid) {
        GameProfileRequestEvent profile = new GameProfileRequestEvent(connection, new GameProfile(uuid, NAME, List.of()), online);
        service.onProfile(profile);
        assertEquals(MinecraftProfiles.offlineUuid(NAME), profile.getGameProfile().getId());
        when(player.isOnlineMode()).thenReturn(online);
        LoginEvent event = new LoginEvent(player);
        run(service.onLogin(event));
        return event;
    }

    private PremiumMessage query(UUID nonce) {
        PremiumMessage request = new PremiumMessage(PremiumMessage.Type.QUERY, nonce, NAME, player.getUniqueId(), null, false, 0, "");
        run(service.onMessage(new PluginMessageEvent(server, player, PremiumProxyService.CHANNEL, PremiumProtocol.encode(request, premium))));
        return lastResponse();
    }

    private PremiumMessage lastResponse() {
        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(server, atLeastOnce()).sendPluginMessage(eq(PremiumProxyService.CHANNEL), bytes.capture());
        return PremiumProtocol.decode(bytes.getValue(), premium);
    }

    private static void run(EventTask task) {
        if (task != null) task.execute(mock(Continuation.class));
    }
}
