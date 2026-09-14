package cn.mythicland.mythiclogin.velocity.service;

import cn.mythicland.mythiclogin.common.util.TextUtil;
import cn.mythicland.mythiclogin.velocity.config.MessageConfig;
import cn.mythicland.mythiclogin.velocity.config.key.MessageKey;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final MessageConfig messageConfig;
    private String prefix;

    public MessageService(MessageConfig messageConfig) {
        this.messageConfig = messageConfig;
        reload();
    }

    public void reload() {
        prefix = TextUtil.color(messageConfig.getString(MessageKey.PREFIX.getPath(), ""));
    }

    public void send(Player player, String path, Object... placeholders) {
        String message = get(path, placeholders);
        if (message.trim().isEmpty()) return;
        player.sendMessage(deserialize(message));
    }

    public void send(Player player, MessageKey key, Object... placeholders) {
        send(player, key.getPath(), placeholders);
    }

    public String get(MessageKey key, Object... placeholders) {
        return get(key.getPath(), placeholders);
    }

    public String get(String path, Object... placeholders) {
        String text = messageConfig.getString(path, path);
        return TextUtil.placeholders(text == null ? "" : text.replace("{prefix}", prefix), placeholders);
    }

    public static Component deserialize(String message) {
        return LEGACY_SERIALIZER.deserialize(message);
    }
}
