package cn.mythicland.mythiclogin.command;

import cn.mythicland.framework.bukkit.CommandComponent;
import cn.mythicland.framework.command.AbstractCommand;
import cn.mythicland.framework.command.CommandArgument;
import cn.mythicland.framework.command.CommandContext;
import cn.mythicland.framework.command.CommandRoute;
import cn.mythicland.framework.command.SuggestionSource;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.PlayerCommandService;

@InjectComponent
@CommandComponent
public final class AdminCommands extends AbstractCommand {
    private final PlayerCommandService commandService;

    public AdminCommands(PlayerCommandService commandService) {
        this.commandService = commandService;
    }

    @CommandRoute(path = {"mythiclogin"}, usages = "/mythiclogin", maxArguments = 0)
    private void root(CommandContext context) {
        this.commandService.sendRootUsage(context.sender(), context.label());
    }

    @CommandRoute(path = {"mythiclogin", "help"}, usages = "/mythiclogin help")
    private void help(CommandContext context) {
        this.commandService.sendHelp(context.sender());
    }

    @CommandRoute(path = {"mythiclogin", "reload"}, permission = "mythiclogin.reload", usages = "/mythiclogin reload")
    private void reload(CommandContext context) {
        this.commandService.reload(context.sender());
    }

    @CommandRoute(
            path = {"mythiclogin", "password"},
            permission = "mythiclogin.password",
            usages = "/mythiclogin password <玩家名> <新密码>",
            minArguments = 2,
            maxArguments = 2,
            arguments = @CommandArgument(index = 0, source = SuggestionSource.ONLINE_PLAYERS)
    )
    private void password(CommandContext context) {
        this.commandService.updatePassword(context.sender(), context.argument(0), context.argument(1));
    }

    @CommandRoute(
            path = {"mythiclogin", "unregister"},
            permission = "mythiclogin.unregister",
            usages = "/mythiclogin unregister <玩家名>",
            minArguments = 1,
            maxArguments = 1,
            arguments = @CommandArgument(index = 0, source = SuggestionSource.ONLINE_PLAYERS)
    )
    private void unregister(CommandContext context) {
        this.commandService.unregister(context.sender(), context.argument(0));
    }
}
