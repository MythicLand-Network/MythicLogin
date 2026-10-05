package cn.mythicland.mythiclogin.command;

import cn.mythicland.framework.bukkit.CommandComponent;
import cn.mythicland.framework.command.AbstractCommand;
import cn.mythicland.framework.command.CommandContext;
import cn.mythicland.framework.command.CommandRoute;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import org.bukkit.entity.Player;

@InjectComponent
@CommandComponent
public final class AuthCommands extends AbstractCommand {
    private final PlayerCommandService commandService;

    public AuthCommands(PlayerCommandService commandService) {
        this.commandService = commandService;
    }

    @CommandRoute(path = {"register"}, usages = "/register <密码> <重复密码>", maxArguments = Integer.MAX_VALUE)
    private void register(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 2) {
            this.commandService.sendAuthHint(player);
            return;
        }
        this.commandService.register(player, context.argument(0), context.argument(1));
    }

    @CommandRoute(path = {"login"}, usages = "/login <密码>", maxArguments = Integer.MAX_VALUE)
    private void login(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 1) {
            this.commandService.sendAuthHint(player);
            return;
        }
        this.commandService.login(player, context.argument(0));
    }

    @CommandRoute(path = {"premium"}, usages = "/premium <原密码>", maxArguments = Integer.MAX_VALUE)
    private void premium(CommandContext context) {
        Player player = requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 1) {
            player.sendMessage("§c用法: /premium <原密码>");
            return;
        }
        commandService.premium(player);
    }

    @CommandRoute(path = {"unpremium"}, usages = "/unpremium <新密码>", maxArguments = Integer.MAX_VALUE)
    private void unpremium(CommandContext context) {
        Player player = requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 1) {
            player.sendMessage("§c用法: /unpremium <新密码>");
            return;
        }
        commandService.unpremium(player);
    }

    @CommandRoute(path = {"logout"}, usages = "/logout", maxArguments = Integer.MAX_VALUE)
    private void logout(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 0) {
            this.commandService.sendAuthHint(player);
            return;
        }
        this.commandService.logout(player);
    }

    @CommandRoute(path = {"changepassword"}, usages = "/changepassword <新密码> <重复新密码>", maxArguments = Integer.MAX_VALUE)
    private void changePassword(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 2) {
            this.commandService.sendChangePasswordHint(player);
            return;
        }
        this.commandService.changePassword(player, context.argument(0), context.argument(1));
    }

    @CommandRoute(path = {"bind", "email"}, usages = "/bind email <邮箱>", maxArguments = Integer.MAX_VALUE)
    private void bindEmail(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 1) {
            this.commandService.sendBindHint(player);
            return;
        }
        this.commandService.bindEmail(player, context.argument(0));
    }

    @CommandRoute(path = {"bind", "code"}, usages = "/bind code <验证码>", maxArguments = Integer.MAX_VALUE)
    private void bindCode(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() != 1) {
            this.commandService.sendBindHint(player);
            return;
        }
        this.commandService.bindCode(player, context.argument(0));
    }

    @CommandRoute(path = {"forgotpassword"}, usages = "/fp [验证码] [新密码] [重复密码]", maxArguments = Integer.MAX_VALUE)
    private void forgotPassword(CommandContext context) {
        Player player = this.requirePlayer(context);
        if (player == null) return;
        if (context.argumentCount() == 0) {
            this.commandService.requestPasswordReset(player);
            return;
        }
        if (context.argumentCount() == 3) {
            this.commandService.resetPassword(
                    player,
                    context.argument(0),
                    context.argument(1),
                    context.argument(2)
            );
            return;
        }
        this.commandService.sendPasswordResetHint(player);
    }

    private Player requirePlayer(CommandContext context) {
        if (context.sender() instanceof Player player) return player;
        context.sender().sendMessage("§c只有玩家可以使用这个命令。");
        return null;
    }
}
