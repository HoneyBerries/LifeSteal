package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.EliminationManager;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Handles player death events in the LifeSteal plugin.
 * This listener implements the mechanics for handling deaths caused by players or natural causes
 * and adjusts the maximum health of the involved players accordingly.
 */
public class PlayerDeathListener implements Listener {

    private final LifeSteal plugin = LifeSteal.getInstance();
    private final Logger logger = plugin.getLogger();

    /**
     * Event handler for player death events.
     * Determines the cause of death and adjusts the maximum health of the victim and killer (if applicable).
     *
     * @param event The PlayerDeathEvent triggered when a player dies.
     */
    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player victim = event.getPlayer();

        // Check if the plugin ignores the victim
        if (victim.hasPermission("lifesteal.debug.bypass")) {
            return;
        }

        // Ignore deaths if the conditions for ignoring are met
        if (!shouldLifeStealTakeAction(victim)) {
            return;
        }

        // Resolve the killer entity (if any) and handle the death cause
        Player killer = resolveKiller(event.getEntity().getKiller());
        handleDeathCause(victim, killer);
    }

    /**
     * Determines whether Lifesteal should take action based on game rule keepInventory and configuration.
     *
     * @param victim The player who died.
     * @return True if LifeSteal should take action, false if the death should be ignored.
     */
    private boolean shouldLifeStealTakeAction(Player victim) {
        World world = victim.getWorld();
        boolean keepInventoryEnabled = Boolean.TRUE.equals(world.getGameRuleValue(GameRule.KEEP_INVENTORY));

        // Skip lifesteal action only when keepInventory is enabled, and we're not configured to ignore that setting
        return !keepInventoryEnabled || LifeStealSettings.isIgnoreKeepInventory();
    }

    /**
     * Resolves the killer entity from the event.
     * If the killer is a player or a projectile shot by a player, returns the player.
     *
     * @param killer The entity that caused the death.
     * @return The player who caused the death, or null if not applicable.
     */
    private Player resolveKiller(Entity killer) {
        if (killer instanceof Player player) {
            return player;
        }
        if (killer instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /**
     * Handles the death cause and adjusts health based on whether the killer is a player or natural cause.
     *
     * @param victim The player who died.
     * @param killer The player who killed the victim, or null if not applicable.
     */
    private void handleDeathCause(Player victim, Player killer) {
        if (killer != null) {
            // Player killed another player
            handlePlayerKill(victim, killer);
        } else {
            // Natural causes (falling, drowning, etc.)
            handleNaturalDeath(victim);
        }
    }

    /**
     * Handles deaths caused by natural causes (e.g., falling, drowning).
     * Reduces the victim's maximum health and enforces the minimum health limit if enabled.
     *
     * @param victim The player who died.
     */
    private void handleNaturalDeath(@NotNull Player victim) {
        double healthLost = calculateHealthLost(victim, LifeStealSettings.getNaturalDeathHealthLost());

        if (healthLost > 0) {
            LifeStealUtil.adjustMaxHealth(victim, -healthLost);
            double heartsLost = healthLost / 2.0;
            victim.sendMessage(Component.text("You lost ")
                .append(Component.text(LifeStealUtil.formatHealth(heartsLost), NamedTextColor.RED))
                .append(Component.text(" " + formatHearts(heartsLost) + " due to a natural death.")));
            logger.info("%s lost %s health (%s %s) from a natural death.".formatted(
                victim.getName(), LifeStealUtil.formatHealth(healthLost), LifeStealUtil.formatHealth(heartsLost), formatHearts(heartsLost)
            ));
        }
        
        // Check for elimination after health loss
        checkForElimination(victim);
    }

    /**
     * Handles deaths caused by other players.
     * Reduces the victim's maximum health, increases the killer's maximum health,
     * and enforces minimum and maximum health limits if enabled.
     *
     * @param victim The player who died.
     * @param killer The player who killed the victim.
     */
    private void handlePlayerKill(@NotNull Player victim, @NotNull Player killer) {
        double healthLost = calculateHealthLost(victim, LifeStealSettings.getPlayerDeathHealthLost());
        double healthGained = calculateHealthGained(killer, LifeStealSettings.getPlayerKillHealthGained());

        if (healthLost > 0) {
            LifeStealUtil.adjustMaxHealth(victim, -healthLost);
            double heartsLost = healthLost / 2.0;
            victim.sendMessage(Component.text("You lost ")
                .append(Component.text(LifeStealUtil.formatHealth(heartsLost), NamedTextColor.RED))
                .append(Component.text(" " + formatHearts(heartsLost) + " because you were killed by " + killer.getName() + ".")));
            logger.info("%s lost %s health (%s %s) after being killed by %s.".formatted(
                victim.getName(), LifeStealUtil.formatHealth(healthLost), LifeStealUtil.formatHealth(heartsLost), formatHearts(heartsLost), killer.getName()
            ));
        }

        if (healthGained > 0) {
            LifeStealUtil.adjustMaxHealth(killer, healthGained);
            double heartsGained = healthGained / 2.0;
            killer.sendMessage(Component.text("You gained ")
                .append(Component.text(LifeStealUtil.formatHealth(heartsGained), NamedTextColor.GREEN))
                .append(Component.text(" " + formatHearts(heartsGained) + " for killing " + victim.getName() + ".")));
            logger.info("%s gained %s health (%s %s) for killing %s.".formatted(
                killer.getName(), LifeStealUtil.formatHealth(healthGained), LifeStealUtil.formatHealth(heartsGained), formatHearts(heartsGained), victim.getName()
            ));
        }
        
        // Check for elimination after health loss
        checkForElimination(victim);
    }

    private double calculateHealthLost(Player victim, double amountToLose) {
        if (amountToLose <= 0) {
            return 0;
        }

        double currentHealth = LifeStealUtil.getMaxHealth(victim);
        if (LifeStealSettings.isMinHealthLimitEnabled()) {
            double minHealth = LifeStealSettings.getMinHealthLimit();
            if (currentHealth <= minHealth) {
                return 0; // Already at or below the minimum
            }
            if (currentHealth - amountToLose < minHealth) {
                double adjustedLoss = currentHealth - minHealth;
                double hearts = minHealth / 2.0;
                victim.sendMessage(Component.text("Your health cannot go below the minimum of ")
                    .append(Component.text(LifeStealUtil.formatHealth(hearts) + " " + formatHearts(hearts), NamedTextColor.RED)));
                return adjustedLoss;
            }
        }
        return amountToLose;
    }

    private double calculateHealthGained(Player killer, double amountToGain) {
        if (amountToGain <= 0) {
            return 0;
        }

        double currentHealth = LifeStealUtil.getMaxHealth(killer);
        if (LifeStealSettings.isMaxHealthLimitEnabled()) {
            double maxHealth = LifeStealSettings.getMaxHealthLimit();
            if (currentHealth >= maxHealth) {
                return 0; // Already at or above the maximum
            }
            if (currentHealth + amountToGain > maxHealth) {
                double adjustedGain = maxHealth - currentHealth;
                double hearts = maxHealth / 2.0;
                killer.sendMessage(Component.text("Your health cannot go above the maximum of ")
                    .append(Component.text(LifeStealUtil.formatHealth(hearts) + " " + formatHearts(hearts), NamedTextColor.RED)));
                return adjustedGain;
            }
        }
        return amountToGain;
    }

    private String formatHearts(double count) {
        return count == 1.0 ? "heart" : "hearts";
    }

    /**
     * Checks if a player should be eliminated based on their current health and the elimination settings.
     * If elimination is enabled and the player is at minimum health, they will be eliminated.
     *
     * @param player The player to check for elimination.
     */
    private void checkForElimination(@NotNull Player player) {
        if (!LifeStealSettings.isUseElimination()) {
            return; // Elimination is disabled
        }

        if (!LifeStealSettings.isMinHealthLimitEnabled()) {
            return; // No minimum health limit, so no elimination
        }

        double currentHealth = LifeStealUtil.getMaxHealth(player);
        double minHealth = LifeStealSettings.getMinHealthLimit();

        // Check if the player is at or below minimum health
        if (currentHealth <= minHealth) {
            EliminationManager.eliminatePlayer(player);
        }
    }
}