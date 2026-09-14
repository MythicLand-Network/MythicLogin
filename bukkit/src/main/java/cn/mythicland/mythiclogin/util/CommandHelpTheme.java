package cn.mythicland.mythiclogin.util;

import cn.mythicland.mythiclogin.common.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public final class CommandHelpTheme {

    private static final String DIVIDER = "&7&m----------------------------------------";

    private CommandHelpTheme() {
    }

    public static void sendDivider(CommandSender sender) {
        sender.sendMessage(TextUtil.color(DIVIDER));
    }

    public static void sendCommand(CommandSender sender, String command, String arguments, String description) {
        StringBuilder message = new StringBuilder("&d").append(command);
        appendUsageTokens(message, arguments);
        if (description != null && !description.trim().isEmpty()) {
            message.append(" &7").append(description.trim());
        }
        sender.sendMessage(TextUtil.color(message.toString()));
    }

    private static void appendUsageTokens(StringBuilder message, String arguments) {
        if (arguments == null) {
            return;
        }

        String trimmed = arguments.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        for (String token : trimmed.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            message.append(' ');
            if (isPlaceholderToken(token)) {
                message.append("&f").append(token);
            } else {
                message.append("&d").append(token);
            }
        }
    }

    private static boolean isPlaceholderToken(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return (normalized.startsWith("<") && normalized.endsWith(">"))
            || (normalized.startsWith("[") && normalized.endsWith("]"));
    }
}
