package cn.mythicland.mythiclogin.protocol;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.manager.ConfigManager;
import cn.mythicland.mythiclogin.service.AuthSessionManager;
import cn.mythicland.mythiclogin.service.MessageService;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import cn.mythicland.mythiclogin.util.PluginLogger;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@InjectComponent
public final class ProtocolGatekeeper {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;
    private final AuthSessionManager authSessionManager;
    private final ConfigManager configManager;
    private final MessageService messageService;
    private final PlayerCommandService playerCommandService;
    private final PluginLogger pluginLogger;
    private PacketAdapter adapter;

    public ProtocolGatekeeper(
        JavaPlugin plugin,
        ProtocolManager protocolManager,
        AuthSessionManager authSessionManager,
        ConfigManager configManager,
        MessageService messageService,
        PlayerCommandService playerCommandService,
        PluginLogger pluginLogger
    ) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
        this.authSessionManager = authSessionManager;
        this.configManager = configManager;
        this.messageService = messageService;
        this.playerCommandService = playerCommandService;
        this.pluginLogger = pluginLogger;
    }

    public void register() {
        adapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
            PacketType.Play.Client.CHAT,
            PacketType.Play.Client.TAB_COMPLETE,
            PacketType.Play.Client.POSITION,
            PacketType.Play.Client.POSITION_LOOK,
            PacketType.Play.Client.LOOK,
            PacketType.Play.Client.BLOCK_DIG,
            PacketType.Play.Client.BLOCK_PLACE,
            PacketType.Play.Client.USE_ENTITY,
            PacketType.Play.Client.WINDOW_CLICK,
            PacketType.Play.Client.CLOSE_WINDOW,
            PacketType.Play.Client.HELD_ITEM_SLOT,
            PacketType.Play.Client.ARM_ANIMATION,
            PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handle(event);
            }
        };
        protocolManager.addPacketListener(adapter);
    }

    public void unregister() {
        if (adapter != null) {
            protocolManager.removePacketListener(adapter);
        }
    }

    private void handle(PacketEvent event) {
        Player player = event.getPlayer();
        if (event.getPacketType() == PacketType.Play.Client.CHAT || event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            String input = event.getPacket().getStrings().read(0);
            if (input != null && playerCommandService.isPremiumCommand(input)) {
                event.setCancelled(true);
                if (event.getPacketType() == PacketType.Play.Client.CHAT) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) playerCommandService.handle(player, input);
                    });
                }
                return;
            }
        }
        if (!authSessionManager.shouldRestrict(player)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT) {
            handleChatPacket(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            handleTabCompletePacket(event);
            return;
        }

        if (isMovementPacket(event.getPacketType())) {
            return;
        }

        event.setCancelled(true);
        if (isInventoryPacket(event.getPacketType())) {
            Bukkit.getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    private void handleChatPacket(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        String message = packet.getStrings().read(0);
        Player player = event.getPlayer();
        if (playerCommandService.isManagedCommand(message)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                playerCommandService.handle(player, message);
            });
            return;
        }
        if (message.startsWith("/")) {
            if (!configManager.getSettings().restrictions().isCommandAllowed(message)) {
                event.setCancelled(true);
                messageService.send(player, MessageKey.AUTH_COMMAND_BLOCKED);
            }
            return;
        }
        if (configManager.getSettings().restrictions().isBlockChat()) {
            event.setCancelled(true);
            messageService.send(player, MessageKey.RESTRICT_CHAT_BLOCKED);
        }
    }

    private void handleTabCompletePacket(PacketEvent event) {
        if (!configManager.getSettings().restrictions().isBlockNonWhitelistTabComplete()) {
            return;
        }

        PacketContainer packet = event.getPacket();
        String message = packet.getStrings().read(0);
        if (message == null) return;

        String trimmed = message.trim();
        if (trimmed.isEmpty()) return;
        if (message.charAt(0) != '/') return;

        boolean allowed = configManager.getSettings().restrictions().isTabCompleteAllowed(message);
        event.setCancelled(true);
        if (!allowed) {
            return;
        }

        sendWhitelistTabCompletions(event.getPlayer(), message);
    }

    private void sendWhitelistTabCompletions(Player player, String input) {
        PacketContainer response = protocolManager.createPacket(PacketType.Play.Server.TAB_COMPLETE);
        List<String> suggestions = buildWhitelistSuggestions(input);
        response.getStringArrays().write(0, suggestions.toArray(new String[0]));
        try {
            protocolManager.sendServerPacket(player, response, false);
        } catch (RuntimeException exception) {
            pluginLogger.warning("Failed to send tab completions to " + player.getName() + ": " + exception.getMessage());
        }
    }

    private List<String> buildWhitelistSuggestions(String input) {
        List<String> filtered = new ArrayList<>();
        String normalizedInput = configManager.getSettings().restrictions().normalizeCommandInput(input);
        if (normalizedInput.indexOf(' ') >= 0) {
            return filtered;
        }

        String typed = input.substring(1);
        for (String prefix : configManager.getSettings().restrictions().getCommandPrefixWhitelist()) {
            if (!"/".equals(normalizedInput) && !prefix.startsWith(normalizedInput)) {
                continue;
            }
            if ("/".equals(prefix)) {
                continue;
            }
            filtered.add(prefix);
        }

        if (filtered.size() <= 1) {
            return filtered;
        }

        List<String> withoutExactTypedAlias = new ArrayList<>();
        for (String suggestion : filtered) {
            if (typed.equals(normalizeSuggestion(suggestion))) {
                continue;
            }
            withoutExactTypedAlias.add(suggestion);
        }
        return withoutExactTypedAlias.isEmpty() ? filtered : withoutExactTypedAlias;
    }

    private String normalizeSuggestion(String suggestion) {
        if (suggestion == null) {
            return null;
        }
        String normalized = suggestion.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.charAt(0) == '/') {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private boolean isMovementPacket(PacketType packetType) {
        return packetType == PacketType.Play.Client.POSITION
            || packetType == PacketType.Play.Client.POSITION_LOOK
            || packetType == PacketType.Play.Client.LOOK;
    }

    private boolean isInventoryPacket(PacketType packetType) {
        return packetType == PacketType.Play.Client.WINDOW_CLICK
            || packetType == PacketType.Play.Client.CLOSE_WINDOW
            || packetType == PacketType.Play.Client.HELD_ITEM_SLOT;
    }
}
