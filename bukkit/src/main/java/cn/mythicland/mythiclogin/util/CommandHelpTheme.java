package cn.mythicland.mythiclogin.util;

import org.bukkit.command.CommandSender;

public final class CommandHelpTheme {
    private static final String HELP_PREFIX = "§4> §c";

    private CommandHelpTheme() {
    }

    public static void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(HELP_PREFIX + usage);
    }
}
