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
    private static final NamespacedKey HEART_ID_KEY = new NamespacedKey(LifeSteal.getInstance(), "unique_heart_id");

    /**
     * Adjusts the player's max health by the specified amount.
     *
     * @param player The player whose health is being adjusted.
     * @param amount The amount to adjust by (positive or negative).
     */
    public static void adjustMaxHealth(@NotNull Player player, double amount) {
        double currentMaxHealth = getMaxHealth(player);
        double newMaxHealth = currentMaxHealth + amount;
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
        String materialName = LifeStealSettings.getHeartItemID();
        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            LifeSteal.getInstance().getLogger().severe("Invalid material ID in config.yml: " + materialName);
            return new ItemStack(Material.AIR); // Return an empty item to avoid errors
        }

        ItemStack heart = new ItemStack(material, quantity);
        ItemMeta meta = heart.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(LifeStealSettings.getHeartItemName()).color(NamedTextColor.DARK_PURPLE));

            double healthPerItem = LifeStealSettings.getHealthPerItem();
            double hearts = healthPerItem / 2.0;
            String heartText = hearts == 1.0 ? "heart" : "hearts";

            meta.lore(List.of(
                Component.text("Gives " + formatHealth(hearts) + " permanent " + heartText)
                    .color(NamedTextColor.DARK_PURPLE)
            ));

            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(HEART_ID_KEY, PersistentDataType.STRING, "heart");
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
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        String identifier = item.getItemMeta().getPersistentDataContainer().get(HEART_ID_KEY, PersistentDataType.STRING);
        return "heart".equals(identifier);
    }
}

