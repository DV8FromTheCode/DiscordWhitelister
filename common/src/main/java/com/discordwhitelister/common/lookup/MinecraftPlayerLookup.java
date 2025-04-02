package com.discordwhitelister.common.lookup;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Minecraft player lookup services
 */
public interface MinecraftPlayerLookup {
    /**
     * Look up a Java Edition player by username
     * 
     * @param username The Minecraft username
     * @return CompletableFuture that completes with player info or null if not found
     */
    CompletableFuture<PlayerInfo> lookupPlayer(String username);
    
    /**
     * Look up a Bedrock Edition player by gamertag and XUID
     * 
     * @param gamertag The Bedrock player's gamertag
     * @param xuid The Bedrock player's XUID
     * @return CompletableFuture that completes with player info
     */
    default CompletableFuture<PlayerInfo> lookupBedrockPlayer(String gamertag, String xuid) {
        // Default implementation just returns the provided information
        return CompletableFuture.completedFuture(new PlayerInfo(gamertag, null, xuid));
    }
    
    /**
     * Record class for player information
     */
    record PlayerInfo(String username, UUID uuid, String xuid) {}
}
