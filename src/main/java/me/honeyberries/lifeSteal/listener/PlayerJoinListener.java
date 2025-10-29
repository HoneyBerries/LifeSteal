package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.manager.EliminationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles player join events for the elimination system.
 * Kicks eliminated players in BAN mode.
 */
public class PlayerJoinListener implements Listener {
    
    private final LifeSteal plugin = LifeSteal.getInstance();
    
    /**
     * Checks if a joining player is eliminated and handles accordingly.
     * 
     * @param event The PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is eliminated
        if (!EliminationManager.isEliminated(player)) {
            return;
        }
        
        // Only kick if elimination is enabled and mode is BAN
        if (!LifeStealSettings.isEliminationEnabled()) {
            return;
        }
        
        String mode = LifeStealSettings.getEliminationMode();
        
        if ("BAN".equalsIgnoreCase(mode)) {
            // Kick the player using Folia scheduler
            player.getScheduler().run(plugin, task -> {
                player.kick(Messages.eliminatedBanKick());
            }, null);
            
            plugin.getLogger().info(player.getName() + " tried to join but is eliminated (BAN mode).");
        }
    }
}
