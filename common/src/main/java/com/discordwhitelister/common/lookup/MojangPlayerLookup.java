package com.discordwhitelister.common.lookup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MinecraftPlayerLookup using Mojang API
 */
public class MojangPlayerLookup implements MinecraftPlayerLookup {
    private static final Logger LOGGER = LoggerFactory.getLogger(MojangPlayerLookup.class);
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public MojangPlayerLookup() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }
    
    @Override
    public CompletableFuture<PlayerInfo> lookupPlayer(String username) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MOJANG_API_URL + username))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    String body = response.body();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    
                    String id = json.get("id").getAsString();
                    String name = json.get("name").getAsString();
                    
                    // Convert the Mojang ID format to UUID format with hyphens
                    UUID uuid = formatUUID(id);
                    
                    return new PlayerInfo(name, uuid, null);
                } else if (response.statusCode() == 204 || response.statusCode() == 404) {
                    // Player not found
                    LOGGER.warn("Player not found: {}", username);
                    return null;
                } else {
                    // Other error
                    LOGGER.error("Error looking up player {}: HTTP {}", username, response.statusCode());
                    return null;
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Exception looking up player {}", username, ex);
                return null;
            });
    }
    
    /**
     * Format a Mojang ID string into a UUID with hyphens
     * 
     * @param id Mojang ID string (32 chars, no hyphens)
     * @return UUID object
     */
    private UUID formatUUID(String id) {
        // Insert hyphens at the correct positions: 8-4-4-4-12
        String uuidString = id.replaceFirst(
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
            "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(uuidString);
    }
}
