package com.discordwhitelister.common.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.discordwhitelister.common.config.WhitelisterConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON implementation of whitelist storage
 */
public class JsonWhitelistStorage implements WhitelistStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonWhitelistStorage.class);
    private final WhitelisterConfig config;
    private final Gson gson;
    private final File whitelistFile;
    private final List<WhitelistedPlayer> whitelist;
    
    public JsonWhitelistStorage(WhitelisterConfig config) {
        this.config = config;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        String filePath = config.getJsonFilePath();
        if (filePath == null || filePath.isEmpty()) {
            filePath = "whitelist.json";
        }
        
        this.whitelistFile = new File(filePath);
        this.whitelist = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public void initialize() {
        if (!whitelistFile.exists()) {
            try {
                File parent = whitelistFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                whitelistFile.createNewFile();
                save(); // Create empty whitelist file
            } catch (IOException e) {
                LOGGER.error("Failed to create whitelist file", e);
            }
        } else {
            try (FileReader reader = new FileReader(whitelistFile)) {
                Type listType = new TypeToken<ArrayList<WhitelistedPlayer>>(){}.getType();
                List<WhitelistedPlayer> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    whitelist.clear();
                    whitelist.addAll(loaded);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load whitelist from file", e);
            }
        }
    }
    
    @Override
    public void save() {
        try (FileWriter writer = new FileWriter(whitelistFile)) {
            gson.toJson(whitelist, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save whitelist to file", e);
        }
    }
    
    @Override
    public boolean addPlayer(String username, UUID uuid, String discordId) {
        if (isWhitelisted(username)) {
            return false;
        }
        
        WhitelistedPlayer player = new WhitelistedPlayer(username, uuid, discordId);
        whitelist.add(player);
        save();
        return true;
    }
    
    @Override
    public boolean addBedrockPlayer(String username, String xuid, String discordId) {
        // Check if player is already whitelisted
        if (isWhitelisted(username) || isBedrockWhitelisted(xuid)) {
            return false;
        }
        
        WhitelistedPlayer player = new WhitelistedPlayer(username, xuid, discordId, true);
        whitelist.add(player);
        save();
        return true;
    }
    
    @Override
    public boolean removePlayer(String username) {
        boolean removed = whitelist.removeIf(player -> 
            player.getUsername().equalsIgnoreCase(username));
        
        if (removed) {
            save();
        }
        
        return removed;
    }
    
    @Override
    public boolean isWhitelisted(String username) {
        return whitelist.stream()
            .anyMatch(player -> player.getUsername().equalsIgnoreCase(username));
    }
    
    @Override
    public boolean isBedrockWhitelisted(String xuid) {
        return whitelist.stream()
            .anyMatch(player -> player.isBedrock() && xuid.equals(player.getXuid()));
    }
    
    @Override
    public List<WhitelistedPlayer> getAllPlayers() {
        return new ArrayList<>(whitelist);
    }
    
    @Override
    public List<WhitelistedPlayer> getAllBedrockPlayers() {
        return whitelist.stream()
            .filter(WhitelistedPlayer::isBedrock)
            .collect(Collectors.toList());
    }
}
