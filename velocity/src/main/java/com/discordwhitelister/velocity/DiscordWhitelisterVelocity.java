package com.discordwhitelister.velocity;

import com.discordwhitelister.common.DiscordWhitelisterService;
import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.discord.DiscordBot;
import com.discordwhitelister.common.lookup.MinecraftPlayerLookup;
import com.discordwhitelister.common.lookup.MojangPlayerLookup;
import com.discordwhitelister.common.storage.JsonWhitelistStorage;
import com.discordwhitelister.common.storage.WhitelistedPlayer;
import com.discordwhitelister.common.storage.WhitelistStorage;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Velocity implementation of Discord Whitelister
 */
@Plugin(
    id = "discordwhitelister",
    name = "Discord Whitelister",
    version = "1.0.0",
    description = "Whitelist players through Discord",
    authors = {"DiscordWhitelister"}
)
public class DiscordWhitelisterVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final CommandManager commandManager;
    
    private DiscordWhitelisterService service;
    private WhitelisterConfig config;
    private WhitelistStorage whitelistStorage;
    private MinecraftPlayerLookup playerLookup;
    private boolean enforceWhitelist = true;

    @Inject
    public DiscordWhitelisterVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, CommandManager commandManager) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.commandManager = commandManager;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Create config directory if it doesn't exist
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create data directory", e);
            return;
        }

        // Create default config if it doesn't exist
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                } else {
                    logger.error("Default config.yml not found in resources");
                    return;
                }
            } catch (IOException e) {
                logger.error("Could not create default config file", e);
                return;
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

        // Register commands
        registerCommands();

        logger.info("Discord Whitelister for Velocity enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (service != null) {
            service.shutdown();
        }
        logger.info("Discord Whitelister for Velocity disabled!");
    }

    private void loadConfiguration() {
        // This is a simplified version - in a real implementation, you would use a proper config library
        // like Configurate to load YAML/HOCON configuration
        
        config = new WhitelisterConfig();
        
        // Set default values
        config.setBotToken("");
        config.setGuildId("");
        config.setChannelId("");
        config.setMessageFormat("whitelist {username}");
        config.setSuccessMessage("You have been whitelisted!");
        config.setRequireRole(false);
        config.setRequiredRoleId("");
        config.setStorageType("json");
        config.setStoragePath(dataDirectory.resolve("whitelist.json").toString());
        
        // In a real implementation, you would load these values from the config file
        
        enforceWhitelist = true;
    }

    private void initializeWhitelistStorage() {
        String storageType = config.getStorageType();
        if (storageType.equalsIgnoreCase("json")) {
            whitelistStorage = new JsonWhitelistStorage(config);
        } else {
            logger.warn("Unknown storage type: {}. Using JSON storage.", storageType);
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

    private void registerCommands() {
        CommandMeta meta = commandManager.metaBuilder("discordwhitelist")
                .aliases("dw")
                .plugin(this)
                .build();
        
        commandManager.register(meta, new DiscordWhitelistCommand());
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!enforceWhitelist) {
            return;
        }

        Player player = event.getPlayer();
        String username = player.getUsername();
        UUID uuid = player.getUniqueId();
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
                event.setResult(LoginEvent.ComponentResult.denied(
                    Component.text("You are not whitelisted on this server!").color(NamedTextColor.RED)
                ));
            }
        } else {
            // This is a Java player
            if (!whitelistStorage.isWhitelisted(username)) {
                event.setResult(LoginEvent.ComponentResult.denied(
                    Component.text("You are not whitelisted on this server!").color(NamedTextColor.RED)
                ));
            }
        }
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
    private class DiscordWhitelistCommand implements SimpleCommand {
        
        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            
            if (args.length == 0) {
                sendHelp(invocation);
                return;
            }
            
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                    handleAddCommand(invocation, args);
                    break;
                case "addbedrock":
                    handleAddBedrockCommand(invocation, args);
                    break;
                case "remove":
                    handleRemoveCommand(invocation, args);
                    break;
                case "list":
                    handleListCommand(invocation, args);
                    break;
                case "status":
                    handleStatusCommand(invocation);
                    break;
                case "reload":
                    handleReloadCommand(invocation);
                    break;
                case "help":
                default:
                    sendHelp(invocation);
                    break;
            }
        }
        
        @Override
        public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
            String[] args = invocation.arguments();
            
            if (args.length == 1) {
                // Subcommands
                List<String> subCommands = Arrays.asList("add", "addbedrock", "remove", "list", "status", "reload", "help");
                return CompletableFuture.completedFuture(filterCompletions(subCommands, args[0]));
            } else if (args.length == 2) {
                // Arguments for subcommands
                String subCommand = args[0].toLowerCase();
                
                if (subCommand.equals("remove")) {
                    // Return list of whitelisted players
                    return CompletableFuture.completedFuture(
                        filterCompletions(
                            getWhitelistedPlayers().stream()
                                .map(WhitelistedPlayer::getUsername)
                                .collect(Collectors.toList()),
                            args[1]
                        )
                    );
                } else if (subCommand.equals("list")) {
                    return CompletableFuture.completedFuture(filterCompletions(Arrays.asList("bedrock"), args[1]));
                }
            }
            
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        private void handleAddCommand(Invocation invocation, String[] args) {
            if (!invocation.source().hasPermission("discordwhitelist.add")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            if (args.length < 2) {
                invocation.source().sendMessage(Component.text("Usage: /discordwhitelist add <username> [discord_id]").color(NamedTextColor.RED));
                return;
            }
            
            String username = args[1];
            String discordId = args.length > 2 ? args[2] : "manual-" + System.currentTimeMillis();
            
            // Try to get UUID from ProxyServer
            Optional<Player> playerOpt = server.getPlayer(username);
            UUID uuid = playerOpt.map(Player::getUniqueId).orElse(null);
            
            // Add to whitelist
            boolean added = addToWhitelist(username, uuid, discordId);
            
            if (added) {
                invocation.source().sendMessage(Component.text("Player " + username + " has been added to the whitelist.").color(NamedTextColor.GREEN));
            } else {
                invocation.source().sendMessage(Component.text("Failed to add player " + username + " to the whitelist. They may already be whitelisted.").color(NamedTextColor.RED));
            }
        }
        
        private void handleAddBedrockCommand(Invocation invocation, String[] args) {
            if (!invocation.source().hasPermission("discordwhitelist.add")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            if (args.length < 3) {
                invocation.source().sendMessage(Component.text("Usage: /discordwhitelist addbedrock <gamertag> <xuid> [discord_id]").color(NamedTextColor.RED));
                return;
            }
            
            String gamertag = args[1];
            String xuid = args[2];
            String discordId = args.length > 3 ? args[3] : "manual-" + System.currentTimeMillis();
            
            // Add Bedrock player to whitelist
            boolean added = addBedrockToWhitelist(gamertag, xuid, discordId);
            
            if (added) {
                invocation.source().sendMessage(Component.text("Bedrock player " + gamertag + " (XUID: " + xuid + ") has been added to the whitelist.").color(NamedTextColor.GREEN));
            } else {
                invocation.source().sendMessage(Component.text("Failed to add Bedrock player " + gamertag + " to the whitelist. They may already be whitelisted.").color(NamedTextColor.RED));
            }
        }
        
        private void handleRemoveCommand(Invocation invocation, String[] args) {
            if (!invocation.source().hasPermission("discordwhitelist.remove")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            if (args.length < 2) {
                invocation.source().sendMessage(Component.text("Usage: /discordwhitelist remove <username>").color(NamedTextColor.RED));
                return;
            }
            
            String username = args[1];
            boolean removed = removeFromWhitelist(username);
            
            if (removed) {
                invocation.source().sendMessage(Component.text("Player " + username + " has been removed from the whitelist.").color(NamedTextColor.GREEN));
            } else {
                invocation.source().sendMessage(Component.text("Failed to remove player " + username + " from the whitelist. They may not be whitelisted.").color(NamedTextColor.RED));
            }
        }
        
        private void handleListCommand(Invocation invocation, String[] args) {
            if (!invocation.source().hasPermission("discordwhitelist.list")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            List<WhitelistedPlayer> players = getWhitelistedPlayers();
            
            if (args.length > 1 && args[1].equalsIgnoreCase("bedrock")) {
                // Show only Bedrock players
                players = getWhitelistedBedrockPlayers();
                
                if (players.isEmpty()) {
                    invocation.source().sendMessage(Component.text("No Bedrock players are whitelisted.").color(NamedTextColor.YELLOW));
                    return;
                }
                
                invocation.source().sendMessage(Component.text("Whitelisted Bedrock Players (" + players.size() + "):").color(NamedTextColor.GREEN));
                for (WhitelistedPlayer player : players) {
                    invocation.source().sendMessage(
                        Component.text("- " + player.getUsername()).color(NamedTextColor.YELLOW)
                            .append(Component.text(" (XUID: " + player.getXuid() + ")").color(NamedTextColor.GRAY))
                    );
                }
            } else {
                // Show all players
                if (players.isEmpty()) {
                    invocation.source().sendMessage(Component.text("No players are whitelisted.").color(NamedTextColor.YELLOW));
                    return;
                }
                
                // Count Java and Bedrock players
                long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
                long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
                
                invocation.source().sendMessage(
                    Component.text("Whitelisted Players (" + players.size() + " total, " + 
                                  javaCount + " Java, " + bedrockCount + " Bedrock):").color(NamedTextColor.GREEN)
                );
                
                // Show Java players first
                for (WhitelistedPlayer player : players) {
                    if (!player.isBedrock()) {
                        invocation.source().sendMessage(
                            Component.text("- " + player.getUsername()).color(NamedTextColor.YELLOW)
                                .append(player.getUuid() != null 
                                    ? Component.text(" (UUID: " + player.getUuid() + ")").color(NamedTextColor.GRAY) 
                                    : Component.empty())
                        );
                    }
                }
                
                // Then show Bedrock players
                if (bedrockCount > 0) {
                    invocation.source().sendMessage(Component.text("Bedrock Players:").color(NamedTextColor.AQUA));
                    for (WhitelistedPlayer player : players) {
                        if (player.isBedrock()) {
                            invocation.source().sendMessage(
                                Component.text("- " + player.getUsername()).color(NamedTextColor.YELLOW)
                                    .append(Component.text(" (XUID: " + player.getXuid() + ")").color(NamedTextColor.GRAY))
                            );
                        }
                    }
                }
            }
        }
        
        private void handleStatusCommand(Invocation invocation) {
            if (!invocation.source().hasPermission("discordwhitelist.status")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            boolean botConnected = getDiscordBot().getJda() != null && 
                                  getDiscordBot().getJda().getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
            
            invocation.source().sendMessage(Component.text("Discord Whitelister Status:").color(NamedTextColor.GREEN));
            invocation.source().sendMessage(
                Component.text("Discord Bot: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(botConnected ? "Connected" : "Disconnected")
                        .color(botConnected ? NamedTextColor.GREEN : NamedTextColor.RED))
            );
            
            List<WhitelistedPlayer> players = getWhitelistedPlayers();
            long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
            long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
            
            invocation.source().sendMessage(
                Component.text("Whitelisted Players: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(players.size() + " (" + javaCount + " Java, " + bedrockCount + " Bedrock)")
                        .color(NamedTextColor.WHITE))
            );
        }
        
        private void handleReloadCommand(Invocation invocation) {
            if (!invocation.source().hasPermission("discordwhitelist.reload")) {
                invocation.source().sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return;
            }
            
            reloadPluginConfig();
            invocation.source().sendMessage(Component.text("Discord Whitelister configuration reloaded.").color(NamedTextColor.GREEN));
        }
        
        private void sendHelp(Invocation invocation) {
            invocation.source().sendMessage(Component.text("===== Discord Whitelister Help =====").color(NamedTextColor.GREEN));
            invocation.source().sendMessage(
                Component.text("/discordwhitelist add <username> [discord_id]").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Add a Java player to the whitelist").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist addbedrock <gamertag> <xuid> [discord_id]").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Add a Bedrock player to the whitelist").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist remove <username>").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Remove a player from the whitelist").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist list [bedrock]").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - List all whitelisted players").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist status").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Check the status of the Discord bot").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist reload").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload the configuration").color(NamedTextColor.WHITE))
            );
            invocation.source().sendMessage(
                Component.text("/discordwhitelist help").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - Show this help message").color(NamedTextColor.WHITE))
            );
        }
    }
    
    private List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) {
            return options;
        }
        
        String lowerInput = input.toLowerCase();
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(lowerInput))
            .collect(Collectors.toList());
    }
}
