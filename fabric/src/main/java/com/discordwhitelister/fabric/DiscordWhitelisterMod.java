package com.discordwhitelister.fabric;

import com.discordwhitelister.common.DiscordWhitelisterService;
import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import com.discordwhitelister.common.util.LogoUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/**
 * Main mod class for Fabric implementation of Discord Whitelister
 */
public class DiscordWhitelisterMod implements ModInitializer {
    public static final String MOD_ID = "discordwhitelister";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private DiscordWhitelisterService service;
    private WhitelisterConfig config;
    private BufferedImage logoImage;
    private Path configDir;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Discord Whitelister mod");
        
        // Setup config directory
        configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        configDir.toFile().mkdirs();
        
        // Load configuration
        loadConfiguration();
        
        // Load logo
        logoImage = LogoUtil.getLogoImage();
        if (logoImage != null) {
            LOGGER.info("Successfully loaded DiscordWhitelister logo");
        } else {
            LOGGER.warn("Could not load DiscordWhitelister logo");
        }
        
        // Initialize the service
        service = new DiscordWhitelisterService(config);
        
        // Register commands
        registerCommands();
        
        // Register event handlers
        registerEventHandlers();
        
        // Start the Discord bot when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            service.start().thenRun(() -> {
                LOGGER.info("Discord bot started successfully!");
            }).exceptionally(ex -> {
                LOGGER.error("Failed to start Discord bot", ex);
                return null;
            });
        });
        
        // Stop the Discord bot when the server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (service != null) {
                service.stop();
            }
        });
        
        LOGGER.info("Discord Whitelister mod has been initialized!");
    }
    
    /**
     * Load configuration from config file
     */
    private void loadConfiguration() {
        File configFile = configDir.resolve("config.properties").toFile();
        Properties properties = new Properties();
        
        if (!configFile.exists()) {
            // Create default config
            try {
                configFile.createNewFile();
                properties.setProperty("discord.token", "YOUR_BOT_TOKEN_HERE");
                properties.setProperty("discord.guild-id", "YOUR_GUILD_ID_HERE");
                properties.setProperty("discord.channel-id", "YOUR_CHANNEL_ID_HERE");
                properties.setProperty("discord.message-format", "Please whitelist my Minecraft username: {username}");
                properties.setProperty("discord.success-message", "You have been whitelisted! You can now join the server.");
                properties.setProperty("discord.require-role", "false");
                properties.setProperty("discord.required-role-id", "");
                properties.setProperty("storage.type", "json");
                properties.setProperty("storage.json.file-path", configDir.resolve("whitelist.json").toString());
                properties.setProperty("storage.database.url", "jdbc:mysql://localhost:3306/whitelist");
                properties.setProperty("storage.database.username", "root");
                properties.setProperty("storage.database.password", "password");
                properties.setProperty("plugin.kick-non-whitelisted", "true");
                properties.setProperty("plugin.kick-message", "You are not whitelisted on this server. Please join our Discord server to get whitelisted.");
                
                properties.store(new FileWriter(configFile), "Discord Whitelister Configuration");
            } catch (IOException e) {
                LOGGER.error("Failed to create default config file", e);
            }
        } else {
            // Load existing config
            try {
                properties.load(new FileReader(configFile));
            } catch (IOException e) {
                LOGGER.error("Failed to load config file", e);
            }
        }
        
        // Create config object
        config = new WhitelisterConfig();
        
        // Discord settings
        config.setBotToken(properties.getProperty("discord.token"));
        config.setGuildId(properties.getProperty("discord.guild-id"));
        config.setChannelId(properties.getProperty("discord.channel-id"));
        config.setMessageFormat(properties.getProperty("discord.message-format", "Please whitelist my Minecraft username: {username}"));
        config.setSuccessMessage(properties.getProperty("discord.success-message", "You have been whitelisted! You can now join the server."));
        config.setRequireRole(Boolean.parseBoolean(properties.getProperty("discord.require-role", "false")));
        config.setRequiredRoleId(properties.getProperty("discord.required-role-id"));
        
        // Storage settings
        String storageType = properties.getProperty("storage.type", "json");
        config.setStorageType(storageType);
        
        if (storageType.equalsIgnoreCase("json")) {
            String jsonPath = properties.getProperty("storage.json.file-path");
            if (jsonPath == null || jsonPath.isEmpty()) {
                jsonPath = configDir.resolve("whitelist.json").toString();
            }
            config.setJsonFilePath(jsonPath);
        } else if (storageType.equalsIgnoreCase("database")) {
            config.setDatabaseUrl(properties.getProperty("storage.database.url"));
            config.setDatabaseUser(properties.getProperty("storage.database.username"));
            config.setDatabasePassword(properties.getProperty("storage.database.password"));
        }
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("discordwhitelist")
                .requires(source -> source.hasPermissionLevel(2)) // Require permission level 2 (op)
                .then(CommandManager.literal("reload")
                    .executes(context -> {
                        loadConfiguration();
                        context.getSource().sendFeedback(() -> Text.literal("Discord Whitelister configuration reloaded."), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("status")
                    .executes(context -> {
                        boolean isConnected = service.getDiscordBot().getJda() != null && 
                                            service.getDiscordBot().getJda().getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
                        
                        context.getSource().sendFeedback(() -> Text.literal("=== Discord Whitelister Status ==="), false);
                        context.getSource().sendFeedback(() -> Text.literal("Discord Bot: " + 
                                                    (isConnected ? "Connected" : "Disconnected")), false);
                        
                        int playerCount = service.getStorage().getAllPlayers().size();
                        context.getSource().sendFeedback(() -> Text.literal("Whitelisted Players: " + playerCount), false);
                        
                        String storageType = config.getStorageType();
                        context.getSource().sendFeedback(() -> Text.literal("Storage Type: " + storageType), false);
                        
                        return 1;
                    })
                )
                // Add more commands here
            );
        });
    }
    
    /**
     * Register event handlers
     */
    private void registerEventHandlers() {
        // Check whitelist on player connection
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String username = player.getName().getString();
            
            // Skip if player is op
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                return;
            }
            
            // Check if player is whitelisted
            if (!service.getStorage().isWhitelisted(username)) {
                String kickMessage = "You are not whitelisted on this server.";
                player.networkHandler.disconnect(Text.literal(kickMessage));
                LOGGER.info("Blocked login attempt by non-whitelisted player: " + username);
            } else {
                LOGGER.info("Whitelisted player joined: " + username);
            }
        });
    }
    
    /**
     * Get the Discord Whitelister service
     * 
     * @return The Discord Whitelister service
     */
    public DiscordWhitelisterService getService() {
        return service;
    }
    
    /**
     * Check if a player is whitelisted
     * 
     * @param username The player's username
     * @return true if whitelisted, false otherwise
     */
    public boolean isWhitelisted(String username) {
        return service.getStorage().isWhitelisted(username);
    }
    
    /**
     * Add a player to the whitelist
     * 
     * @param username The player's username
     * @param uuid The player's UUID
     * @param discordId The Discord user ID who requested the whitelist
     * @return true if added successfully, false otherwise
     */
    public boolean addToWhitelist(String username, UUID uuid, String discordId) {
        return service.getStorage().addPlayer(username, uuid, discordId);
    }
    
    /**
     * Remove a player from the whitelist
     * 
     * @param username The player's username
     * @return true if removed successfully, false otherwise
     */
    public boolean removeFromWhitelist(String username) {
        return service.getStorage().removePlayer(username);
    }
    
    /**
     * Get the logo image
     * 
     * @return The logo image
     */
    public BufferedImage getLogoImage() {
        return logoImage;
    }
}
