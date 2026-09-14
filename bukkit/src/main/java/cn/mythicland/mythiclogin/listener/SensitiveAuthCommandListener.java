package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.service.PlayerCommandService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class SensitiveAuthCommandListener implements Listener {

    private final PlayerCommandService playerCommandService;

    public SensitiveAuthCommandListener(PlayerCommandService playerCommandService) {
        this.playerCommandService = playerCommandService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (playerCommandService.handle(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
