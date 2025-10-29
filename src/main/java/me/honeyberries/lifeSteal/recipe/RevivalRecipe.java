package me.honeyberries.lifeSteal.recipe;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealConstants;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers the custom crafting recipe for the "Revival" item.
 */
public class RevivalRecipe {
    
    private static final LifeSteal plugin = LifeSteal.getInstance();
    public static final NamespacedKey recipeKey = new NamespacedKey(plugin, LifeStealConstants.REVIVAL_RECIPE_KEY);
    
    /**
     * Registers the custom crafting recipe for the "Revival" item.
     * The recipe uses heart items in a specific pattern to create a revival item.
     */
    public static void registerRevivalRecipe() {
        ShapedRecipe revivalRecipe = new ShapedRecipe(recipeKey, LifeStealUtil.createRevivalItem(1));
        
        // Simple recipe: H = Heart item, G = Gold Block, A = Apple
        // Pattern:
        // G G G
        // G H G
        // G A G
        revivalRecipe.shape("GGG", "GHG", "GAG");
        revivalRecipe.setIngredient('G', Material.GOLD_BLOCK);
        revivalRecipe.setIngredient('H', LifeStealUtil.createHeartItem(1));
        revivalRecipe.setIngredient('A', Material.APPLE);
        
        Bukkit.addRecipe(revivalRecipe);
    }
}
