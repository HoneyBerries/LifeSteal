package me.honeyberries.lifeSteal.manager;

import me.honeyberries.lifeSteal.LifeSteal;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages persistent storage of eliminated players in a YAML file.
 * This provides a workaround for the read-only PDC limitation on OfflinePlayer.
 */
public class EliminatedPlayersData {

    private static final LifeSteal plugin = LifeSteal.getInstance();
    private static File dataFile;
    private static YamlConfiguration dataConfig;
    private static final String ELIMINATED_PLAYERS_KEY = "eliminated-players";

    /**
     * Initializes the eliminated players data file.
     */
    public static void initialize() {
        dataFile = new File(plugin.getDataFolder(), "eliminated_players.yml");

        if (!dataFile.exists()) {
            plugin.saveResource("eliminated_players.yml", false);
        }

        reload();
    }

    /**
     * Reloads the data from the file.
     */
    public static void reload() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Saves the data to the file.
     */
    private static void save() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save eliminated_players.yml", e);
        }
    }

    /**
     * Checks if a player is eliminated.
     *
     * @param uuid The UUID of the player
     * @return true if the player is eliminated, false otherwise
     */
    public static boolean isEliminated(UUID uuid) {
        List<String> eliminatedPlayers = dataConfig.getStringList(ELIMINATED_PLAYERS_KEY);
        return eliminatedPlayers.contains(uuid.toString());
    }

    /**
     * Marks a player as eliminated.
     *
     * @param uuid The UUID of the player
     */
    public static void setEliminated(UUID uuid) {
        List<String> eliminatedPlayers = dataConfig.getStringList(ELIMINATED_PLAYERS_KEY);
        String uuidString = uuid.toString();

        if (!eliminatedPlayers.contains(uuidString)) {
            eliminatedPlayers.add(uuidString);
            dataConfig.set(ELIMINATED_PLAYERS_KEY, eliminatedPlayers);
            save();
        }
    }

    /**
     * Removes a player from the eliminated list (revives them).
     *
     * @param uuid The UUID of the player
     */
    public static void removeEliminated(UUID uuid) {
        List<String> eliminatedPlayers = dataConfig.getStringList(ELIMINATED_PLAYERS_KEY);
        String uuidString = uuid.toString();

        if (eliminatedPlayers.remove(uuidString)) {
            dataConfig.set(ELIMINATED_PLAYERS_KEY, eliminatedPlayers);
            save();
        }
    }

    /**
     * Gets all eliminated player UUIDs.
     *
     * @return List of eliminated player UUIDs
     */
    public static List<UUID> getEliminatedPlayers() {
        List<String> eliminatedPlayers = dataConfig.getStringList(ELIMINATED_PLAYERS_KEY);
        List<UUID> uuids = new ArrayList<>();

        for (String uuidString : eliminatedPlayers) {
            try {
                uuids.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in eliminated_players.yml: " + uuidString);
            }
        }

        return uuids;
    }

    /**
     * Clears all eliminated players.
     */
    public static void clearAll() {
        dataConfig.set(ELIMINATED_PLAYERS_KEY, new ArrayList<>());
        save();
    }
}
