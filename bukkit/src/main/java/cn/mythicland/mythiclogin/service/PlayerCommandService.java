package cn.mythicland.mythiclogin.service;

import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.util.CommandHelpTheme;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class PlayerCommandService {

    private final AuthService authService;
    private final MessageService messageService;
    private final Runnable reloadAction;

    public PlayerCommandService(AuthService authService, MessageService messageService, Runnable reloadAction) {
        this.authService = authService;
        this.messageService = messageService;
        this.reloadAction = reloadAction;
    }

    public boolean isManagedCommand(String message) {
        ParsedCommand command = parse(message);
        if (command == null) {
            return false;
        }
        return isLoginCommand(command.label())
            || isRegisterCommand(command.label())
            || isChangePasswordCommand(command.label())
            || isLogoutCommand(command.label())
            || isAdminCommand(command.label());
    }

    public boolean handle(Player player, String message) {
        ParsedCommand command = parse(message);
        if (command == null) {
            return false;
        }

        String[] parts = command.parts();
        String label = command.label();
        if (isLoginCommand(label)) {
            if (parts.length != 2) {
                player.sendMessage(authService.authCommandHint(player));
                return true;
            }
            login(player, parts[1]);
            return true;
        }

        if (isRegisterCommand(label)) {
            if (parts.length != 3) {
                player.sendMessage(authService.authCommandHint(player));
                return true;
            }
            register(player, parts[1], parts[2]);
            return true;
        }

        if (isChangePasswordCommand(label)) {
            if (parts.length != 3) {
                player.sendMessage(authService.changePasswordHint(player));
                return true;
            }
            changePassword(player, parts[1], parts[2]);
            return true;
        }

        if (isLogoutCommand(label)) {
            logout(player);
            return true;
        }

        if (isAdminCommand(label)) {
            handleAdminCommand(player, parts);
            return true;
        }

        return false;
    }

    public void sendAuthHint(Player player) {
        player.sendMessage(authService.authCommandHint(player));
    }

    public void sendChangePasswordHint(Player player) {
        player.sendMessage(authService.changePasswordHint(player));
    }

    public void login(Player player, String password) {
        authService.sendMessageWhenComplete(player, authService.login(player, password));
    }

    public void register(Player player, String password, String confirmation) {
        authService.sendMessageWhenComplete(player, authService.register(player, password, confirmation));
    }

    public void changePassword(Player player, String newPassword, String confirmation) {
        authService.sendMessageWhenComplete(
                player,
                authService.changePassword(player, newPassword, confirmation)
        );
    }

    public void logout(Player player) {
        player.sendMessage(authService.logout(player));
    }

    private void handleAdminCommand(Player player, String[] parts) {
        if (parts.length <= 1) {
            sendHelp(player);
            return;
        }

        String subCommand = parts[1].toLowerCase(Locale.ROOT);
        if ("reload".equals(subCommand)) {
            handleReload(player, parts);
            return;
        }
        if ("password".equals(subCommand)) {
            handlePassword(player, parts);
            return;
        }
        if ("unregister".equals(subCommand)) {
            handleUnregister(player, parts);
            return;
        }
        sendHelp(player);
    }

    private void handleReload(Player player, String[] parts) {
        if (parts.length != 2) {
            sendHelp(player);
            return;
        }
        reload(player);
    }

    public void reload(CommandSender sender) {
        if (!sender.hasPermission("mythiclogin.reload")) {
            sender.sendMessage(messageService.get("system.no-permission"));
            return;
        }
        reloadAction.run();
        sender.sendMessage(messageService.get(MessageKey.SYSTEM_CONFIG_RELOADED));
    }

    private void handlePassword(Player player, String[] parts) {
        if (parts.length != 4) {
            sendHelp(player);
            return;
        }
        updatePassword(player, parts[2], parts[3]);
    }

    public void updatePassword(CommandSender sender, String playerName, String newPassword) {
        if (!sender.hasPermission("mythiclogin.password")) {
            sender.sendMessage(messageService.get("system.no-permission"));
            return;
        }
        if (!authService.isPasswordLengthValid(newPassword)) {
            sender.sendMessage(messageService.get(
                    MessageKey.AUTH_PASSWORD_LENGTH,
                    authService.passwordBoundsPlaceholders()
            ));
            return;
        }

        CompletableFuture<String> result = authService.accountExists(playerName)
                .thenCompose(exists -> {
                    if (exists) return authService.updatePassword(playerName, newPassword);
                    return CompletableFuture.completedFuture(null);
                })
                .thenApply(success -> {
                    if (success == null) {
                        return messageService.get(MessageKey.ADMIN_ACCOUNT_MISSING, playerName);
                    }
                    return success
                            ? messageService.get(MessageKey.ADMIN_PASSWORD_UPDATED, playerName)
                            : messageService.get(MessageKey.ADMIN_PASSWORD_UPDATE_FAILED);
                });
        authService.sendMessageWhenComplete(sender, result);
    }

    private void handleUnregister(Player player, String[] parts) {
        if (parts.length != 3) {
            sendHelp(player);
            return;
        }
        unregister(player, parts[2]);
    }

    public void unregister(CommandSender sender, String playerName) {
        if (!sender.hasPermission("mythiclogin.unregister")) {
            sender.sendMessage(messageService.get("system.no-permission"));
            return;
        }
        CompletableFuture<String> result = authService.accountExists(playerName)
                .thenCompose(exists -> {
                    if (exists) return authService.unregisterAccount(playerName);
                    return CompletableFuture.completedFuture(null);
                })
                .thenApply(success -> {
                    if (success == null) {
                        return messageService.get(MessageKey.ADMIN_ACCOUNT_MISSING, playerName);
                    }
                    return success
                            ? messageService.get(MessageKey.ADMIN_UNREGISTER_SUCCESS, playerName)
                            : messageService.get(MessageKey.ADMIN_UNREGISTER_FAILED);
                });
        authService.sendMessageWhenComplete(sender, result);
    }

    public void sendHelp(CommandSender sender) {
        CommandHelpTheme.sendUsage(sender, "/login <密码>");
        CommandHelpTheme.sendUsage(sender, "/register <密码> <密码>");
        CommandHelpTheme.sendUsage(sender, "/logout");
        CommandHelpTheme.sendUsage(sender, "/cp <新密码> <重复新密码>");
        CommandHelpTheme.sendUsage(sender, "/mlg reload");
        CommandHelpTheme.sendUsage(sender, "/mlg password <玩家名> <新密码>");
        CommandHelpTheme.sendUsage(sender, "/mlg unregister <玩家名>");
    }

    private ParsedCommand parse(String message) {
        if (message == null || message.length() <= 1 || message.charAt(0) != '/') {
            return null;
        }

        String content = message.substring(1).trim();
        if (content.isEmpty()) {
            return null;
        }

        String[] parts = content.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }
        return new ParsedCommand(parts[0].toLowerCase(Locale.ROOT), parts);
    }

    private boolean isLoginCommand(String label) {
        return "login".equals(label) || "l".equals(label);
    }

    private boolean isRegisterCommand(String label) {
        return "register".equals(label) || "reg".equals(label);
    }

    private boolean isChangePasswordCommand(String label) {
        return "cp".equals(label) || "changepassword".equals(label) || "changepass".equals(label);
    }

    private boolean isLogoutCommand(String label) {
        return "logout".equals(label);
    }

    private boolean isAdminCommand(String label) {
        return "mythiclogin".equals(label) || "mlg".equals(label) || "ml".equals(label);
    }

    private record ParsedCommand(String label, String[] parts) {
    }
}
