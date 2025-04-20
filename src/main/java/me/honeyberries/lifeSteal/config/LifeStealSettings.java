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

    // --- Configuration Properties ---

    /** The maximum health a player can have. A value of 0 or less disables this limit. */
    private static int maxHealthLimit;

    /** The minimum health a player can have. A value of 0 or less disables this limit. */
    private static int minHealthLimit;

    /** The amount of health lost upon a natural death (e.g., starvation, fall damage). */
    private static int naturalDeathHealthLost;

    /** The amount of health lost when killed by a monster. */
    private static int monsterDeathHealthLost;

    /** The amount of health lost when killed by another player. */
    private static int playerDeathHealthLost;

    /** The amount of health gained when a player kills another player. */
    private static int playerKillHealthGained;

    /** The amount of health restored when a player consumes a heart item. */
    private static int healthPerItem;

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
        // Represents the configuration file in the plugin's data folder.
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // Check if the configuration file exists. If not, save the default one from the plugin's resources.
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        try {
            // Load the YAML configuration from the specified file.
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // --- Load Integer Values ---
            maxHealthLimit = config.getInt("max-health-limit");
            minHealthLimit = config.getInt("min-health-limit");
            naturalDeathHealthLost = config.getInt("death-settings.natural-death.health-lost");
            monsterDeathHealthLost = config.getInt("death-settings.monster-death.health-lost");
            playerDeathHealthLost = config.getInt("death-settings.player-death.health-lost");
            playerKillHealthGained = config.getInt("death-settings.player-death.health-gained");
            healthPerItem = config.getInt("heart-item.health-per-item");

            // --- Load Boolean Values ---
            allowWithdraw = config.getBoolean("features.allow-withdraw.enabled");
            allowCrafting = config.getBoolean("heart-item.allow-crafting");
            ignoreKeepInventory = config.getBoolean("features.ignore-keep-inventory.enabled");

            // --- Load String Values ---
            heartItemName = config.getString("heart-item.heart-item-name");
            heartItemID = config.getString("heart-item.heart-item-id");
            recipeShape = config.getStringList("heart-item.recipe.shape").toArray(new String[0]);

            // --- Load Recipe Ingredients ---
            recipeIngredients = new HashMap<>();
            if (config.isConfigurationSection("heart-item.recipe.ingredients")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("heart-item.recipe.ingredients")).getKeys(false)) {
                    String materialName = config.getString("heart-item.recipe.ingredients." + key);
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

            // --- Register/Unregister Custom Heart Recipe based on Configuration ---
            NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");
            Bukkit.removeRecipe(recipeKey); // Ensure any existing recipe is removed before potentially re-registering.
            LOGGER.log(Level.INFO, "Removed any existing heart recipe.");

            if (isAllowCrafting()) {
                // Only attempt to register the recipe if crafting is enabled and the recipe definition is valid.
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
            // Catch any exceptions that occur during the configuration loading process.
            LOGGER.log(Level.SEVERE, "Failed to load config.yml. Plugin will use default values.", e);

            // --- Apply Default Values in Case of Loading Failure ---
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
            heartItemName = "Heart";
            heartItemID = "NETHER_STAR";
            recipeShape = new String[]{"XXX", "XXX", "XXX"};
            recipeIngredients = new HashMap<>();

            LOGGER.warning("Plugin is running with default configuration values due to config load failure!");
        }

        // Log the loaded configuration to the console.
        logConfiguration();
    }

    /**
     * Logs the currently loaded configuration settings to the server console.
     * This is useful for administrators to quickly verify the plugin's configuration.
     */
    private static void logConfiguration() {
        LOGGER.log(Level.INFO, "----------- LifeSteal Configuration -----------");

        LOGGER.log(Level.INFO, "--- Health Limits ---");
        LOGGER.log(Level.INFO, "  Max Health: " + (maxHealthLimit > 0 ? maxHealthLimit : "Disabled"));
        LOGGER.log(Level.INFO, "  Min Health: " + (minHealthLimit > 0 ? minHealthLimit : "Disabled"));

        LOGGER.log(Level.INFO, "--- Death Settings ---");
        LOGGER.log(Level.INFO, "  Natural Death HP Loss: " + naturalDeathHealthLost);
        LOGGER.log(Level.INFO, "  Monster Death HP Loss: " + monsterDeathHealthLost);
        LOGGER.log(Level.INFO, "  Player Death HP Loss: " + playerDeathHealthLost);
        LOGGER.log(Level.INFO, "  Player Kill HP Gain: " + playerKillHealthGained);

        LOGGER.log(Level.INFO, "--- Heart Item Configuration ---");
        LOGGER.log(Level.INFO, "  Health Per Item: " + healthPerItem);
        LOGGER.log(Level.INFO, "  Item Name: '" + heartItemName + "'");
        LOGGER.log(Level.INFO, "  Material ID: " + heartItemID);
        LOGGER.log(Level.INFO, "  Allow Crafting: " + allowCrafting);
        if (allowCrafting && recipeShape != null) {
            LOGGER.log(Level.INFO, "  Recipe Shape:");
            for (String row : recipeShape) {
                LOGGER.log(Level.INFO, "    " + row);
            }
            if (!recipeIngredients.isEmpty()) {
                LOGGER.log(Level.INFO, "  Recipe Ingredients:");
                for (Map.Entry<Character, Material> entry : recipeIngredients.entrySet()) {
                    LOGGER.log(Level.INFO, "    " + entry.getKey() + ": " + entry.getValue());
                }
            } else {
                LOGGER.log(Level.WARNING, "  Recipe Ingredients are empty.");
            }
        }

        LOGGER.log(Level.INFO, "--- Feature Settings ---");
        LOGGER.log(Level.INFO, "  Allow Withdraw: " + allowWithdraw);
        LOGGER.log(Level.INFO, "  Ignore KeepInventory: " + ignoreKeepInventory);

        LOGGER.log(Level.INFO, "--------------------------------------------");
    }

    /**
     * Returns the maximum health limit for players.
     *
     * @return The maximum health limit.
     */
    public static int getMaxHealthLimit() {
        return maxHealthLimit;
    }

    /**
     * Returns the minimum health limit for players.
     *
     * @return The minimum health limit.
     */
    public static int getMinHealthLimit() {
        return minHealthLimit;
    }

    /**
     * Returns the amount of health lost upon a natural death.
     *
     * @return The health lost on natural death.
     */
    public static int getNaturalDeathHealthLost() {
        return naturalDeathHealthLost;
    }

    /**
     * Returns the amount of health lost when killed by a monster.
     *
     * @return The health lost on monster death.
     */
    public static int getMonsterDeathHealthLost() {
        return monsterDeathHealthLost;
    }

    /**
     * Returns the amount of health lost when killed by another player.
     *
     * @return The health lost on player death.
     */
    public static int getPlayerDeathHealthLost() {
        return playerDeathHealthLost;
    }

    /**
     * Returns the amount of health gained when a player kills another player.
     *
     * @return The health gained on player kill.
     */
    public static int getPlayerKillHealthGained() {
        return playerKillHealthGained;
    }

    /**
     * Returns the amount of health restored when a player consumes a heart item.
     *
     * @return The health restored per heart item.
     */
    public static int getHealthPerItem() {
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