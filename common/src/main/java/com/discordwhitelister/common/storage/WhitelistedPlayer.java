package com.discordwhitelister.common.storage;

import java.util.UUID;
import java.time.Instant;

/**
 * Represents a whitelisted player
 */
public class WhitelistedPlayer {
    private String username;
    private UUID uuid;
    private String discordId;
    private Instant whitelistedAt;
    private boolean isBedrock;
    private String xuid;
    
    /**
     * Constructor for Java Edition players
     * 
     * @param username The Minecraft username
     * @param uuid The Minecraft UUID
     * @param discordId The Discord user ID
     */
    public WhitelistedPlayer(String username, UUID uuid, String discordId) {
        this.username = username;
        this.uuid = uuid;
        this.discordId = discordId;
        this.whitelistedAt = Instant.now();
        this.isBedrock = false;
        this.xuid = null;
    }
    
    /**
     * Constructor for Bedrock Edition players
     * 
     * @param username The Minecraft gamertag/username
     * @param xuid The Xbox User ID (XUID)
     * @param discordId The Discord user ID
     * @param isBedrock Flag indicating this is a Bedrock player
     */
    public WhitelistedPlayer(String username, String xuid, String discordId, boolean isBedrock) {
        this.username = username;
        this.uuid = null; // Bedrock players don't have Java UUIDs
        this.xuid = xuid;
        this.discordId = discordId;
        this.whitelistedAt = Instant.now();
        this.isBedrock = true;
    }
    
    /**
     * Constructor for deserialization
     */
    public WhitelistedPlayer(String username, UUID uuid, String xuid, String discordId, Instant whitelistedAt, boolean isBedrock) {
        this.username = username;
        this.uuid = uuid;
        this.xuid = xuid;
        this.discordId = discordId;
        this.whitelistedAt = whitelistedAt;
        this.isBedrock = isBedrock;
    }
    
    public String getUsername() {
        return username;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getXuid() {
        return xuid;
    }
    
    public String getDiscordId() {
        return discordId;
    }
    
    public Instant getWhitelistedAt() {
        return whitelistedAt;
    }
    
    public boolean isBedrock() {
        return isBedrock;
    }
    
    /**
     * Get the player's identifier (UUID for Java, XUID for Bedrock)
     * 
     * @return The player's identifier as a string
     */
    public String getIdentifier() {
        if (isBedrock) {
            return xuid;
        } else {
            return uuid != null ? uuid.toString() : null;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WhitelistedPlayer that = (WhitelistedPlayer) obj;
        
        // For Bedrock players, compare XUID if available
        if (isBedrock && that.isBedrock) {
            if (xuid != null && that.xuid != null) {
                return xuid.equals(that.xuid);
            }
        }
        
        // Otherwise, compare usernames case-insensitively
        return username.equalsIgnoreCase(that.username);
    }
    
    @Override
    public int hashCode() {
        // For Bedrock players with XUID, use XUID hashcode
        if (isBedrock && xuid != null) {
            return xuid.hashCode();
        }
        
        // Otherwise, use lowercase username hashcode
        return username.toLowerCase().hashCode();
    }
}
