package cn.mythicland.mythiclogin.velocity.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.common.util.TextUtil;
import cn.mythicland.mythiclogin.velocity.config.key.MessageKey;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@InjectComponent
public final class MessageService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public void send(Player player, MessageKey key, Object... placeholders) {
        player.sendMessage(deserialize(get(key, placeholders)));
    }

    public String get(MessageKey key, Object... placeholders) {
        return TextUtil.placeholders(key.getTemplate(), placeholders);
    }

    public static Component deserialize(String message) {
        return LEGACY_SERIALIZER.deserialize(message);
    }
}
