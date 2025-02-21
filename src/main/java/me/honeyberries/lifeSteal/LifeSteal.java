package me.honeyberries.lifeSteal;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * The main class for the LifeSteal plugin.
 * This plugin handles the LifeSteal mechanics, where players can steal hearts and craft heart items.
 * It includes functionality for player deaths, item usage, crafting recipes, and heart withdrawals.
 */
public final class LifeSteal extends JavaPlugin {

    private BukkitTask invScanTask;

    /**
     * Called when the plugin is enabled.
     * This method handles the initial setup, such as registering event listeners,
     * setting command executors, and registering custom recipes and tasks.
     */
    @Override
    public void onEnable() {
        // Log a message to the console when the plugin is enabled
        getLogger().info("LifeSteal has been enabled! Life is much harder now!");

        // Register event listeners to handle specific in-game events
        // KillListener handles events related to player kills (e.g., stealing hearts)
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // HeartUsageListener handles events when players use the custom Heart item
        getServer().getPluginManager().registerEvents(new HeartUsageListener(), this);

        // Register the /heart command and link it to the HeartCommand class
        Objects.requireNonNull(getServer().getPluginCommand("heart")).setExecutor(new HeartCommand());

        // Register the /withdraw command, which allows players to withdraw hearts
        Objects.requireNonNull(getServer().getPluginCommand("withdraw")).setExecutor(new WithdrawCommand());

        // Register the custom crafting recipe for the Heart item
        HeartRecipe.registerHeartRecipe();

        // Run the heart recipe discovery task so when a player picks up a totem, they get the heart recipe
        invScanTask = getServer().getScheduler().runTaskTimer(this, HeartRecipeDiscovery.getInstance(), 0, 0);
    }

    /**
     * Called when the plugin is disabled.
     * This method handles the cleanup, such as stopping the inventory scanning task.
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("LifeSteal has been disabled!");

        // Stop the invScanTask if it is running
        if (invScanTask != null && invScanTask.isCancelled()) {
            invScanTask.cancel();
        }
    }

    /**
     * Gets the instance of the LifeSteal plugin.
     *
     * @return The LifeSteal plugin instance.
     */
    public static @NotNull LifeSteal getInstance() {
        return getPlugin(LifeSteal.class);
    }
}
