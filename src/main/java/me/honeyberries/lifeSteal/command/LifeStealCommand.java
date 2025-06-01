package me.honeyberries.lifeSteal.command;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles the `/lifesteal` command, which provides administrative functionality for the LifeSteal plugin.
 * <p>
 * Command formats:
 * - /lifesteal reload - Reloads the plugin configuration.
 * - /lifesteal help - Displays the help message.
 */
public class LifeStealCommand implements TabExecutor {

    // Plugin instance for accessing configuration and utilities
    private final LifeSteal plugin = LifeSteal.getInstance();

    /**
     * Main entry point for the `/lifesteal` command.
     * Handles subcommands like "reload" and displays help if no valid subcommand is provided.
     * 
     * @param sender  The command sender.
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    The command arguments.
     * @return true to signal that the command was processed.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lifesteal.command.reload")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            LifeStealSettings.loadConfig();
            sender.sendMessage(Component.text("LifeSteal configuration reloaded successfully!").color(NamedTextColor.GREEN));
            return true;
        } 
        else if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }
        else {
            // If no arguments or unknown command, show help
            sendHelpMessage(sender);
            return true;
        }
    }
    
    /**
     * Displays the help message for the LifeSteal command.
     * 
     * @param sender The command sender who will see the help message.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("----- Lifesteal Command Help -----").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/lifesteal reload").color(NamedTextColor.AQUA)
                .append(Component.text(" - Reload the plugin configuration.").color(NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/lifesteal help").color(NamedTextColor.AQUA)
                .append(Component.text(" - Show this help message.").color(NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("-------------------------------").color(NamedTextColor.GREEN));
    }

    /**
     * Provides tab-completion suggestions for the `/lifesteal` command.
     * Suggests valid subcommands based on the argument position.
     * 
     * @param sender  The command sender.
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    The command arguments so far.
     * @return A list of possible suggestions based on argument position.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "help");
        }
        return List.of();
    }
}
