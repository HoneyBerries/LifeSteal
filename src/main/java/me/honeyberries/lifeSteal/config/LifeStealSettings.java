package me.honeyberries.lifeSteal.config;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.recipe.HeartRecipe;
import me.honeyberries.lifeSteal.recipe.RevivalItemRecipe;
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
 * Manages all configuration settings for the LifeSteal plugin.
 * <p>
 * This class handles loading, validating, and providing access to gameplay settings
 * from the config.yml file, including health limits, death mechanics, items, and recipes.
 * <p>
 * Configuration is loaded via {@link #loadConfig()} and accessed through static getter methods.
 * All settings use safe defaults if the config file is missing or invalid.
 */
public class LifeStealSettings {

    // ═══════════════════════════════════════════════════════════════════════════
    //                                CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final LifeSteal PLUGIN = LifeSteal.getInstance();
    private static final Logger LOGGER = PLUGIN.getLogger();

    // ─────────────────────────────────────────────────────────────────────────
    // Config Path Constants
    // ─────────────────────────────────────────────────────────────────────────

    // Player Health
    private static final String PLAYER_HEALTH_MAX = "player-health.max";
    private static final String PLAYER_HEALTH_MIN = "player-health.min";

    // Combat Deaths
    private static final String PVP_VICTIM_LOSES = "on-pvp-death.victim-loses";
    private static final String PVP_KILLER_GAINS = "on-pvp-death.killer-gains";

    // Natural Deaths
    private static final String NATURAL_HP_LOST = "on-natural-death.hp-lost";

    // Elimination
    private static final String ELIMINATION_ENABLED = "elimination.enabled";
    private static final String ELIMINATION_MODE = "elimination.when-eliminated";

    // Heart Items
    private static final String HEARTS_DISPLAY_NAME = "hearts.display-name";
    private static final String HEARTS_ITEM_TYPE = "hearts.item-type";
    private static final String HEARTS_HP_RESTORED = "hearts.hp-restored";
    private static final String HEARTS_RECIPE_ENABLED = "hearts.recipe.enabled";
    private static final String HEARTS_RECIPE_PATTERN = "hearts.recipe.pattern";
    private static final String HEARTS_RECIPE_MATERIALS = "hearts.recipe.materials";

    // Revival
    private static final String REVIVAL_ENABLED = "revival.enabled";
    private static final String REVIVAL_DISPLAY_NAME = "revival.item.display-name";
    private static final String REVIVAL_ITEM_TYPE = "revival.item.item-type";
    private static final String REVIVAL_STARTING_HP = "revival.item.starting-hp";
    private static final String REVIVAL_RECIPE_ENABLED = "revival.item.recipe.enabled";
    private static final String REVIVAL_RECIPE_PATTERN = "revival.item.recipe.pattern";
    private static final String REVIVAL_RECIPE_MATERIALS = "revival.item.recipe.materials";

    // Advanced Options
    private static final String WITHDRAW_ENABLED = "commands.withdraw-enabled";
    private static final String OVERRIDE_KEEP_INVENTORY = "game-rules.override-keep-inventory";

    // ═══════════════════════════════════════════════════════════════════════════
    //                                  ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Defines what happens to players when they are eliminated (reach minimum health).
     */
    public enum EliminationMode {
        /** Ban the player from the server */
        BAN,
        /** Change the player to spectator mode */
        SPECTATOR
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                            CONFIGURATION FIELDS
    // ═══════════════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────────────────
    // Player Health
    // ─────────────────────────────────────────────────────────────────────────
    private static double maxHealth;
    private static double minHealth;

    // ─────────────────────────────────────────────────────────────────────────
    // Death Mechanics
    // ─────────────────────────────────────────────────────────────────────────
    private static double pvpVictimLoses;
    private static double pvpKillerGains;
    private static double naturalHpLost;

    // ─────────────────────────────────────────────────────────────────────────
    // Elimination
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean eliminationEnabled;
    private static EliminationMode eliminationMode;

    // ─────────────────────────────────────────────────────────────────────────
    // Heart Items
    // ─────────────────────────────────────────────────────────────────────────
    private static String heartDisplayName;
    private static String heartItemType;
    private static double heartHpRestored;
    private static boolean heartRecipeEnabled;
    private static String[] heartRecipePattern;
    private static Map<Character, Material> heartRecipeMaterials;

    // ─────────────────────────────────────────────────────────────────────────
    // Revival
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean revivalEnabled;
    private static String revivalDisplayName;
    private static String revivalItemType;
    private static double revivalStartingHp;
    private static boolean revivalRecipeEnabled;
    private static String[] revivalRecipePattern;
    private static Map<Character, Material> revivalRecipeMaterials;

    // ─────────────────────────────────────────────────────────────────────────
    // Advanced Options
    // ─────────────────────────────────────────────────────────────────────────
    private static boolean withdrawEnabled;
    private static boolean overrideKeepInventory;

    // ═══════════════════════════════════════════════════════════════════════════
    //                          PUBLIC API - LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loads all configuration settings from config.yml.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates config.yml from defaults if it doesn't exist</li>
     *   <li>Loads all configuration values</li>
     *   <li>Validates settings for logical consistency</li>
     *   <li>Registers crafting recipes if enabled</li>
     *   <li>Logs the configuration to console</li>
     * </ol>
     * <p>
     * If loading fails, safe default values are used and the plugin continues to function
     * in a minimal state (no health changes, no elimination).
     */
    public static void loadConfig() {
        try {
            PLUGIN.saveDefaultConfig();
            File configFile = new File(PLUGIN.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            loadPlayerHealth(config);
            loadDeathMechanics(config);
            loadElimination(config);
            loadHeartItems(config);
            loadRevival(config);
            loadAdvancedOptions(config);

            validateSettings();
            registerRecipes();

            LOGGER.info("Configuration loaded successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load config.yml! Using safe defaults.", e);
            loadDefaults();
            LOGGER.warning("Plugin running with default values (no gameplay changes).");
        }

        logConfiguration();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                        PRIVATE - CONFIGURATION LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loads player health limit settings.
     *
     * @param config the YAML configuration
     */
    private static void loadPlayerHealth(YamlConfiguration config) {
        maxHealth = config.getDouble(PLAYER_HEALTH_MAX, -1);
        minHealth = config.getDouble(PLAYER_HEALTH_MIN, -1);
    }

    /**
     * Loads death mechanic settings for PvP and natural deaths.
     *
     * @param config the YAML configuration
     */
    private static void loadDeathMechanics(YamlConfiguration config) {
        pvpVictimLoses = config.getDouble(PVP_VICTIM_LOSES, 0);
        pvpKillerGains = config.getDouble(PVP_KILLER_GAINS, 0);
        naturalHpLost = config.getDouble(NATURAL_HP_LOST, 0);
    }

    /**
     * Loads elimination settings.
     *
     * @param config the YAML configuration
     */
    private static void loadElimination(YamlConfiguration config) {
        eliminationEnabled = config.getBoolean(ELIMINATION_ENABLED, false);

        String modeStr = config.getString(ELIMINATION_MODE, "SPECTATOR");
        try {
            eliminationMode = EliminationMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid elimination mode '" + modeStr + "'. Using SPECTATOR.");
            eliminationMode = EliminationMode.SPECTATOR;
        }
    }

    /**
     * Loads heart item settings including recipe.
     *
     * @param config the YAML configuration
     */
    private static void loadHeartItems(YamlConfiguration config) {
        heartDisplayName = config.getString(HEARTS_DISPLAY_NAME, "CONFIG ERROR");
        heartItemType = config.getString(HEARTS_ITEM_TYPE, "AIR");
        heartHpRestored = config.getDouble(HEARTS_HP_RESTORED, 0);
        heartRecipeEnabled = config.getBoolean(HEARTS_RECIPE_ENABLED, false);
        heartRecipePattern = config.getStringList(HEARTS_RECIPE_PATTERN).toArray(new String[0]);
        heartRecipeMaterials = loadRecipeMaterials(config, HEARTS_RECIPE_MATERIALS, "heart");
    }

    /**
     * Loads revival settings including item and recipe configuration.
     *
     * @param config the YAML configuration
     */
    private static void loadRevival(YamlConfiguration config) {
        revivalEnabled = config.getBoolean(REVIVAL_ENABLED, false);
        revivalDisplayName = config.getString(REVIVAL_DISPLAY_NAME, "CONFIG ERROR");
        revivalItemType = config.getString(REVIVAL_ITEM_TYPE, "AIR");
        revivalStartingHp = config.getDouble(REVIVAL_STARTING_HP, 0);
        revivalRecipeEnabled = config.getBoolean(REVIVAL_RECIPE_ENABLED, false);
        revivalRecipePattern = config.getStringList(REVIVAL_RECIPE_PATTERN).toArray(new String[0]);
        revivalRecipeMaterials = loadRecipeMaterials(config, REVIVAL_RECIPE_MATERIALS, "revival");
    }

    /**
     * Loads advanced option settings.
     *
     * @param config the YAML configuration
     */
    private static void loadAdvancedOptions(YamlConfiguration config) {
        withdrawEnabled = config.getBoolean(WITHDRAW_ENABLED, false);
        overrideKeepInventory = config.getBoolean(OVERRIDE_KEEP_INVENTORY, false);
    }

    /**
     * Loads recipe materials from the config and maps characters to Material types.
     * <p>
     * Invalid materials are logged as warnings and skipped.
     *
     * @param config the YAML configuration
     * @param path the config path to the materials section
     * @param recipeType a descriptive name for logging (e.g., "heart", "revival")
     * @return a map of characters to Materials
     */
    private static Map<Character, Material> loadRecipeMaterials(
            YamlConfiguration config,
            String path,
            String recipeType
    ) {
        Map<Character, Material> materials = new HashMap<>();

        if (!config.isConfigurationSection(path)) {
            return materials;
        }

        for (String key : Objects.requireNonNull(config.getConfigurationSection(path)).getKeys(false)) {
            String materialName = config.getString(path + "." + key);

            if (materialName == null) {
                LOGGER.warning("Missing material for '" + key + "' in " + recipeType + " recipe.");
                continue;
            }

            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                LOGGER.warning("Invalid material '" + materialName + "' for '" + key + "' in " + recipeType + " recipe.");
                continue;
            }

            materials.put(key.charAt(0), material);
        }

        return materials;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                      PRIVATE - VALIDATION & REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates loaded settings for logical consistency.
     * <p>
     * Checks include:
     * <ul>
     *   <li>Minimum health not exceeding maximum health (when max is set)</li>
     *   <li>Minimum health is at least 1 if enabled</li>
     * </ul>
     * Invalid settings are automatically corrected with warnings.
     */
    private static void validateSettings() {
        // Validate health limits
        if (isMaxHealthEnabled() && isMinHealthEnabled() && minHealth > maxHealth) {
            LOGGER.warning("Minimum health (" + minHealth + ") exceeds maximum (" + maxHealth + "). Setting min = max.");
            minHealth = maxHealth;
        }

        if (isMinHealthEnabled() && minHealth < 1) {
            LOGGER.warning("Minimum health cannot be less than 1. Setting to 1.");
            minHealth = 1;
        }
    }

    /**
     * Registers all enabled crafting recipes with the server.
     * <p>
     * Removes any existing recipes before registering new ones.
     * Only registers recipes that are enabled and have valid definitions.
     */
    private static void registerRecipes() {
        registerHeartRecipe();
        registerRevivalRecipe();
    }

    /**
     * Registers the heart item crafting recipe if enabled and valid.
     */
    private static void registerHeartRecipe() {
        NamespacedKey key = new NamespacedKey(PLUGIN, "custom_heart_recipe");
        Bukkit.removeRecipe(key);

        if (!heartRecipeEnabled) {
            LOGGER.info("Heart recipe disabled.");
            return;
        }

        if (isRecipeValid(heartRecipePattern, heartRecipeMaterials)) {
            HeartRecipe.registerHeartRecipe();
            LOGGER.info("Registered heart recipe.");
        } else {
            LOGGER.warning("Heart recipe is invalid or incomplete. Not registered.");
        }
    }

    /**
     * Registers the revival item crafting recipe if enabled and valid.
     */
    private static void registerRevivalRecipe() {
        NamespacedKey key = new NamespacedKey(PLUGIN, "custom_revival_item_recipe");
        Bukkit.removeRecipe(key);

        if (!revivalRecipeEnabled) {
            LOGGER.info("Revival recipe disabled.");
            return;
        }

        if (isRecipeValid(revivalRecipePattern, revivalRecipeMaterials)) {
            RevivalItemRecipe.registerRevivalRecipe();
            LOGGER.info("Registered revival recipe.");
        } else {
            LOGGER.warning("Revival recipe is invalid or incomplete. Not registered.");
        }
    }

    /**
     * Checks if a recipe definition is valid.
     *
     * @param pattern the recipe pattern
     * @param materials the recipe materials
     * @return true if valid, false otherwise
     */
    private static boolean isRecipeValid(String[] pattern, Map<Character, Material> materials) {
        return pattern != null && pattern.length > 0 && !materials.isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                          PRIVATE - DEFAULTS & LOGGING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loads safe default values for all settings.
     * <p>
     * Defaults are designed to make the plugin functionally inactive:
     * no health changes, no elimination, no special items.
     */
    private static void loadDefaults() {
        // Player Health
        maxHealth = -1;
        minHealth = -1;

        // Death Mechanics
        pvpVictimLoses = 0;
        pvpKillerGains = 0;
        naturalHpLost = 0;

        // Elimination
        eliminationEnabled = false;
        eliminationMode = EliminationMode.SPECTATOR;

        // Heart Items
        heartDisplayName = "CONFIG ERROR";
        heartItemType = "AIR";
        heartHpRestored = 0;
        heartRecipeEnabled = false;
        heartRecipePattern = new String[0];
        heartRecipeMaterials = new HashMap<>();

        // Revival
        revivalEnabled = false;
        revivalDisplayName = "CONFIG ERROR";
        revivalItemType = "AIR";
        revivalStartingHp = 0;
        revivalRecipeEnabled = false;
        revivalRecipePattern = new String[0];
        revivalRecipeMaterials = new HashMap<>();

        // Advanced Options
        withdrawEnabled = false;
        overrideKeepInventory = false;
    }

    /**
     * Logs the current configuration to the console for verification.
     */
    private static void logConfiguration() {
        LOGGER.info("============ LifeSteal Configuration ============");

        LOGGER.info("Player Health:");
        LOGGER.info("  Max: " + (isMaxHealthEnabled() ? maxHealth + " HP" : "Disabled"));
        LOGGER.info("  Min: " + (isMinHealthEnabled() ? minHealth + " HP" : "Disabled"));

        LOGGER.info("Death Mechanics:");
        LOGGER.info("  PvP - Victim loses: " + pvpVictimLoses + " HP, Killer gains: " + pvpKillerGains + " HP");
        LOGGER.info("  Natural - Loses: " + naturalHpLost + " HP");

        LOGGER.info("Elimination:");
        LOGGER.info("  Enabled: " + eliminationEnabled + ", Mode: " + eliminationMode);

        LOGGER.info("Heart Items:");
        LOGGER.info("  Name: '" + heartDisplayName + "', Type: " + heartItemType);
        LOGGER.info("  HP Restored: " + heartHpRestored + ", Recipe: " + (heartRecipeEnabled ? "Enabled" : "Disabled"));

        LOGGER.info("Revival:");
        LOGGER.info("  Enabled: " + revivalEnabled);
        LOGGER.info("  Name: '" + revivalDisplayName + "', Type: " + revivalItemType);
        LOGGER.info("  Starting HP: " + revivalStartingHp + ", Recipe: " + (revivalRecipeEnabled ? "Enabled" : "Disabled"));

        LOGGER.info("Commands & Rules:");
        LOGGER.info("  Withdraw: " + withdrawEnabled + ", Override KeepInventory: " + overrideKeepInventory);

        LOGGER.info("=================================================");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                        PUBLIC API - PLAYER HEALTH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the maximum health limit for players.
     * <p>
     * A value of -1 or less means no maximum limit is enforced.
     *
     * @return the maximum health in HP
     */
    public static double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Gets the minimum health limit for players.
     * <p>
     * A value of -1 or less means no minimum limit is enforced.
     *
     * @return the minimum health in HP
     */
    public static double getMinHealth() {
        return minHealth;
    }

    /**
     * Checks if the maximum health limit is enabled.
     *
     * @return true if max health is greater than 0
     */
    public static boolean isMaxHealthEnabled() {
        return maxHealth > 0;
    }

    /**
     * Checks if the minimum health limit is enabled.
     *
     * @return true if min health is greater than 0
     */
    public static boolean isMinHealthEnabled() {
        return minHealth > 0;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                       PUBLIC API - DEATH MECHANICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the HP lost by a victim in PvP combat.
     *
     * @return the HP lost by the victim
     */
    public static double getPvpVictimLoses() {
        return pvpVictimLoses;
    }

    /**
     * Gets the HP gained by a killer in PvP combat.
     *
     * @return the HP gained by the killer
     */
    public static double getPvpKillerGains() {
        return pvpKillerGains;
    }

    /**
     * Gets the HP lost on natural deaths (fall damage, drowning, etc.).
     *
     * @return the HP lost on natural death
     */
    public static double getNaturalHpLost() {
        return naturalHpLost;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                         PUBLIC API - ELIMINATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Checks if the elimination feature is enabled.
     *
     * @return true if elimination is enabled
     */
    public static boolean isEliminationEnabled() {
        return eliminationEnabled;
    }

    /**
     * Gets the elimination mode (BAN or SPECTATOR).
     *
     * @return the elimination mode
     */
    @NotNull
    public static EliminationMode getEliminationMode() {
        return eliminationMode;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                         PUBLIC API - HEART ITEMS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the display name for heart items.
     *
     * @return the heart item display name
     */
    @NotNull
    public static String getHeartDisplayName() {
        return heartDisplayName;
    }

    /**
     * Gets the item type for heart items.
     *
     * @return the heart item type (Material name)
     */
    @NotNull
    public static String getHeartItemType() {
        return heartItemType;
    }

    /**
     * Gets the HP restored when using a heart item.
     *
     * @return the HP restored
     */
    public static double getHeartHpRestored() {
        return heartHpRestored;
    }

    /**
     * Checks if heart item crafting is enabled.
     *
     * @return true if crafting is enabled
     */
    public static boolean isHeartRecipeEnabled() {
        return heartRecipeEnabled;
    }

    /**
     * Gets the crafting recipe pattern for heart items.
     * <p>
     * Each string represents a row in the 3x3 crafting grid.
     *
     * @return the recipe pattern
     */
    @NotNull
    public static String[] getHeartRecipePattern() {
        return heartRecipePattern;
    }

    /**
     * Gets the materials used in the heart item recipe.
     * <p>
     * Maps pattern characters to Material types.
     *
     * @return the recipe materials map
     */
    @NotNull
    public static Map<Character, Material> getHeartRecipeMaterials() {
        return heartRecipeMaterials;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                           PUBLIC API - REVIVAL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Checks if the revival feature is enabled.
     *
     * @return true if revival is enabled
     */
    public static boolean isRevivalEnabled() {
        return revivalEnabled;
    }

    /**
     * Gets the display name for revival items.
     *
     * @return the revival item display name
     */
    @NotNull
    public static String getRevivalDisplayName() {
        return revivalDisplayName;
    }

    /**
     * Gets the item type for revival items.
     *
     * @return the revival item type (Material name)
     */
    @NotNull
    public static String getRevivalItemType() {
        return revivalItemType;
    }

    /**
     * Gets the starting HP for revived players.
     *
     * @return the starting HP after revival
     */
    public static double getRevivalStartingHp() {
        return revivalStartingHp;
    }

    /**
     * Checks if revival item crafting is enabled.
     *
     * @return true if crafting is enabled
     */
    public static boolean isRevivalRecipeEnabled() {
        return revivalRecipeEnabled;
    }

    /**
     * Gets the crafting recipe pattern for revival items.
     * <p>
     * Each string represents a row in the 3x3 crafting grid.
     *
     * @return the recipe pattern
     */
    @NotNull
    public static String[] getRevivalRecipePattern() {
        return revivalRecipePattern;
    }

    /**
     * Gets the materials used in the revival item recipe.
     * <p>
     * Maps pattern characters to Material types.
     *
     * @return the recipe materials map
     */
    @NotNull
    public static Map<Character, Material> getRevivalRecipeMaterials() {
        return revivalRecipeMaterials;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //                      PUBLIC API - ADVANCED OPTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Checks if the withdraw command is enabled.
     * <p>
     * When enabled, players can use /withdraw to convert HP into heart items.
     *
     * @return true if withdraw is enabled
     */
    public static boolean isWithdrawEnabled() {
        return withdrawEnabled;
    }

    /**
     * Checks if the plugin should override the keepInventory game rule.
     * <p>
     * When enabled, health changes apply even when keepInventory is true.
     *
     * @return true if keepInventory should be overridden
     */
    public static boolean shouldOverrideKeepInventory() {
        return overrideKeepInventory;
    }
}