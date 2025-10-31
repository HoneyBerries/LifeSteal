package me.honeyberries.lifeSteal.manager;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealConstants;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages player elimination state using file-based storage.
 * Handles both BAN and SPECTATOR elimination modes.
 */
public class EliminationManager {

    private static final LifeSteal plugin = LifeSteal.getInstance();

    /**
     * Checks if a player is eliminated.
     *
     * @param player The player to check
     * @return true if the player is eliminated, false otherwise
     */
    public static boolean isEliminated(OfflinePlayer player) {
        return EliminatedPlayersData.isEliminated(player.getUniqueId());
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

        // Mark player as eliminated in file storage
        EliminatedPlayersData.setEliminated(player.getUniqueId());

        String mode = LifeStealSettings.getEliminationMode();

        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            // Set to spectator mode using Folia scheduler
            player.getScheduler().run(plugin, task -> player.setGameMode(GameMode.SPECTATOR), null);
            player.sendMessage(Messages.eliminatedSpectator());

            plugin.getLogger().info(player.getName() + " has been eliminated and set to spectator mode.");
        } else if ("BAN".equalsIgnoreCase(mode)) {
            // Kick the player with a message
            player.getScheduler().run(plugin, task -> player.kick(Messages.eliminatedBanKick()), null);

            plugin.getLogger().info(player.getName() + " has been eliminated and kicked from the server.");
        }
    }

    /**
     * Revives an eliminated player, giving them the configured revival health.
     * Works for both online and offline players using file-based storage.
     *
     * @param player The player to revive
     * @return true if the player was successfully revived, false if they were not eliminated
     */
    public static boolean revivePlayer(OfflinePlayer player) {
        if (!isEliminated(player)) {
            return false;
        }

        // Remove eliminated status from file storage
        EliminatedPlayersData.removeEliminated(player.getUniqueId());

        // If player is online, update their health and gamemode
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                // Set their health and gamemode on entity scheduler
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
        List<UUID> eliminatedUUIDs = EliminatedPlayersData.getEliminatedPlayers();

        for (UUID uuid : eliminatedUUIDs) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            eliminated.add(player);
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
        return currentHealth <= LifeStealConstants.MIN_HEALTH_EPSILON;
    }
}

