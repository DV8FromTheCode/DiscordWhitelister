package com.discordwhitelister.common;

import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.discord.DiscordBot;
import com.discordwhitelister.common.lookup.MinecraftPlayerLookup;
import com.discordwhitelister.common.lookup.MojangPlayerLookup;
import com.discordwhitelister.common.storage.DatabaseWhitelistStorage;
import com.discordwhitelister.common.storage.JsonWhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Main service class for Discord Whitelister
 */
public class DiscordWhitelisterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordWhitelisterService.class);
    
    private final WhitelisterConfig config;
    private WhitelistStorage whitelistStorage;
    private MinecraftPlayerLookup playerLookup;
    private DiscordBot discordBot;
    
    public DiscordWhitelisterService(WhitelisterConfig config) {
        this.config = config;
    }
    
    /**
     * Initialize the service
     */
    public void initialize() {
        // Initialize player lookup
        playerLookup = new MojangPlayerLookup();
        
        // Initialize storage
        initializeStorage();
        
        // Initialize Discord bot
        discordBot = new DiscordBot(config, whitelistStorage, playerLookup);
        
        // Start the Discord bot
        start();
    }
    
    /**
     * Initialize the storage based on configuration
     */
    private void initializeStorage() {
        String storageType = config.getStorageType();
        
        if (storageType.equalsIgnoreCase("database")) {
            whitelistStorage = new DatabaseWhitelistStorage(config);
        } else {
            // Default to JSON storage
            whitelistStorage = new JsonWhitelistStorage(config);
        }
        
        whitelistStorage.initialize();
        LOGGER.info("Initialized {} storage", storageType);
    }
    
    /**
     * Start the service
     * 
     * @return CompletableFuture that completes when the service is started
     */
    public CompletableFuture<Void> start() {
        return discordBot.start().exceptionally(ex -> {
            LOGGER.error("Failed to start Discord bot", ex);
            return null;
        });
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        if (discordBot != null) {
            discordBot.stop();
        }
        
        if (whitelistStorage instanceof AutoCloseable) {
            try {
                ((AutoCloseable) whitelistStorage).close();
            } catch (Exception e) {
                LOGGER.error("Error closing whitelist storage", e);
            }
        }
    }
    
    /**
     * Get the whitelist storage
     * 
     * @return The whitelist storage
     */
    public WhitelistStorage getWhitelistStorage() {
        return whitelistStorage;
    }
    
    /**
     * Get the Discord bot
     * 
     * @return The Discord bot
     */
    public DiscordBot getDiscordBot() {
        return discordBot;
    }
    
    /**
     * Get the configuration
     * 
     * @return The configuration
     */
    public WhitelisterConfig getConfig() {
        return config;
    }
}
