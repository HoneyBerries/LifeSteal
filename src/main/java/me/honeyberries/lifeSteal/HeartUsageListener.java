package me.honeyberries.lifeSteal;

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
 * Listens for player interactions with "Heart" items and applies the corresponding effects.
 * Specifically, when a player uses a "Heart" item, their max health is increased.
 */
public class HeartUsageListener implements Listener {

    /**
     * Handles the event when a player interacts with an item in their hand.
     * If the item is a "Heart", it will increase the player's max health and consume the heart.
     *
     * @param event The event triggered when a player interacts with an item.
     */
    @EventHandler
    public void onPlayerUseHeart(PlayerInteractEvent event) {
        // Ensure the event is for the main hand to prevent double execution
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the item is valid and has item metadata
        if (item != null && item.hasItemMeta()) {
            PersistentDataContainer dataContainer = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");

            // Check if the item is a custom "Heart"
            if (dataContainer.has(key, PersistentDataType.STRING) && "heart".equals(dataContainer.get(key, PersistentDataType.STRING))) {
                event.setCancelled(true); // Prevent default interaction behavior

                // Increase player's max health by 2 (representing 1 heart)
                LifeStealHelper.adjustMaxHealth(player, 2);

                // Send feedback to the player
                player.sendMessage(Component.text("You have gained a heart!").color(NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Consume one "Heart" item
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}
