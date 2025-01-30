package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LifeStealHelper {

    /**
     * Swaps one heart (2 health points) from one player to another.
     *
     * @param gainer The player gaining the heart.
     * @param loser  The player losing the heart.
     */
    public static void swapHeart(@NotNull Player gainer, @NotNull Player loser) {
        double loserMaxHealth = getMaxHealth(loser);

        // Ensure the loser has at least 2 hearts to give
        if (loserMaxHealth > 2.0) {
            adjustMaxHealth(loser, -2.0);
            adjustMaxHealth(gainer, 2.0);
            Bukkit.getLogger().info(loser.getName() + " lost 1 heart, and " + gainer.getName() + " gained 1 heart!");
        } else {
            Bukkit.getLogger().info("Swap failed: " + loser.getName() + " does not have enough hearts to lose.");
        }
    }

    /**
     * Adjusts the player's max health by the specified amount.
     * Ensures the player's health does not fall below 2.0 (1 heart).
     *
     * @param player The player whose health is being adjusted.
     * @param amount The amount to adjust by (positive or negative).
     */
    public static void adjustMaxHealth(@NotNull Player player, double amount) {
        double currentMaxHealth = getMaxHealth(player);
        double newMaxHealth = Math.max(2.0, currentMaxHealth + amount); // Minimum of 1 heart
        setMaxHealth(player, newMaxHealth);
    }

    /**
     * Sets the player's max health to a specific value.
     * Ensures the player's health does not fall below 2.0 (1 heart).
     *
     * @param player The player whose max health is being set.
     * @param health The new max health value.
     */
    public static void setMaxHealth(@NotNull Player player, double health) {
        double newMaxHealth = Math.max(2.0, health); // Minimum of 1 heart
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
    }

    /**
     * Retrieves the player's current max health.
     *
     * @param player The player whose max health is being retrieved.
     * @return The player's max health.
     */
    public static double getMaxHealth(@NotNull Player player) {
        return player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
    }
}
