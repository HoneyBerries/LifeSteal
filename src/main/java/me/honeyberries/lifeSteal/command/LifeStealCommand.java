package me.honeyberries.lifeSteal.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Handles the LifeSteal plugin commands using the Brigadier API.
 * Provides commands for reloading configuration, uninstalling the plugin, and displaying help.
 */
public class LifeStealCommand {

    // Reference to the main plugin instance
    private static final LifeSteal plugin = LifeSteal.getInstance();

    // Namespaced key for the custom heart recipe
    private static final NamespacedKey recipeKey = new NamespacedKey(plugin, "custom_heart_recipe");

    /**
     * Builds the LifeSteal command tree using the Brigadier API.
     *
     * @return A LiteralCommandNode representing the root of the LifeSteal command tree.
     */
    public static LiteralCommandNode<CommandSourceStack> getBuildCommand() {
        return Commands.literal("lifesteal")
            .requires(source -> source.getSender().hasPermission("lifesteal.command.lifesteal"))
            .executes(context -> {
                sendHelpMessage(context.getSource());
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.lifesteal"))
                .executes(context -> {
                    reloadConfig(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("uninstall")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.lifesteal"))
                .executes(context -> {
                    uninstallPlugin(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("help")
                .executes(context -> {
                    sendHelpMessage(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }

    /**
     * Sends the help message for the LifeSteal command to the command sender.
     *
     * @param source The command source (sender) to send the help message to.
     */
    private static void sendHelpMessage(CommandSourceStack source) {
        source.getSender().sendMessage(
            Component.text("----- Lifesteal Command Help -----").color(NamedTextColor.GREEN)
        );
        source.getSender().sendMessage(
            Component.text("/lifesteal reload").color(NamedTextColor.AQUA)
                .append(Component.text(" - Reload the plugin configuration.").color(NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/lifesteal uninstall").color(NamedTextColor.AQUA)
                .append(Component.text(" - Uninstall Lifesteal and reset player health.").color(NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/lifesteal help").color(NamedTextColor.AQUA)
                .append(Component.text(" - Show this help message.").color(NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("-------------------------------").color(NamedTextColor.GREEN)
        );
    }

    /**
     * Reloads the plugin configuration and notifies the sender.
     *
     * @param source The command source (sender).
     */
    private static void reloadConfig(CommandSourceStack source) {
        CompletableFuture<Void> reloadFuture = new CompletableFuture<>();

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                LifeStealSettings.loadConfig();
                Messages.loadMessages();
                reloadFuture.complete(null);
            } catch (Throwable throwable) {
                reloadFuture.completeExceptionally(throwable);
            }
        });

        try {
            reloadFuture.join();
            source.getSender().sendMessage(Messages.configReloaded());
        } catch (CompletionException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload LifeSteal configuration", exception.getCause());
            source.getSender().sendMessage(Component.text(
                "Failed to reload LifeSteal configuration. Check console for details.",
                NamedTextColor.RED
            ));
        }
    }

    /**
     * Uninstalls the plugin by resetting all players' health and removing the custom recipe.
     *
     * @param source The command source (sender).
     */
    private static void uninstallPlugin(CommandSourceStack source) {
        // Reset each player's health to the default maximum value
        for (Player player : Bukkit.getOnlinePlayers()) {
            LifeStealUtil.setMaxHealth(player,
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getDefaultValue());
        }
        Bukkit.removeRecipe(recipeKey);
        source.getSender().sendMessage(Messages.pluginUninstalled());
    }
}