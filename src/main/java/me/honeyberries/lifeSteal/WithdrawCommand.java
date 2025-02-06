package me.honeyberries.lifeSteal;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WithdrawCommand implements CommandExecutor, TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            int quantity = 1; // Default quantity

            // Check if an argument is provided
            if (args.length > 0) {
                try {
                    quantity = Integer.parseInt(args[0]);
                    if (quantity <= 0) {
                        player.sendMessage(ChatColor.RED + "Please enter a positive number.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number. Please enter a valid integer.");
                    return true;
                }
            }

            // Remove the hearts from the player. Makes sure they have enough hearts to withdraw.
            if (LifeStealHelper.getMaxHealth(player) - 2 * quantity >= 2) {
                LifeStealHelper.adjustMaxHealth(player, -2 * quantity);
                // Create the custom "Heart" item with the specified quantity
                ItemStack heartItem = LifeStealHelper.createHeartItem(quantity);

                // Add the item to the player's inventory
                player.getInventory().addItem(heartItem);

                // Send the message to the player
                player.sendMessage(ChatColor.GOLD + "You have withdrawn " + ChatColor.GREEN + quantity + ChatColor.GOLD + " Heart" + (quantity > 1 ? "s" : "") + ".");

            }

            // Not enough hearts to withdraw
            else {
                player.sendMessage(ChatColor.RED + "You don't have enough hearts to withdraw " + ChatColor.GREEN + quantity + ChatColor.RED + " hearts!");
            }

        }

        else {
            sender.sendMessage(ChatColor.RED + "You must be a player to use the command!");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }

}
