package com.discordwhitelister.spigot.gui;

import com.discordwhitelister.common.discord.DiscordBot;
import com.discordwhitelister.common.storage.WhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI for managing the whitelist
 */
public class WhitelistManagerGUI {
    private final WhitelistStorage storage;
    private final DiscordBot discordBot;
    private final Inventory inventory;
    
    public WhitelistManagerGUI(WhitelistStorage storage, DiscordBot discordBot) {
        this.storage = storage;
        this.discordBot = discordBot;
        this.inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Discord Whitelist Manager");
        
        initializeItems();
    }
    
    private void initializeItems() {
        // Add header items
        ItemStack logo = createGuiItem(Material.PURPLE_WOOL, ChatColor.DARK_PURPLE + "Discord Whitelister", 
                ChatColor.GRAY + "Manage your whitelist");
        inventory.setItem(4, logo);
        
        // Add Java player section header
        ItemStack javaHeader = createGuiItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Java Edition Players", 
                ChatColor.GRAY + "Players with standard UUIDs");
        inventory.setItem(9, javaHeader);
        
        // Add Bedrock player section header
        ItemStack bedrockHeader = createGuiItem(Material.BEDROCK, ChatColor.BLUE + "Bedrock Edition Players", 
                ChatColor.GRAY + "Players with XUIDs");
        inventory.setItem(27, bedrockHeader);
        
        // Fill with player heads
        updatePlayerList();
    }
    
    public void updatePlayerList() {
        // Clear player slots
        for (int i = 10; i < 27; i++) {
            inventory.setItem(i, null);
        }
        for (int i = 28; i < 45; i++) {
            inventory.setItem(i, null);
        }
        
        // Add Java Edition players
        List<WhitelistedPlayer> allPlayers = storage.getAllPlayers();
        int javaSlot = 10;
        int bedrockSlot = 28;
        
        for (WhitelistedPlayer player : allPlayers) {
            if (player.isBedrock()) {
                // Bedrock player
                if (bedrockSlot < 45) {
                    ItemStack playerItem = createPlayerItem(player);
                    inventory.setItem(bedrockSlot++, playerItem);
                }
            } else {
                // Java player
                if (javaSlot < 27) {
                    ItemStack playerItem = createPlayerItem(player);
                    inventory.setItem(javaSlot++, playerItem);
                }
            }
        }
        
        // Add status indicator
        boolean botConnected = discordBot.getJda() != null && discordBot.getJda().getStatus().equals(Status.CONNECTED);
        Material statusMaterial = botConnected ? Material.LIME_WOOL : Material.RED_WOOL;
        String statusText = botConnected ? "Discord Bot: Connected" : "Discord Bot: Disconnected";
        ItemStack statusItem = createGuiItem(statusMaterial, statusText);
        inventory.setItem(49, statusItem);
        
        // Add refresh button
        ItemStack refreshItem = createGuiItem(Material.CLOCK, ChatColor.YELLOW + "Refresh");
        inventory.setItem(50, refreshItem);
        
        // Add close button
        ItemStack closeItem = createGuiItem(Material.BARRIER, ChatColor.RED + "Close");
        inventory.setItem(53, closeItem);
    }
    
    private ItemStack createPlayerItem(WhitelistedPlayer player) {
        ItemStack item;
        
        if (player.isBedrock()) {
            // For Bedrock players, use a different item
            item = new ItemStack(Material.DIAMOND);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + player.getUsername() + " (Bedrock)");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "XUID: " + player.getXuid());
            lore.add(ChatColor.GRAY + "Discord ID: " + player.getDiscordId());
            lore.add(ChatColor.GRAY + "Whitelisted: " + player.getWhitelistedAt());
            lore.add("");
            lore.add(ChatColor.RED + "Click to remove from whitelist");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            // For Java players, use player head
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + player.getUsername());
            
            List<String> lore = new ArrayList<>();
            if (player.getUuid() != null) {
                lore.add(ChatColor.GRAY + "UUID: " + player.getUuid());
            } else {
                lore.add(ChatColor.GRAY + "UUID: Unknown (offline mode)");
            }
            lore.add(ChatColor.GRAY + "Discord ID: " + player.getDiscordId());
            lore.add(ChatColor.GRAY + "Whitelisted: " + player.getWhitelistedAt());
            lore.add("");
            lore.add(ChatColor.RED + "Click to remove from whitelist");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public void open(Player player) {
        player.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}
