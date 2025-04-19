package me.honeyberries.lifeSteal.task;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the automatic discovery of heart crafting recipes in the LifeSteal plugin.
 * This class periodically scans the inventories of all online players to check if they
 * possess specific items (e.g., Totem of Undying or custom Heart items) that grant access
 * to heart crafting recipes.
 */
public class HeartRecipeDiscoveryTask implements Runnable {

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
            // Scan the player's inventory for specific items
            scanPlayerInventory(player);
        }
    }

    /**
     * Scans a player's inventory to check for items that should trigger recipe discovery.
     * If the player possesses a Totem of Undying or a custom Heart item, they are granted
     * access to the heart crafting recipe.
     *
     * @param player The player whose inventory is being scanned.
     */
    private static void scanPlayerInventory(Player player) {
        // Flag to track if the player should be granted the recipe
        boolean knowsRecipe = false;

        // Loop through all items in the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                // Check if the item is a Totem of Undying
                if (item.getType() == Material.TOTEM_OF_UNDYING || LifeStealUtil.isHeartItem(item)) {
                    knowsRecipe = true;
                    break; // Exit the loop once a match is found
                }
            }
        }

        // If the player possesses a relevant item, grant them the recipe
        if (knowsRecipe) {
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
        NamespacedKey recipeKey = new NamespacedKey(LifeSteal.getInstance(), "custom_heart_recipe");

        // Unlock the recipe for the player
        player.discoverRecipe(recipeKey);
    }
}