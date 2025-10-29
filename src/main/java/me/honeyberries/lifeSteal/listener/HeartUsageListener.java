package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.config.LifeStealConstants;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
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
            player.sendMessage(Messages.heartDisabled());
            return;
        }

        // Check if the player has reached the maximum health limit
        if (LifeStealSettings.isMaxHealthLimitEnabled()) {
            double maxHealth = LifeStealSettings.getMaxHealthLimit();
            double currentHealth = LifeStealUtil.getMaxHealth(player);

            if (currentHealth >= maxHealth) {
                double hearts = maxHealth / LifeStealConstants.HEALTH_POINTS_PER_HEART;
                String heartsWord = hearts == 1.0 ? "heart" : "hearts";
                player.sendMessage(Messages.maxHealthLimitReached(LifeStealUtil.formatHealth(hearts), heartsWord));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);
                return;
            }

            // If adding exceeds the max, adjust the amount to add
            if (currentHealth + healthToAdd > maxHealth) {
                double hearts = maxHealth / LifeStealConstants.HEALTH_POINTS_PER_HEART;
                String heartsWord = hearts == 1.0 ? "heart" : "hearts";
                player.sendMessage(Messages.maxHealthLimitExceeded(LifeStealUtil.formatHealth(hearts), heartsWord));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);
                return;
            }
        }

        // Apply the health increase
        LifeStealUtil.adjustMaxHealth(player, healthToAdd);

        // Provide feedback to the player
        double hearts = healthToAdd / LifeStealConstants.HEALTH_POINTS_PER_HEART;
        String heartsWord = hearts == 1.0 ? "heart" : "hearts";
        player.sendMessage(Messages.heartUsed(LifeStealUtil.formatHealth(hearts), heartsWord));

        // Play a sound effect for feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);

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

