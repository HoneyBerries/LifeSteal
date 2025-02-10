package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class HeartRecipe {

    public static void registerHeartRecipe() {
        // Craft the custom "Heart" item

        // Define the recipe's namespaced key1
        NamespacedKey key1 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart1");

        // Create a shaped recipe for the "Heart" item. 2 recipes for different orientations
        ShapedRecipe heartRecipe1 = new ShapedRecipe(key1, LifeStealHelper.createHeartItem(1));

        // Define the shape of the recipe
        heartRecipe1.shape("GRG", "DTD", "GIG");

        // Set the ingredients for the recipe
        heartRecipe1.setIngredient('D', Material.DIAMOND_BLOCK);
        heartRecipe1.setIngredient('R', Material.REDSTONE_BLOCK);
        heartRecipe1.setIngredient('T', Material.TOTEM_OF_UNDYING);
        heartRecipe1.setIngredient('G', Material.GOLD_INGOT);
        heartRecipe1.setIngredient('I', Material.IRON_BLOCK);

        // Register the recipe with the server
        Bukkit.addRecipe(heartRecipe1);
    }

}
