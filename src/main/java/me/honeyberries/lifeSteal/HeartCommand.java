package me.honeyberries.lifeSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeartCommand implements TabExecutor {

    /**
     * Executes the /heart command to manage player health.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (args.length == 0) {
                if (sender instanceof Player player) {
                    handleSelfHealthCheck(sender, player);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command without arguments!");
                }
            } else if (args.length == 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player != null) {
                    handleSelfHealthCheck(sender, player);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid username! Player not found.");
                    LifeSteal.getInstance().getLogger().log(Level.WARNING, "Invalid username entered: " + args[0]);
                }
            } else if (args.length == 2) {
                if (sender instanceof Player player) {
                    handleHealthModification(sender, player, args);
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command without a player argument!");
                }
            } else if (args.length == 3) {
                Player player = Bukkit.getPlayer(args[2]);
                if (player != null) {
                    handleHealthModification(sender, player, args);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid username! Player not found.");
                    LifeSteal.getInstance().getLogger().warning("Invalid username entered: " + args[2]);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid usage! Try /heart [set|add|remove] <amount>");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command.");
            LifeSteal.getInstance().getLogger().log(Level.SEVERE, "Error executing /heart command", e);
        }
        return true;
    }

    /**
     * Provides tab completion options for the /heart command.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()); // Use Collectors.toList() to get a mutable list

            List<String> actions = Stream.of("set", "add", "remove")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()); // Use Collectors.toList() to get a mutable list

            actions.addAll(playerNames); // Now this will work since actions is mutable

            return actions;

        } else if (args.length == 2) {
            return List.of();

        } else if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Handles self health check when a player runs /heart.
     *
     * @param sender The command sender.
     * @param target The player checking their health.
     */
    private void handleSelfHealthCheck(@NotNull CommandSender sender, @NotNull Player target) {
        double health = LifeStealHelper.getMaxHealth(target);
        sender.sendMessage(ChatColor.GOLD + target.getName() + " has " + ChatColor.GREEN + health / 2 + ChatColor.GOLD + " maximum hearts!");
    }

    /**
     * Handles health modification commands (set, add, remove) for a player.
     *
     * @param sender The command sender.
     * @param target The player whose health is modified.
     * @param args   The command arguments.
     */
    private void handleHealthModification(@NotNull CommandSender sender, @NotNull Player target, @NotNull String[] args) {
        try {
            String action = args[0].toLowerCase();
            double amount = Double.parseDouble(args[1]) * 2;

            switch (action) {
                case "set" -> setHealth(sender, target, amount);
                case "add" -> adjustHealth(sender, target, amount);
                case "remove" -> adjustHealth(sender, target, -amount);
                default -> sender.sendMessage(ChatColor.RED + "Unknown action! Use: set, add, or remove!");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number! Use: /heart [set|add|remove] <amount>");
            LifeSteal.getInstance().getLogger().warning("Invalid number format: " + args[1]);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while modifying health.");
            LifeSteal.getInstance().getLogger().log(Level.SEVERE, "Error modifying health", e);
        }
    }

    /**
     * Sets a player's maximum health.
     *
     * @param sender    The command sender.
     * @param player    The player whose health is being set.
     * @param newHealth The new max health value.
     */
    public void setHealth(@NotNull CommandSender sender, @NotNull Player player, double newHealth) {
        if (newHealth < 2) {
            sender.sendMessage(ChatColor.RED + "Health must be at least 1 heart!");
            return;
        }
        LifeStealHelper.setMaxHealth(player, newHealth);
        if (sender != player) {
            sender.sendMessage(ChatColor.GOLD + player.getName() + "'s max health is now " + ChatColor.GREEN + LifeStealHelper.getMaxHealth(player) / 2 + ChatColor.GOLD + " hearts!");
        }
        player.sendMessage(ChatColor.GOLD + "Your max health is now " + ChatColor.GREEN + LifeStealHelper.getMaxHealth(player) / 2 + ChatColor.GOLD + " hearts!");
    }

    /**
     * Adjusts a player's maximum health by a specified amount.
     *
     * @param sender The command sender.
     * @param player The player whose health is being modified.
     * @param amount The amount to adjust health by.
     */
    public void adjustHealth(@NotNull CommandSender sender, @NotNull Player player, double amount) {
        double currentHealth = LifeStealHelper.getMaxHealth(player);
        double newHealth = currentHealth + amount;

        if (newHealth < 2) {
            sender.sendMessage(ChatColor.RED + "You can't have less than 1 heart!");
            return;
        }

        LifeStealHelper.adjustMaxHealth(player, amount);
        if (sender != player) {
            sender.sendMessage(ChatColor.GOLD + player.getName() + "'s max health is now " + ChatColor.GREEN + LifeStealHelper.getMaxHealth(player) / 2 + ChatColor.GOLD + " hearts!");
        }
        player.sendMessage(ChatColor.GOLD + "Your max health is now " + ChatColor.GREEN + LifeStealHelper.getMaxHealth(player) / 2 + ChatColor.GOLD + " hearts!");

    }
}