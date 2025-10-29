package me.honeyberries.lifeSteal.config;

import me.honeyberries.lifeSteal.LifeSteal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all plugin messages with MiniMessage support.
 * All messages are configurable and support MiniMessage formatting.
 */
public class Messages {
    
    private static final LifeSteal plugin = LifeSteal.getInstance();
    private static final Logger LOGGER = plugin.getLogger();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    // Death messages
    private static String naturalDeathLoss;
    private static String playerDeathLoss;
    private static String playerKillGain;
    private static String minHealthReached;
    private static String maxHealthReached;
    
    // Heart item messages
    private static String heartUsed;
    private static String heartDisabled;
    private static String maxHealthLimitReached;
    private static String maxHealthLimitExceeded;
    
    // Withdraw messages
    private static String withdrawDisabled;
    private static String withdrawNotEnoughHealth;
    private static String withdrawSuccess;
    private static String withdrawSuccessOther;
    private static String withdrawInventoryFull;
    
    // Health command messages
    private static String healthView;
    private static String healthSet;
    private static String healthAdd;
    private static String healthRemove;
    private static String healthCannotBeZero;
    private static String playerNotFound;
    private static String consolePlayerRequired;
    
    // Elimination messages
    private static String eliminatedSpectator;
    private static String eliminatedBanKick;
    private static String playerRevived;
    private static String noEliminatedPlayers;
    private static String revivalItemDisabled;
    
    // General messages
    private static String configReloaded;
    private static String pluginUninstalled;
    
    /**
     * Loads all messages from the configuration file.
     */
    public static void loadMessages() {
        try {
            plugin.saveDefaultConfig();
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // Load death messages
            naturalDeathLoss = config.getString("messages.death.natural-death-loss", 
                "<gray>You lost <red>{hearts} {hearts_word}</red> due to a natural death.");
            playerDeathLoss = config.getString("messages.death.player-death-loss",
                "<gray>You lost <red>{hearts} {hearts_word}</red> because you were killed by <yellow>{killer}</yellow>.");
            playerKillGain = config.getString("messages.death.player-kill-gain",
                "<gray>You gained <green>{hearts} {hearts_word}</green> for killing <yellow>{victim}</yellow>.");
            minHealthReached = config.getString("messages.death.min-health-reached",
                "<gray>Your health cannot go below the minimum of <red>{hearts} {hearts_word}</red>.");
            maxHealthReached = config.getString("messages.death.max-health-reached",
                "<gray>Your health cannot go above the maximum of <gold>{hearts} {hearts_word}</gold>.");
            
            // Load heart item messages
            heartUsed = config.getString("messages.heart-item.used",
                "<gray>You gained <green>{hearts} {hearts_word}</green>!");
            heartDisabled = config.getString("messages.heart-item.disabled",
                "<red>Heart items are currently disabled on this server.");
            maxHealthLimitReached = config.getString("messages.heart-item.max-health-limit-reached",
                "<red>You have reached the maximum health limit of <gold>{hearts} {hearts_word}</gold>.");
            maxHealthLimitExceeded = config.getString("messages.heart-item.max-health-limit-exceeded",
                "<red>You will exceed the maximum health limit of <gold>{hearts} {hearts_word}</gold>.");
            
            // Load withdraw messages
            withdrawDisabled = config.getString("messages.withdraw.disabled",
                "<red>Heart withdrawal is disabled on this server.");
            withdrawNotEnoughHealth = config.getString("messages.withdraw.not-enough-health",
                "<red>{player} doesn't have enough health to withdraw {hearts} {hearts_word} (requires {required_hearts} hearts)!");
            withdrawSuccess = config.getString("messages.withdraw.success",
                "<gold>You have withdrawn <green>{hearts} {hearts_word} ({health_points} health points)</green>.");
            withdrawSuccessOther = config.getString("messages.withdraw.success-other",
                "<red>{sender} has withdrawn <red>{hearts} {hearts_word} ({health_points} health points)</red> from you!");
            withdrawInventoryFull = config.getString("messages.withdraw.inventory-full",
                "<yellow>Warning: Some heart items were dropped due to a full inventory!");
            
            // Load health command messages
            healthView = config.getString("messages.health.view",
                "<aqua>{possessive} health: <gold>{health_points} health points</gold> <gray>(<green>{hearts} hearts</green>)</gray>");
            healthSet = config.getString("messages.health.set",
                "<aqua>{possessive} max health has been {direction} from <gold>{old_health}</gold> to <{color}>{new_health} health points</{color}> <gray>(<red>{new_hearts} hearts</red>)</gray>");
            healthAdd = config.getString("messages.health.add",
                "<aqua>{possessive} max health has been increased from <gold>{old_health}</gold> to <green>{new_health} health points</green> <gray>(<red>{new_hearts} hearts</red>)</gray>");
            healthRemove = config.getString("messages.health.remove",
                "<aqua>{possessive} max health has been decreased from <gold>{old_health}</gold> to <red>{new_health} health points</red> <gray>(<red>{new_hearts} hearts</red>)</gray>");
            healthCannotBeZero = config.getString("messages.health.cannot-be-zero",
                "<red>Health cannot be set to 0 or lower.");
            playerNotFound = config.getString("messages.general.player-not-found",
                "<red>Player '{player}' is not online.");
            consolePlayerRequired = config.getString("messages.general.console-player-required",
                "<red>Console must specify a player.");
            
            // Load elimination messages
            eliminatedSpectator = config.getString("messages.elimination.spectator",
                "<red>You have been eliminated! You are now a spectator.");
            eliminatedBanKick = config.getString("messages.elimination.ban-kick",
                "<red>You have been eliminated! You ran out of hearts.");
            playerRevived = config.getString("messages.elimination.player-revived",
                "<green>{player} has been revived and given {hearts} {hearts_word}!");
            noEliminatedPlayers = config.getString("messages.elimination.no-eliminated-players",
                "<red>There are no eliminated players to revive.");
            revivalItemDisabled = config.getString("messages.elimination.revival-disabled",
                "<red>Revival is currently disabled on this server.");
            
            // Load general messages
            configReloaded = config.getString("messages.general.config-reloaded",
                "<green>LifeSteal configuration reloaded successfully!");
            pluginUninstalled = config.getString("messages.general.plugin-uninstalled",
                "<green>LifeSteal uninstalled successfully!");
            
            LOGGER.info("Messages loaded successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load messages from config.yml", e);
            loadDefaultMessages();
        }
    }
    
    /**
     * Loads default English messages if config loading fails.
     */
    private static void loadDefaultMessages() {
        naturalDeathLoss = "<gray>You lost <red>{hearts} {hearts_word}</red> due to a natural death.";
        playerDeathLoss = "<gray>You lost <red>{hearts} {hearts_word}</red> because you were killed by <yellow>{killer}</yellow>.";
        playerKillGain = "<gray>You gained <green>{hearts} {hearts_word}</green> for killing <yellow>{victim}</yellow>.";
        minHealthReached = "<gray>Your health cannot go below the minimum of <red>{hearts} {hearts_word}</red>.";
        maxHealthReached = "<gray>Your health cannot go above the maximum of <gold>{hearts} {hearts_word}</gold>.";
        
        heartUsed = "<gray>You gained <green>{hearts} {hearts_word}</green>!";
        heartDisabled = "<red>Heart items are currently disabled on this server.";
        maxHealthLimitReached = "<red>You have reached the maximum health limit of <gold>{hearts} {hearts_word}</gold>.";
        maxHealthLimitExceeded = "<red>You will exceed the maximum health limit of <gold>{hearts} {hearts_word}</gold>.";
        
        withdrawDisabled = "<red>Heart withdrawal is disabled on this server.";
        withdrawNotEnoughHealth = "<red>{player} doesn't have enough health to withdraw {hearts} {hearts_word} (requires {required_hearts} hearts)!";
        withdrawSuccess = "<gold>You have withdrawn <green>{hearts} {hearts_word} ({health_points} health points)</green>.";
        withdrawSuccessOther = "<red>{sender} has withdrawn <red>{hearts} {hearts_word} ({health_points} health points)</red> from you!";
        withdrawInventoryFull = "<yellow>Warning: Some heart items were dropped due to a full inventory!";
        
        healthView = "<aqua>{possessive} health: <gold>{health_points} health points</gold> <gray>(<green>{hearts} hearts</green>)</gray>";
        healthSet = "<aqua>{possessive} max health has been {direction} from <gold>{old_health}</gold> to <{color}>{new_health} health points</{color}> <gray>(<red>{new_hearts} hearts</red>)</gray>";
        healthAdd = "<aqua>{possessive} max health has been increased from <gold>{old_health}</gold> to <green>{new_health} health points</green> <gray>(<red>{new_hearts} hearts</red>)</gray>";
        healthRemove = "<aqua>{possessive} max health has been decreased from <gold>{old_health}</gold> to <red>{new_health} health points</red> <gray>(<red>{new_hearts} hearts</red>)</gray>";
        healthCannotBeZero = "<red>Health cannot be set to 0 or lower.";
        playerNotFound = "<red>Player '{player}' is not online.";
        consolePlayerRequired = "<red>Console must specify a player.";
        
        eliminatedSpectator = "<red>You have been eliminated! You are now a spectator.";
        eliminatedBanKick = "<red>You have been eliminated! You ran out of hearts.";
        playerRevived = "<green>{player} has been revived and given {hearts} {hearts_word}!";
        noEliminatedPlayers = "<red>There are no eliminated players to revive.";
        revivalItemDisabled = "<red>Revival is currently disabled on this server.";
        
        configReloaded = "<green>LifeSteal configuration reloaded successfully!";
        pluginUninstalled = "<green>LifeSteal uninstalled successfully!";
    }
    
    // Helper method to replace placeholders and format message
    private static Component format(String template, String... replacements) {
        String message = template;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return MINI_MESSAGE.deserialize(message);
    }
    
    // Death messages
    public static Component naturalDeathLoss(String hearts, String heartsWord) {
        return format(naturalDeathLoss, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    public static Component playerDeathLoss(String hearts, String heartsWord, String killer) {
        return format(playerDeathLoss, "hearts", hearts, "hearts_word", heartsWord, "killer", killer);
    }
    
    public static Component playerKillGain(String hearts, String heartsWord, String victim) {
        return format(playerKillGain, "hearts", hearts, "hearts_word", heartsWord, "victim", victim);
    }
    
    public static Component minHealthReached(String hearts, String heartsWord) {
        return format(minHealthReached, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    public static Component maxHealthReached(String hearts, String heartsWord) {
        return format(maxHealthReached, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    // Heart item messages
    public static Component heartUsed(String hearts, String heartsWord) {
        return format(heartUsed, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    public static Component heartDisabled() {
        return format(heartDisabled);
    }
    
    public static Component maxHealthLimitReached(String hearts, String heartsWord) {
        return format(maxHealthLimitReached, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    public static Component maxHealthLimitExceeded(String hearts, String heartsWord) {
        return format(maxHealthLimitExceeded, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    // Withdraw messages
    public static Component withdrawDisabled() {
        return format(withdrawDisabled);
    }
    
    public static Component withdrawNotEnoughHealth(String player, String hearts, String heartsWord, String requiredHearts) {
        return format(withdrawNotEnoughHealth, "player", player, "hearts", hearts, "hearts_word", heartsWord, "required_hearts", requiredHearts);
    }
    
    public static Component withdrawSuccess(String hearts, String heartsWord, String healthPoints) {
        return format(withdrawSuccess, "hearts", hearts, "hearts_word", heartsWord, "health_points", healthPoints);
    }
    
    public static Component withdrawSuccessOther(String sender, String hearts, String heartsWord, String healthPoints) {
        return format(withdrawSuccessOther, "sender", sender, "hearts", hearts, "hearts_word", heartsWord, "health_points", healthPoints);
    }
    
    public static Component withdrawInventoryFull() {
        return format(withdrawInventoryFull);
    }
    
    // Health command messages
    public static Component healthView(String possessive, String healthPoints, String hearts) {
        return format(healthView, "possessive", possessive, "health_points", healthPoints, "hearts", hearts);
    }
    
    public static Component healthSet(String possessive, String direction, String oldHealth, String newHealth, String newHearts, String color) {
        return format(healthSet, "possessive", possessive, "direction", direction, "old_health", oldHealth, "new_health", newHealth, "new_hearts", newHearts, "color", color);
    }
    
    public static Component healthCannotBeZero() {
        return format(healthCannotBeZero);
    }
    
    public static Component playerNotFound(String player) {
        return format(playerNotFound, "player", player);
    }
    
    public static Component consolePlayerRequired() {
        return format(consolePlayerRequired);
    }
    
    // Elimination messages
    public static Component eliminatedSpectator() {
        return format(eliminatedSpectator);
    }
    
    public static Component eliminatedBanKick() {
        return format(eliminatedBanKick);
    }
    
    public static Component playerRevived(String player, String hearts, String heartsWord) {
        return format(playerRevived, "player", player, "hearts", hearts, "hearts_word", heartsWord);
    }
    
    public static Component noEliminatedPlayers() {
        return format(noEliminatedPlayers);
    }
    
    public static Component revivalItemDisabled() {
        return format(revivalItemDisabled);
    }
    
    // General messages
    public static Component configReloaded() {
        return format(configReloaded);
    }
    
    public static Component pluginUninstalled() {
        return format(pluginUninstalled);
    }
}
