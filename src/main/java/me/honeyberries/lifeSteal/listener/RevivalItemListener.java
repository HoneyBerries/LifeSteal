package me.honeyberries.lifeSteal.listener;

import me.honeyberries.lifeSteal.LifeSteal;
import me.honeyberries.lifeSteal.config.LifeStealConstants;
import me.honeyberries.lifeSteal.config.LifeStealSettings;
import me.honeyberries.lifeSteal.config.Messages;
import me.honeyberries.lifeSteal.manager.EliminationManager;
import me.honeyberries.lifeSteal.util.LifeStealUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Handles revival item usage and the revival GUI.
 */
public class RevivalItemListener implements Listener {
    
    private final LifeSteal plugin = LifeSteal.getInstance();
    
    /**
     * Handles when a player right-clicks with a revival item.
     */
    @EventHandler
    public void onRevivalItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        
        // Check if the item is a valid Revival item
        if (!LifeStealUtil.isRevivalItem(item)) {
            return;
        }
        
        // Cancel the original event
        event.setCancelled(true);
        
        // Check if revival is enabled
        if (!LifeStealSettings.isAllowRevival()) {
            player.sendMessage(Messages.revivalItemDisabled());
            return;
        }
        
        // Get list of eliminated players
        List<OfflinePlayer> eliminatedPlayers = EliminationManager.getEliminatedPlayers();
        
        if (eliminatedPlayers.isEmpty()) {
            player.sendMessage(Messages.noEliminatedPlayers());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return;
        }
        
        // Open GUI with eliminated players using Folia scheduler
        player.getScheduler().run(plugin, task -> {
            openRevivalGUI(player, eliminatedPlayers, item);
        }, null);
    }
    
    /**
     * Opens the revival GUI showing eliminated players.
     */
    private void openRevivalGUI(Player player, List<OfflinePlayer> eliminatedPlayers, ItemStack revivalItem) {
        // Create inventory with size based on number of eliminated players (round up to nearest 9)
        int size = Math.min(54, ((eliminatedPlayers.size() + 8) / 9) * 9);
        Inventory gui = Bukkit.createInventory(null, size, Component.text(LifeStealConstants.REVIVAL_GUI_TITLE));
        
        // Add player heads for each eliminated player
        for (int i = 0; i < eliminatedPlayers.size() && i < size; i++) {
            OfflinePlayer eliminated = eliminatedPlayers.get(i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(eliminated);
                skullMeta.displayName(Component.text(eliminated.getName() != null ? eliminated.getName() : "Unknown")
                    .color(NamedTextColor.YELLOW));
                skullMeta.lore(List.of(
                    Component.text("Click to revive").color(NamedTextColor.GREEN)
                ));
                skull.setItemMeta(skullMeta);
            }
            
            gui.setItem(i, skull);
        }
        
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);
    }
    
    /**
     * Handles clicking on a player head in the revival GUI.
     */
    @EventHandler
    public void onRevivalGUIClick(InventoryClickEvent event) {
        // Check if this is the revival GUI
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!event.getView().title().equals(Component.text(LifeStealConstants.REVIVAL_GUI_TITLE))) {
            return;
        }
        
        // Cancel the event to prevent item movement
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) {
            return;
        }
        
        // Get the player to revive from the skull meta
        SkullMeta skullMeta = (SkullMeta) clickedItem.getItemMeta();
        if (skullMeta == null || skullMeta.getOwningPlayer() == null) {
            return;
        }
        
        OfflinePlayer toRevive = skullMeta.getOwningPlayer();
        
        // Close the inventory
        player.closeInventory();
        
        // Attempt to revive the player using Folia scheduler
        player.getScheduler().run(plugin, task -> {
            if (EliminationManager.revivePlayer(toRevive)) {
                // Consume the revival item
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                ItemStack offHand = player.getInventory().getItemInOffHand();
                
                if (LifeStealUtil.isRevivalItem(mainHand)) {
                    if (mainHand.getAmount() > 1) {
                        mainHand.setAmount(mainHand.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                } else if (LifeStealUtil.isRevivalItem(offHand)) {
                    if (offHand.getAmount() > 1) {
                        offHand.setAmount(offHand.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
                
                // Send success message
                double revivalHealth = LifeStealSettings.getRevivalHealth();
                double hearts = revivalHealth / LifeStealConstants.HEALTH_POINTS_PER_HEART;
                String heartsWord = hearts == 1.0 ? "heart" : "hearts";
                player.sendMessage(Messages.playerRevived(
                    toRevive.getName() != null ? toRevive.getName() : "Unknown",
                    LifeStealUtil.formatHealth(hearts),
                    heartsWord
                ));
                
                // Notify revived player if online
                if (toRevive.isOnline() && toRevive.getPlayer() != null) {
                    toRevive.getPlayer().sendMessage(Messages.playerRevived(
                        toRevive.getName() != null ? toRevive.getName() : "You",
                        LifeStealUtil.formatHealth(hearts),
                        heartsWord
                    ));
                }
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);
            } else {
                player.sendMessage(Component.text("Failed to revive player.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, LifeStealConstants.SOUND_VOLUME, LifeStealConstants.SOUND_PITCH);
            }
        }, null);
    }
}
