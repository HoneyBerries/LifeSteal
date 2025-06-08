package me.honeyberries.lifeSteal;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.honeyberries.lifeSteal.command.HealthCommand;
import me.honeyberries.lifeSteal.command.LifeStealCommand;
import me.honeyberries.lifeSteal.command.WithdrawCommand;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.listener.HeartUsageListener;
import me.honeyberries.lifeSteal.listener.PlayerDeathListener;
import me.honeyberries.lifeSteal.task.HeartRecipeDiscoveryTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * The main class for the LifeSteal plugin.
 * This plugin implements mechanics for stealing hearts, crafting heart items, and managing player health.
 */
public final class LifeSteal extends JavaPlugin {

    private ScheduledTask invScanTask; // Task for scanning player inventories

    /**
     * Called when the plugin is enabled.
     * Handles setup tasks such as registering commands, listeners, and scheduled tasks.
     */
    @Override
    public void onEnable() {
        getLogger().info("LifeSteal plugin is starting...");

        // Load configuration settings
        LifeStealSettings.loadConfig();

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();


        // Schedule the inventory scanning task
        startInventoryScanTask();

        getLogger().info("LifeSteal plugin has been successfully enabled!");
    }

    /**
     * Called when the plugin is disabled.
     * Handles cleanup tasks such as stopping scheduled tasks.
     */
    @Override
    public void onDisable() {
        getLogger().info("LifeSteal plugin is shutting down...");

        // Stop the inventory scanning task if it is running
        if (invScanTask != null && !invScanTask.isCancelled()) {
            invScanTask.cancel();
        }

        getLogger().info("LifeSteal plugin has been successfully disabled!");
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new HeartUsageListener(), this);
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        Objects.requireNonNull(getServer().getPluginCommand("health"))
                .setExecutor(new HealthCommand());
        Objects.requireNonNull(getServer().getPluginCommand("withdraw"))
                .setExecutor(new WithdrawCommand());

        Objects.requireNonNull(getServer().getPluginCommand("lifesteal")).setExecutor(new LifeStealCommand());
    }

    /**
     * Starts the inventory scanning task to automatically discover heart recipes.
     */
    private void startInventoryScanTask() {
       invScanTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
               this,
               task -> HeartRecipeDiscoveryTask.getInstance().run(),
               1L, // Initial delay (1 ticks)
               1L // Repeat interval (20 ticks = 1 second)
       );
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