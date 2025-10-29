package me.honeyberries.lifeSteal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.honeyberries.lifeSteal.command.HealthCommand;
import me.honeyberries.lifeSteal.command.LifeStealCommand;
import me.honeyberries.lifeSteal.command.WithdrawCommand;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.listener.HeartUsageListener;
import me.honeyberries.lifeSteal.listener.PlayerDeathListener;
import me.honeyberries.lifeSteal.listener.PlayerJoinListener;
import me.honeyberries.lifeSteal.listener.RevivalItemListener;
import me.honeyberries.lifeSteal.task.HeartRecipeDiscoveryTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import java.util.List;

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
        
        // Load messages
        me.honeyberries.lifeSteal.config.Messages.loadMessages();

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
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new RevivalItemListener(), this);
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
     * Starts the inventory scanning task to automatically discover heart recipes.
     */

    /**
     * Starts the inventory scanning task to automatically discover heart recipes.
     */
    private void startInventoryScanTask() {
       invScanTask = getServer().getGlobalRegionScheduler().runAtFixedRate(
               this,
               new HeartRecipeDiscoveryTask(this),
               LifeStealConstants.RECIPE_DISCOVERY_INITIAL_DELAY,
               LifeStealConstants.RECIPE_DISCOVERY_REPEAT_INTERVAL
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
