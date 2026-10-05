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
        verify(packets).sendTitle(player, "§e请重新连接", "§7请在 §f60 §7秒内使用正版客户端重新进入", 10, 20, 10);
        displays.showPremiumBoundTitle(player);
        verify(packets).sendTitle(player, "§a绑定成功", "§7已为您开启正版免登录。", 10, 20, 10);
        displays.showPremiumFailureTitle(player, "账号已变更");
        verify(packets).sendTitle(player, "§c绑定失败", "§7账号已变更", 10, 20, 10);
        displays.showPremiumUnboundTitle(player);
        verify(packets).sendTitle(player, "§a取消成功", "§7已为您关闭正版免登录。", 10, 20, 10);
        displays.showPremiumUnbindFailureTitle(player, "账号已变更");
        verify(packets).sendTitle(player, "§c取消失败", "§7账号已变更", 10, 20, 10);
        displays.showPasswordFailureTitle(player);
        verify(packets).sendTitle(player, "§c密码错误!", "§7请重新输入正确密码", 0, 20, 0);
        verifyNoInteractions(plugin);
    }
}
