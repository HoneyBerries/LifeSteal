package me.honeyberries.lifeSteal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/**
 * Handles the Withdraw command for the LifeSteal plugin.
 * Allows players to withdraw "Heart" items by decreasing their maximum health.
 */
public class WithdrawCommand implements CommandExecutor, TabExecutor {

    /**
     * Executes the withdraw command.
     *
     * @param sender The entity that executed the command.
     * @param command The command that was executed.
     * @param label The alias of the command used.
     * @param args The arguments passed to the command.
     * @return true if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            int quantity = 1; // Default quantity of hearts to withdraw

            // Check if an argument is provided and try to parse it
            if (args.length > 0) {
                try {
                    quantity = Integer.parseInt(args[0]);
                    if (quantity <= 0) {
                        player.sendMessage(Component.text("Please enter a positive number.").color(NamedTextColor.RED));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid number. Please enter a valid integer.").color(NamedTextColor.RED));
                    return true;
                }
            }

            // Check if the player has enough health to withdraw the specified number of hearts
            if (LifeStealHelper.getMaxHealth(player) - 2 * quantity >= 2) {
                // Decrease the player's max health by the appropriate amount
                LifeStealHelper.adjustMaxHealth(player, -2 * quantity);

                // Create a custom Heart item with the specified quantity
                ItemStack heartItem = LifeStealHelper.createHeartItem(quantity);

                // Try to add the heart item to the player's inventory
                HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(heartItem);

                // Drop any remaining items if the inventory is full
                for (ItemStack item : remainingItems.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }

                // Send a success message to the player
                player.sendMessage(Component.text("You have withdrawn ").color(NamedTextColor.GOLD)
                        .append(Component.text(quantity + " Heart" + (quantity > 1 ? "s" : "")).color(NamedTextColor.GREEN)));

            } else {
                // If the player doesn't have enough hearts to withdraw, send an error message
                player.sendMessage(Component.text("You don't have enough hearts to withdraw " + quantity + " hearts!").color(NamedTextColor.RED));
            }

        } else {
            // If the sender is not a player, send a message saying the command can only be used by players
            sender.sendMessage(Component.text("You must be a player to use the command!").color(NamedTextColor.RED));
        }
        return true;
    }

    /**
     * Handles tab completion for the withdraw command. Currently, no tab completion is implemented.
     *
     * @param sender The entity that initiated the tab completion.
     * @param command The command being tab completed.
     * @param label The alias of the command used.
     * @param args The arguments passed to the command.
     * @return A list of possible tab completions, or null if no suggestions are available.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
