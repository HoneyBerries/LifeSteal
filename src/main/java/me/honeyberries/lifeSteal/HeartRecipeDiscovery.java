package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the automatic discovery of heart crafting recipes in the LifeSteal plugin.
 * This class continuously scans player inventories to check if they should be granted
 * access to heart crafting recipes based on specific item possession.
 */
public class HeartRecipeDiscovery implements Runnable {

    private static final HeartRecipeDiscovery INSTANCE = new HeartRecipeDiscovery();

    /**
     * Returns the singleton instance of HeartRecipeDiscovery.
     *
     * @return The singleton instance of HeartRecipeDiscovery.
     */
    public static @NotNull HeartRecipeDiscovery getInstance() {
        return INSTANCE;
    }

    /**
     * The method to run periodically that checks the inventories of all online players.
     * It scans each player's inventory to determine if they should be granted crafting recipes.
     */
    @Override
    public void run() {
        // Loop through all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            scanPlayerInventory(player);
        }
    }

    /**
     * Scans a player's inventory for items that should trigger recipe discovery.
     * Currently checks for Totem of Undying and custom Heart items.
     *
     * @param player The player whose inventory should be scanned.
     */
    private static void scanPlayerInventory(Player player) {
        boolean knowsRecipe = false;

        // Iterate through the player's inventory to find relevant items
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                // Check if the item is a Totem of Undying
                if (item.getType() == Material.TOTEM_OF_UNDYING) {
                    knowsRecipe = true;
                    break; // Stop checking once found
                }
                // Check if the item is a custom Heart
                if (LifeStealHelper.isHeartItem(item)) {
                    knowsRecipe = true;
                    break; // Stop checking once found
                }
            }
        }

        // If the player has an item that grants the recipe, discover the recipe for them
        if (knowsRecipe) {
            discoverRecipe(player);
        }
    }

    /**
     * Grants the player access to heart crafting recipes.
     * This method unlocks two different heart recipes identified by their NamespacedKeys.
     *
     * @param player The player who should discover the recipes.
     */
    private static void discoverRecipe(Player player) {
        // Define the NamespacedKey for the custom heart recipe
        NamespacedKey recipeKey1 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart_recipe1");

        // Grant the player access to the custom heart recipe
        player.discoverRecipe(recipeKey1);
    }
}
