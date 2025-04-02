package com.discordwhitelister.common.discord;

import com.discordwhitelister.common.lookup.MinecraftPlayerLookup;
import com.discordwhitelister.common.lookup.PlayerInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MinecraftPlayerLookup that uses Mojang API
 */
public class MojangPlayerLookup implements MinecraftPlayerLookup {
    private static final Logger LOGGER = LoggerFactory.getLogger(MojangPlayerLookup.class);
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    
    private final HttpClient httpClient;
    
    public MojangPlayerLookup() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    @Override
    public CompletableFuture<PlayerInfo> lookupPlayer(String username) {
        CompletableFuture<PlayerInfo> future = new CompletableFuture<>();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MOJANG_API_URL + username))
            .GET()
            .build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(body -> {
                try {
                    if (body == null || body.isEmpty()) {
                        future.complete(null);
                        return;
                    }
                    
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    String id = json.get("id").getAsString();
                    String name = json.get("name").getAsString();
                    
                    // Mojang API returns UUIDs without hyphens, so we need to add them
                    String formattedUuid = id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                    );
                    
                    UUID uuid = UUID.fromString(formattedUuid);
                    future.complete(new PlayerInfo(name, uuid, null));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse Mojang API response", e);
                    future.complete(null);
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to lookup player", ex);
                future.complete(null);
                return null;
            });
        
        return future;
    }
    
    @Override
    public CompletableFuture<PlayerInfo> lookupBedrockPlayer(String gamertag, String xuid) {
        // For now, we just trust the user-provided XUID
        // In the future, this could be expanded to verify the XUID against the gamertag
        return CompletableFuture.completedFuture(new PlayerInfo(gamertag, null, xuid));
    }
}
