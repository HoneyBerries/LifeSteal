package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.EliminationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Handles player join events for eliminated players.
 * <p>
 * This listener checks if a joining player is eliminated. Depending on the elimination mode,
 * it will either kick them with a message or put them in spectator mode.
 */
public class EliminatedPlayerJoinListener implements Listener {

    /**
     * Event handler for player join events.
     * Checks if the player is eliminated and takes appropriate action.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the server.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the player is eliminated
        if (!EliminationManager.isEliminated(player)) {
            return;
        }

        LifeStealSettings.EliminationMode mode = LifeStealSettings.getEliminationMode();

        switch (mode) {
            case BAN -> {
                // Kick the player with an elimination message
                player.kick(Component.text("You are eliminated!", NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("You ran out of hearts and cannot join until revived.", NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("Ask an admin or get a revival item to return!", NamedTextColor.YELLOW)));
            }
            case SPECTATOR -> {
                // Put the player in spectator mode
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Component.text("You are eliminated and in spectator mode!", NamedTextColor.RED));
            }
        }
    }
}
