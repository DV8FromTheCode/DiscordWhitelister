package com.discordwhitelister.common.lookup;

import java.util.UUID;

/**
 * Information about a Minecraft player
 * Supports both Java Edition (with UUID) and Bedrock Edition (with XUID) players
 */
public record PlayerInfo(String username, UUID uuid, String xuid) {
    /**
     * Constructor for Java Edition players
     * 
     * @param username Player's username
     * @param uuid Player's UUID
     */
    public PlayerInfo(String username, UUID uuid) {
        this(username, uuid, null);
    }
    
    /**
     * Check if this player is a Bedrock player
     * 
     * @return true if this is a Bedrock player (has XUID), false otherwise
     */
    public boolean isBedrock() {
        return xuid != null && !xuid.isEmpty();
    }
}
