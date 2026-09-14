package cn.mythicland.mythiclogin.listener;

import cn.mythicland.mythiclogin.config.key.MessageKey;
import cn.mythicland.mythiclogin.service.AuthService;
import cn.mythicland.mythiclogin.service.MessageService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class FallbackRestrictionListener implements Listener {

    private final AuthService authService;
    private final MessageService messageService;

    public FallbackRestrictionListener(AuthService authService, MessageService messageService) {
        this.authService = authService;
        this.messageService = messageService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        if (!shouldBlock(event.getPlayer())) return;
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) <= 0) return;
        Location locked = event.getFrom().clone();
        locked.setYaw(event.getTo().getYaw());
        locked.setPitch(event.getTo().getPitch());
        event.setTo(locked);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        block(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        block(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        block(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        block(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        block(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        block(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        block(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemHeld(PlayerItemHeldEvent event) {
        block(event.getPlayer(), event);
    }

    private void block(Player player, Cancellable cancellable) {
        if (!shouldBlock(player)) {
            return;
        }
        cancellable.setCancelled(true);
        messageService.send(player, MessageKey.RESTRICT_INTERACTION_BLOCKED);
    }

    private boolean shouldBlock(Player player) {
        return authService.shouldRestrict(player);
    }
}
