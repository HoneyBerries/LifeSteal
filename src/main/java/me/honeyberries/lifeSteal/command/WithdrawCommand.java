package me.honeyberries.lifeSteal.command;

import me.honeyberries.lifeSteal.util.LifeStealUtil;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles the /withdraw command in the LifeSteal plugin.
 * This command allows players or the console to withdraw a portion of a player's maximum health
 * and convert it into heart items that can be stored in inventory, dropped, or given to others.
 * <p>
 * The command integrates with the plugin's configuration settings to determine how much health
 * each heart item represents (default: 2.0 health points per item, equivalent to 1 Minecraft heart).
 */
public class WithdrawCommand implements TabExecutor {

    /**
     * Handles the execution of the /withdraw command with various argument patterns.
     * Supported command formats:
     * - /withdraw help: Displays the help message.
     * - /withdraw: Withdraws 1 heart item from the sender (if the sender is a player).
     * - /withdraw [amount]: Withdraws the specified number of heart items from the sender.
     * - /withdraw [amount] [player]: Withdraws heart items from the specified player.
     *
     * @param sender  The entity (player or console) that issued the command.
     * @param command The command object representing the command being executed.
     * @param label   The alias of the command that was used (e.g., "withdraw").
     * @param args    The arguments provided with the command (maybe empty).
     * @return true in all cases to indicate the command was handled.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if the sender has permission to use the command.
        if (!sender.hasPermission("lifesteal.withdraw")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        // Check if heart withdrawal is enabled in the server configuration.
        if (!LifeStealSettings.isAllowWithdraw()) {
            sender.sendMessage(Component.text("Heart withdrawal is disabled on this server.").color(NamedTextColor.RED));
            return true;
        }

        // Handle the "help" subcommand explicitly.
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        // Parse and validate the command arguments.
        CommandData data = parseCommand(sender, args);
        if (!data.isValid()) {
            // Show the help message if arguments are invalid.
            sendHelpMessage(sender);
            return true;
        }

        // Execute the heart withdrawal operation with validated parameters.
        withdrawHearts(sender, data.target, data.hearts);
        return true;
    }

    /**
     * Parses the command arguments to extract the target player and number of heart items to withdraw.
     * Handles multiple command formats and validates the arguments.
     *
     * @param sender The entity (player or console) that issued the command.
     * @param args   The arguments provided with the command.
     * @return A CommandData object containing the parsed target player and heart amount, or
     *         an invalid CommandData if arguments couldn't be parsed correctly.
     */
    private CommandData parseCommand(CommandSender sender, String[] args) {
        // Try to process the command in a single pass with minimal branching
        try {
            // Handle player validation once at the beginning
            Player senderPlayer = null;
            if (sender instanceof Player) {
                senderPlayer = (Player) sender;
            }

            // Parse command based on argument count
            switch (args.length) {
                case 0:
                    // No args - must be player withdrawing 1 heart
                    if (senderPlayer == null) {
                        sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
                        return CommandData.invalid();
                    }
                    return new CommandData(senderPlayer, 1);

                case 1:
                    // One arg - parse hearts amount
                    int hearts = Integer.parseInt(args[0]);
                    if (hearts <= 0) throw new NumberFormatException("Non-positive number");

                    if (senderPlayer == null) {
                        sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
                        return CommandData.invalid();
                    }
                    return new CommandData(senderPlayer, hearts);

                case 2:
                    // Two args - parse hearts and player name
                    hearts = Integer.parseInt(args[0]);
                    if (hearts <= 0) throw new NumberFormatException("Non-positive number");

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                        return CommandData.invalid();
                    }

                    // Check permission if targeting another player
                    if (senderPlayer != target && senderPlayer != null && !senderPlayer.hasPermission("lifesteal.withdraw.others")) {
                        sender.sendMessage(Component.text("You don't have permission to withdraw hearts from other players.").color(NamedTextColor.RED));
                        return CommandData.invalid();
                    }

                    return new CommandData(target, hearts);

                default:
                    // Too many arguments
                    sender.sendMessage(Component.text("Too many arguments. Usage: /withdraw [amount] [player]").color(NamedTextColor.RED));
                    return CommandData.invalid();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number. Please enter a positive number.").color(NamedTextColor.RED));
            return CommandData.invalid();
        }
    }

    /**
     * Executes the heart withdrawal operation for a player.
     * This method:
     * 1. Verifies the player has enough health to withdraw the requested amount.
     * 2. Creates and adds heart items to the player's inventory.
     * 3. Adjusts the player's maximum health.
     * 4. Handles overflow items if the inventory is full.
     * 5. Sends appropriate notifications.
     *
     * @param sender The command sender who initiated the withdrawal.
     * @param target The player whose health will be reduced to create heart items.
     * @param hearts The number of heart items to withdraw.
     */
    private void withdrawHearts(CommandSender sender, Player target, int hearts) {
        // Get the health value per heart item from the configuration (default: 2.0 health points per item).
        double healthPerItem = LifeStealSettings.getHealthPerItem();
        double healthPoints = hearts * healthPerItem;

        // Ensure the player will have at least 1 heart (2 health points) remaining after withdrawal.
        if (LifeStealUtil.getMaxHealth(target) - healthPoints < 2.0) {
            sender.sendMessage(Component.text(target.getName() + " doesn't have enough health to withdraw ")
                .append(Component.text(hearts + " " + (hearts == 1 ? "heart" : "hearts") + " (requires " + healthPoints + " hp)!", NamedTextColor.RED)));
            return;
        }

        // Create the heart items to be added to the player's inventory.
        ItemStack heartItem = LifeStealUtil.createHeartItem(hearts);

        // Try to add items to the inventory and track any items that couldn't fit.
        HashMap<Integer, ItemStack> remainingItems = target.getInventory().addItem(heartItem);

        // Calculate how many heart items were successfully added to the inventory.
        double itemsWithdrawn = hearts - remainingItems.values().stream()
                .mapToInt(ItemStack::getAmount).sum();

        if (itemsWithdrawn > 0) {
            // Only withdraw health for items that were successfully added to the inventory.
            double healthWithdrawn = itemsWithdrawn * healthPerItem;

            // Reduce the player's maximum health.
            LifeStealUtil.adjustMaxHealth(target, -healthWithdrawn);

            // Notify the player about the withdrawal.
            target.sendMessage(Component.text("You have withdrawn ")
                    .append(Component.text("%.1f %s (%.1f health points)".formatted(itemsWithdrawn, itemsWithdrawn == 1 ? "heart" : "hearts", healthWithdrawn), NamedTextColor.GREEN)));

            // Play a success sound effect.
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Handle any remaining items that couldn't fit in the inventory.
        if (!remainingItems.isEmpty()) {
            // Drop the remaining items on the ground near the player.
            remainingItems.values().forEach(item ->
                    target.getWorld().dropItemNaturally(target.getLocation(), item));

            // Notify the player about the dropped items.
            target.sendMessage(Component.text("Some " + (remainingItems.size() == 1 ? "heart item was" : "heart items were") + " dropped due to a full inventory!").color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Displays a formatted help message for the /withdraw command.
     * Shows available subcommands, syntax options, and brief descriptions.
     *
     * @param sender The entity to send the help message to.
     */
    private void sendHelpMessage(CommandSender sender) {
        double healthPerItem = LifeStealSettings.getHealthPerItem();
        double heartsPerItem = healthPerItem / 2.0; // Convert health points to hearts for display.

        sender.sendMessage(Component.text("------ Withdraw Command Help ------", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/withdraw help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw [hearts]", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items worth %.1f %s each (default 1).".formatted(heartsPerItem, heartsPerItem == 1 ? "heart" : "hearts"), NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw <hearts> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items from a specific player.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GREEN));
    }

    /**
     * Provides tab completion suggestions for the /withdraw command.
     * Suggests appropriate values based on the current argument position.
     *
     * @param sender  The entity requesting tab completion.
     * @param command The command being tab-completed.
     * @param label   The command alias used.
     * @param args    The current argument array.
     * @return A list of possible completion strings, or an empty list if no suggestions.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // First argument: suggest the "help" subcommand and common heart amounts.
            return Stream.of("help", "1", "2", "3", "4", "5")
                    .filter(s -> s.startsWith(args[0].toLowerCase())) // Filter suggestions based on current input
                    .toList();
        } else if (args.length == 2) {
            // Second argument: suggest online player names for targeting.
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        return List.of(); // No suggestions for other positions.
    }

    /**
     * Immutable data class to hold parsed and validated command parameters.
     * Encapsulates the target player and heart amount for withdrawal operations.
     */
    private record CommandData(Player target, int hearts) {
        /**
         * Creates an invalid CommandData object to represent failed parsing.
         * Using this pattern avoids null checks throughout the command handler.
         *
         * @return An invalid CommandData object with a null target.
         */
        static CommandData invalid() {
            return new CommandData(null, 0);
        }

        /**
         * Checks if this CommandData contains valid parameters for withdrawal.
         *
         * @return true if the target player is specified, false otherwise.
         */
        boolean isValid() {
            return target != null;
        }
    }
}
