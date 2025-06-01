package me.honeyberries.lifeSteal.task;

import me.honeyberries.lifeSteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Handles the automatic discovery of heart crafting recipes in the LifeSteal plugin.
 * This class periodically scans the inventories of all online players to check if they
 * possess specific items (e.g., Totem of Undying or custom Heart items) that grant access
 * to heart crafting recipes.
 */
public class HeartRecipeDiscoveryTask implements Runnable {

    // Reference to the LifeSteal plugin instance
    private static final LifeSteal plugin = LifeSteal.getInstance();

    // Singleton instance of HeartRecipeDiscoveryTask
    private static final HeartRecipeDiscoveryTask INSTANCE = new HeartRecipeDiscoveryTask();

    /**
     * Provides access to the singleton instance of HeartRecipeDiscoveryTask.
     * This ensures that only one instance of the class is used throughout the plugin.
     *
     * @return The singleton instance of HeartRecipeDiscoveryTask.
     */
    public static HeartRecipeDiscoveryTask getInstance() {
        return INSTANCE;
    }

    /**
     * Periodically executed method that scans the inventories of all online players.
     * This method is triggered by the Bukkit scheduler at regular intervals.
     */
    @Override
    public void run() {
        // Iterate through all online players on the server
        for (Player player : Bukkit.getOnlinePlayers()) {
            // discover the heart crafting recipe for each player
            discoverRecipe(player);
        }
    }

    /**
     * Grants the player access to the heart crafting recipe.
     * This method unlocks the recipe identified by its NamespacedKey.
     *
     * @param player The player who should discover the recipe.
     */
    private static void discoverRecipe(Player player) {
        // Define the NamespacedKey for the custom heart recipe
        NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");

        // Unlock the recipe for the player if they haven't discovered it yet
        if (!player.hasDiscoveredRecipe(recipeKey)) {
            player.getScheduler().run(plugin, task -> {
                player.discoverRecipe(recipeKey);
            }, null);
        }
    }
}