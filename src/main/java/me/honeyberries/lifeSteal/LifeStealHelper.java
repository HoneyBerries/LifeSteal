package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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
            LifeSteal.getInstance().getLogger().info(loser.getName() + " lost 1 heart, and " + gainer.getName() + " gained 1 heart!");
        } else {
            LifeSteal.getInstance().getLogger().info("Swap failed: " + loser.getName() + " does not have enough hearts to lose.");
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
        player.setHealth(Math.min(player.getHealth(), newMaxHealth));
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

    /**
     * Creates a custom "Heart" item represented by a Nether Star with unique properties.
     * <p>
     * This item is designed to be used in a lifesteal plugin, granting players a permanent heart upon use.
     * It features a custom display name, lore, a glowing effect, and unique metadata to distinguish it from regular Nether Stars.
     *
     * @param quantity The number of "Heart" items to create.
     * @return An ItemStack representing the custom "Heart" item with the specified quantity.
     */
    public static ItemStack createHeartItem(int quantity) {
        // Create the ItemStack with the specified quantity
        ItemStack heart = new ItemStack(Material.NETHER_STAR, quantity);

        // Get the ItemMeta
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            // Set the display name
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Heart");

            // Set the lore (description)
            meta.setLore(Arrays.asList(ChatColor.DARK_PURPLE + "Gives a permanent", ChatColor.DARK_PURPLE + "heart by using it"));

            // Add a harmless enchantment to create a glowing effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            // Hide the enchantment details to keep the glow without showing the enchantment
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Add custom persistent data to uniquely identify the item
            NamespacedKey key = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "heart");

            // Apply the ItemMeta to the ItemStack
            heart.setItemMeta(meta);
        }

        return heart;
    }

    /**
     * Checks if the given {@link ItemStack} is a custom Heart item.
     *
     * This method verifies that the item is a {@link Material#NETHER_STAR} and contains
     * a unique persistent data key ("unique_heart_id") with the value "heart".
     * This ensures accurate identification even if the item's name or lore has been modified.
     *
     * @param item The {@link ItemStack} to check (can be null).
     * @return {@code true} if the item is a custom Heart, {@code false} otherwise.
     */
    public static boolean isHeartItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false; // Not a Nether Star, can't be a Heart
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Check for the unique persistent data
        NamespacedKey key = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");
        String identifier = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        return "heart".equals(identifier);
    }
}
