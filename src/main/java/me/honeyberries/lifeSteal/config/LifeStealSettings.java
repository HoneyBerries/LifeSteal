package me.honeyberries.lifeSteal.config;

import me.honeyberries.lifeSteal.recipe.HeartRecipe;
import me.honeyberries.lifeSteal.LifeSteal;
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
 * This class is responsible for loading configuration options from the `config.yml`
 * file and providing access to various gameplay settings, such as health loss,
 * crafting rules, and custom recipe definitions for the heart item.
 */
public class LifeStealSettings {

    // Reference to the main plugin instance
    private static final LifeSteal plugin = LifeSteal.getInstance();
    // Logger for logging messages related to configuration
    private static final Logger LOGGER = plugin.getLogger();

    // Configuration values
    private static int maxHealthLimit; // Maximum health limit for players
    private static int minHealthLimit; // Minimum health limit for players
    private static int naturalDeathHealthLost; // Health lost on natural death
    private static int monsterDeathHealthLost; // Health lost on monster-related death
    private static int playerDeathHealthLost; // Health lost on player-related death
    private static int playerKillHealthGained; // Health gained when a player kills another player
    private static int healthPerItem; // Health restored per heart item
    private static boolean allowWithdraw; // Whether withdrawing hearts is allowed
    private static boolean allowCrafting; // Whether crafting heart items is allowed
    private static boolean ignoreKeepInventory; // Whether to ignore the keepInventory game rule
    private static String heartItemName; // Custom name for the heart item
    private static String heartItemID; // Material ID for the heart item
    private static String[] recipeShape; // Shape of the crafting recipe for the heart item
    private static Map<Character, Material> recipeIngredients; // Ingredients for the crafting recipe

    /**
     * Loads the configuration from the `config.yml` file.
     * If the file does not exist, it is created using the plugin's default resource.
     * Any errors encountered during the loading process are logged.
     */
    public static void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // Create the config file if it does not exist, using the default resource
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        try {
            // Load the configuration file
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // Load integer values
            maxHealthLimit = config.getInt("max-health-limit");
            minHealthLimit = config.getInt("min-health-limit");
            naturalDeathHealthLost = config.getInt("death-settings.natural-death.health-lost");
            monsterDeathHealthLost = config.getInt("death-settings.monster-death.health-lost");
            playerDeathHealthLost = config.getInt("death-settings.player-death.health-lost");
            playerKillHealthGained = config.getInt("death-settings.player-death.health-gained");
            healthPerItem = config.getInt("heart-item.health-per-item");

            // Load boolean values
            allowWithdraw = config.getBoolean("features.allow-withdraw.enabled");
            allowCrafting = config.getBoolean("heart-item.allow-crafting");
            ignoreKeepInventory = config.getBoolean("features.ignore-keep-inventory.enabled");

            // Load string values
            heartItemName = config.getString("heart-item.heart-item-name");
            heartItemID = config.getString("heart-item.heart-item-id");
            recipeShape = config.getStringList("heart-item.recipe.shape").toArray(new String[0]);

            // Initialize the recipeIngredients map to store the recipe's character-to-material mappings
            recipeIngredients = new HashMap<>();

            // Check if the "heart-item.recipe.ingredients" section exists in the configuration file
            if (config.isConfigurationSection("heart-item.recipe.ingredients")) {
                // Iterate through each key in the "ingredients" section
                for (String key : Objects.requireNonNull(config.getConfigurationSection("heart-item.recipe.ingredients")).getKeys(false)) {

                    // Retrieve the material name associated with the current key
                    String materialName = config.getString("heart-item.recipe.ingredients." + key);

                    // Log a warning and skip this key if the material name is missing
                    if (materialName == null) {
                        LOGGER.warning(() -> "Missing material for key: " + key + " in recipe ingredients.");
                        continue;
                    }

                    // Attempt to match the material name to a valid Material enum
                    Material material = Material.matchMaterial(materialName);

                    // Log a warning and skip this key if the material name is invalid
                    if (material == null) {
                        LOGGER.warning(() -> "Invalid material \"" + materialName + "\" for key: " + key + " in recipe ingredients.");
                        continue;
                    }

                    // Add the valid character-to-material mapping to the recipeIngredients map
                    recipeIngredients.put(key.charAt(0), material);
                }
            }

            // When loading the config
            allowCrafting = config.getBoolean("heart-item.allow-crafting");

            // Later in the same method
            NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");

            // Always remove any existing recipe first
            Bukkit.removeRecipe(recipeKey);
            LOGGER.log(Level.INFO, "Removed any existing heart recipe");

            // Only register if crafting is enabled
            if (isAllowCrafting()) {
                if (recipeShape != null && recipeShape.length > 0 && !recipeIngredients.isEmpty()) {
                    HeartRecipe.registerHeartRecipe();
                    LOGGER.log(Level.INFO, "Registered heart recipe.");
                } else {
                    LOGGER.log(Level.WARNING, "Could not register heart recipe: invalid recipe definition.");
                }
            } else {
                LOGGER.log(Level.INFO, "Crafting is disabled. Heart recipe not registered.");
            }

            LOGGER.log(Level.INFO, "Configuration loaded successfully.");

        } catch (Exception e) {
            // Log an error if the configuration fails to load
            LOGGER.log(Level.SEVERE, "Failed to load config.yml. Plugin will use default values.", e);

            // Set default values to prevent plugin failure
            allowWithdraw = false;
            allowCrafting = false;
            ignoreKeepInventory = false;
            maxHealthLimit = 0;
            minHealthLimit = 0;
            naturalDeathHealthLost = 0;
            monsterDeathHealthLost = 0;
            playerDeathHealthLost = 0;
            playerKillHealthGained = 0;
            healthPerItem = 0;
            heartItemName = "Heart"; // Default to "Heart"
            heartItemID = "NETHER_STAR"; // Default to Nether Star
            recipeShape = new String[]{"XXX", "XXX", "XXX"}; // Set a default shape
            recipeIngredients = new HashMap<>(); // Prevent NPE in recipe registration

            LOGGER.warning("Plugin is running with default configuration values due to config load failure!");
        }

        // Log the loaded configuration values for debugging and verification
        LOGGER.log(Level.INFO, "maxHealthLimit: " + maxHealthLimit);
        LOGGER.log(Level.INFO, "minHealthLimit: " + minHealthLimit);
        LOGGER.log(Level.INFO, "naturalDeathHealthLost: " + naturalDeathHealthLost);
        LOGGER.log(Level.INFO, "monsterDeathHealthLost: " + monsterDeathHealthLost);
        LOGGER.log(Level.INFO, "playerDeathHealthLost: " + playerDeathHealthLost);
        LOGGER.log(Level.INFO, "playerKillHealthGained: " + playerKillHealthGained);
        LOGGER.log(Level.INFO, "healthPerItem: " + healthPerItem);
        LOGGER.log(Level.INFO, "allowWithdraw: " + allowWithdraw);
        LOGGER.log(Level.INFO, "allowCrafting: " + allowCrafting);
        LOGGER.log(Level.INFO, "ignoreKeepInventory: " + ignoreKeepInventory);
        LOGGER.log(Level.INFO, "heartItemName: " + heartItemName);
        LOGGER.log(Level.INFO, "heartItemID: " + heartItemID);
    }

    // Getters for configuration values

    /**
     * @return The maximum health limit for players.
     */
    public static int getMaxHealthLimit() {
        return maxHealthLimit;
    }

    /**
     * @return The minimum health limit for players.
     */
    public static int getMinHealthLimit() {
        return minHealthLimit;
    }

    /**
     * @return The health lost on natural death.
     */
    public static int getNaturalDeathHealthLost() {
        return naturalDeathHealthLost;
    }

    /**
     * @return The health lost on monster-related death.
     */
    public static int getMonsterDeathHealthLost() {
        return monsterDeathHealthLost;
    }

    /**
     * @return The health lost on player-related death.
     */
    public static int getPlayerDeathHealthLost() {
        return playerDeathHealthLost;
    }

    /**
     * @return The health gained when a player kills another player.
     */
    public static int getPlayerKillHealthGained() {
        return playerKillHealthGained;
    }

    /**
     * @return The health restored per heart item.
     */
    public static int getHealthPerItem() {
        return healthPerItem;
    }

    /**
     * @return Whether withdrawing hearts is allowed.
     */
    public static boolean isAllowWithdraw() {
        return allowWithdraw;
    }

    /**
     * @return Whether crafting heart items is allowed.
     */
    public static boolean isAllowCrafting() {
        return allowCrafting;
    }

    /**
     * @return Whether the keepInventory game rule is ignored.
     */
    public static boolean isIgnoreKeepInventory() {
        return ignoreKeepInventory;
    }

    /**
     * @return The custom name for the heart item.
     */
    public static @NotNull String getHeartItemName() {
        return heartItemName;
    }

    /**
     * @return The material ID for the heart item.
     */
    public static @NotNull String getHeartItemID() {
        return heartItemID;
    }

    /**
     * @return The shape of the crafting recipe for the heart item.
     */
    public static @NotNull String[] getRecipeShape() {
        return recipeShape;
    }

    /**
     * @return The ingredients for the crafting recipe.
     */
    public static @NotNull Map<Character, Material> getRecipeIngredients() {
        return recipeIngredients;
    }

    /**
     * @return Whether the maximum health limit is enabled.
     */
    public static boolean isMaxHealthLimitEnabled() {
        return maxHealthLimit > 0;
    }

    /**
     * @return Whether the minimum health limit is enabled.
     */
    public static boolean isMinHealthLimitEnabled() {
        return minHealthLimit > 0;
    }
}