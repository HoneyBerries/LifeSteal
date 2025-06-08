package me.honeyberries.lifeSteal.command;

import me.honeyberries.lifeSteal.LifeSteal;
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
 * Handles the `/health` command which allows players to view or modify their maximum health.
 * Commands work directly with health points rather than hearts.
 * <p>
 * Command formats:
 * - /health - View your own health
 * - /health <player> - View another player's health
 * - /health set <health> [player] - Set max health for yourself or another player
 * - /health add <health> [player] - Add health to yourself or another player
 * - /health remove <health> [player] - Remove health from yourself or another player
 * - /health help - Display command usage information
 */
public class HealthCommand implements TabExecutor {

    // Minimum allowed health - set to 0 to enforce positive values only
    private static final double MIN_HEALTH = 0.0;

    // Supported actions for modifying health - used for command validation and tab completion
    private static final List<String> ACTIONS = List.of("set", "add", "remove", "help");

    // Plugin instance and logger for debugging and informational messages
    private final LifeSteal plugin = LifeSteal.getInstance();
    private final Logger logger = plugin.getLogger();

    /**
     * Main entry point for the `/health` command.
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
        // Check if the sender has permission to use the command
        if (!sender.hasPermission("lifesteal.command.health")) {
            sendError(sender, "You do not have permission to use this command.");
            return true;
        }

        // No arguments: check own health (must be a player)
        if (args.length == 0) {
            handleSelfHealthCheck(sender);
            return true;
        }

        // Single argument: either display help or check a specific player's health
        else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(sender);
                return true;
            }
            handlePlayerHealthCheck(sender, args[0]);
            return true;
        }

        // Two or three arguments: modify health (action, amount, [optional target])
        else if (args.length == 2 || args.length == 3) {
            handleHealthModification(sender, args);
            return true;
        }

        // Too many arguments - show help
        sendHelpMessage(sender);
        return true;
    }

    /**
     * Provides tab-completion suggestions for the `/health` command.
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
        // Only provide basic suggestions if sender has basic health command permission
        if (!sender.hasPermission("lifesteal.command.health")) {
            return List.of();
        }

        if (args.length == 1) {
            // First argument: suggest player names (if has permission) and actions (if has permission)
            String input = args[0].toLowerCase();
            Stream<String> suggestions = Stream.of();

            // Add "help" option for everyone with basic permission
            suggestions = Stream.concat(suggestions, Stream.of("help").filter(s -> s.startsWith(input)));

            // Add player names for those with permission to check others
            if (sender.hasPermission("lifesteal.command.health.others")) {
                suggestions = Stream.concat(suggestions,
                    getOnlinePlayerNames().filter(name -> name.toLowerCase().startsWith(input)));
            }

            // Add modification actions for those with appropriate permissions
            if (sender.hasPermission("lifesteal.command.health.modify") ||
                sender.hasPermission("lifesteal.command.health.modify.others")) {
                suggestions = Stream.concat(suggestions,
                    ACTIONS.stream()
                        .filter(action -> !action.equals("help"))
                        .filter(action -> action.startsWith(input)));
            }

            return suggestions.sorted().toList();

        } else if (args.length == 2) {
            // Second argument: if first arg is an action, suggest numbers
            String action = args[0].toLowerCase();
            if (ACTIONS.contains(action) && !action.equals("help")) {
                // Only show if user has permission to modify health
                if (sender.hasPermission("lifesteal.command.health.modify") ||
                    sender.hasPermission("lifesteal.command.health.modify.others")) {
                    return List.of("<amount>");
                }
            }
            return List.of();
        } else if (args.length == 3) {
            // Third argument: suggest player names (for targeting) only if has permission
            if (sender.hasPermission("lifesteal.command.health.modify.others")) {
                String input = args[2].toLowerCase();
                return getOnlinePlayerNames()
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .sorted().toList();
            }
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
        sender.sendMessage(Component.text("------ Health Command Help ------", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/health help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/health", NamedTextColor.AQUA)
                .append(Component.text(" - View your current max health", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/health <player>", NamedTextColor.AQUA)
                .append(Component.text(" - View another player's max health", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/health set <health> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Set max health to a specific amount", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/health add <health> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Add to max health", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/health remove <health> [player]", NamedTextColor.AQUA)
                .append(Component.text(" - Remove from max health", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("Health range: " + MIN_HEALTH + " points and above", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GREEN));
    }

    /**
     * Handles health check for the sender if no arguments are provided.
     * Ensures the sender is a player and displays their current max health.
     *
     * @param sender The command sender.
     */
    private void handleSelfHealthCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "This command can only be used by players to check their own health.");
            return;
        }
        // Player is checking their own health
        sendHealthMessage(sender, player);
    }

    /**
     * Handles health check for a specific player when a player name is provided.
     * Validates that the target player exists and is online before displaying their health.
     *
     * @param sender     The command sender.
     * @param playerName The name of the player whose health is to be checked.
     */
    private void handlePlayerHealthCheck(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendError(sender, "Player '" + playerName + "' is not online or doesn't exist.");
            logger.info("Attempted to check health for non-existent player: " + playerName);
            return;
        }

        // Check permission to view other players' health
        if (!sender.equals(target) && !sender.hasPermission("lifesteal.command.health.others")) {
            sendError(sender, "You don't have permission to view other players' health.");
            return;
        }

        // Show the target player's health to the sender
        sendHealthMessage(sender, target);
    }

    /**
     * Processes health modification commands (set, add, remove).
     * Validates the action, parses the health amount, and identifies the target player.
     *
     * @param sender The command sender.
     * @param args   The command arguments.
     */
    private void handleHealthModification(CommandSender sender, String[] args) {
        String action = args[0].toLowerCase();
        if (!ACTIONS.contains(action)) {
            sendError(sender, "Invalid action '" + args[0] + "'. Valid actions are: set, add, remove, help.");
            return;
        }
        if (action.equals("help")) {
            sendHelpMessage(sender);
            return;
        }

        // Parse and validate health amount
        double healthInput;
        try {
            healthInput = Double.parseDouble(args[1]);
            if (healthInput <= 0) {
                sendError(sender, "Health amount must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            sendError(sender, "'" + args[1] + "' is not a valid number. Please use a numeric value.");
            logger.warning("Invalid number format provided: " + args[1]);
            return;
        }

        // Determine the target: specified player or self (if sender is a player)
        Player target;
        if (args.length == 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sendError(sender, "Player '" + args[2] + "' is not online or doesn't exist.");
                logger.warning("Invalid target player: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sendError(sender, "Console must specify a target player for this command.");
            return;
        }

        // Check permission for self-modification
        if (sender.equals(target) && !sender.hasPermission("lifesteal.command.health.modify")) {
            sendError(sender, "You don't have permission to modify your own health.");
            return;
        }

        // Check permission for modifying other players' health
        if (!sender.equals(target) && !sender.hasPermission("lifesteal.command.health.modify.others")) {
            sendError(sender, "You don't have permission to modify other players' health.");
            return;
        }

        // Execute the appropriate action
        switch (action) {
            case "set" -> setHealth(sender, target, healthInput);
            case "add" -> adjustHealth(sender, target, healthInput);
            case "remove" -> adjustHealth(sender, target, -healthInput);
        }
    }

    /**
     * Sets the target player's maximum health to a specific value.
     * Debug command with lower limit of greater than 0.
     *
     * @param sender The command sender.
     * @param target The target player.
     * @param health The new health value to set.
     */
    private void setHealth(CommandSender sender, Player target, double health) {
        // Enforce minimum health value
        if (health <= MIN_HEALTH) {
            sendError(sender, "Health cannot be set to 0 or lower. Please choose a positive value.");
            return;
        }

        // Capture the old health before making changes
        double oldHealth = LifeStealUtil.getMaxHealth(target);

        // Update the player's max health
        LifeStealUtil.setMaxHealth(target, health);

        // Display the old and new values
        sendHealthUpdate(sender, target, oldHealth, health);

        // Log the change for audit purposes
        logger.info(String.format("Health modified by %s: %s's health set from %.1f to %.1f",
            sender.getName(), target.getName(), oldHealth, health));
    }

    /**
     * Adjusts the target player's maximum health by adding or removing health points.
     * Debug command with lower limit check to prevent health from going to 0 or below.
     *
     * @param sender      The command sender.
     * @param target      The target player.
     * @param healthDelta The amount of health to add (positive) or remove (negative).
     */
    private void adjustHealth(CommandSender sender, Player target, double healthDelta) {
        // Capture the old health before making changes
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        double newHealth = oldHealth + healthDelta;

        // Enforce minimum health value
        if (newHealth <= MIN_HEALTH) {
            sendError(sender, "Cannot reduce health to 0 or lower. Current health: " +
                String.format("%.1f", oldHealth) + ", attempted change: " +
                String.format("%.1f", healthDelta));
            return;
        }

        // Apply the health change to the target player
        LifeStealUtil.adjustMaxHealth(target, healthDelta);

        // Display the old and new values
        sendHealthUpdate(sender, target, oldHealth, newHealth);

        // Log the change for audit purposes
        logger.info(String.format("Health modified by %s: %s's health changed from %.1f to %.1f (delta: %.1f)",
            sender.getName(), target.getName(), oldHealth, newHealth, healthDelta));
    }

    /**
     * Displays the target player's current health.
     *
     * @param viewer The command sender viewing the message.
     * @param target The player whose health is being displayed.
     */
    private void sendHealthMessage(CommandSender viewer, Player target) {
        double health = LifeStealUtil.getMaxHealth(target);
        String possessive = viewer.equals(target) ? "Your" : target.getName() + "'s";

        Component message = Component.text(possessive + " health: ", NamedTextColor.AQUA)
            .append(Component.text(String.format("%.1f health points", health), NamedTextColor.GOLD))
            .append(Component.text(" (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f hearts", health / 2.0), NamedTextColor.GREEN))
            .append(Component.text(")", NamedTextColor.GRAY));
        viewer.sendMessage(message);
    }

    /**
     * Notifies about a player's health update.
     *
     * @param sender    The command sender.
     * @param target    The target player whose health was updated.
     * @param oldHealth The previous health value.
     * @param newHealth The new health value.
     */
    private void sendHealthUpdate(CommandSender sender, Player target, double oldHealth, double newHealth) {
        if (Double.compare(newHealth, oldHealth) == 0) {
            Component noChangeMsg = Component.text("No change: ", NamedTextColor.YELLOW)
                .append(Component.text(target.equals(sender) ? "You are" : target.getName() + " is", NamedTextColor.AQUA))
                .append(Component.text(" still at ", NamedTextColor.YELLOW))
                .append(Component.text(String.format("%.1f health points", oldHealth), NamedTextColor.GOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f hearts", oldHealth / 2.0), NamedTextColor.GREEN))
                .append(Component.text(")", NamedTextColor.GRAY));
            sender.sendMessage(noChangeMsg);
            if (!sender.equals(target)) {
                target.sendMessage(noChangeMsg);
            }
            return;
        }

        String direction = newHealth > oldHealth ? "increased" : "decreased";
        NamedTextColor changeColor = newHealth > oldHealth ? NamedTextColor.GREEN : NamedTextColor.RED;

        Component targetMessage = Component.text("Your max health has been " + direction + " from ", NamedTextColor.AQUA)
            .append(Component.text(String.format("%.1f", oldHealth), NamedTextColor.GOLD))
            .append(Component.text(" to ", NamedTextColor.AQUA))
            .append(Component.text(String.format("%.1f health points", newHealth), changeColor))
            .append(Component.text(" (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f hearts", newHealth / 2.0), NamedTextColor.RED))
            .append(Component.text(")", NamedTextColor.GRAY));
        target.sendMessage(targetMessage);

        if (!sender.equals(target)) {
            Component senderMessage = Component.text(target.getName() + "'s max health has been " + direction + " from ", NamedTextColor.AQUA)
                .append(Component.text(String.format("%.1f", oldHealth), NamedTextColor.GOLD))
                .append(Component.text(" to ", NamedTextColor.AQUA))
                .append(Component.text(String.format("%.1f health points", newHealth), changeColor))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f hearts", newHealth / 2.0), NamedTextColor.RED))
                .append(Component.text(")", NamedTextColor.GRAY));
            sender.sendMessage(senderMessage);
        }
    }

    /**
     * Sends an error message to the command sender.
     *
     * @param sender  The command sender.
     * @param message The error message to send.
     */
    private void sendError(CommandSender sender, String message) {
        sender.sendMessage(Component.text("Error: " + message, NamedTextColor.RED));
    }

    /**
     * Retrieves the names of all currently online players as a Stream.
     *
     * @return A Stream of player names of currently online players.
     */
    private Stream<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName);
    }
}
