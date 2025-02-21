package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers the custom crafting recipe for the "Heart" item.
 * This recipe allows players to craft a "Heart" using various materials.
 */
public class HeartRecipe {

    /**
     * Registers the custom crafting recipe for the "Heart" item.
     * The recipe requires:
     * - Diamond Block (D)
     * - Redstone Block (R)
     * - Totem of Undying (T)
     * - Gold Block (G)
     * - Iron Block (I)
     */
    public static void registerHeartRecipe() {
        // Define the recipe's namespaced key
        NamespacedKey key1 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart_recipe1");

        // Create a shaped recipe for the "Heart" item
        ShapedRecipe heartRecipe1 = new ShapedRecipe(key1, LifeStealHelper.createHeartItem(1));

        // Define the shape of the recipe
        heartRecipe1.shape("GRG", "DTD", "GIG");

        // Set the ingredients for the recipe
        heartRecipe1.setIngredient('D', Material.DIAMOND_BLOCK);
        heartRecipe1.setIngredient('R', Material.REDSTONE_BLOCK);
        heartRecipe1.setIngredient('T', Material.TOTEM_OF_UNDYING);
        heartRecipe1.setIngredient('G', Material.GOLD_BLOCK);
        heartRecipe1.setIngredient('I', Material.IRON_BLOCK);

        // Register the recipe with the server
        Bukkit.addRecipe(heartRecipe1);
    }
}
