package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.common.util.TextUtil;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import org.bukkit.entity.Player;

@InjectComponent
public final class MessageService {

    public void send(Player player, MessageKey key, Object... placeholders) {
        player.sendMessage(get(key, placeholders));
    }

    public String get(MessageKey key, Object... placeholders) {
        return format(key.getTemplate(), placeholders);
    }

    public String format(String text, Object... placeholders) {
        return TextUtil.placeholders(text == null ? "" : text, placeholders);
    }
}
