package me.honeyberries.lifeSteal;

import org.bukkit.ChatColor;
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

public class HeartUseListener implements Listener {

    @EventHandler
    public void onPlayerUseHeart(PlayerInteractEvent event) {
        // Ensure the event is for the main hand to prevent double execution
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta()) {
            PersistentDataContainer dataContainer = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");

            // Check if the item is our custom "Heart"
            if (dataContainer.has(key, PersistentDataType.STRING) && "heart".equals(dataContainer.get(key, PersistentDataType.STRING))) {
                event.setCancelled(true); // Prevent default interaction behavior

                // Increase player's max health by 2 (1 heart), without exceeding the max health
                LifeStealHelper.adjustMaxHealth(player, 2);

                // Provide feedback to the player
                player.sendMessage(ChatColor.GREEN + "You have gained 1 heart!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Consume one "Heart" item
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}
