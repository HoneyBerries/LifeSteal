package me.honeyberries.lifeSteal.command;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Handles the `/heart` command which allows players to view or modify their maximum hearts.
 * Commands are entered in hearts for simplicity; internally all calculations use health points.
 *
 * Command formats:
 * - /heart - View your own hearts
 * - /heart <player> - View another player's hearts
 * - /heart set <hearts> [player] - Set max hearts for yourself or another player
 * - /heart add <hearts> [player] - Add hearts to yourself or another player
 * - /heart remove <hearts> [player] - Remove hearts from yourself or another player
 * - /heart help - Display command usage information
 */
public class HeartCommand implements TabExecutor {

    // Minimum allowed health (2 health points = 1 heart) - Minecraft's default minimum
    private static final double MIN_HEALTH = LifeStealSettings.getMinHealthLimit();

    // Supported actions for modifying hearts - used for command validation and tab completion
    private static final List<String> ACTIONS = List.of("set", "add", "remove", "help");

    // Plugin instance and logger for debugging and informational messages
    private final LifeSteal plugin = LifeSteal.getInstance();
    private final Logger logger = plugin.getLogger();

    /**
     * Main entry point for the `/heart` command.
     * Handles different subcommands based on the number and type of arguments provided.
     *
     * @param sender  The command sender (player or console).
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    The command arguments.
     * @return true to signal that the command was processed.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // No arguments: check own hearts (must be a player)
        if (args.length == 0) {
            handleSelfHealthCheck(sender);
            return true;
        }

        // Single argument: either display help or check a specific player's hearts
        else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(sender);
                return true;
            }
            handlePlayerHealthCheck(sender, args[0]);
            return true;
        }

        // Two or three arguments: modify hearts (action, amount, [optional target])
        else if (args.length == 2 || args.length == 3) {
            handleHealthModification(sender, args);
            return true;
        }
        // If none of the above conditions met, send an error message
        sendHelpMessage(sender);
        return true;
    }

    /**
     * Provides tab-completion suggestions for the `/heart` command.
     * Suggests valid actions or player names based on the argument position.
     *
     * @param sender  The command sender.
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    The command arguments so far.
     * @return A list of possible suggestions based on argument position.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            // First argument: suggest player names and actions
            String input = args[0].toLowerCase();
            return Stream.concat(
                    getOnlinePlayerNames().filter(name -> name.toLowerCase().startsWith(input)),
                    ACTIONS.stream().filter(action -> action.startsWith(input))
            ).sorted().toList();
        } else if (args.length == 3) {
            // Third argument: suggest player names (for targeting)
            String input = args[2].toLowerCase();
            return getOnlinePlayerNames()
                    .filter(name -> name.toLowerCase().startsWith(input)).sorted().toList();
        }
        // No suggestions for other positions
        return List.of();
    }

    /**
     * Displays the help message to the CommandSender.
     * Provides a detailed explanation of all available subcommands.
     *
     * @param sender The command sender who will receive the help message.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("------ Heart Command Help ------", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/heart help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/heart", NamedTextColor.AQUA)
                .append(Component.text(" - View your current max hearts.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/heart <player>", NamedTextColor.AQUA)
                .append(Component.text(" - View another player's max hearts.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/heart set <hearts> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Set max hearts to a specific amount.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/heart add <hearts> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Add to max hearts.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/heart remove <hearts> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Remove from max hearts.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GREEN));
    }

    /**
     * Handles health check for the sender if no arguments are provided.
     * Ensures the sender is a player and displays their current max hearts.
     *
     * @param sender The command sender.
     */
    private void handleSelfHealthCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "This command can only be used by players to check their own hearts.");
            return;
        }
        // Player is checking their own health, so both viewer and target are the same
        sendHealthMessage(sender, player, player);
    }

    /**
     * Handles health check for a specific player when a player name is provided.
     * Validates that the target player exists and is online before displaying their health.
     *
     * @param sender     The command sender.
     * @param playerName The name of the player whose hearts are to be checked.
     */
    private void handlePlayerHealthCheck(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendError(sender, "Player " + playerName + " is not online or doesn't exist.");
            logger.severe("Attempted to check hearts for non-existent player: " + playerName);
            return;
        }
        // Show target player's health to the sender
        sendHealthMessage(sender, target, target);
    }

    /**
     * Processes heart modification commands (set, add, remove).
     * Validates the action, parses the heart amount, and identifies the target player.
     *
     * @param sender The command sender.
     * @param args   The command arguments.
     */
    private void handleHealthModification(CommandSender sender, String[] args) {
        String action = args[0].toLowerCase();
        if (!ACTIONS.contains(action)) {
            sendError(sender, "Invalid action " + args[0] + ". Valid actions are: set, add, remove, help.");
            return;
        }
        if (action.equals("help")) {
            sendHelpMessage(sender);
            return;
        }

        // Parse and validate heart amount
        double heartsInput;
        try {
            heartsInput = Integer.parseInt(args[1]);
            if (heartsInput <= 0) {
                sendError(sender, "Heart amount must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(sender, args[1] + " is not a valid number. Please use a numeric value.");
            logger.warning("Invalid number format provided: " + args[1]);
            return;
        }
        // Convert hearts to health points (internal calculation unit)
        double healthValue = heartsInput * 2D;

        // Determine the target: specified player or self (if sender is a player)
        Player target = (args.length == 3) ? Bukkit.getPlayer(args[2]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            sendError(sender, (args.length == 3) ?
                    "Player " + args[2] + " is not online or doesn't exist." :
                    "Console must specify a target player for this command.");
            if (args.length == 3) {
                logger.warning("Invalid target player: " + args[2]);
            }
            return;
        }

        // Execute the appropriate action
        switch (action) {
            case "set" -> setHealth(sender, target, healthValue);
            case "add" -> adjustHealth(sender, target, healthValue);
            case "remove" -> adjustHealth(sender, target, -healthValue);
        }
    }

    /**
     * Sets the target player's maximum health to a specific value.
     * Ensures the new health value doesn't fall below the minimum allowed health.
     *
     * @param sender The command sender.
     * @param target The target player.
     * @param health The new health value to set (in health points, not hearts).
     */
    private void setHealth(CommandSender sender, Player target, double health) {
        if (health < MIN_HEALTH) {
            sendError(sender, "Health cannot be set below " + MIN_HEALTH + " health points.");
            return;
        }
        
        // Capture the old health before making changes
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        
        // Update the player's max health using health points directly
        LifeStealUtil.setMaxHealth(target, health);
        
        // Display the old and new values
        sendHealthUpdate(sender, target, oldHealth, health);
    }
    
    /**
     * Adjusts the target player's maximum health by adding or removing health points.
     * Ensures the new health value doesn't fall below the minimum allowed health.
     *
     * @param sender      The command sender.
     * @param target      The target player.
     * @param healthDelta The amount of health to add (positive) or remove (negative).
     */
    private void adjustHealth(CommandSender sender, Player target, double healthDelta) {
        // Capture the old health before making changes
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        double newHealth = oldHealth + healthDelta;
        
        if (newHealth < MIN_HEALTH) {
            sendError(sender, "Cannot reduce health below " + MIN_HEALTH + " health points.");
            return;
        }
        
        // Apply the health change to the target player
        LifeStealUtil.adjustMaxHealth(target, healthDelta);
        
        // Display the old and new values
        sendHealthUpdate(sender, target, oldHealth, newHealth);
    }

    /**
     * Displays the target player's current health as hearts.
     * Converts health points to hearts for user-friendly display.
     *
     * @param sender The command sender.
     * @param viewer The player viewing the message.
     * @param target The player whose health is being displayed.
     */
    private void sendHealthMessage(CommandSender sender, Player viewer, Player target) {
        // Convert health points to hearts for user-friendly display
        double hearts = LifeStealUtil.getMaxHealth(target) / 2.0;
        Component message = Component.text(target.getName() + "'s health: ")
            .append(Component.text(String.format("%.1f %s", hearts, hearts == 1.0 ? "heart" : "hearts"), NamedTextColor.GOLD));
        viewer.sendMessage(message);
    }

    /**
     * Notifies about a player's health update.
     * Sends different messages to the target and sender (if they're not the same).
     *
     * @param sender    The command sender.
     * @param target    The target player whose health was updated.
     * @param oldHealth The previous health value in health points.
     * @param newHealth The new health value in health points.
     */
    private void sendHealthUpdate(CommandSender sender, Player target, double oldHealth, double newHealth) {
        // Convert health points to hearts for display
        double oldHearts = oldHealth / 2.0;
        double newHearts = newHealth / 2.0;
    
        // Notify the target player about their health change
        String formatedMessage = String.format("%.1f %s.", newHearts, newHearts == 1.0 ? "heart" : "hearts");

        Component targetMessage = Component.text("Your max hearts have been updated from ")
            .append(Component.text(String.format("%.1f", oldHearts), NamedTextColor.RED))
            .append(Component.text(" to ", NamedTextColor.GOLD))
            .append(Component.text(formatedMessage, NamedTextColor.GREEN));

        target.sendMessage(targetMessage);

        // If sender is not the target, notify the sender as well
        Component senderMessage = Component.text(target.getName() + "'s max hearts have been updated from ")
            .append(Component.text(String.format("%.1f", oldHearts), NamedTextColor.RED))
            .append(Component.text(" to ", NamedTextColor.GOLD))
            .append(Component.text(formatedMessage, NamedTextColor.GREEN));
        sender.sendMessage(senderMessage);
    }

    /**
     * Sends an error message to the command sender.
     * Formats all error messages with a consistent red color.
     *
     * @param sender  The command sender.
     * @param message The error message to send.
     */
    private void sendError(CommandSender sender, String message) {
        sender.sendMessage(Component.text("Error: " + message).color(NamedTextColor.RED));
    }

    /**
     * Retrieves the names of all currently online players as a Stream.
     * Used for tab completion to suggest player names.
     *
     * @return A Stream of player names of currently online players.
     */
    private Stream<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName);
    }
}
