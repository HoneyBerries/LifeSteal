package me.honeyberries.lifeSteal.recipe;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import java.util.Map;

/**
 * Registers the custom crafting recipe for the "Heart" item.
 * This recipe allows players to craft a "Heart" using various materials.
 */
public class HeartRecipe {

    // plugin instance
    private static final LifeSteal plugin = LifeSteal.getInstance();


    /**
     * Registers the custom crafting recipe for the "Heart" item.
     */
    public static void registerHeartRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "custom_heart_recipe");
        Bukkit.removeRecipe(key); // remove the old recipe if it exists

        ShapedRecipe heartRecipe = new ShapedRecipe(key, LifeStealUtil.createHeartItem(1));
        heartRecipe.shape(LifeStealSettings.getRecipeShape());

        // set ingredients from config
        for (Map.Entry<Character, Material> entry : LifeStealSettings.getRecipeIngredients().entrySet()) {
            heartRecipe.setIngredient(entry.getKey(), entry.getValue());
        }

        Bukkit.addRecipe(heartRecipe);
    }

}
