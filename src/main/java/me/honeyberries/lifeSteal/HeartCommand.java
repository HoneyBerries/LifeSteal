package me.honeyberries.lifeSteal;

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
                    sender.sendMessage(Component.text("Only players can use this command without arguments!").color(NamedTextColor.RED));
                }
            } else if (args.length == 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player != null) {
                    handleSelfHealthCheck(sender, player);
                } else {
                    sender.sendMessage(Component.text("Invalid username! Player not found.").color(NamedTextColor.RED));
                    LifeSteal.getInstance().getLogger().log(Level.WARNING, "Invalid username entered: " + args[0]);
                }
            } else if (args.length == 2) {
                if (sender instanceof Player player) {
                    handleHealthModification(sender, player, args);
                } else {
                    sender.sendMessage(Component.text("Only players can use this command without a player argument!").color(NamedTextColor.RED));
                }
            } else if (args.length == 3) {
                Player player = Bukkit.getPlayer(args[2]);
                if (player != null) {
                    handleHealthModification(sender, player, args);
                } else {
                    sender.sendMessage(Component.text("Invalid username! Player not found.").color(NamedTextColor.RED));
                    LifeSteal.getInstance().getLogger().warning("Invalid username entered: " + args[2]);
                }
            } else {
                sender.sendMessage(Component.text("Invalid usage! Try /heart [set|add|remove] <amount>").color(NamedTextColor.RED));
            }
        } catch (Exception e) {
            sender.sendMessage(Component.text("An error occurred while executing the command.").color(NamedTextColor.RED));
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
            return Stream.concat(
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())),
                    Stream.of("set", "add", "remove")
                            .filter(option -> option.startsWith(args[0].toLowerCase()))
            ).toList();

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
        sender.sendMessage(Component.text(target.getName() + " has " + health / 2 + " maximum hearts!").color(NamedTextColor.GOLD)
                .append(Component.text(" (" + health / 2 + " hearts)").color(NamedTextColor.GREEN)));
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
                default -> sender.sendMessage(Component.text("Unknown action! Use: set, add, or remove!").color(NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number! Use: /heart [set|add|remove] <amount>").color(NamedTextColor.RED));
            LifeSteal.getInstance().getLogger().warning("Invalid number format: " + args[1]);
        } catch (Exception e) {
            sender.sendMessage(Component.text("An error occurred while modifying health.").color(NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Health must be at least 1 heart!").color(NamedTextColor.RED));
            return;
        }
        LifeStealHelper.setMaxHealth(player, newHealth);
        if (sender != player) {
            sender.sendMessage(Component.text(player.getName() + "'s max health is now " + LifeStealHelper.getMaxHealth(player) / 2 + " hearts!").color(NamedTextColor.GOLD)
                    .append(Component.text(" (Set to " + LifeStealHelper.getMaxHealth(player) / 2 + " hearts)").color(NamedTextColor.GREEN)));
        }
        player.sendMessage(Component.text("Your max health is now " + LifeStealHelper.getMaxHealth(player) / 2 + " hearts!").color(NamedTextColor.GOLD));
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
            sender.sendMessage(Component.text("You can't have less than 1 heart!").color(NamedTextColor.RED));
            return;
        }

        LifeStealHelper.adjustMaxHealth(player, amount);
        if (sender != player) {
            sender.sendMessage(Component.text(player.getName() + "'s max health is now " + LifeStealHelper.getMaxHealth(player) / 2 + " hearts!").color(NamedTextColor.GOLD)
                    .append(Component.text(" (Adjusted by " + amount / 2 + " hearts)").color(NamedTextColor.GREEN)));
        }
        player.sendMessage(Component.text("Your max health is now " + LifeStealHelper.getMaxHealth(player) / 2 + " hearts!").color(NamedTextColor.GOLD));
    }
}

    /**
     just changing to test rn
     */
