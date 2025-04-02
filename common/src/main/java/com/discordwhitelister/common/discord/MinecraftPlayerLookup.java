package com.discordwhitelister.common.discord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Minecraft player lookup services
 */
public interface MinecraftPlayerLookup {
    /**
     * Look up a player by username
     * 
     * @param username The Minecraft username
     * @return CompletableFuture that completes with player info or null if not found
     */
    CompletableFuture<PlayerInfo> lookupPlayer(String username);
    
    /**
     * Record class for player information
     */
    record PlayerInfo(String username, UUID uuid) {}
}
