package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.model.DisplaySettings;
import cn.mythicland.mythiclogin.config.model.PluginSettings;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.protocol.PacketDisplayAdapter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class PremiumDisplayTest {
    @Test
    void usesExistingColorsAndTimingWithoutSchedulingLoginPromptRestoration() {
        ConfigManager config = mock(ConfigManager.class);
        PluginSettings settings = new PluginSettings(null, null, null, DisplaySettings.defaults());
        when(config.getSettings()).thenReturn(settings);
        PacketDisplayAdapter packets = mock(PacketDisplayAdapter.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Tester");
        JavaPlugin plugin = mock(JavaPlugin.class);
        DisplayService displays = new DisplayService(plugin, packets, config, new MessageService());

        displays.showPremiumReconnectTitle(player);
        displays.showPremiumBoundTitle(player);
        displays.showPremiumFailureTitle(player, "账号已变更");
        displays.showPremiumUnboundTitle(player);
        displays.showPremiumUnbindFailureTitle(player, "账号已变更");
        displays.showPasswordFailureTitle(player);
        verify(packets, times(6)).sendTitle(eq(player), anyString(), anyString(), anyInt(), anyInt(), anyInt());
        verifyNoInteractions(plugin);
    }
}
