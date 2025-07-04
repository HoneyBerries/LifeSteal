package me.honeyberries.lifeSteal.util;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.List;
import java.util.Objects;

public class LifeStealUtil {
        /**
     * Adjusts the player's max health by the specified amount.
     * Ensures the player's health does not fall below 2.0 (1 heart).
     *
     * @param player The player whose health is being adjusted.
     * @param amount The amount to adjust by (positive or negative).
     */
    public static void adjustMaxHealth(@NotNull Player player, double amount) {
        double currentMaxHealth = getMaxHealth(player);
        double newMaxHealth = Math.max(2.0, currentMaxHealth + amount); // Minimum of 2 health (1 heart)
        setMaxHealth(player, newMaxHealth);

    }
    /**
     * Sets the player's max health to a specific value.
     *
     * @param player The player whose max health is being set.
     * @param health The new max health value.
     */
    public static void setMaxHealth(@NotNull Player player, double health) {
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
    }

    /**
     * Retrieves the player's current max health.
     *
     * @param player The player whose max health is being retrieved.
     * @return The player's max health.
     */
    public static double getMaxHealth(@NotNull Player player) {
        return Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
    }

    /**
     * Formats a health value for display, removing decimal places for whole numbers.
     * For example: 2.0 becomes "2", but 2.5 becomes "2.5"
     *
     * @param value The health value to format
     * @return A formatted string representation of the health value
     */
    public static String formatHealth(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int)value);
        } else {
            return String.format("%.1f", value);
        }
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
        Material material = Material.matchMaterial(LifeStealSettings.getHeartItemID());

        assert material != null;
        ItemStack heart = new ItemStack(material, quantity);

        // Get the ItemMeta
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            // Set the display name
            meta.displayName(Component.text(LifeStealSettings.getHeartItemName()).color(NamedTextColor.DARK_PURPLE));

            // Set the lore (description)
            meta.lore(List.of(
                    Component.text("Gives " + formatHealth(LifeStealSettings.getHealthPerItem() / 2.0) + " permanent " +
                            (LifeStealSettings.getHealthPerItem() == 2.0 ? "heart" : "hearts"))
                            .color(NamedTextColor.DARK_PURPLE)
            ));

            // Add a harmless enchantment to create a glowing effect
            meta.addEnchant(Enchantment.MENDING, 1, true);

            // Hide the enchantment details to keep the glow without showing the enchantment
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Add custom persistent data to uniquely identify the item
            NamespacedKey heartIDKey = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");
            meta.getPersistentDataContainer().set(heartIDKey, PersistentDataType.STRING, "heart");

            // Apply the ItemMeta to the ItemStack
            heart.setItemMeta(meta);
        }

        return heart;
    }

    /**
     * Checks if the given {@link ItemStack} is a custom Heart item.
     * <p>
     * This method verifies that the item is a heart item and contains
     * a unique persistent data key ("unique_heart_id") with the value "heart".
     * This ensures accurate identification even if the item's name or lore has been modified.
     *
     * @param item The {@link ItemStack} to check (can be null).
     * @return {@code true} if the item is a custom Heart, {@code false} otherwise.
     */

    public static boolean isHeartItem(ItemStack item) {
        if (item == null || item.getType() != Material.matchMaterial(LifeStealSettings.getHeartItemID())) {
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

