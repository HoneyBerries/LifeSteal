package me.honeyberries.lifeSteal.task;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.honeyberries.lifeSteal.LifeSteal;

public class RevivalItemRecipeDiscoveryTask implements Consumer<ScheduledTask> {

    private final LifeSteal plugin;

    public RevivalItemRecipeDiscoveryTask(LifeSteal plugin) {
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
        NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_revival_item_recipe");

        if (!player.hasDiscoveredRecipe(recipeKey)) {
            player.getScheduler().run(plugin, scheduledTask -> player.discoverRecipe(recipeKey), null);
        }
    }

}
