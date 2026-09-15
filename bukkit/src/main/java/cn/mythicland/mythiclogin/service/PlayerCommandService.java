package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.util.CommandHelpTheme;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@InjectComponent
public final class PlayerCommandService {

    private final AuthService authService;
    private final MessageService messageService;
    private final EmailAuthService emailAuthService;
    private final Runnable reloadAction;

    public PlayerCommandService(
            AuthService authService,
            EmailAuthService emailAuthService,
            MessageService messageService,
            Runnable reloadAction
    ) {
        this.authService = authService;
        this.emailAuthService = emailAuthService;
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
            || isBindCommand(command.label())
            || isForgotPasswordCommand(command.label())
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

        if (isBindCommand(label)) {
            if (parts.length != 3) {
                sendBindHint(player);
                return true;
            }
            String subCommand = parts[1].toLowerCase(Locale.ROOT);
            if ("email".equals(subCommand)) {
                bindEmail(player, parts[2]);
                return true;
            }
            if ("code".equals(subCommand)) {
                bindCode(player, parts[2]);
                return true;
            }
            sendBindHint(player);
            return true;
        }

        if (isForgotPasswordCommand(label)) {
            if (parts.length == 1) {
                requestPasswordReset(player);
                return true;
            }
            if (parts.length == 4) {
                resetPassword(player, parts[1], parts[2], parts[3]);
                return true;
            }
            sendPasswordResetHint(player);
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

    public void sendBindHint(Player player) {
        player.sendMessage(messageService.get(MessageKey.EMAIL_BIND_USAGE));
    }

    public void sendPasswordResetHint(Player player) {
        player.sendMessage(messageService.get(MessageKey.EMAIL_RESET_USAGE));
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

    public void bindEmail(Player player, String email) {
        authService.sendMessageWhenComplete(player, emailAuthService.requestBinding(player, email));
    }

    public void bindCode(Player player, String code) {
        authService.sendMessageWhenComplete(player, emailAuthService.verifyBinding(player, code));
    }

    public void requestPasswordReset(Player player) {
        authService.sendMessageWhenComplete(player, emailAuthService.requestPasswordReset(player));
    }

    public void resetPassword(Player player, String code, String newPassword, String confirmation) {
        authService.sendMessageWhenComplete(
                player,
                emailAuthService.resetPassword(player, code, newPassword, confirmation)
        );
    }

    private void handleAdminCommand(Player player, String[] parts) {
        if (parts.length <= 1) {
            sendRootUsage(player, parts[0]);
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
            sender.sendMessage(messageService.get(MessageKey.SYSTEM_NO_PERMISSION));
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
            sender.sendMessage(messageService.get(MessageKey.SYSTEM_NO_PERMISSION));
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
            sender.sendMessage(messageService.get(MessageKey.SYSTEM_NO_PERMISSION));
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

    public void sendRootUsage(CommandSender sender, String label) {
        sender.sendMessage("§c用法: /" + label + " <子命令>");
        sender.sendMessage("§c使用 /" + label + " help 查看所有命令。");
    }

    public void sendHelp(CommandSender sender) {
        CommandHelpTheme.sendUsage(sender, "/login <密码>");
        CommandHelpTheme.sendUsage(sender, "/register <密码> <密码>");
        CommandHelpTheme.sendUsage(sender, "/logout");
        CommandHelpTheme.sendUsage(sender, "/cp <新密码> <重复新密码>");
        CommandHelpTheme.sendUsage(sender, "/bind email <邮箱>");
        CommandHelpTheme.sendUsage(sender, "/bind code <验证码>");
        CommandHelpTheme.sendUsage(sender, "/fp [验证码] [新密码] [重复密码]");
        CommandHelpTheme.sendUsage(sender, "/mlg help");
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

    private boolean isBindCommand(String label) {
        return "bind".equals(label);
    }

    private boolean isForgotPasswordCommand(String label) {
        return "forgotpassword".equals(label) || "fp".equals(label);
    }

    private boolean isAdminCommand(String label) {
        return "mythiclogin".equals(label) || "mlg".equals(label) || "ml".equals(label);
    }

    private record ParsedCommand(String label, String[] parts) {
    }
}
