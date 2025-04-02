package com.discordwhitelister.bungeecord;

import com.discordwhitelister.common.DiscordWhitelisterService;
import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.discord.DiscordBot;
import com.discordwhitelister.common.lookup.MinecraftPlayerLookup;
import com.discordwhitelister.common.lookup.MojangPlayerLookup;
import com.discordwhitelister.common.storage.JsonWhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import com.discordwhitelister.common.storage.WhitelistStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * BungeeCord implementation of Discord Whitelister
 */
public class DiscordWhitelisterBungee extends Plugin implements Listener {
    private DiscordWhitelisterService service;
    private WhitelisterConfig config;
    private WhitelistStorage whitelistStorage;
    private MinecraftPlayerLookup playerLookup;
    private boolean enforceWhitelist = true;

    @Override
    public void onEnable() {
        // Create config directory if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create default config if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create default config file", e);
            }
        }

        // Load configuration
        loadConfiguration();

        // Initialize whitelist storage
        initializeWhitelistStorage();

        // Initialize player lookup
        playerLookup = new MojangPlayerLookup();

        // Initialize Discord bot
        initializeDiscordBot();

        // Register event listener
        getProxy().getPluginManager().registerListener(this, this);

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new DiscordWhitelistCommand());

        getLogger().info("Discord Whitelister for BungeeCord enabled!");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
        }
        getLogger().info("Discord Whitelister for BungeeCord disabled!");
    }

    private void loadConfiguration() {
        try {
            Configuration bungeeConfig = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));

            config = new WhitelisterConfig();
            config.setBotToken(bungeeConfig.getString("discord.bot-token", ""));
            config.setGuildId(bungeeConfig.getString("discord.guild-id", ""));
            config.setChannelId(bungeeConfig.getString("discord.channel-id", ""));
            config.setMessageFormat(bungeeConfig.getString("discord.message-format", "whitelist {username}"));
            config.setSuccessMessage(bungeeConfig.getString("discord.success-message", "You have been whitelisted!"));
            config.setRequireRole(bungeeConfig.getBoolean("discord.require-role", false));
            config.setRequiredRoleId(bungeeConfig.getString("discord.required-role-id", ""));

            config.setStorageType(bungeeConfig.getString("storage.type", "json"));
            config.setStoragePath(new File(getDataFolder(), bungeeConfig.getString("storage.file", "whitelist.json")).getAbsolutePath());

            enforceWhitelist = bungeeConfig.getBoolean("enforce-whitelist", true);

        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load config.yml", e);
        }
    }

    private void initializeWhitelistStorage() {
        String storageType = config.getStorageType();
        if (storageType.equalsIgnoreCase("json")) {
            whitelistStorage = new JsonWhitelistStorage(config);
        } else {
            getLogger().warning("Unknown storage type: " + storageType + ". Using JSON storage.");
            whitelistStorage = new JsonWhitelistStorage(config);
        }
        
        whitelistStorage.initialize();
    }

    private void initializeDiscordBot() {
        DiscordBot discordBot = new DiscordBot(config, whitelistStorage, playerLookup);
        service = new DiscordWhitelisterService(config);
        service.initialize();
        service.start();
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        if (!enforceWhitelist) {
            return;
        }

        String username = event.getConnection().getName();
        UUID uuid = event.getConnection().getUniqueId();
        String xuid = null;

        // Check if this is a Bedrock player via Geyser
        // Geyser players have a specific prefix in their UUID
        if (uuid.toString().startsWith("00000000-0000-0000-")) {
            // This is likely a Bedrock player, extract XUID from the UUID
            // The format is typically: 00000000-0000-0000-XXXX-XXXXXXXXXXXX
            // where the X's represent the XUID
            String uuidStr = uuid.toString();
            xuid = uuidStr.substring(uuidStr.lastIndexOf("-") + 1);
            
            // Check if Bedrock player is whitelisted
            if (!whitelistStorage.isBedrockWhitelisted(xuid)) {
                event.setCancelled(true);
                event.setCancelReason(new TextComponent(ChatColor.RED + "You are not whitelisted on this server!"));
            }
        } else {
            // This is a Java player
            if (!whitelistStorage.isWhitelisted(username)) {
                event.setCancelled(true);
                event.setCancelReason(new TextComponent(ChatColor.RED + "You are not whitelisted on this server!"));
            }
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // You can add additional logic here if needed
    }

    public boolean addToWhitelist(String username, UUID uuid, String discordId) {
        return whitelistStorage.addPlayer(username, uuid, discordId);
    }

    public boolean addBedrockToWhitelist(String username, String xuid, String discordId) {
        return whitelistStorage.addBedrockPlayer(username, xuid, discordId);
    }

    public boolean removeFromWhitelist(String username) {
        return whitelistStorage.removePlayer(username);
    }

    public List<WhitelistedPlayer> getWhitelistedPlayers() {
        return whitelistStorage.getAllPlayers();
    }

    public List<WhitelistedPlayer> getWhitelistedBedrockPlayers() {
        List<WhitelistedPlayer> allPlayers = whitelistStorage.getAllPlayers();
        List<WhitelistedPlayer> bedrockPlayers = new ArrayList<>();
        
        for (WhitelistedPlayer player : allPlayers) {
            if (player.isBedrock()) {
                bedrockPlayers.add(player);
            }
        }
        
        return bedrockPlayers;
    }

    public DiscordBot getDiscordBot() {
        return service.getDiscordBot();
    }

    public void reloadPluginConfig() {
        // Stop the current service
        if (service != null) {
            service.shutdown();
        }
        
        // Reload configuration
        loadConfiguration();
        
        // Initialize whitelist storage
        initializeWhitelistStorage();
        
        // Initialize Discord bot
        initializeDiscordBot();
    }

    /**
     * Command handler for the /discordwhitelist command
     */
    private class DiscordWhitelistCommand extends Command {
        
        public DiscordWhitelistCommand() {
            super("discordwhitelist", "discordwhitelist.admin", "dw");
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                sendHelp(sender);
                return;
            }
            
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                    handleAddCommand(sender, args);
                    break;
                case "addbedrock":
                    handleAddBedrockCommand(sender, args);
                    break;
                case "remove":
                    handleRemoveCommand(sender, args);
                    break;
                case "list":
                    handleListCommand(sender, args);
                    break;
                case "status":
                    handleStatusCommand(sender);
                    break;
                case "reload":
                    handleReloadCommand(sender);
                    break;
                case "help":
                default:
                    sendHelp(sender);
                    break;
            }
        }
        
        private void handleAddCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("discordwhitelist.add")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            if (args.length < 2) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Usage: /discordwhitelist add <username> [discord_id]"));
                return;
            }
            
            String username = args[1];
            String discordId = args.length > 2 ? args[2] : "manual-" + System.currentTimeMillis();
            
            // Try to get UUID from ProxyServer
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
            UUID uuid = player != null ? player.getUniqueId() : null;
            
            // Add to whitelist
            boolean added = addToWhitelist(username, uuid, discordId);
            
            if (added) {
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Player " + username + " has been added to the whitelist."));
            } else {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to add player " + username + " to the whitelist. They may already be whitelisted."));
            }
        }
        
        private void handleAddBedrockCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("discordwhitelist.add")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            if (args.length < 3) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Usage: /discordwhitelist addbedrock <gamertag> <xuid> [discord_id]"));
                return;
            }
            
            String gamertag = args[1];
            String xuid = args[2];
            String discordId = args.length > 3 ? args[3] : "manual-" + System.currentTimeMillis();
            
            // Add Bedrock player to whitelist
            boolean added = addBedrockToWhitelist(gamertag, xuid, discordId);
            
            if (added) {
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Bedrock player " + gamertag + " (XUID: " + xuid + ") has been added to the whitelist."));
            } else {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to add Bedrock player " + gamertag + " to the whitelist. They may already be whitelisted."));
            }
        }
        
        private void handleRemoveCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("discordwhitelist.remove")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            if (args.length < 2) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Usage: /discordwhitelist remove <username>"));
                return;
            }
            
            String username = args[1];
            boolean removed = removeFromWhitelist(username);
            
            if (removed) {
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Player " + username + " has been removed from the whitelist."));
            } else {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to remove player " + username + " from the whitelist. They may not be whitelisted."));
            }
        }
        
        private void handleListCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("discordwhitelist.list")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            List<WhitelistedPlayer> players = getWhitelistedPlayers();
            
            if (args.length > 1 && args[1].equalsIgnoreCase("bedrock")) {
                // Show only Bedrock players
                players = getWhitelistedBedrockPlayers();
                
                if (players.isEmpty()) {
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "No Bedrock players are whitelisted."));
                    return;
                }
                
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Whitelisted Bedrock Players (" + players.size() + "):"));
                for (WhitelistedPlayer player : players) {
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "- " + player.getUsername() + 
                                      ChatColor.GRAY + " (XUID: " + player.getXuid() + ")"));
                }
            } else {
                // Show all players
                if (players.isEmpty()) {
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "No players are whitelisted."));
                    return;
                }
                
                // Count Java and Bedrock players
                long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
                long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
                
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Whitelisted Players (" + players.size() + " total, " + 
                                  javaCount + " Java, " + bedrockCount + " Bedrock):"));
                
                // Show Java players first
                for (WhitelistedPlayer player : players) {
                    if (!player.isBedrock()) {
                        sender.sendMessage(new TextComponent(ChatColor.YELLOW + "- " + player.getUsername() + 
                                          (player.getUuid() != null ? ChatColor.GRAY + " (UUID: " + player.getUuid() + ")" : "")));
                    }
                }
                
                // Then show Bedrock players
                if (bedrockCount > 0) {
                    sender.sendMessage(new TextComponent(ChatColor.AQUA + "Bedrock Players:"));
                    for (WhitelistedPlayer player : players) {
                        if (player.isBedrock()) {
                            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "- " + player.getUsername() + 
                                              ChatColor.GRAY + " (XUID: " + player.getXuid() + ")"));
                        }
                    }
                }
            }
        }
        
        private void handleStatusCommand(CommandSender sender) {
            if (!sender.hasPermission("discordwhitelist.status")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            boolean botConnected = getDiscordBot().getJda() != null && 
                                  getDiscordBot().getJda().getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
            
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Discord Whitelister Status:"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Discord Bot: " + 
                              (botConnected ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected")));
            
            List<WhitelistedPlayer> players = getWhitelistedPlayers();
            long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
            long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
            
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Whitelisted Players: " + ChatColor.WHITE + players.size() + 
                              " (" + javaCount + " Java, " + bedrockCount + " Bedrock)"));
        }
        
        private void handleReloadCommand(CommandSender sender) {
            if (!sender.hasPermission("discordwhitelist.reload")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command."));
                return;
            }
            
            reloadPluginConfig();
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Discord Whitelister configuration reloaded."));
        }
        
        private void sendHelp(CommandSender sender) {
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "===== Discord Whitelister Help ====="));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist add <username> [discord_id]" + ChatColor.WHITE + " - Add a Java player to the whitelist"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist addbedrock <gamertag> <xuid> [discord_id]" + ChatColor.WHITE + " - Add a Bedrock player to the whitelist"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist remove <username>" + ChatColor.WHITE + " - Remove a player from the whitelist"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist list [bedrock]" + ChatColor.WHITE + " - List all whitelisted players"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist status" + ChatColor.WHITE + " - Check the status of the Discord bot"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist reload" + ChatColor.WHITE + " - Reload the configuration"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "/discordwhitelist help" + ChatColor.WHITE + " - Show this help message"));
        }
    }
}
