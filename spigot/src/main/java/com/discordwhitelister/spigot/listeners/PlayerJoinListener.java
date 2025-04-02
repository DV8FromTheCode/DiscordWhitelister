package com.discordwhitelister.spigot.listeners;

import com.discordwhitelister.spigot.DiscordWhitelisterPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener for player join events to enforce whitelist
 */
public class PlayerJoinListener implements Listener {
    private final DiscordWhitelisterPlugin plugin;
    
    public PlayerJoinListener(DiscordWhitelisterPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // Skip if whitelist enforcement is disabled
        if (!plugin.getConfig().getBoolean("plugin.kick-non-whitelisted", true)) {
            return;
        }
        
        String username = event.getName();
        
        // Check if player is whitelisted
        if (!plugin.isWhitelisted(username)) {
            String kickMessage = plugin.getConfig().getString(
                "plugin.kick-message", 
                "You are not whitelisted on this server. Please join our Discord server to get whitelisted."
            );
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMessage);
            plugin.getLogger().info("Blocked login attempt by non-whitelisted player: " + username);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an operator, don't check whitelist
        if (player.isOp()) {
            return;
        }
        
        // Log whitelisted player join
        if (plugin.isWhitelisted(player.getName())) {
            plugin.getLogger().info("Whitelisted player joined: " + player.getName());
        }
    }
}
