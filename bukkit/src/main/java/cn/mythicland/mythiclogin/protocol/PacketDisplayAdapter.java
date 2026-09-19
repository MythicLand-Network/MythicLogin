package cn.mythicland.mythiclogin.protocol;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.common.util.TextUtil;
import cn.mythicland.mythiclogin.util.PluginLogger;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


@InjectComponent
public final class PacketDisplayAdapter {

    private final ProtocolManager protocolManager;
    private final PluginLogger pluginLogger;

    public PacketDisplayAdapter(JavaPlugin plugin, PluginLogger pluginLogger) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.pluginLogger = pluginLogger;
    }

    public void sendActionBar(Player player, String text) {
        if (text == null || text.trim().isEmpty()) return;
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.CHAT);
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(TextUtil.color(text)));
        packet.getBytes().write(0, (byte) 2);
        sendPacket(player, packet);
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        boolean blankTitle = title == null || title.trim().isEmpty();
        boolean blankSubtitle = subtitle == null || subtitle.trim().isEmpty();
        if (blankTitle && blankSubtitle) return;

        sendTitleTimes(player, fadeIn, stay, fadeOut);

        if (!blankTitle) {
            sendTitleText(player, title);
        }

        if (!blankSubtitle) {
            sendSubtitleText(player, subtitle);
        }
    }

    public void sendTitleTimes(Player player, int fadeIn, int stay, int fadeOut) {
        PacketContainer times = protocolManager.createPacket(PacketType.Play.Server.TITLE);
        times.getTitleActions().write(0, EnumWrappers.TitleAction.TIMES);
        times.getIntegers().write(0, fadeIn).write(1, stay).write(2, fadeOut);
        sendPacket(player, times);
    }

    public void sendTitleText(Player player, String title) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        PacketContainer titlePacket = protocolManager.createPacket(PacketType.Play.Server.TITLE);
        titlePacket.getTitleActions().write(0, EnumWrappers.TitleAction.TITLE);
        titlePacket.getChatComponents().write(0, WrappedChatComponent.fromText(TextUtil.color(title)));
        sendPacket(player, titlePacket);
    }

    public void sendSubtitleText(Player player, String subtitle) {
        if (subtitle == null || subtitle.trim().isEmpty()) {
            return;
        }
        PacketContainer subtitlePacket = protocolManager.createPacket(PacketType.Play.Server.TITLE);
        subtitlePacket.getTitleActions().write(0, EnumWrappers.TitleAction.SUBTITLE);
        subtitlePacket.getChatComponents().write(0, WrappedChatComponent.fromText(TextUtil.color(subtitle)));
        sendPacket(player, subtitlePacket);
    }

    private void sendPacket(Player player, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (RuntimeException exception) {
            pluginLogger.warning("Failed to send packet to " + player.getName() + ": " + exception.getMessage());
        }
    }
}
