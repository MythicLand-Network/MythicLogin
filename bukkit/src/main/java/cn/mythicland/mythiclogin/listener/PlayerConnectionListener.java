package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.service.AuthService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final AuthService authService;

    public PlayerConnectionListener(AuthService authService) {
        this.authService = authService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        authService.prepareJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        authService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authService.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        authService.handleQuit(event.getPlayer());
    }
}
