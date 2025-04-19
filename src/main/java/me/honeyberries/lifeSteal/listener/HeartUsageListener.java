package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener class for handling the usage of heart items in the LifeSteal plugin.
 * This class listens for player interactions with heart items and adjusts the player's
 * maximum health accordingly.
 */
public class HeartUsageListener implements Listener {

    /**
     * Event handler for player interactions with heart items.
     * When a player right-clicks while holding a heart item, their maximum health
     * is increased, and the item is consumed.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with an item.
     */
    @EventHandler
    public void onPlayerUseHeart(PlayerInteractEvent event) {
        // Ensure the interaction is a right-click with the main hand
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the item has metadata and contains the unique heart identifier
        if (item != null && item.hasItemMeta()) {
            PersistentDataContainer dataContainer = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");

            if (dataContainer.has(key, PersistentDataType.STRING) &&
                    "heart".equals(dataContainer.get(key, PersistentDataType.STRING))) {
                // Cancel the event to prevent default behavior
                event.setCancelled(true);

                // Retrieve the health gain value from the plugin settings
                double healthGained = LifeStealSettings.getHealthPerItem();


                double currentHealth = LifeStealUtil.getMaxHealth(player);

                // Check if the player would exceed the max health limit
                if (LifeStealSettings.isMaxHealthLimitEnabled()) {
                    int maxHealth = LifeStealSettings.getMaxHealthLimit();
                    if (currentHealth >= maxHealth) {
                        player.sendMessage(Component.text("You have reached the maximum health limit of ")
                                .append(Component.text(maxHealth / 2.0 + " " + (maxHealth == 2 ? "heart" : "hearts")).color(NamedTextColor.RED)));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;

                    // If the player is below max health, check if health gain would exceed it
                    } else if (currentHealth + healthGained > maxHealth) {
                        // Cap the health gain to the maximum allowed
                        healthGained = maxHealth - currentHealth;
                    }
                }

                // Apply health increase
                LifeStealUtil.adjustMaxHealth(player, healthGained);

                // Notify the player about the health gain
                player.sendMessage(Component.text("You have gained ")
                        .append(Component.text("%.1f %s".formatted(healthGained / 2.0, healthGained == 2.0 ? "heart" : "hearts")).color(NamedTextColor.GREEN)));

                // Play a sound effect to indicate success
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Consume the heart item properly
                int newAmount = item.getAmount() - 1;
                if (newAmount <= 0) {
                    // Remove the item completely from the player's hand
                    player.getInventory().setItemInMainHand(null);
                } else {
                    // Reduce the count
                    item.setAmount(newAmount);
                }
            }
        }
    }
}
