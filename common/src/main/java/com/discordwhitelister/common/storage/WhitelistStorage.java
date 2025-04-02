package com.discordwhitelister.common.storage;

import java.util.List;
import java.util.UUID;

/**
 * Interface for whitelist storage implementations
 */
public interface WhitelistStorage {
    /**
     * Add a Java Edition player to the whitelist
     * 
     * @param username The Minecraft username
     * @param uuid The Minecraft UUID (can be null for offline mode servers)
     * @param discordId The Discord user ID who requested the whitelist
     * @return true if successfully added, false otherwise
     */
    boolean addPlayer(String username, UUID uuid, String discordId);
    
    /**
     * Add a Bedrock Edition player to the whitelist
     * 
     * @param username The Minecraft gamertag/username
     * @param xuid The Xbox User ID (XUID)
     * @param discordId The Discord user ID who requested the whitelist
     * @return true if successfully added, false otherwise
     */
    boolean addBedrockPlayer(String username, String xuid, String discordId);
    
    /**
     * Remove a player from the whitelist
     * 
     * @param username The Minecraft username
     * @return true if successfully removed, false otherwise
     */
    boolean removePlayer(String username);
    
    /**
     * Check if a player is whitelisted
     * 
     * @param username The Minecraft username
     * @return true if whitelisted, false otherwise
     */
    boolean isWhitelisted(String username);
    
    /**
     * Check if a Bedrock player is whitelisted by XUID
     * 
     * @param xuid The Xbox User ID (XUID)
     * @return true if whitelisted, false otherwise
     */
    boolean isBedrockWhitelisted(String xuid);
    
    /**
     * Get all whitelisted players
     * 
     * @return List of all whitelisted players
     */
    List<WhitelistedPlayer> getAllPlayers();
    
    /**
     * Get all whitelisted Bedrock players
     * 
     * @return List of all whitelisted Bedrock players
     */
    List<WhitelistedPlayer> getAllBedrockPlayers();
    
    /**
     * Initialize the storage
     */
    void initialize();
    
    /**
     * Save changes to storage
     */
    void save();
}
