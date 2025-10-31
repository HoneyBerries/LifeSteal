package me.honeyberries.lifeSteal.task;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.honeyberries.lifeSteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Handles the automatic discovery of heart crafting recipes in the LifeSteal plugin.
 * This class periodically scans the inventories of all online players to check if they
 * possess specific items (e.g., Totem of Undying or custom Heart items) that grant access
 * to heart crafting recipes.
 */
public class HeartRecipeDiscoveryTask implements Consumer<ScheduledTask> {
    private final LifeSteal plugin;

    public HeartRecipeDiscoveryTask(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public void accept(ScheduledTask task) {
        // Iterate through all online players on the server
        for (Player player : Bukkit.getOnlinePlayers()) {
            // discover the heart crafting recipe for each player
            discoverRecipe(player);
        }
    }

    private void discoverRecipe(Player player) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");

        if (!player.hasDiscoveredRecipe(recipeKey)) {
            player.getScheduler().run(plugin, scheduledTask -> player.discoverRecipe(recipeKey), null);
        }
    }
}
