package cn.mythicland.mythiclogin.velocity.listener;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.velocity.manager.IProxyRestrictionManager;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.lang.reflect.Field;

@InjectComponent
public final class ProxyCommandListener {

    private static final Field CHILDREN_FIELD = resolveField("children");
    private static final Field LITERALS_FIELD = resolveField("literals");
    private static final Field ARGUMENTS_FIELD = resolveField("arguments");

    private final IProxyRestrictionManager proxyRestrictionManager;

    public ProxyCommandListener(IProxyRestrictionManager proxyRestrictionManager) {
        this.proxyRestrictionManager = proxyRestrictionManager;
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        CommandSource commandSource = event.getCommandSource();
        if (!(commandSource instanceof Player)) {
            return;
        }
        Player player = (Player) commandSource;
        String command = "/" + event.getCommand();
        if (!proxyRestrictionManager.shouldBlockCommand(player, command)) {
            return;
        }
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        proxyRestrictionManager.sendBlockedMessage(player);
    }

    @Subscribe
    public void onTabComplete(TabCompleteEvent event) {
        List<String> filtered = proxyRestrictionManager.filterSuggestions(event.getPlayer(), new ArrayList<>(event.getSuggestions()));
        event.getSuggestions().clear();
        event.getSuggestions().addAll(filtered);
    }

    @Subscribe
    public void onPlayerAvailableCommands(PlayerAvailableCommandsEvent event) {
        Player player = event.getPlayer();
        List<String> commandNames = new ArrayList<>();
        for (CommandNode<?> child : event.getRootNode().getChildren()) {
            commandNames.add(child.getName());
        }
        for (String commandName : commandNames) {
            if (!proxyRestrictionManager.shouldBlockCommand(player, "/" + commandName)) {
                continue;
            }
            removeCommand(event.getRootNode(), commandName);
        }
    }

    @SuppressWarnings("unchecked")
    private void removeCommand(RootCommandNode<?> rootNode, String commandName) {
        if (CHILDREN_FIELD == null || LITERALS_FIELD == null || ARGUMENTS_FIELD == null) {
            return;
        }
        try {
            ((Map<String, ?>) CHILDREN_FIELD.get(rootNode)).remove(commandName);
            ((Map<String, ?>) LITERALS_FIELD.get(rootNode)).remove(commandName);
            ((Map<String, ?>) ARGUMENTS_FIELD.get(rootNode)).remove(commandName);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveField(String fieldName) {
        try {
            Field field = CommandNode.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
