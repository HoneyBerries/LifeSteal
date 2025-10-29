package me.honeyberries.lifeSteal.manager;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages player elimination state using PersistentDataContainer.
 * Handles both BAN and SPECTATOR elimination modes.
 */
public class EliminationManager {
    
    private static final LifeSteal plugin = LifeSteal.getInstance();
    private static final NamespacedKey ELIMINATED_KEY = new NamespacedKey(plugin, "eliminated");
    
    /**
     * Checks if a player is eliminated.
     * 
     * @param player The player to check
     * @return true if the player is eliminated, false otherwise
     */
    public static boolean isEliminated(OfflinePlayer player) {
        return player.getPersistentDataContainer().has(ELIMINATED_KEY, PersistentDataType.BOOLEAN);
    }
    
    /**
     * Eliminates a player based on the configured elimination mode.
     * 
     * @param player The player to eliminate
     */
    public static void eliminatePlayer(Player player) {
        if (!LifeStealSettings.isEliminationEnabled()) {
            return;
        }
        
        // Mark player as eliminated
        player.getPersistentDataContainer().set(ELIMINATED_KEY, PersistentDataType.BOOLEAN, true);
        
        String mode = LifeStealSettings.getEliminationMode();
        
        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            // Set to spectator mode using Folia scheduler
            player.getScheduler().run(plugin, task -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Messages.eliminatedSpectator());
            }, null);
            
            plugin.getLogger().info(player.getName() + " has been eliminated and set to spectator mode.");
        } else if ("BAN".equalsIgnoreCase(mode)) {
            // Kick the player with a message
            player.getScheduler().run(plugin, task -> {
                player.kick(Messages.eliminatedBanKick());
            }, null);
            
            plugin.getLogger().info(player.getName() + " has been eliminated and kicked from the server.");
        }
    }
    
    /**
     * Revives an eliminated player, giving them the configured revival health.
     * 
     * @param player The player to revive
     * @return true if the player was successfully revived, false if they were not eliminated
     */
    public static boolean revivePlayer(OfflinePlayer player) {
        if (!isEliminated(player)) {
            return false;
        }
        
        // Remove eliminated status
        player.getPersistentDataContainer().remove(ELIMINATED_KEY);
        
        // If player is online, set their health and gamemode
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.getScheduler().run(plugin, task -> {
                    // Set health to revival health
                    double revivalHealth = LifeStealSettings.getRevivalHealth();
                    LifeStealUtil.setMaxHealth(onlinePlayer, revivalHealth);
                    onlinePlayer.setHealth(revivalHealth);
                    
                    // Set to survival mode if they were in spectator
                    if (onlinePlayer.getGameMode() == GameMode.SPECTATOR) {
                        onlinePlayer.setGameMode(GameMode.SURVIVAL);
                    }
                    
                    // Send revival message
                    double hearts = revivalHealth / 2.0;
                    String heartsWord = hearts == 1.0 ? "heart" : "hearts";
                    onlinePlayer.sendMessage(Messages.playerRevived(
                        onlinePlayer.getName(), 
                        LifeStealUtil.formatHealth(hearts), 
                        heartsWord
                    ));
                }, null);
            }
        }
        
        plugin.getLogger().info(player.getName() + " has been revived.");
        return true;
    }
    
    /**
     * Gets a list of all eliminated players.
     * 
     * @return List of eliminated players
     */
    public static List<OfflinePlayer> getEliminatedPlayers() {
        List<OfflinePlayer> eliminated = new ArrayList<>();
        
        // Check all offline players who have played before
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (isEliminated(player)) {
                eliminated.add(player);
            }
        }
        
        return eliminated;
    }
    
    /**
     * Checks if a player should be eliminated based on their current health.
     * 
     * @param player The player to check
     * @return true if the player should be eliminated, false otherwise
     */
    public static boolean shouldBeEliminated(Player player) {
        if (!LifeStealSettings.isEliminationEnabled()) {
            return false;
        }
        
        double currentHealth = LifeStealUtil.getMaxHealth(player);
        
        // When elimination is enabled, check if player has run out of hearts (at or below 0)
        // Use a small epsilon to account for floating point precision
        return currentHealth <= 0.01;
    }
}
