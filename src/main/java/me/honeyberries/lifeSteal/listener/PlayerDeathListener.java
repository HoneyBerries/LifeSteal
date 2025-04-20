package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import java.util.logging.Logger;

/**
 * Handles player death events in the LifeSteal plugin.
 * This listener implements the mechanics for handling deaths caused by players, monsters, or natural causes,
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
        if (victim.hasPermission("lifesteal.ignore")) {
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
     * Handles the death cause and adjusts health based on whether the killer is a player, monster, or natural cause.
     *
     * @param victim The player who died.
     * @param killer The player who killed the victim, or null if not applicable.
     */
    private void handleDeathCause(Player victim, Player killer) {
        if (killer != null) {

            // Player killed another player
            handlePlayerKill(victim, killer);
        } else if (victim.getLastDamageCause() != null &&
                  victim.getLastDamageCause().getEntity() instanceof Monster) {
            // Monster killed the player
            handleMonsterDeath(victim);
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
        // Get health lost from config
        int healthLost = LifeStealSettings.getNaturalDeathHealthLost();
        if (healthLost <= 0) {
            return; // Feature disabled in config
        }

        // Calculate new health
        double currentHealth = LifeStealUtil.getMaxHealth(victim);
        double newHealth = currentHealth - healthLost;

        // Check for minimum health limit
        if (LifeStealSettings.isMinHealthLimitEnabled()) {
            int minHealth = LifeStealSettings.getMinHealthLimit();
            if (newHealth < minHealth) {
                healthLost = (int)(currentHealth - minHealth);
                newHealth = minHealth;
                victim.sendMessage(Component.text("Your health can't go below the minimum of ")
                        .append(Component.text(minHealth / 2.0 + " " + (minHealth == 2 ? "heart" : "hearts"), NamedTextColor.RED)));
            }
        }

        // Only adjust health if there's a change
        if (healthLost > 0) {
            LifeStealUtil.adjustMaxHealth(victim, -healthLost);
            double heartsLost = healthLost / 2.0;

            // Send message to the player
            victim.sendMessage(Component.text("You lost ")
                    .append(Component.text(String.format("%.1f", heartsLost), NamedTextColor.RED))
                    .append(Component.text(" " + (heartsLost == 1.0 ? "heart" : "hearts") + " due to death.")));

            logger.info(victim.getName() + " lost " + healthLost + " health points (" +
                        heartsLost + " hearts) due to natural death.");
        }
    }

    /**
     * Handles deaths caused by monsters.
     * Reduces the victim's maximum health and enforces the minimum health limit if enabled.
     *
     * @param victim The player who died.
     */
    private void handleMonsterDeath(@NotNull Player victim) {
        // Get health lost from config
        int healthLost = LifeStealSettings.getMonsterDeathHealthLost();
        if (healthLost <= 0) {
            return; // Feature disabled in config
        }

        // Get monster name safely
        String monsterName = "unknown monster";
        if (victim.getLastDamageCause() != null) {
            victim.getLastDamageCause();
            monsterName = victim.getLastDamageCause().getEntity().getType().name();
        }

        // Calculate new health
        double currentHealth = LifeStealUtil.getMaxHealth(victim);
        double newHealth = currentHealth - healthLost;

        // Check for minimum health limit
        if (LifeStealSettings.isMinHealthLimitEnabled()) {
            int minHealth = LifeStealSettings.getMinHealthLimit();
            if (newHealth < minHealth) {
                healthLost = (int)(currentHealth - minHealth);
                newHealth = minHealth;
                victim.sendMessage(Component.text("Your health can't go below the minimum of ")
                        .append(Component.text(minHealth / 2.0 + " " + (minHealth == 2 ? "heart" : "hearts"), NamedTextColor.RED)));
            }
        }

        // Only adjust health if there's a change
        if (healthLost > 0) {
            LifeStealUtil.adjustMaxHealth(victim, -healthLost);
            double heartsLost = healthLost / 2.0;
            victim.sendMessage(Component.text("You lost ")
                    .append(Component.text(String.format("%.1f", heartsLost), NamedTextColor.RED))
                    .append(Component.text(" " + (heartsLost == 1.0 ? "heart" : "hearts") + " due to a " + monsterName + ".")));

            logger.info(victim.getName() + " lost " + healthLost + " health points (" +
                        heartsLost + " hearts) due to death by " + monsterName + ".");
        }
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
        double healthLost = LifeStealSettings.getPlayerDeathHealthLost();
        double healthGained = LifeStealSettings.getPlayerKillHealthGained();
        double victimNewHealth = LifeStealUtil.getMaxHealth(victim) - healthLost;
        double killerNewHealth = LifeStealUtil.getMaxHealth(killer) + healthGained;

        // Check for minimum health limit for victim
        if (LifeStealSettings.isMinHealthLimitEnabled()) {
            int minHealth = LifeStealSettings.getMinHealthLimit();
            if (victimNewHealth < minHealth) {
                healthLost = LifeStealUtil.getMaxHealth(victim) - minHealth;
                if (healthLost <= 0) {
                    healthLost = 0; // Already at or below minimum
                } else {
                    // Notify victim they've reached the minimum health limit
                    victim.sendMessage(Component.text("You've reached the minimum health limit of ")
                            .append(Component.text(minHealth / 2.0 + " " + (minHealth == 2 ? "heart" : "hearts")).color(NamedTextColor.RED)));
                }
            }
        }

        // Check for maximum health limit for killer
        if (LifeStealSettings.isMaxHealthLimitEnabled()) {
            int maxHealth = LifeStealSettings.getMaxHealthLimit();
            if (killerNewHealth > maxHealth) {
                healthGained = maxHealth - LifeStealUtil.getMaxHealth(killer);
                if (healthGained <= 0) {
                    healthGained = 0; // Already at or above maximum
                } else {
                    // Notify killer they've reached the maximum health limit
                    killer.sendMessage(Component.text("You've reached the maximum health limit of ")
                            .append(Component.text(maxHealth / 2.0 + " " + (maxHealth == 2 ? "heart" : "hearts")).color(NamedTextColor.RED)));
                }
            }
        }

        logger.info("%s was killed by %s and lost %.1f health points.".formatted(
                victim.getName(), killer.getName(), healthLost));
        logger.info("%s gained %.1f health points by killing %s.".formatted(
                killer.getName(), healthGained, victim.getName()));

        if (healthLost > 0) {
            LifeStealUtil.adjustMaxHealth(victim, -healthLost);
        }

        if (healthGained > 0) {
            LifeStealUtil.adjustMaxHealth(killer, healthGained);
        }
    }
}
