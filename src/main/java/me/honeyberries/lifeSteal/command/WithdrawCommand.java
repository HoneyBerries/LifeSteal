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
 * Handles the withdrawal command to convert a portion of players&apos; health into Heart items.
 * <p>
 * Usage:
 *   /withdraw help                   - Show help message.
 *   /withdraw [hearts]               - Withdraw the specified number of hearts for yourself.
 *   /withdraw [hearts] [player]      - Withdraw hearts for another player (requires extra permission).
 */
public class WithdrawCommand implements TabExecutor {

    /**
     * Processes the /withdraw command.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param label   The alias used.
     * @param args    The command arguments.
     * @return true to indicate the command has been processed.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Verify that the sender has basic permission for withdrawing hearts.
        if (!sender.hasPermission("lifesteal.withdraw")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        // Check if the server configuration allows withdrawal.
        if (!LifeStealSettings.isAllowWithdraw()) {
            sender.sendMessage(Component.text("Heart withdrawal is disabled on this server.").color(NamedTextColor.RED));
            return true;
        }

        // Display help when requested.
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        int hearts;      // Number of heart items to withdraw.
        Player target;   // Target player for heart withdrawal.

        // Determine the command usage based on the number of arguments.
        if (args.length == 0) {
            // Default to 1 heart for players executing the command.
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
                return true;
            }

            hearts = 1;
            target = (Player) sender;
        } else if (args.length == 1) {

            // Single argument: expects the number of hearts.
            hearts = parseHearts(sender, args[0]);
            if (hearts <= 0) return true;

             // Error message already sent in parseHearts
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;

        } else if (args.length == 2) {
            // Two arguments: first is the number of hearts, second is the target player name.
            hearts = parseHearts(sender, args[0]);
            if (hearts <= 0) return true;
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                return true;
            }

            // If sender is not the target, validate the extra permission.
            if (sender instanceof Player) {
                if (!((Player) sender).getUniqueId().equals(target.getUniqueId()) &&
                        !sender.hasPermission("lifesteal.withdraw.others")) {
                    sender.sendMessage(Component.text("You don't have permission to withdraw hearts from other players.").color(NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            // More than two arguments; output usage instructions.
            sendHelpMessage(sender);
            return true;
        }

        // Calculate the health required to withdraw the requested number of hearts.
        double healthPerItem = LifeStealSettings.getHealthPerItem();
        double requiredHealth = hearts * healthPerItem;
        double currentHealth = LifeStealUtil.getMaxHealth(target);

        // Ensure target player has health above minimum threshold (2.0)
        if (currentHealth - requiredHealth < LifeStealSettings.getMinHealthLimit()) {
            sender.sendMessage(
                Component.text(target.getName() + " doesn't have enough health to withdraw ")
                    .append(Component.text(hearts + " " + (hearts == 1 ? "heart" : "hearts") + " (requires " + requiredHealth + " hp)!", NamedTextColor.RED))
            );
            return true;
        }

        // Create the Heart item.
        ItemStack heartItem = LifeStealUtil.createHeartItem(hearts);

        // Try to add the Heart item(s) to the target player's inventory.
        HashMap<Integer, ItemStack> remainingItems = target.getInventory().addItem(heartItem);

        // Determine the number of items successfully withdrawn.
        double itemsWithdrawn = hearts - remainingItems.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (itemsWithdrawn > 0) {
            // Adjust the target player's max health.
            double healthWithdrawn = itemsWithdrawn * healthPerItem;
            LifeStealUtil.adjustMaxHealth(target, -healthWithdrawn);
            String content = String.format("%.1f", itemsWithdrawn) + " " + (itemsWithdrawn == 1 ? "heart" : "hearts") + " (" + healthWithdrawn + " health points)";
            sender.sendMessage(
                Component.text("You have withdrawn ")
                    .append(Component.text(content, NamedTextColor.GREEN))
            );
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // Notify the target player if the sender is not the same as the target.
            if (!sender.equals(target)) {
                target.sendMessage(
                    Component.text(sender.getName() + " has withdrawn ")
                        .append(Component.text(content, NamedTextColor.RED))
                );
            }
        }

        // If some items could not be added, drop them on the ground near the target.
        if (!remainingItems.isEmpty()) {
            remainingItems.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            target.sendMessage(Component.text("Some heart items were dropped due to a full inventory!").color(NamedTextColor.YELLOW));
        }
        return true;
    }

    /**
     * Parses the heart amount from the argument.
     *
     * @param sender    The command sender.
     * @param argument  The argument representing the number of hearts.
     * @return The number of hearts as an integer. Returns the minimum signed integer if parsing fails.
     */
    private int parseHearts(CommandSender sender, String argument) {
        int hearts;
        try {
            hearts = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number. Please enter a positive number.").color(NamedTextColor.RED));
            return Integer.MIN_VALUE;
        }
        if (hearts <= 0) {
            sender.sendMessage(Component.text("Please enter a positive number.").color(NamedTextColor.RED));
            return Integer.MIN_VALUE;
        }
        return hearts;
    }

    /**
     * Sends a help message detailing the withdrawal command usage.
     *
     * @param sender The command sender.
     */
    private void sendHelpMessage(CommandSender sender) {
        double healthPerItem = LifeStealSettings.getHealthPerItem();
        double heartsPerItem = healthPerItem / 2.0;
        sender.sendMessage(Component.text("------ Withdraw Command Help ------", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/withdraw help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw [hearts]", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items worth " + heartsPerItem + " " + (heartsPerItem == 1 ? "heart" : "hearts") + " each (default 1).", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw <hearts> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items from a specific player.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("-----------------------------------", NamedTextColor.GREEN));
    }

    /**
     * Provides suggestions for command tab completion.
     *
     * @param sender  The command sender.
     * @param command The command object.
     * @param label   The alias used.
     * @param args    The current command arguments.
     * @return A list of suggested completions.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("help", "1", "2", "3", "4", "5")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}