package com.discordwhitelister.spigot.gui;

import com.discordwhitelister.common.storage.WhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Listener for GUI interactions
 */
public class WhitelistManagerGUIListener implements Listener {
    private final WhitelistStorage storage;
    private final WhitelistManagerGUI gui;
    
    public WhitelistManagerGUIListener(WhitelistStorage storage, WhitelistManagerGUI gui) {
        this.storage = storage;
        this.gui = gui;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != gui.getInventory()) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Handle refresh button
        if (clickedItem.getType() == Material.CLOCK) {
            gui.updatePlayerList();
            player.sendMessage(ChatColor.GREEN + "Whitelist refreshed!");
            return;
        }
        
        // Handle close button
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        // Handle player removal (Java Edition)
        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            handlePlayerRemoval(player, clickedItem, false);
            return;
        }
        
        // Handle player removal (Bedrock Edition)
        if (clickedItem.getType() == Material.DIAMOND) {
            handlePlayerRemoval(player, clickedItem, true);
            return;
        }
    }
    
    private void handlePlayerRemoval(Player player, ItemStack clickedItem, boolean isBedrock) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String username;
        
        if (isBedrock) {
            // For Bedrock players, remove the " (Bedrock)" suffix
            username = displayName.replace(" (Bedrock)", "");
        } else {
            username = displayName;
        }
        
        // Check if player exists in whitelist
        List<WhitelistedPlayer> players = storage.getAllPlayers();
        WhitelistedPlayer targetPlayer = null;
        
        for (WhitelistedPlayer wp : players) {
            if (wp.getUsername().equalsIgnoreCase(username) && wp.isBedrock() == isBedrock) {
                targetPlayer = wp;
                break;
            }
        }
        
        if (targetPlayer != null) {
            boolean removed = storage.removePlayer(username);
            if (removed) {
                player.sendMessage(ChatColor.GREEN + "Player " + username + (isBedrock ? " (Bedrock)" : "") + " removed from whitelist!");
                gui.updatePlayerList();
            } else {
                player.sendMessage(ChatColor.RED + "Failed to remove player from whitelist!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Player not found in whitelist!");
        }
    }
}
