package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.util.EliminationManager;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import me.honeyberries.lifeSteal.util.RevivalGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.honeyberries.lifeSteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Handles interactions with the revival item.
 * <p>
 * This listener manages right-clicking revival items to open the revival GUI,
 * and handles clicking on player heads in the GUI to revive eliminated players.
 */
public class RevivalItemUsageListener implements Listener {

    /**
     * Handles the event when a player right-clicks with a revival item.
     * Opens the revival GUI showing all eliminated players.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with an item.
     */
    @EventHandler
    public void onRevivalItemUse(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only handle right-click air or block actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only process main hand events to avoid duplicate processing
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();

        // Check if the item is a valid revival item
        if (!LifeStealUtil.isRevivalItem(item)) {
            return;
        }

        // Restrict revival feature if not allowed
        if (!LifeStealSettings.isAllowRevival()) {
            player.sendMessage(Component.text("Revival is currently disabled on this server.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }

        // Cancel the event to prevent normal item usage
        event.setCancelled(true);

        // Check if there are any eliminated players
        if (EliminationManager.getEliminatedCount() == 0) {
            player.sendMessage(Component.text("There are no eliminated players to revive!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }

        // Open the revival GUI
        RevivalGUI.openRevivalGUI(player);

        // Mark the player as having the revival GUI open to prevent abuse
        player.setMetadata("IsRevivalGUIOpen", new org.bukkit.metadata.FixedMetadataValue(LifeSteal.getInstance(), true));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
    }

    /**
     * Handles clicking on items in the revival GUI.
     * When a player head is clicked, revives the corresponding eliminated player.
     * This method prevents item manipulation and dragging to protect the GUI.
     *
     * @param event The InventoryClickEvent triggered when a player clicks in an inventory.
     */
    @EventHandler
    public void onRevivalGUIClick(@NotNull InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Check if the player has the revival GUI open
        if (!player.hasMetadata("IsRevivalGUIOpen")) {
            return; // Not our GUI, ignore
        }

        Inventory revivalInventory = event.getInventory(); // get the revival inventory

        if (event.getClickedInventory() == null) {
            return; // Clicked outside the inventory
        }

        // Prevent putting items into the revival GUI
        if (event.getClickedInventory().equals(revivalInventory)) {
            event.setCancelled(true); // Prevent item manipulation in GUI

            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return; // Empty slot clicked, nothing to do
            }

            // Check if it's a player head (eliminated player)
            if (clickedItem.getType() != Material.PLAYER_HEAD) {
                return; // Filler or other items clicked, ignore
            }

            // Restrict revival feature if not allowed
            if (!LifeStealSettings.isAllowRevival()) {
                player.sendMessage(Component.text("Revival is currently disabled on this server.", NamedTextColor.RED));
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                return;
            }

            // Extract the UUID from the player head
            UUID targetUUID = RevivalGUI.getPlayerUUIDFromHead(clickedItem);
            if (targetUUID == null) {
                player.sendMessage(Component.text("Failed to identify the player.", NamedTextColor.RED));
                return;
            }

            // Get the offline player to revive
            org.bukkit.OfflinePlayer revivedOfflinePlayer = Bukkit.getOfflinePlayer(targetUUID);

            // Check if the player still has a revival item
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();

            boolean hasRevivalItem = false;
            EquipmentSlot usedSlot = null;

            if (LifeStealUtil.isRevivalItem(mainHandItem)) {
                hasRevivalItem = true;
                usedSlot = EquipmentSlot.HAND;
            } else if (LifeStealUtil.isRevivalItem(offHandItem)) {
                hasRevivalItem = true;
                usedSlot = EquipmentSlot.OFF_HAND;
            }

            if (!hasRevivalItem) {
                player.sendMessage(Component.text("You no longer have a revival item!", NamedTextColor.RED));
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                return;
            }

            // Attempt to revive the player
            boolean success = EliminationManager.revivePlayer(revivedOfflinePlayer);
            if (success) {
                org.bukkit.OfflinePlayer revivedPlayer = revivedOfflinePlayer;

                // Consume the revival item
                ItemStack itemToConsume = usedSlot == EquipmentSlot.HAND ? mainHandItem : offHandItem;
                if (itemToConsume.getAmount() > 1) {
                    itemToConsume.setAmount(itemToConsume.getAmount() - 1);
                } else {
                    if (usedSlot == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                }

                // Success message and sound
                player.sendMessage(Component.text("Successfully revived ", NamedTextColor.GREEN)
                        .append(Component.text(revivedPlayer.getName(), NamedTextColor.GOLD))
                        .append(Component.text("!", NamedTextColor.GREEN)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

                // Broadcast to server
                Bukkit.broadcast(
                        Component.text(player.getName() + " revived " + revivedPlayer.getName() + "!", NamedTextColor.GREEN)
                );

                // Close the inventory
                player.closeInventory();
            } else {
                player.sendMessage(Component.text("Failed to revive the player. They may not be eliminated.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                player.closeInventory();
            }

            // If they click their own inventory while revival GUI is open
        } else {
            event.setCancelled(event.getClickedInventory().getType() != InventoryType.PLAYER);
        }
    }

    /**
     * Handles the InventoryCloseEvent for the revival GUI.
     * Removes the metadata tag when the GUI is closed.
     *
     * @param event The InventoryCloseEvent.
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (player.hasMetadata("IsRevivalGUIOpen")) {
            player.removeMetadata("IsRevivalGUIOpen", LifeSteal.getInstance());
        }
    }
}