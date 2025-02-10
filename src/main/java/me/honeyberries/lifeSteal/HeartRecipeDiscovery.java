package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles the automatic discovery of heart crafting recipes in the LifeSteal plugin.
 * This class continuously scans player inventories to check if they should be granted
 * access to heart crafting recipes based on specific item possession.
 */
public class HeartRecipeDiscovery {

    /**
     * Initiates a repeating task that scans all online players' inventories
     * to check if they should discover heart crafting recipes.
     * This task runs continuously while the server is running.
     */
    public static void startInventoryScanTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Loop through all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    scanPlayerInventory(player);
                }
            }
        }.runTaskTimer(LifeSteal.getInstance(), 0, 0); // Runs every second (20 ticks)
    }

    /**
     * Scans a player's inventory for items that should trigger recipe discovery.
     * Currently checks for Totem of Undying and custom Heart items.
     *
     * @param player The player whose inventory should be scanned
     */
    private static void scanPlayerInventory(Player player) {
        boolean knowsRecipe = false;

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

        // Discover recipes if they haven't been discovered yet
        if (knowsRecipe) {
            discoverRecipe(player);
        }
    }

    /**
     * Grants the player access to heart crafting recipes.
     * This method unlocks two different heart recipes identified by their NamespacedKeys.
     *
     * @param player The player who should discover the recipes
     */
    private static void discoverRecipe(Player player) {
        NamespacedKey key1 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart1");

        player.discoverRecipe(key1);
    }
}