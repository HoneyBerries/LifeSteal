package me.honeyberries.lifeSteal.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Handles the "withdraw" command, allowing players to withdraw health as heart items.
 */
public class WithdrawCommand {

    /**
     * Builds the Brigadier command tree for the "withdraw" command.
     *
     * @return The root node of the "withdraw" command.
     */
    public static LiteralCommandNode<CommandSourceStack> getBuildCommand() {
        return Commands.literal("withdraw")
            .requires(source -> source.getSender().hasPermission("lifesteal.command.withdraw"))
            .executes(ctx -> {
                withdrawHeartsFromSelf(ctx, 1);
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("help")
                .executes(ctx -> {
                    sendHelpMessage(ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.argument("hearts", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                .executes(ctx -> {
                    int hearts = IntegerArgumentType.getInteger(ctx, "hearts");
                    withdrawHeartsFromSelf(ctx, hearts);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("player", StringArgumentType.string())
                    .suggests((ctx, builder) -> {
                        Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .requires(source -> source.getSender().hasPermission("lifesteal.command.withdraw.others"))
                    .executes(ctx -> {
                        int hearts = IntegerArgumentType.getInteger(ctx, "hearts");
                        Player target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "player"));
                        withdrawHeartsFromOthers(ctx, hearts, target);
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
        .build();
    }

    /**
     * Handles withdrawing hearts for the command sender (self).
     *
     * @param ctx    The command context.
     * @param hearts The number of hearts to withdraw.
     */
    private static void withdrawHeartsFromSelf(CommandContext<CommandSourceStack> ctx, int hearts) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Console must specify a player.").color(NamedTextColor.RED));
            return;
        }
        processHeartWithdraw(sender, player, hearts);
    }

    /**
     * Handles withdrawing hearts from another player.
     *
     * @param ctx    The command context.
     * @param hearts The number of hearts to withdraw.
     * @param target The target player to withdraw hearts from.
     */
    private static void withdrawHeartsFromOthers(CommandContext<CommandSourceStack> ctx, int hearts, Player target) {
        CommandSender sender = ctx.getSource().getSender();
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or not online.").color(NamedTextColor.RED));
            return;
        }
        processHeartWithdraw(sender, target, hearts);
    }

    /**
     * Processes the heart withdrawal logic for a player.
     *
     * @param sender The command sender.
     * @param target The target player whose hearts are being withdrawn.
     * @param hearts The number of hearts to withdraw.
     */
    private static void processHeartWithdraw(CommandSender sender, Player target, int hearts) {
        if (!LifeStealSettings.isWithdrawEnabled()) {
            sender.sendMessage(Component.text("Heart withdrawal is disabled on this server.").color(NamedTextColor.RED));
            return;
        }
        double healthPerItem = LifeStealSettings.getHeartHpRestored();
        double requiredHealth = hearts * healthPerItem;
        double currentHealth = LifeStealUtil.getMaxHealth(target);

        if (currentHealth - requiredHealth < LifeStealSettings.getMinHealth()) {
            sender.sendMessage(
                Component.text(target.getName() + " doesn't have enough health to withdraw ")
                    .append(Component.text(hearts + " " + (hearts == 1 ? "heart" : "hearts") + " (requires " + requiredHealth / 2 + " hearts)!", NamedTextColor.RED))
            );
            return;
        }

        ItemStack heartItem = LifeStealUtil.createHeartItem(hearts);
        HashMap<Integer, ItemStack> remainingItems = target.getInventory().addItem(heartItem);
        LifeStealUtil.adjustMaxHealth(target, -requiredHealth);

        String content = String.format("%d %s (%d health points)", hearts, (hearts == 1 ? "heart" : "hearts"), hearts * 2);
        sender.sendMessage(
            Component.text("You have withdrawn ").color(NamedTextColor.GOLD)
                .append(Component.text(content, NamedTextColor.GREEN))
        );
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (!sender.equals(target)) {
            target.sendMessage(
                Component.text(sender.getName() + " has withdrawn ")
                    .append(Component.text(content, NamedTextColor.RED))
                    .append(Component.text(" from you!", NamedTextColor.RED))
            );
        }

        if (!remainingItems.isEmpty()) {
            remainingItems.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            target.sendMessage(Component.text("Warning: Some heart items were dropped due to a full inventory!").color(NamedTextColor.YELLOW));
        }
    }

    /**
     * Sends the help message for the "withdraw" command.
     *
     * @param sender The command sender.
     */
    private static void sendHelpMessage(CommandSender sender) {
        double heartsPerItem = LifeStealSettings.getHeartHpRestored() / 2.0; // Convert health to hearts

        sender.sendMessage(Component.text("---------- Withdraw Command Help ----------", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("/withdraw help", NamedTextColor.AQUA)
                .append(Component.text(" - Shows this help message.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw <hearts>", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items worth " + heartsPerItem + " " + (heartsPerItem == 1 ? "heart" : "hearts") + " each (default 1).", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("/withdraw <hearts> <player>", NamedTextColor.AQUA)
                .append(Component.text(" - Withdraws heart items from a specific player.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("-------------------------------------------", NamedTextColor.GREEN));
    }
}