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
 * Registers the custom crafting recipe for the "Revival Item".
 * This recipe allows players to craft a revival item using various materials.
 */
public class RevivalItemRecipe {

    // Plugin instance
    private static final LifeSteal plugin = LifeSteal.getInstance();

    // Revival recipe namespaced key
    public static final NamespacedKey revivalRecipeKey = new NamespacedKey(plugin, "custom_revival_item_recipe");

    /**
     * Registers the custom crafting recipe for the revival item.
     */
    public static void registerRevivalRecipe() {
        ShapedRecipe revivalItemRecipe = new ShapedRecipe(revivalRecipeKey, LifeStealUtil.createRevivalItem(1));
        revivalItemRecipe.shape(LifeStealSettings.getRevivalRecipeShape());

        // Set ingredients from config
        for (Map.Entry<Character, Material> entry : LifeStealSettings.getRevivalRecipeIngredients().entrySet()) {
            revivalItemRecipe.setIngredient(entry.getKey(), entry.getValue());
        }

        Bukkit.addRecipe(revivalItemRecipe);
    }
}
