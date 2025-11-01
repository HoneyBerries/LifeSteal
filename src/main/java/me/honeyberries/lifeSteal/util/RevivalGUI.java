package me.honeyberries.lifeSteal.util;

import me.honeyberries.lifeSteal.config.LifeStealSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages the GUI for reviving eliminated players.
 * <p>
 * This class creates and manages an inventory GUI that displays all eliminated players
 * as player heads. Players can click on a head to revive that eliminated player.
 */
public class RevivalGUI {

    private static final int GUI_SIZE = 54; // 6 rows
    private static final Component GUI_TITLE = Component.text("Revival Selection").color(NamedTextColor.GREEN);

    /**
     * Opens the revival GUI for the specified player.
     *
     * @param player The player who will view the GUI.
     */
    public static void openRevivalGUI(@NotNull Player player) {
        Set<UUID> eliminatedPlayers = EliminationManager.getEliminatedPlayers();

        if (eliminatedPlayers.isEmpty()) {
            player.sendMessage(Component.text("There are no eliminated players to revive!", NamedTextColor.RED));
            return;
        }

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        int slot = 0;
        for (UUID uuid : eliminatedPlayers) {
            if (slot >= GUI_SIZE) {
                break; // GUI is full
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            ItemStack playerHead = createPlayerHead(offlinePlayer);
            gui.setItem(slot, playerHead);
            slot++;
        }

        // Fill the remaining slots with air
        ItemStack filler = new ItemStack(Material.AIR);

        for (int i = slot; i < GUI_SIZE; i++) {
            gui.setItem(i, filler);
        }

        player.openInventory(gui);
    }

    /**
     * Creates a player head item for the GUI.
     *
     * @param offlinePlayer The offline player whose head to create.
     * @return An ItemStack representing the player's head.
     */
    private static ItemStack createPlayerHead(@NotNull OfflinePlayer offlinePlayer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta itemMeta = head.getItemMeta();

        if (itemMeta instanceof SkullMeta meta) {
            meta.setOwningPlayer(offlinePlayer);
            
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";
            meta.displayName(Component.text(playerName).color(NamedTextColor.GOLD));

            double revivalHealth = LifeStealSettings.getHealthPerRevivalItem();
            double hearts = revivalHealth / 2.0;
            String heartText = hearts == 1.0 ? "heart" : "hearts";

            meta.lore(List.of(
                Component.text("Click to revive this player").color(NamedTextColor.YELLOW),
                Component.text("They will return with " + LifeStealUtil.formatHealth(hearts) + " " + heartText).color(NamedTextColor.GRAY),
                Component.text("UUID: " + offlinePlayer.getUniqueId()).color(NamedTextColor.DARK_GRAY)
            ));

            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Extracts the UUID from a player head item in the GUI.
     *
     * @param item The item to extract the UUID from.
     * @return The UUID of the player, or null if not found.
     */
    public static UUID getPlayerUUIDFromHead(@NotNull ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD) {
            return null;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) {
            return null;
        }

        return meta.getOwningPlayer().getUniqueId();
    }
}
