package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.common.util.TextUtil;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class MessageService {

    private final ConfigManager configManager;
    private String prefix;

    public MessageService(ConfigManager configManager) {
        this.configManager = configManager;
        reload();
    }

    public void reload() {
        prefix = TextUtil.color(configManager.getMessages().getString(MessageKey.PREFIX.getPath(), ""));
    }

    public void send(Player player, String path) {
        String message = get(path);
        if (message.trim().isEmpty()) return;
        player.sendMessage(message);
    }

    public void send(Player player, String path, Object... placeholders) {
        String message = get(path, placeholders);
        if (message.trim().isEmpty()) return;
        player.sendMessage(message);
    }

    public void send(Player player, MessageKey key, Object... placeholders) {
        send(player, key.getPath(), placeholders);
    }

    public String get(MessageKey key, Object... placeholders) {
        return get(key.getPath(), placeholders);
    }

    public String get(String path) {
        return get(path, new Object[0]);
    }

    public String get(String path, Object... placeholders) {
        YamlConfiguration messages = configManager.getMessages();
        String text = messages.getString(path, path);
        return TextUtil.placeholders(text == null ? "" : text.replace("{prefix}", prefix), placeholders);
    }

    public String format(String text, Object... placeholders) {
        return TextUtil.placeholders(text == null ? "" : text.replace("{prefix}", prefix), placeholders);
    }
}
