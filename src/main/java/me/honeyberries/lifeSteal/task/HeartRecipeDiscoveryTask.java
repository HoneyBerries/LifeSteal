package me.honeyberries.lifeSteal.task;

import me.honeyberries.lifeSteal.LifeSteal;
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
            // discover the heart and revival crafting recipes for each player
            discoverRecipes(player);
        }
    }

    private void discoverRecipes(Player player) {
        NamespacedKey heartRecipeKey = new NamespacedKey(plugin, "custom_heart_recipe");
        NamespacedKey revivalRecipeKey = new NamespacedKey(plugin, "custom_revival_recipe");

        if (!player.hasDiscoveredRecipe(heartRecipeKey)) {
            player.getScheduler().run(plugin, task -> player.discoverRecipe(heartRecipeKey), null);
        }
        
        if (!player.hasDiscoveredRecipe(revivalRecipeKey)) {
            player.getScheduler().run(plugin, task -> player.discoverRecipe(revivalRecipeKey), null);
        }
    }
}
