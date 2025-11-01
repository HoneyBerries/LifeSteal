package me.honeyberries.lifeSteal.util;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the elimination system for the LifeSteal plugin.
 * <p>
 * This class handles tracking eliminated players, storing their data in eliminated-players.yml,
 * and managing elimination and revival operations. Eliminated players are stored by UUID
 * to prevent confusion with actual server bans.
 */
public class EliminationManager {

    private static final LifeSteal plugin = LifeSteal.getInstance();
    private static final Logger logger = plugin.getLogger();
    
    private static File eliminatedPlayersFile;
    private static YamlConfiguration eliminatedPlayersConfig;
    
    // In-memory cache for quick lookups
    private static final Set<UUID> eliminatedPlayers = new HashSet<>();

    /**
     * Initializes the EliminationManager by loading the eliminated players file.
     * This should be called when the plugin is enabled.
     */
    public static void initialize() {
        eliminatedPlayersFile = new File(plugin.getDataFolder(), "eliminated-players.yml");
        
        // Create the file if it doesn't exist
        if (!eliminatedPlayersFile.exists()) {
            try {
                eliminatedPlayersFile.createNewFile();
                logger.info("Created eliminated-players.yml file.");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create eliminated-players.yml file!", e);
            }
        }
        
        // Load the configuration
        eliminatedPlayersConfig = YamlConfiguration.loadConfiguration(eliminatedPlayersFile);
        
        // Load eliminated players into memory
        loadEliminatedPlayers();
        
        logger.info("EliminationManager initialized with " + eliminatedPlayers.size() + " eliminated player(s).");
    }

    /**
     * Loads all eliminated players from the YAML file into the in-memory cache.
     */
    private static void loadEliminatedPlayers() {
        eliminatedPlayers.clear();
        
        if (eliminatedPlayersConfig.contains("eliminated-players")) {
            List<String> uuidStrings = eliminatedPlayersConfig.getStringList("eliminated-players");
            for (String uuidString : uuidStrings) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    eliminatedPlayers.add(uuid);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in eliminated-players.yml: " + uuidString);
                }
            }
        }
    }

    /**
     * Saves the eliminated players to the YAML file.
     */
    private static void saveEliminatedPlayers() {
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : eliminatedPlayers) {
            uuidStrings.add(uuid.toString());
        }
        
        eliminatedPlayersConfig.set("eliminated-players", uuidStrings);
        
        try {
            eliminatedPlayersConfig.save(eliminatedPlayersFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save eliminated-players.yml!", e);
        }
    }

    /**
     * Checks if a player is currently eliminated.
     *
     * @param player The player to check.
     * @return true if the player is eliminated, false otherwise.
     */
    public static boolean isEliminated(@NotNull OfflinePlayer player) {
        return eliminatedPlayers.contains(player.getUniqueId());
    }

    /**
     * Eliminates a player from the server based on the configured elimination mode.
     * <p>
     * This method will either put the player in spectator mode or kick them from the server,
     * depending on the elimination mode setting. The player's UUID is stored in eliminated-players.yml.
     *
     * @param player The player to eliminate.
     */
    public static void eliminatePlayer(@NotNull Player player) {
        if (isEliminated(player)) {
            logger.warning("Attempted to eliminate player " + player.getName() + " who is already eliminated.");
            return;
        }

        // Add to eliminated players
        eliminatedPlayers.add(player.getUniqueId());
        saveEliminatedPlayers();

        LifeStealSettings.EliminationMode mode = LifeStealSettings.getEliminationMode();

        switch (mode) {
            case BAN -> {
                // Schedule the kick to occur after a short delay to ensure the player sees messages
                player.getScheduler().runDelayed(plugin, task -> {
                    player.kick(Component.text("You have been eliminated for running out of hearts!")
                        .color(NamedTextColor.RED));

                    // Broadcast elimination message
                    Bukkit.broadcast(
                        Component.text(player.getName() + " has been eliminated!", 
                            NamedTextColor.RED)
                    );

                    logger.info(player.getName() + " has been eliminated (KICKED) for running out of hearts.");
                }, null, 60L); // Delay in ticks (60 ticks = 3 seconds)
            }
            case SPECTATOR -> {
                // Put player in spectator mode immediately
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(net.kyori.adventure.text.Component.text("You have been eliminated and are now in spectator mode!", 
                        net.kyori.adventure.text.format.NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("You ran out of hearts!", 
                        NamedTextColor.GRAY)));

                // Broadcast elimination message
                Bukkit.broadcast(Component.text(player.getName() + " has been eliminated and is now a spectator!", 
                    NamedTextColor.YELLOW));
                logger.info(player.getName() + " has been eliminated (SPECTATOR) for running out of hearts.");
            }
        }
    }

    /**
     * Revives an eliminated player, restoring them to the server with the configured starting health.
     *
     * @param offlinePlayer The OfflinePlayer to revive.
     * @return true if the player was successfully revived, false if they were not eliminated.
     */
    public static boolean revivePlayer(@NotNull OfflinePlayer offlinePlayer) {
        if (!isEliminated(offlinePlayer)) {
            return false;
        }

        // Remove from eliminated players
        eliminatedPlayers.remove(offlinePlayer.getUniqueId());
        saveEliminatedPlayers();

        // If the player is online, restore their game mode and set their health
        if (offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                // Set to survival mode if they're in spectator
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SURVIVAL);
                }

                // Set their health to the revival amount
                double revivalHealth = LifeStealSettings.getHealthPerRevivalItem();
                LifeStealUtil.setMaxHealth(player, revivalHealth);
                player.setHealth(revivalHealth); // Also set current health

                player.sendMessage(Component.text("You have been revived!", 
                        NamedTextColor.GREEN)
                    .append(Component.newline())
                    .append(Component.text("Welcome back!", 
                        NamedTextColor.GOLD)));
            }
        }

        logger.info("Player " + offlinePlayer.getName() + " (" + offlinePlayer.getUniqueId() + ") has been revived.");
        return true;
    }

    /**
     * Gets a list of all eliminated player UUIDs.
     *
     * @return An unmodifiable set of eliminated player UUIDs.
     */
    @NotNull
    public static Set<UUID> getEliminatedPlayers() {
        return Collections.unmodifiableSet(eliminatedPlayers);
    }

    /**
     * Gets the total number of eliminated players.
     *
     * @return The number of eliminated players.
     */
    public static int getEliminatedCount() {
        return eliminatedPlayers.size();
    }

    /**
     * Clears all eliminated players. This should be used with caution, typically only for
     * administrative purposes or plugin uninstallation.
     */
    public static void clearAllEliminatedPlayers() {
        eliminatedPlayers.clear();
        saveEliminatedPlayers();
        logger.info("All eliminated players have been cleared.");
    }

    /**
     * Reloads the eliminated players from the file. Useful after manual edits to the YAML file.
     */
    public static void reload() {
        eliminatedPlayersConfig = YamlConfiguration.loadConfiguration(eliminatedPlayersFile);
        loadEliminatedPlayers();
        logger.info("EliminationManager reloaded with " + eliminatedPlayers.size() + " eliminated player(s).");
    }
}
