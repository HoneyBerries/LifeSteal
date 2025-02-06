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
        NamespacedKey key2 = new NamespacedKey(LifeSteal.getInstance(), "custom_heart2");

        // Create a shaped recipe for the "Heart" item. 2 recipes for different orientations
        ShapedRecipe heartRecipe1 = new ShapedRecipe(key1, LifeStealHelper.createHeartItem(1));
        ShapedRecipe heartRecipe2 = new ShapedRecipe(key2, LifeStealHelper.createHeartItem(1));

        // Define the shape of the recipe
        heartRecipe1.shape("RGR", "DTD", "RGR");
        heartRecipe2.shape("RDR", "GTG", "RDR");

        // Set the ingredients for the recipe
        heartRecipe1.setIngredient('D', Material.DIAMOND);
        heartRecipe1.setIngredient('R', Material.REDSTONE);
        heartRecipe1.setIngredient('T', Material.TOTEM_OF_UNDYING);
        heartRecipe1.setIngredient('G', Material.GOLD_INGOT);

        heartRecipe2.setIngredient('D', Material.DIAMOND);
        heartRecipe2.setIngredient('R', Material.REDSTONE);
        heartRecipe2.setIngredient('T', Material.TOTEM_OF_UNDYING);
        heartRecipe2.setIngredient('G', Material.GOLD_INGOT);

        // Register the recipe with the server
        Bukkit.addRecipe(heartRecipe1);
        Bukkit.addRecipe(heartRecipe2);
    }

}
