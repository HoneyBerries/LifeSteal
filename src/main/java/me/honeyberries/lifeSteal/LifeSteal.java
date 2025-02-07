package me.honeyberries.lifeSteal;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LifeSteal extends JavaPlugin {

    @Override
    public void onEnable() {
        // Log a message to the console when the plugin is enabled
        getLogger().info("LifeSteal has been enabled! Life is much harder now!");

        // Register event listeners to handle specific in-game events
        // KillListener handles events related to player kills (e.g., stealing hearts)
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // HeartUsageListener handles events when players use the custom Heart item
        getServer().getPluginManager().registerEvents(new HeartUsageListener(), this);

        //Register the heart recipe discovery so when a player picks up a totem, they get the heart recipe
        getServer().getPluginManager().registerEvents(new HeartRecipeDiscovery(), this);

        // Register the /heart command and link it to the HeartCommand class
        getServer().getPluginCommand("heart").setExecutor(new HeartCommand());

        // Register the /withdraw command, which allows players to withdraw hearts
        getServer().getPluginCommand("withdraw").setExecutor(new WithdrawCommand());

        // Register the custom crafting recipe for the Heart item
        HeartRecipe.registerHeartRecipe();
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("LifeSteal has been disabled!");
    }

    public static @NotNull LifeSteal getInstance() {
        return getPlugin(LifeSteal.class);
    }

}