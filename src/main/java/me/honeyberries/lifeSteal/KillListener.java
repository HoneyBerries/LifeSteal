package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

public class KillListener implements Listener {

    /**
     * Handles the event when a player dies. Adjusts the player's max health based on the cause of death.
     * If the player is killed by another player, a heart is transferred from the victim to the killer.
     * If the player dies naturally or to a monster, they lose a heart.
     *
     * @param event The PlayerDeathEvent triggered when a player dies.
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player victim = event.getEntity(); // The player who died
        Entity killer = victim.getKiller(); // Entity that caused the death (could be null for natural death)

        switch (killer) {
            case null -> {
                Bukkit.getLogger().info(victim.getName() + " lost a heart due to natural causes.");
                LifeStealHelper.adjustMaxHealth(victim, -2);
            }
            case Monster monster -> {
                Bukkit.getLogger().info(victim.getName() + " lost a heart due to a " + monster.getType().name().toLowerCase().replace("_", " "));
                LifeStealHelper.adjustMaxHealth(victim, -2);
            }
            case Player murderer -> {
                Bukkit.getLogger().info(victim.getName() + " lost a heart due to " + murderer.getName());
                LifeStealHelper.swapHeart(murderer, victim);
            }
            default -> Bukkit.getLogger().info("Something went wrong with the life stealer!");
        }
    }

    /**
     * Handles the event when a player respawns. Ensures the player's health is properly scaled
     * to their max health after respawning. The player's health will be set to at least 0.5 hearts
     * but will not exceed their max health.
     *
     * @param event The PlayerRespawnEvent triggered when a player respawns.
     */
    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        // Ensure player's current health is scaled properly after respawn
        player.setHealth(Math.max(1.0, Math.min(player.getHealth(), maxHealth))); // Set health to at least 0.5 hearts but not exceed max health
    }
}