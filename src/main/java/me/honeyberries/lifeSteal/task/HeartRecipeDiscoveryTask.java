package me.honeyberries.lifeSteal.task;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealConstants;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Handles the automatic discovery of heart and revival crafting recipes in the LifeSteal plugin.
 * This class periodically scans the inventories of all online players to check if they
 * possess specific items that grant access to crafting recipes.
 */
public class HeartRecipeDiscoveryTask implements Runnable {
    private final LifeSteal plugin;

    public HeartRecipeDiscoveryTask(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Iterate through all online players on the server
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Schedule recipe discovery on the player's entity scheduler for Folia compatibility
            player.getScheduler().run(plugin, task -> discoverRecipes(player), null);
        }
    }

    private void discoverRecipes(Player player) {
        NamespacedKey heartRecipeKey = new NamespacedKey(plugin, LifeStealConstants.HEART_RECIPE_KEY);
        NamespacedKey revivalRecipeKey = new NamespacedKey(plugin, LifeStealConstants.REVIVAL_RECIPE_KEY);

        // These operations are now running on the entity scheduler, so they can be executed directly
        if (!player.hasDiscoveredRecipe(heartRecipeKey)) {
            player.discoverRecipe(heartRecipeKey);
        }
        
        if (!player.hasDiscoveredRecipe(revivalRecipeKey)) {
            player.discoverRecipe(revivalRecipeKey);
        }
    }
}
