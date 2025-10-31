package me.honeyberries.lifeSteal.recipe;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealConstants;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import java.util.Map;

/**
 * Registers the custom crafting recipe for the "Revival" item.
 */
public class RevivalRecipe {
    
    private static final LifeSteal plugin = LifeSteal.getInstance();
    public static final NamespacedKey recipeKey = new NamespacedKey(plugin, LifeStealConstants.REVIVAL_RECIPE_KEY);
    
    /**
     * Registers the custom crafting recipe for the "Revival" item.
     * The recipe uses ingredients defined in the config file.
     */
    public static void registerRevivalRecipe() {
        ShapedRecipe revivalRecipe = new ShapedRecipe(recipeKey, LifeStealUtil.createRevivalItem(1));
        revivalRecipe.shape(LifeStealSettings.getRevivalRecipeShape());

        // set ingredients from config
        for (Map.Entry<Character, Material> entry : LifeStealSettings.getRevivalRecipeIngredients().entrySet()) {
            revivalRecipe.setIngredient(entry.getKey(), entry.getValue());
        }

        Bukkit.addRecipe(revivalRecipe);
    }
}


