package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class HeartRecipeDiscovery implements Listener {

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack pickedUpItem = event.getItem().getItemStack();

        // Check if the player picked up a Totem of Undying
        if (pickedUpItem.getType().equals(Material.TOTEM_OF_UNDYING)) {
            // Discover recipes when the player picks up a Totem of Undying
            discoverRecipes(player);
        }
    }

    public void startRecipeDiscoveryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Loop through all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkInventoryForTotem(player);
                }
            }
        }.runTaskTimer(LifeSteal.getInstance(), 0, 0); // Run every tick (0)
    }

    // Check if the player has a Totem of Undying in their inventory
    private void checkInventoryForTotem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                discoverRecipes(player);
                return; // Stop once we find the Totem
            }
        }
    }

    // Discover recipes for the player
    private void discoverRecipes(Player player) {
        NamespacedKey key1 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart1");
        NamespacedKey key2 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart2");

        player.discoverRecipe(key1);
        player.discoverRecipe(key2);
    }

}
