package cn.mythicland.mythiclogin.listener;

import cn.mythicland.framework.bukkit.ListenerComponent;
import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.PlayerCommandService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

@InjectComponent
@ListenerComponent
public final class SensitiveAuthCommandListener implements Listener {

    private final AuthService authService;
    private final PlayerCommandService playerCommandService;

    public SensitiveAuthCommandListener(
            AuthService authService,
            PlayerCommandService playerCommandService
    ) {
        this.authService = authService;
        this.playerCommandService = playerCommandService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!authService.shouldRestrict(event.getPlayer()) && !playerCommandService.isPremiumCommand(event.getMessage())) return;
        if (playerCommandService.handle(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
