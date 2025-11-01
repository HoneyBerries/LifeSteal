package me.honeyberries.lifeSteal.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.util.EliminationManager;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Command handler for the /health command.
 *
 * <p>This command provides functionality to view, modify, and manage player health
 * in the LifeSteal plugin. It supports various subcommands for different operations:</p>
 *
 * <ul>
 *   <li>Viewing health (self and others)</li>
 *   <li>Setting absolute health values</li>
 *   <li>Adding/removing health incrementally</li>
 *   <li>Eliminating players</li>
 *   <li>Reviving eliminated players</li>
 * </ul>
 *
 * <p>All operations respect permission checks and provide user-friendly feedback.</p>
 *
 * @author honeyberries
 * @version 1.0
 * @since 1.0
 */
public class HealthCommand {

    /**
     * The minimum allowed health value for players.
     * Set to {@link Double#MIN_VALUE} to allow extremely low values.
     */
    private static final double MIN_HEALTH = Double.MIN_VALUE;

    /**
     * Instance of the main plugin class.
     */
    private static final LifeSteal plugin = LifeSteal.getInstance();

    /**
     * Logger instance for recording command operations.
     */
    private static final Logger logger = plugin.getLogger();

    /**
     * Suggestion provider for online players.
     * Filters suggestions based on the current input, case-insensitive.
     */
    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) -> {
        Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> startsWithIgnoreCase(name, builder.getRemaining()))
            .forEach(builder::suggest);
        return builder.buildFuture();
    };

    /**
     * Suggestion provider for eliminated players.
     * Filters suggestions based on the current input, case-insensitive.
     * Only suggests players who are currently eliminated.
     */
    private static final SuggestionProvider<CommandSourceStack> ELIMINATED_PLAYERS = (ctx, builder) -> {
        EliminationManager.getEliminatedPlayers().stream()
            .map(Bukkit::getOfflinePlayer)
            .map(org.bukkit.OfflinePlayer::getName)
            .filter(name -> name != null && startsWithIgnoreCase(name, builder.getRemaining()))
            .forEach(builder::suggest);
        return builder.buildFuture();
    };

    /**
     * Builds and returns the complete command tree for the /health command.
     *
     * <p>The command structure includes:</p>
     * <pre>
     * /health - View own health
     * /health help - Show help message
     * /health view [player] - View health
     * /health set &lt;amount&gt; [player] - Set health
     * /health add &lt;amount&gt; [player] - Add health
     * /health remove &lt;amount&gt; [player] - Remove health
     * /health eliminate &lt;player&gt; - Eliminate a player
     * /health revive &lt;player&gt; - Revive an eliminated player
     * </pre>
     *
     * @return The constructed command tree as a LiteralCommandNode
     */
    public static LiteralCommandNode<CommandSourceStack> getBuildCommand() {
        return Commands.literal("health")
            .requires(source -> source.getSender().hasPermission("lifesteal.command.health"))
            .executes(ctx -> executeCommand(ctx, HealthCommand::showSelfHealth))
            .then(Commands.literal("help")
                .executes(ctx -> executeCommand(ctx, HealthCommand::sendHelpMessage))
            )
            .then(Commands.literal("view")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.view"))
                .executes(ctx -> executeCommand(ctx, HealthCommand::showSelfHealth))
                .then(createPlayerArgument("lifesteal.command.health.view.others",
                    ONLINE_PLAYERS, HealthCommand::showOtherHealth))
            )
            .then(Commands.literal("set")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> executeCommand(ctx, HealthCommand::setHealthSelf))
                    .then(createPlayerArgument("lifesteal.command.health.modify.others",
                        ONLINE_PLAYERS, HealthCommand::setHealthOther))
                )
            )
            .then(Commands.literal("add")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> executeCommand(ctx, HealthCommand::addHealthSelf))
                    .then(createPlayerArgument("lifesteal.command.health.modify.others",
                        ONLINE_PLAYERS, HealthCommand::addHealthOther))
                )
            )
            .then(Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> executeCommand(ctx, HealthCommand::removeHealthSelf))
                    .then(createPlayerArgument("lifesteal.command.health.modify.others",
                        ONLINE_PLAYERS, HealthCommand::removeHealthOther))
                )
            )
            .then(Commands.literal("eliminate")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.eliminate"))
                .then(createPlayerArgument(null, ONLINE_PLAYERS, HealthCommand::eliminatePlayer))
            )
            .then(Commands.literal("revive")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.revive"))
                .then(createPlayerArgument(null, ELIMINATED_PLAYERS, HealthCommand::revivePlayer))
            )
            .build();
    }

    /**
     * Creates a player argument builder with optional permission check.
     *
     * <p>This helper method reduces code duplication when creating player arguments
     * throughout the command tree.</p>
     *
     * @param permission The permission required to execute this argument, or null for no requirement
     * @param suggestionProvider The provider for player name suggestions
     * @param executor The executor to run when this argument is invoked
     * @return A configured RequiredArgumentBuilder for a player argument
     */
    private static RequiredArgumentBuilder<CommandSourceStack, String> createPlayerArgument(
            String permission,
            SuggestionProvider<CommandSourceStack> suggestionProvider,
            CommandExecutor executor) {
        var builder = Commands.argument("player", StringArgumentType.word())
            .suggests(suggestionProvider)
            .executes(ctx -> executeCommand(ctx, executor));

        if (permission != null) {
            builder.requires(source -> source.getSender().hasPermission(permission));
        }
        return builder;
    }

    /**
     * Executes a command with a consistent return value.
     *
     * <p>This wrapper ensures all command executions return {@link Command#SINGLE_SUCCESS}
     * after the executor completes.</p>
     *
     * @param ctx The command context
     * @param executor The command executor to run
     * @return Always returns {@link Command#SINGLE_SUCCESS}
     */
    private static int executeCommand(CommandContext<CommandSourceStack> ctx, CommandExecutor executor) {
        executor.execute(ctx);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Functional interface for command execution logic.
     *
     * <p>This interface allows command handlers to be passed as method references,
     * simplifying the command registration code.</p>
     */
    @FunctionalInterface
    private interface CommandExecutor {
        /**
         * Executes the command logic.
         *
         * @param ctx The command context containing sender and arguments
         */
        void execute(CommandContext<CommandSourceStack> ctx);
    }

    /**
     * Checks if a string starts with a prefix, ignoring case.
     *
     * <p>Used for filtering command suggestions based on user input.</p>
     *
     * @param value The string to check
     * @param prefix The prefix to match
     * @return true if value starts with prefix (case-insensitive), false otherwise
     */
    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * Retrieves an online player from a command argument.
     *
     * <p>If the player is not found or is offline, sends an error message
     * to the command sender.</p>
     *
     * @param ctx The command context
     * @return The Player object if online, null otherwise
     */
    private static Player getOnlinePlayer(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Player '" + playerName + "' is not online.", NamedTextColor.RED)
            );
        }
        return target;
    }

    /**
     * Ensures the command sender is a player.
     *
     * <p>If the sender is not a player (e.g., console), sends an error message.</p>
     *
     * @param ctx The command context
     * @return The Player if sender is a player, null otherwise
     */
    private static Player requirePlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Console must specify a player.", NamedTextColor.RED)
            );
            return null;
        }
        return player;
    }

    /**
     * Displays the sender's own health.
     *
     * <p>Command: {@code /health} or {@code /health view}</p>
     *
     * @param ctx The command context
     */
    private static void showSelfHealth(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player != null) {
            sendHealthMessage(ctx.getSource().getSender(), player);
        }
    }

    /**
     * Displays another player's health.
     *
     * <p>Command: {@code /health view <player>}</p>
     *
     * @param ctx The command context containing the target player argument
     */
    private static void showOtherHealth(CommandContext<CommandSourceStack> ctx) {
        Player target = getOnlinePlayer(ctx);
        if (target != null) {
            sendHealthMessage(ctx.getSource().getSender(), target);
        }
    }

    /**
     * Sets the sender's health to a specific value.
     *
     * <p>Command: {@code /health set <amount>}</p>
     *
     * @param ctx The command context containing the amount argument
     */
    private static void setHealthSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            setHealth(ctx.getSource().getSender(), player, amount);
        }
    }

    /**
     * Sets another player's health to a specific value.
     *
     * <p>Command: {@code /health set <amount> <player>}</p>
     *
     * @param ctx The command context containing amount and player arguments
     */
    private static void setHealthOther(CommandContext<CommandSourceStack> ctx) {
        Player target = getOnlinePlayer(ctx);
        if (target != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            setHealth(ctx.getSource().getSender(), target, amount);
        }
    }

    /**
     * Adds health to the sender's current health.
     *
     * <p>Command: {@code /health add <amount>}</p>
     *
     * @param ctx The command context containing the amount argument
     */
    private static void addHealthSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            adjustHealth(ctx.getSource().getSender(), player, amount);
        }
    }

    /**
     * Adds health to another player's current health.
     *
     * <p>Command: {@code /health add <amount> <player>}</p>
     *
     * @param ctx The command context containing amount and player arguments
     */
    private static void addHealthOther(CommandContext<CommandSourceStack> ctx) {
        Player target = getOnlinePlayer(ctx);
        if (target != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            adjustHealth(ctx.getSource().getSender(), target, amount);
        }
    }

    /**
     * Removes health from the sender's current health.
     *
     * <p>Command: {@code /health remove <amount>}</p>
     *
     * @param ctx The command context containing the amount argument
     */
    private static void removeHealthSelf(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            adjustHealth(ctx.getSource().getSender(), player, -amount);
        }
    }

    /**
     * Removes health from another player's current health.
     *
     * <p>Command: {@code /health remove <amount> <player>}</p>
     *
     * @param ctx The command context containing amount and player arguments
     */
    private static void removeHealthOther(CommandContext<CommandSourceStack> ctx) {
        Player target = getOnlinePlayer(ctx);
        if (target != null) {
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            adjustHealth(ctx.getSource().getSender(), target, -amount);
        }
    }

    /**
     * Eliminates a player from the game.
     *
     * <p>Command: {@code /health eliminate <player>}</p>
     *
     * <p>This command removes a player from active gameplay, typically used
     * when they run out of hearts. Eliminated players can be revived using
     * the revive command.</p>
     *
     * @param ctx The command context containing the player argument
     */
    private static void eliminatePlayer(CommandContext<CommandSourceStack> ctx) {
        Player target = getOnlinePlayer(ctx);
        if (target == null) return;

        // Check if player is already eliminated
        if (EliminationManager.isEliminated(target)) {
            ctx.getSource().getSender().sendMessage(
                Component.text(target.getName() + " is already eliminated.", NamedTextColor.RED)
            );
            return;
        }

        // Perform elimination
        EliminationManager.eliminatePlayer(target);
        ctx.getSource().getSender().sendMessage(
            Component.text("Successfully eliminated ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                .append(Component.text(".", NamedTextColor.GREEN))
        );
        logger.info(ctx.getSource().getSender().getName() + " eliminated player " + target.getName());
    }

    /**
     * Revives an eliminated player.
     *
     * <p>Command: {@code /health revive <player>}</p>
     *
     * <p>This command restores a previously eliminated player, allowing them
     * to rejoin active gameplay. Broadcasts a message to all players upon
     * successful revival.</p>
     *
     * @param ctx The command context containing the player argument
     */
    private static void revivePlayer(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");

        OfflinePlayer offlinePlayer = EliminationManager.getEliminatedPlayers().stream()
            .map(Bukkit::getOfflinePlayer)
            .filter(op -> playerName.equalsIgnoreCase(op.getName()))
            .findFirst()
            .orElseGet(() -> {
                ctx.getSource().getSender().sendMessage(
                    Component.text("Player '" + playerName + "' is not eliminated or does not exist.", NamedTextColor.RED)
                );
                return null;
            });

        if (offlinePlayer == null) return;

        // Attempt revival
        if (EliminationManager.revivePlayer(offlinePlayer)) {
            ctx.getSource().getSender().sendMessage(
                Component.text("Successfully revived ", NamedTextColor.GREEN)
                    .append(Component.text(Objects.requireNonNull(offlinePlayer.getName()), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN))
            );

            // Broadcast to all players
            Bukkit.broadcast(Component.text(offlinePlayer.getName() + " has been revived!", NamedTextColor.GREEN));
            logger.info(ctx.getSource().getSender().getName() + " revived player " + offlinePlayer.getName());
        } else {
            ctx.getSource().getSender().sendMessage(
                Component.text("Failed to revive " + offlinePlayer.getName() + ".", NamedTextColor.RED)
            );
        }
    }

    /**
     * Sets a player's maximum health to an absolute value.
     *
     * <p>Validates that the new health is above the minimum threshold,
     * updates the player's health, sends feedback messages, and logs the change.</p>
     *
     * @param sender The command sender who initiated the change
     * @param target The player whose health is being modified
     * @param health The new absolute health value
     */
    private static void setHealth(CommandSender sender, Player target, double health) {
        if (health <= MIN_HEALTH) {
            sender.sendMessage(Component.text("Health cannot be set to 0 or lower.", NamedTextColor.RED));
            return;
        }
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        LifeStealUtil.setMaxHealth(target, health);
        sendHealthUpdate(sender, target, oldHealth, health);
        logger.info(String.format("Health modified by %s: %s's health set from %.1f to %.1f",
            sender.getName(), target.getName(), oldHealth, health));
    }

    /**
     * Adjusts a player's maximum health by a delta value.
     *
     * <p>Calculates the new health by adding the delta to current health,
     * validates it's above minimum, updates the health, and provides feedback.</p>
     *
     * @param sender The command sender who initiated the change
     * @param target The player whose health is being modified
     * @param delta The amount to add (positive) or subtract (negative) from current health
     */
    private static void adjustHealth(CommandSender sender, Player target, double delta) {
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        double newHealth = oldHealth + delta;

        if (newHealth <= MIN_HEALTH) {
            sender.sendMessage(
                Component.text("Cannot reduce health to 0 or lower. Current health: " +
                    String.format("%.1f", oldHealth) + ", attempted change: " +
                    String.format("%.1f", delta), NamedTextColor.RED)
            );
            return;
        }

        LifeStealUtil.setMaxHealth(target, newHealth);
        sendHealthUpdate(sender, target, oldHealth, newHealth);
        logger.info(String.format("Health modified by %s: %s's health changed from %.1f to %.1f (delta: %.1f)",
            sender.getName(), target.getName(), oldHealth, newHealth, delta));
    }

    /**
     * Sends a formatted health information message to a viewer.
     *
     * <p>Displays the target's current maximum health in both health points
     * and hearts (half of health points).</p>
     *
     * @param viewer The command sender viewing the health information
     * @param target The player whose health is being displayed
     */
    private static void sendHealthMessage(CommandSender viewer, Player target) {
        double health = LifeStealUtil.getMaxHealth(target);
        String possessive = viewer.equals(target) ? "Your" : target.getName() + "'s";
        viewer.sendMessage(
            Component.text(possessive + " health: ", NamedTextColor.AQUA)
                .append(Component.text(String.format("%.1f health points", health), NamedTextColor.GOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f hearts", health / 2.0), NamedTextColor.GREEN))
                .append(Component.text(")", NamedTextColor.GRAY))
        );
    }

    /**
     * Sends health update messages to both the command sender and target player.
     *
     * <p>If health hasn't changed, sends a "no change" message. Otherwise,
     * sends appropriate increase/decrease messages with color coding.</p>
     *
     * @param sender The command sender who initiated the change
     * @param target The player whose health was modified
     * @param oldHealth The previous health value
     * @param newHealth The new health value
     */
    private static void sendHealthUpdate(CommandSender sender, Player target, double oldHealth, double newHealth) {
        // Handle no-change scenario
        if (Double.compare(newHealth, oldHealth) == 0) {
            Component noChangeMsg = buildHealthMessage(target, sender, oldHealth);
            sender.sendMessage(noChangeMsg);
            if (!sender.equals(target)) {
                target.sendMessage(noChangeMsg);
            }
            return;
        }

        // Determine change direction and color
        String direction = newHealth > oldHealth ? "increased" : "decreased";
        NamedTextColor changeColor = newHealth > oldHealth ? NamedTextColor.GREEN : NamedTextColor.RED;

        // Send message to target
        Component targetMessage = buildHealthChangeMessage(direction, oldHealth, newHealth, changeColor);
        target.sendMessage(targetMessage);

        // Send message to sender if different from target
        if (!sender.equals(target)) {
            Component senderMessage = buildHealthChangeMessage(direction, oldHealth, newHealth, changeColor)
                .replaceText(config -> config.matchLiteral("Your").replacement(target.getName() + "'s"));
            sender.sendMessage(senderMessage);
        }
    }

    /**
     * Builds a health status message (for no-change scenarios).
     *
     * @param target The player whose health is displayed
     * @param viewer The command sender viewing the message
     * @param health The current health value
     * @return A formatted Component message
     */
    private static Component buildHealthMessage(Player target, CommandSender viewer, double health) {
        String subject = target.equals(viewer) ? "You are" : target.getName() + " is";
        return Component.text("No change: ", NamedTextColor.YELLOW)
            .append(Component.text(subject, NamedTextColor.AQUA))
            .append(Component.text(" " + "still at" + " ", NamedTextColor.YELLOW))
            .append(Component.text(String.format("%.1f health points", health), NamedTextColor.GOLD))
            .append(Component.text(" (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f hearts", health / 2.0), NamedTextColor.GREEN))
            .append(Component.text(")", NamedTextColor.GRAY));
    }

    /**
     * Builds a health change message showing old and new values.
     *
     * @param direction The direction of change ("increased" or "decreased")
     * @param oldHealth The previous health value
     * @param newHealth The new health value
     * @param changeColor The color representing the change (green for increase, red for decrease)
     * @return A formatted Component message
     */
    private static Component buildHealthChangeMessage(String direction, double oldHealth, double newHealth, NamedTextColor changeColor) {
        return Component.text("Your max health has been " + direction + " from ", NamedTextColor.AQUA)
            .append(Component.text(String.format("%.1f", oldHealth), NamedTextColor.GOLD))
            .append(Component.text(" to ", NamedTextColor.AQUA))
            .append(Component.text(String.format("%.1f health points", newHealth), changeColor))
            .append(Component.text(" (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f hearts", newHealth / 2.0), NamedTextColor.RED))
            .append(Component.text(")", NamedTextColor.GRAY));
    }

    /**
     * Sends a comprehensive help message listing all available commands.
     *
     * <p>Command: {@code /health help}</p>
     *
     * <p>Displays usage information for all subcommands with descriptions
     * and the minimum health constraint.</p>
     *
     * @param ctx The command context
     */
    private static void sendHelpMessage(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("------------ Health Command Help ------------", NamedTextColor.GREEN));

        // Command usage and description pairs
        String[][] commands = {
            {"/health help", "Shows this help message"},
            {"/health", "View your current max health"},
            {"/health check", "View your current max health"},
            {"/health check <player>", "View another player's max health"},
            {"/health set <amount>", "Set your max health to a specific amount"},
            {"/health set <amount> <player>", "Set another player's max health"},
            {"/health add <amount>", "Add to your max health"},
            {"/health add <amount> <player>", "Add to another player's max health"},
            {"/health remove <amount>", "Remove from your max health"},
            {"/health remove <amount> <player>", "Remove from another player's max health"},
            {"/health eliminate <player>", "Eliminate a player from the game"},
            {"/health revive <player>", "Revive an eliminated player"}
        };

        // Send each command with its description
        for (String[] command : commands) {
            sender.sendMessage(
                Component.text(command[0], NamedTextColor.AQUA)
                    .append(Component.text(" - " + command[1], NamedTextColor.GOLD))
            );
        }

        // Display health constraints
        sender.sendMessage(Component.text("Health range: " + MIN_HEALTH + " points and above", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("-----------------------------------------------", NamedTextColor.GREEN));
    }
}