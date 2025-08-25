package me.honeyberries.lifeSteal.config;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.recipe.HeartRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the configuration settings for the LifeSteal plugin.
 * <p>
 * This class handles loading options from the `config.yml` file, providing
 * access to gameplay settings such as health limits, death penalties, heart item
 * properties, and crafting recipes.
 */
public class LifeStealSettings {

    // Static instance of the main plugin for accessing plugin-related functionalities.
    private static final LifeSteal plugin = LifeSteal.getInstance();
    // Static logger instance for recording plugin-related events and errors, specifically for configuration management.
    private static final Logger LOGGER = plugin.getLogger();

    // --- Configuration Keys ---
    private static final String MAX_HEALTH_LIMIT_KEY = "max-health-limit";
    private static final String MIN_HEALTH_LIMIT_KEY = "min-health-limit";
    private static final String NATURAL_DEATH_HEALTH_LOST_KEY = "death-settings.natural-death.health-lost";
    private static final String MONSTER_DEATH_HEALTH_LOST_KEY = "death-settings.monster-death.health-lost";
    private static final String PLAYER_DEATH_HEALTH_LOST_KEY = "death-settings.player-death.health-lost";
    private static final String PLAYER_KILL_HEALTH_GAINED_KEY = "death-settings.player-death.health-gained";
    private static final String HEALTH_PER_ITEM_KEY = "heart-item.health-per-item";
    private static final String ALLOW_WITHDRAW_KEY = "features.allow-withdraw.enabled";
    private static final String ALLOW_CRAFTING_KEY = "heart-item.allow-crafting";
    private static final String IGNORE_KEEP_INVENTORY_KEY = "features.ignore-keep-inventory.enabled";
    private static final String HEART_ITEM_NAME_KEY = "heart-item.heart-item-name";
    private static final String HEART_ITEM_ID_KEY = "heart-item.heart-item-id";
    private static final String RECIPE_SHAPE_KEY = "heart-item.recipe.shape";
    private static final String RECIPE_INGREDIENTS_KEY = "heart-item.recipe.ingredients";


    // --- Configuration Properties ---

    /** The maximum health a player can have. A value of 0 or less disables this limit. */
    private static double maxHealthLimit;

    /** The minimum health a player can have */
    private static double minHealthLimit;

    /** The amount of health lost upon a natural death (e.g., starvation, fall damage). */
    private static double naturalDeathHealthLost;

    /** The amount of health lost when killed by a monster. */
    private static double monsterDeathHealthLost;

    /** The amount of health lost when killed by another player. */
    private static double playerDeathHealthLost;

    /** The amount of health gained when a player kills another player. */
    private static double playerKillHealthGained;

    /** The amount of health restored when a player consumes a heart item. */
    private static double healthPerItem;

    /** Determines if players are allowed to withdraw health to create heart items. */
    private static boolean allowWithdraw;

    /** Determines if players are allowed to craft the heart item. */
    private static boolean allowCrafting;

    /** Determines if the plugin should ignore the server's `keepInventory` game rule when applying health loss on death. */
    private static boolean ignoreKeepInventory;

    /** The custom display name for the heart item. */
    private static String heartItemName;

    /** The Material ID (e.g., "NETHER_STAR") of the heart item. */
    private static String heartItemID;

    /** An array of strings defining the shape of the crafting recipe for the heart item. Each string represents a row. */
    private static String[] recipeShape;

    /** A map defining the ingredients of the crafting recipe. Each character in the {@link #recipeShape} maps to a {@link Material}. */
    private static Map<Character, Material> recipeIngredients;


    /**
     * Loads the configuration settings from the `config.yml` file.
     * <p>
     * If the `config.yml` file does not exist in the plugin's data folder,
     * it will be created by copying the default configuration file from the
     * plugin's resources. Any exceptions encountered during the loading process
     * are caught and logged as severe errors, with default values being applied
     * to ensure the plugin can still function (albeit with default settings).
     * After loading, the {@link #logConfiguration()} method is called to output
     * the loaded settings to the server console for verification.
     */
    public static void loadConfig() {
        try {
            // Ensure the default config.yml is present and load it.
            plugin.saveDefaultConfig();
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Load all settings from the config file.
            loadCoreSettings(config);
            loadDeathSettings(config);
            loadHeartItemSettings(config);
            loadRecipe(config);

            // Validate and adjust settings as needed.
            validateHealthSettings();

            // Register or unregister the custom recipe based on the loaded config.
            updateHeartRecipe();

            LOGGER.info("Configuration loaded successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load config.yml. Plugin will use default values.", e);
            loadDefaultValues();
            LOGGER.warning("Plugin is running with default configuration values due to config load failure!");
        }
        // Log the final configuration.
        logConfiguration();
    }

    private static void loadCoreSettings(YamlConfiguration config) {
        maxHealthLimit = config.getDouble(MAX_HEALTH_LIMIT_KEY, 0);
        minHealthLimit = config.getDouble(MIN_HEALTH_LIMIT_KEY, 1);
        allowWithdraw = config.getBoolean(ALLOW_WITHDRAW_KEY, false);
        ignoreKeepInventory = config.getBoolean(IGNORE_KEEP_INVENTORY_KEY, false);
    }

    private static void loadDeathSettings(YamlConfiguration config) {
        naturalDeathHealthLost = config.getDouble(NATURAL_DEATH_HEALTH_LOST_KEY, 0);
        monsterDeathHealthLost = config.getDouble(MONSTER_DEATH_HEALTH_LOST_KEY, 0);
        playerDeathHealthLost = config.getDouble(PLAYER_DEATH_HEALTH_LOST_KEY, 0);
        playerKillHealthGained = config.getDouble(PLAYER_KILL_HEALTH_GAINED_KEY, 0);
    }

    private static void loadHeartItemSettings(YamlConfiguration config) {
        healthPerItem = config.getDouble(HEALTH_PER_ITEM_KEY, 0);
        heartItemName = config.getString(HEART_ITEM_NAME_KEY, "Heart");
        heartItemID = config.getString(HEART_ITEM_ID_KEY, "NETHER_STAR");
        allowCrafting = config.getBoolean(ALLOW_CRAFTING_KEY, false);
    }

    private static void loadRecipe(YamlConfiguration config) {
        recipeShape = config.getStringList(RECIPE_SHAPE_KEY).toArray(new String[0]);
        recipeIngredients = new HashMap<>();
        if (config.isConfigurationSection(RECIPE_INGREDIENTS_KEY)) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection(RECIPE_INGREDIENTS_KEY)).getKeys(false)) {
                String materialName = config.getString(RECIPE_INGREDIENTS_KEY + "." + key);
                if (materialName == null) {
                    LOGGER.warning(() -> "Missing material for key: " + key + " in recipe ingredients.");
                    continue;
                }
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    LOGGER.warning(() -> "Invalid material \"" + materialName + "\" for key: " + key + " in recipe ingredients.");
                    continue;
                }
                recipeIngredients.put(key.charAt(0), material);
            }
        }
    }

    private static void validateHealthSettings() {
        if (minHealthLimit > maxHealthLimit && maxHealthLimit > 0) {
            LOGGER.warning("Minimum health limit is greater than maximum health limit! Adjusting minimum health limit to " + maxHealthLimit);
            minHealthLimit = maxHealthLimit;
        }
        if (minHealthLimit < 1) {
            LOGGER.warning("Minimum health limit cannot be less than 1. Setting to 1.");
            minHealthLimit = 1;
        }
    }

    private static void updateHeartRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");
        // Always remove the old recipe before trying to add a new one.
        Bukkit.removeRecipe(recipeKey);

        if (isAllowCrafting()) {
            boolean isRecipeValid = recipeShape != null && recipeShape.length > 0 && !recipeIngredients.isEmpty();
            if (isRecipeValid) {
                HeartRecipe.registerHeartRecipe();
                LOGGER.info("Registered custom heart recipe.");
            } else {
                LOGGER.warning("Could not register heart recipe: invalid recipe definition in config.yml.");
            }
        } else {
            LOGGER.info("Crafting is disabled. Heart recipe not registered.");
        }
    }

    private static void loadDefaultValues() {
        maxHealthLimit = 0;
        minHealthLimit = 1;
        naturalDeathHealthLost = 0;
        monsterDeathHealthLost = 0;
        playerDeathHealthLost = 0;
        playerKillHealthGained = 0;
        healthPerItem = 0;
        allowWithdraw = false;
        allowCrafting = false;
        ignoreKeepInventory = false;
        heartItemName = "Heart";
        heartItemID = "NETHER_STAR";
        recipeShape = new String[0];
        recipeIngredients = new HashMap<>();
    }

    /**
     * Logs the currently loaded configuration settings to the server console.
     * This is useful for administrators to quickly verify the plugin's configuration.
     */
    private static void logConfiguration() {
        LOGGER.info("----------- LifeSteal Configuration -----------");
        LOGGER.info("Health Limits: Max = " + (maxHealthLimit > 0 ? maxHealthLimit : "Disabled") + ", Min = " + minHealthLimit);
        LOGGER.info("Death Settings: Natural Loss = " + naturalDeathHealthLost + ", Monster Loss = " + monsterDeathHealthLost + ", Player Loss = " + playerDeathHealthLost + ", Player Gain = " + playerKillHealthGained);
        LOGGER.info("Heart Item: Health = " + healthPerItem + ", Name = '" + heartItemName + "', Material = " + heartItemID + ", Crafting = " + allowCrafting);
        if (allowCrafting) {
            LOGGER.info("  Recipe Ingredients: " + recipeIngredients.size() + " ingredients defined.");
        }
        LOGGER.info("Features: Allow Withdraw = " + allowWithdraw + ", Ignore KeepInventory = " + ignoreKeepInventory);
        LOGGER.info("--------------------------------------------");
    }

    /**
     * Returns the maximum health limit for players.
     *
     * @return The maximum health limit.
     */
    public static double getMaxHealthLimit() {
        return maxHealthLimit;
    }

    /**
     * Returns the minimum health limit for players.
     *
     * @return The minimum health limit.
     */
    public static double getMinHealthLimit() {
        return minHealthLimit;
    }

    /**
     * Returns the amount of health lost upon a natural death.
     *
     * @return The health lost on natural death.
     */
    public static double getNaturalDeathHealthLost() {
        return naturalDeathHealthLost;
    }

    /**
     * Returns the amount of health lost when killed by a monster.
     *
     * @return The health lost on monster death.
     */
    public static double getMonsterDeathHealthLost() {
        return monsterDeathHealthLost;
    }

    /**
     * Returns the amount of health lost when killed by another player.
     *
     * @return The health lost on player death.
     */
    public static double getPlayerDeathHealthLost() {
        return playerDeathHealthLost;
    }

    /**
     * Returns the amount of health gained when a player kills another player.
     *
     * @return The health gained on player kill.
     */
    public static double getPlayerKillHealthGained() {
        return playerKillHealthGained;
    }

    /**
     * Returns the amount of health restored when a player consumes a heart item.
     *
     * @return The health restored per heart item.
     */
    public static double getHealthPerItem() {
        return healthPerItem;
    }

    /**
     * Indicates whether players are allowed to withdraw health to create heart items.
     *
     * @return `true` if withdrawing is allowed, `false` otherwise.
     */
    public static boolean isAllowWithdraw() {
        return allowWithdraw;
    }

    /**
     * Indicates whether crafting of the heart item is enabled.
     *
     * @return `true` if crafting is allowed, `false` otherwise.
     */
    public static boolean isAllowCrafting() {
        return allowCrafting;
    }

    /**
     * Indicates whether the plugin should ignore the server's `keepInventory` game rule.
     *
     * @return `true` if `keepInventory` is ignored, `false` otherwise.
     */
    public static boolean isIgnoreKeepInventory() {
        return ignoreKeepInventory;
    }

    /**
     * Returns the custom display name for the heart item.
     *
     * @return The heart item's name.
     */
    @NotNull
    public static String getHeartItemName() {
        return heartItemName;
    }

    /**
     * Returns the Material ID of the heart item.
     *
     * @return The heart item's Material ID.
     */
    @NotNull
    public static String getHeartItemID() {
        return heartItemID;
    }

    /**
     * Returns the shape of the crafting recipe for the heart item. Each string in the array represents a row.
     *
     * @return An array of strings representing the recipe shape.
     */
    @NotNull
    public static String[] getRecipeShape() {
        return recipeShape;
    }

    /**
     * Returns the ingredients required for the crafting recipe of the heart item.
     * The keys in the map are the characters used in the {@link #getRecipeShape()}
     * and the values are the corresponding {@link Material}s.
     *
     * @return A map of recipe ingredients.
     */
    @NotNull
    public static Map<Character, Material> getRecipeIngredients() {
        return recipeIngredients;
    }

    /**
     * Checks if the maximum health limit is enabled (i.e., if {@link #maxHealthLimit} is greater than 0).
     *
     * @return `true` if the maximum health limit is enabled, `false` otherwise.
     */
    public static boolean isMaxHealthLimitEnabled() {
        return maxHealthLimit > 0;
    }

    /**
     * Checks if the minimum health limit is enabled (i.e., if {@link #minHealthLimit} is greater than 0).
     *
     * @return `true` if the minimum health limit is enabled, `false` otherwise.
     */
    public static boolean isMinHealthLimitEnabled() {
        return minHealthLimit > 0;
    }
}