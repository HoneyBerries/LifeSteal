package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Handles when players use the custom Heart items to gain health.
 * <p>
 * This listener detects when players right-click with a Heart item in their hand
 * and applies the health gain according to the plugin's settings. It ensures the
 * item is consumed properly and respects the maximum health limits.
 */
public class HeartUsageListener implements Listener {

    /**
     * Handles the event when a player right-clicks with a Heart item.
     * <p>
     * This method:
     * 1. Verifies the item is a Heart item
     * 2. Checks if the player can gain more health
     * 3. Applies the health gain
     * 4. Consumes the item
     * 5. Provides feedback to the player
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with an item
     */
    @EventHandler
    public void onPlayerUseHeart(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle right-click air actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }


        ItemStack item = event.getItem();

        // Check if the item is a valid Heart item
        if (!LifeStealUtil.isHeartItem(item)) {
            return;
        }

        // Cancel the original event to prevent normal item usage
        event.setCancelled(true);

        // Get the health amount to add from configuration
        double healthToAdd = LifeStealSettings.getHealthPerItem();

        final boolean isAllowWithdraw = LifeStealSettings.isAllowWithdraw();

        // If health gain is disabled in config, inform the player and return
        if (!isAllowWithdraw) {
            player.sendMessage(Component.text("Heart items are currently disabled on this server.").color(NamedTextColor.RED));
            return;
        }

        // Check if the player has reached the maximum health limit
        if (LifeStealSettings.isMaxHealthLimitEnabled()) {
            double maxHealth = LifeStealSettings.getMaxHealthLimit();
            double currentHealth = LifeStealUtil.getMaxHealth(player);

            if (currentHealth >= maxHealth) {
                player.sendMessage(Component.text("You have reached the maximum health limit of ")
                        .color(NamedTextColor.RED)
                        .append(Component.text(LifeStealUtil.formatHealth(maxHealth / 2.0) + " " +
                        (maxHealth == 2.0 ? "heart" : "hearts")).color(NamedTextColor.GOLD)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                return;
            }

            // If adding exceeds the max, adjust the amount to add
            if (currentHealth + healthToAdd > maxHealth) {
                player.sendMessage(Component.text("You will exceed the maximum health limit of ")
                        .color(NamedTextColor.RED)
                        .append(Component.text(LifeStealUtil.formatHealth(maxHealth / 2.0) + " " +
                        (maxHealth == 2.0 ? "heart" : "hearts")).color(NamedTextColor.GOLD)));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                return;
            }
        }

        // Apply the health increase
        LifeStealUtil.adjustMaxHealth(player, healthToAdd);

        // Provide feedback to the player
        player.sendMessage(Component.text("You gained ")
                .append(Component.text(LifeStealUtil.formatHealth(healthToAdd / 2.0), NamedTextColor.GREEN))
                .append(Component.text(" " + (healthToAdd == 2.0 ? "heart" : "hearts") + "!").
                color(NamedTextColor.GOLD)));

        // Play a sound effect for feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

        // Consume one heart item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            // If it's the last item, remove it completely

            if (event.getHand() == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }
}

