package me.honeyberries.lifeSteal.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.logging.Logger;

public class HealthCommand {

    private static final double MIN_HEALTH = Double.MIN_VALUE;
    private static final LifeSteal plugin = LifeSteal.getInstance();
    private static final Logger logger = plugin.getLogger();

    /**
     * Returns the built command tree for the health command.
     *
     * @return The command tree for the health command.
     */
    public static LiteralCommandNode<CommandSourceStack> getBuildCommand() {
        return Commands.literal("health")
            .requires(source -> source.getSender().hasPermission("lifesteal.command.health"))
            .executes(ctx -> {
                showSelfHealth(ctx);
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("help")
                .executes(ctx -> {
                    sendHelpMessage(ctx);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("view")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.view"))
                .executes(ctx -> {
                    showSelfHealth(ctx);
                    return Command.SINGLE_SUCCESS;
                })
                .then(
                    Commands.argument("player", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> startsWithIgnoreCase(name, builder.getRemaining()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .requires(source -> source.getSender().hasPermission("lifesteal.command.health.view.others"))
                        .executes(ctx -> {
                            showOtherHealth(ctx);
                            return Command.SINGLE_SUCCESS;
                        })
                )
            )
            .then(Commands.literal("set")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> {
                        setHealthSelf(ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> startsWithIgnoreCase(name, builder.getRemaining()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify.others"))
                        .executes(ctx -> {
                            setHealthOther(ctx);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(Commands.literal("add")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> {
                        addHealthSelf(ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> startsWithIgnoreCase(name, builder.getRemaining()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify.others"))
                        .executes(ctx -> {
                            addHealthOther(ctx);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
            .then(Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify"))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(Double.MIN_VALUE))
                    .executes(ctx -> {
                        removeHealthSelf(ctx);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> startsWithIgnoreCase(name, builder.getRemaining()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .requires(source -> source.getSender().hasPermission("lifesteal.command.health.modify.others"))
                        .executes(ctx -> {
                            removeHealthOther(ctx);
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                )
            )
        .build();
    }

    /**
     * Case-insensitive startsWith helper for suggestions.
     */
    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.toLowerCase().startsWith(prefix.toLowerCase());
    }


    /**
     * Handles the '/health' command showing the sender their own health.
     *
     * @param ctx the command context.
     */
    private static void showSelfHealth(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Messages.consolePlayerRequired());
            return;
        }
        sendHealthMessage(ctx.getSource().getSender(), player);
    }

    /**
     * Handles the '/health <player>' command to show another player's health.
     *
     * @param ctx the command context.
     */
    private static void showOtherHealth(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(Messages.playerNotFound(playerName));
            return;
        }
        sendHealthMessage(ctx.getSource().getSender(), target);
    }
    /**
     * Handles the '/health set <amount>' command for players setting their own health.
     *
     * @param ctx the command context.
     */
    private static void setHealthSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Messages.consolePlayerRequired());
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        setHealth(ctx.getSource().getSender(), player, amount);
    }

    /**
     * Handles the '/health set <amount> <player>' command to set another player's health.
     *
     * @param ctx the command context.
     */
    private static void setHealthOther(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(Messages.playerNotFound(playerName));
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        setHealth(ctx.getSource().getSender(), target, amount);
    }

    /**
     * Handles the '/health add <amount>' command to add health for the sender.
     *
     * @param ctx the command context.
     */
    private static void addHealthSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Messages.consolePlayerRequired());
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        adjustHealth(ctx.getSource().getSender(), player, amount);
    }

    /**
     * Handles the '/health add <amount> <player>' command to add health for another player.
     *
     * @param ctx the command context.
     */
    private static void addHealthOther(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(Messages.playerNotFound(playerName));
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        adjustHealth(ctx.getSource().getSender(), target, amount);
    }

    /**
     * Handles the '/health remove <amount>' command to remove health for the sender.
     *
     * @param ctx the command context.
     */
    private static void removeHealthSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Messages.consolePlayerRequired());
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        adjustHealth(ctx.getSource().getSender(), player, -amount);
    }

    /**
     * Handles the '/health remove <amount> <player>' command to remove health for another player.
     *
     * @param ctx the command context.
     */
    private static void removeHealthOther(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(Messages.playerNotFound(playerName));
            return;
        }
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        adjustHealth(ctx.getSource().getSender(), target, -amount);
    }



    /**
     * Sets a player's health to the specified value.
     *
     * @param sender the command sender.
     * @param target the player whose health is being set.
     * @param health the new health value.
     */
    private static void setHealth(CommandSender sender, Player target, double health) {
        if (health <= MIN_HEALTH) {
            sender.sendMessage(
                Component.text("Health cannot be set to 0 or lower.", NamedTextColor.RED)
            );
            return;
        }
    private static void setHealth(CommandSender sender, Player target, double health) {
        if (health <= MIN_HEALTH) {
            sender.sendMessage(Messages.healthCannotBeZero());
            return;
        }
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        LifeStealUtil.setMaxHealth(target, health);
        sendHealthUpdate(sender, target, oldHealth, health);
        logger.info(String.format("Health modified by %s: %s's health set from %.1f to %.1f",
            sender.getName(), target.getName(), oldHealth, health));
    }

    /**
     * Adjusts a player's health by the specified delta.
     *
     * @param sender the command sender.
     * @param target the player whose health is being adjusted.
     * @param delta  the amount to adjust health by.
     */
    private static void adjustHealth(CommandSender sender, Player target, double delta) {
        double oldHealth = LifeStealUtil.getMaxHealth(target);
        double newHealth = oldHealth + delta;
        if (newHealth <= MIN_HEALTH) {
            sender.sendMessage(Messages.healthCannotBeZero());
            return;
        }
        LifeStealUtil.setMaxHealth(target, newHealth);
        sendHealthUpdate(sender, target, oldHealth, newHealth);
        logger.info(String.format("Health modified by %s: %s's health changed from %.1f to %.1f (delta: %.1f)",
            sender.getName(), target.getName(), oldHealth, newHealth, delta));
    }

    /**
     * Sends the health message to the viewer about the target's health.
     *
     * @param viewer the command sender who views the message.
     * @param target the player whose health is being displayed.
     */
    private static void sendHealthMessage(CommandSender viewer, Player target) {
        double health = LifeStealUtil.getMaxHealth(target);
        String possessive = viewer.equals(target) ? "Your" : target.getName() + "'s";
        String healthPoints = String.format("%.1f", health);
        String hearts = String.format("%.1f", health / 2.0);
        viewer.sendMessage(Messages.healthView(possessive, healthPoints, hearts));
    }

    /**
     * Sends both the sender and target an update message after a health change.
     *
     * @param sender    the command sender initiating the change.
     * @param target    the player whose health was changed.
     * @param oldHealth the previous health value.
     * @param newHealth the new health value.
     */
    private static void sendHealthUpdate(CommandSender sender, Player target, double oldHealth, double newHealth) {
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
     * Sends the help message for the health command.
     *
     * @param ctx the command context.
     */
    private static void sendHelpMessage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.getSender().sendMessage(
            Component.text("------------ Health Command Help ------------", NamedTextColor.GREEN)
        );
        source.getSender().sendMessage(
            Component.text("/health help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health", NamedTextColor.AQUA)
                .append(Component.text(" - View your current max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health check", NamedTextColor.AQUA)
                .append(Component.text(" - View your current max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health check <player>", NamedTextColor.AQUA)
                .append(Component.text(" - View another player's max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health set <amount>", NamedTextColor.AQUA)
                .append(Component.text(" - Set your max health to a specific amount", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health set <amount> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Set another player's max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health add <amount>", NamedTextColor.AQUA)
                .append(Component.text(" - Add to your max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health add <amount> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Add to another player's max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health remove <amount>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove from your max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("/health remove <amount> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove from another player's max health", NamedTextColor.GOLD))
        );
        source.getSender().sendMessage(
            Component.text("Health range: " + MIN_HEALTH + " points and above", NamedTextColor.YELLOW)
        );
        source.getSender().sendMessage(
            Component.text("-----------------------------------------------", NamedTextColor.GREEN)
        );
    }

}