package me.honeyberries.lifeSteal;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.honeyberries.lifeSteal.command.HealthCommand;
import me.honeyberries.lifeSteal.command.LifeStealCommand;
import me.honeyberries.lifeSteal.command.WithdrawCommand;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.listener.EliminatedPlayerJoinListener;
import me.honeyberries.lifeSteal.listener.HeartUsageListener;
import me.honeyberries.lifeSteal.listener.PlayerDeathListener;
import me.honeyberries.lifeSteal.listener.RevivalItemUsageListener;
import me.honeyberries.lifeSteal.task.HeartRecipeDiscoveryTask;
import me.honeyberries.lifeSteal.task.RevivalItemRecipeDiscoveryTask;
import me.honeyberries.lifeSteal.util.EliminationManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The main class for the LifeSteal plugin.
 * This plugin implements mechanics for stealing hearts, crafting heart items, and managing player health.
 */
public final class LifeSteal extends JavaPlugin {

    // Scheduled tasks for inventory scanning
    private ScheduledTask heartItemInvScanTask;
    private ScheduledTask revivalItemInvScanTask;

    /**
     * Called when the plugin is enabled.
     * Handles setup tasks such as registering commands, listeners, and scheduled tasks.
     */
    @Override
    public void onEnable() {
        getLogger().info("LifeSteal plugin is starting...");

        // Load configuration settings
        LifeStealSettings.loadConfig();

        // Initialize the EliminationManager
        EliminationManager.initialize();

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
        if (heartItemInvScanTask != null && !heartItemInvScanTask.isCancelled()) {
            heartItemInvScanTask.cancel();
        }

        if (revivalItemInvScanTask != null && !revivalItemInvScanTask.isCancelled()) {
            revivalItemInvScanTask.cancel();
        }

        getLogger().info("LifeSteal plugin has been successfully disabled!");
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new HeartUsageListener(), this);
        getServer().getPluginManager().registerEvents(new EliminatedPlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new RevivalItemUsageListener(), this);
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        getLifecycleManager().registerEventHandler(
            LifecycleEvents.COMMANDS, commands -> {
                commands.registrar().register(LifeStealCommand.getBuildCommand());
                commands.registrar().register(WithdrawCommand.getBuildCommand());
                commands.registrar().register(HealthCommand.getBuildCommand());
            }
        );
    }


    /**
     * Starts the inventory scanning task to automatically discover heart and revival item recipes.
     */
    private void startInventoryScanTask() {
       heartItemInvScanTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
               this,
               new HeartRecipeDiscoveryTask(this),
               1L, // Initial delay (1 ticks)
               1L // Repeat interval (20 ticks = 1 second)
       );

       revivalItemInvScanTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
               this,
               new RevivalItemRecipeDiscoveryTask(this),
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
