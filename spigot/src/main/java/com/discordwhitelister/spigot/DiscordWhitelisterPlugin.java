package com.discordwhitelister.spigot;

import com.discordwhitelister.common.DiscordWhitelisterService;
import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.discord.DiscordBot;
import com.discordwhitelister.common.storage.WhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import com.discordwhitelister.spigot.commands.DiscordWhitelistCommand;
import com.discordwhitelister.spigot.gui.WhitelistManagerGUI;
import com.discordwhitelister.spigot.gui.WhitelistManagerGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Main plugin class for Spigot implementation
 */
public class DiscordWhitelisterPlugin extends JavaPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordWhitelisterPlugin.class);
    
    private DiscordWhitelisterService service;
    private WhitelistManagerGUI whitelistManagerGUI;
    
    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Create WhitelisterConfig from Bukkit config
        WhitelisterConfig config = new WhitelisterConfig();
        config.setBotToken(getConfig().getString("discord.token"));
        config.setGuildId(getConfig().getString("discord.guild_id"));
        config.setChannelId(getConfig().getString("discord.channel_id"));
        config.setMessageFormat(getConfig().getString("discord.message_format", "whitelist {username}"));
        config.setSuccessMessage(getConfig().getString("discord.success_message", "You have been whitelisted!"));
        config.setRequireRole(getConfig().getBoolean("discord.require_role", false));
        config.setRequiredRoleId(getConfig().getString("discord.required_role_id"));
        config.setStorageType(getConfig().getString("storage.type", "json"));
        config.setStoragePath(getConfig().getString("storage.path", "plugins/DiscordWhitelister/whitelist.json"));
        
        // For database storage
        if (config.getStorageType().equalsIgnoreCase("database")) {
            config.setDatabaseUrl(getConfig().getString("storage.database.url"));
            config.setDatabaseUser(getConfig().getString("storage.database.user"));
            config.setDatabasePassword(getConfig().getString("storage.database.password"));
        }
        
        // Initialize service
        service = new DiscordWhitelisterService(config);
        service.initialize();
        
        // Register command
        PluginCommand command = getCommand("discordwhitelist");
        if (command != null) {
            DiscordWhitelistCommand commandExecutor = new DiscordWhitelistCommand(this);
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
        
        // Initialize GUI
        initializeGUI();
        
        // Sync Minecraft whitelist with our storage
        syncWhitelist();
        
        LOGGER.info("DiscordWhitelister plugin enabled");
    }
    
    private void initializeGUI() {
        // Create GUI components
        whitelistManagerGUI = new WhitelistManagerGUI(getWhitelistStorage(), getDiscordBot());
        WhitelistManagerGUIListener guiListener = new WhitelistManagerGUIListener(getWhitelistStorage(), whitelistManagerGUI);
        
        // Register GUI listener
        getServer().getPluginManager().registerEvents(guiListener, this);
    }
    
    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
        }
        LOGGER.info("DiscordWhitelister plugin disabled");
    }
    
    /**
     * Sync the Minecraft whitelist with our storage
     */
    public void syncWhitelist() {
        // Get all whitelisted players from our storage
        List<WhitelistedPlayer> players = getWhitelistStorage().getAllPlayers();
        
        // Sync Java Edition players
        for (WhitelistedPlayer player : players) {
            if (!player.isBedrock()) {
                // Only sync Java Edition players with UUIDs
                if (player.getUuid() != null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUuid());
                    if (!offlinePlayer.isWhitelisted()) {
                        offlinePlayer.setWhitelisted(true);
                        LOGGER.info("Added Java player {} to Minecraft whitelist", player.getUsername());
                    }
                }
            } else {
                // For Bedrock players, we need to check if the server supports them
                // This depends on the server implementation (e.g., Geyser)
                // For now, we'll just log that we found a Bedrock player
                LOGGER.info("Found Bedrock player {} (XUID: {}) in whitelist", 
                           player.getUsername(), player.getXuid());
            }
        }
    }
    
    /**
     * Add a player to the whitelist
     * 
     * @param username The player's username
     * @param uuid The player's UUID (can be null for offline mode)
     * @param discordId The Discord ID of the user who requested the whitelist
     * @return true if the player was added, false otherwise
     */
    public boolean addToWhitelist(String username, UUID uuid, String discordId) {
        boolean added = getWhitelistStorage().addPlayer(username, uuid, discordId);
        
        if (added && uuid != null) {
            // Also add to Minecraft whitelist
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            offlinePlayer.setWhitelisted(true);
        }
        
        return added;
    }
    
    /**
     * Add a Bedrock player to the whitelist
     * 
     * @param username The player's gamertag
     * @param xuid The player's XUID
     * @param discordId The Discord ID of the user who requested the whitelist
     * @return true if the player was added, false otherwise
     */
    public boolean addBedrockToWhitelist(String username, String xuid, String discordId) {
        return getWhitelistStorage().addBedrockPlayer(username, xuid, discordId);
    }
    
    /**
     * Remove a player from the whitelist
     * 
     * @param username The player's username
     * @return true if the player was removed, false otherwise
     */
    public boolean removeFromWhitelist(String username) {
        boolean removed = getWhitelistStorage().removePlayer(username);
        
        if (removed) {
            // Try to find the player in the Minecraft whitelist
            for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
                if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(username)) {
                    offlinePlayer.setWhitelisted(false);
                    break;
                }
            }
        }
        
        return removed;
    }
    
    /**
     * Open the whitelist manager GUI for a player
     * 
     * @param player The player to show the GUI to
     */
    public void openWhitelistManager(Player player) {
        whitelistManagerGUI.open(player);
    }
    
    /**
     * Get the whitelist storage
     * 
     * @return The whitelist storage
     */
    public WhitelistStorage getWhitelistStorage() {
        return service.getWhitelistStorage();
    }
    
    /**
     * Get the Discord bot
     * 
     * @return The Discord bot
     */
    public DiscordBot getDiscordBot() {
        return service.getDiscordBot();
    }
    
    /**
     * Get all whitelisted players
     * 
     * @return List of all whitelisted players
     */
    public List<WhitelistedPlayer> getWhitelistedPlayers() {
        return getWhitelistStorage().getAllPlayers();
    }
    
    /**
     * Get all whitelisted Bedrock players
     * 
     * @return List of all whitelisted Bedrock players
     */
    public List<WhitelistedPlayer> getWhitelistedBedrockPlayers() {
        return getWhitelistStorage().getAllBedrockPlayers();
    }
    
    /**
     * Check if a player is whitelisted
     * 
     * @param username The player's username
     * @return true if the player is whitelisted, false otherwise
     */
    public boolean isWhitelisted(String username) {
        return getWhitelistStorage().isWhitelisted(username);
    }
    
    /**
     * Check if a Bedrock player is whitelisted
     * 
     * @param xuid The player's XUID
     * @return true if the player is whitelisted, false otherwise
     */
    public boolean isBedrockWhitelisted(String xuid) {
        return getWhitelistStorage().isBedrockWhitelisted(xuid);
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reloadPluginConfig() {
        reloadConfig();
        
        // Update service config
        WhitelisterConfig config = service.getConfig();
        config.setBotToken(getConfig().getString("discord.token"));
        config.setGuildId(getConfig().getString("discord.guild_id"));
        config.setChannelId(getConfig().getString("discord.channel_id"));
        config.setMessageFormat(getConfig().getString("discord.message_format", "whitelist {username}"));
        config.setSuccessMessage(getConfig().getString("discord.success_message", "You have been whitelisted!"));
        config.setRequireRole(getConfig().getBoolean("discord.require_role", false));
        config.setRequiredRoleId(getConfig().getString("discord.required_role_id"));
        
        // Restart Discord bot if it was running
        if (service.getDiscordBot().getJda() != null) {
            service.getDiscordBot().stop();
            service.getDiscordBot().start();
        }
    }
}
