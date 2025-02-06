package me.honeyberries.lifeSteal;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class LifeSteal extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("LifeSteal has been enabled! Life is much harder now!");
        getServer().getPluginManager().registerEvents(new KillListener(), this);
        getServer().getPluginManager().registerEvents(new HeartUseListener(), this);
        getServer().getPluginCommand("heart").setExecutor(new HeartCommand());
        getServer().getPluginCommand("withdraw").setExecutor(new WithdrawCommand());
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