package com.discordwhitelister.spigot.commands;

import com.discordwhitelister.common.storage.WhitelistedPlayer;
import com.discordwhitelister.spigot.DiscordWhitelisterPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command handler for the /discordwhitelist command
 */
public class DiscordWhitelistCommand implements CommandExecutor, TabCompleter {
    private final DiscordWhitelisterPlugin plugin;
    
    public DiscordWhitelistCommand(DiscordWhitelisterPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                return handleAddCommand(sender, args);
            case "addbedrock":
                return handleAddBedrockCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "status":
                return handleStatusCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "gui":
                return handleGuiCommand(sender);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("discordwhitelist.add")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /discordwhitelist add <username> [discord_id]");
            return true;
        }
        
        String username = args[1];
        String discordId = args.length > 2 ? args[2] : "manual-" + System.currentTimeMillis();
        
        // Try to get UUID from Mojang API
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        UUID uuid = offlinePlayer.getUniqueId();
        
        // Add to whitelist
        boolean added = plugin.addToWhitelist(username, uuid, discordId);
        
        if (added) {
            sender.sendMessage(ChatColor.GREEN + "Player " + username + " has been added to the whitelist.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to add player " + username + " to the whitelist. They may already be whitelisted.");
        }
        
        return true;
    }
    
    private boolean handleAddBedrockCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("discordwhitelist.add")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /discordwhitelist addbedrock <gamertag> <xuid> [discord_id]");
            return true;
        }
        
        String gamertag = args[1];
        String xuid = args[2];
        String discordId = args.length > 3 ? args[3] : "manual-" + System.currentTimeMillis();
        
        // Add Bedrock player to whitelist
        boolean added = plugin.addBedrockToWhitelist(gamertag, xuid, discordId);
        
        if (added) {
            sender.sendMessage(ChatColor.GREEN + "Bedrock player " + gamertag + " (XUID: " + xuid + ") has been added to the whitelist.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to add Bedrock player " + gamertag + " to the whitelist. They may already be whitelisted.");
        }
        
        return true;
    }
    
    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("discordwhitelist.remove")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /discordwhitelist remove <username>");
            return true;
        }
        
        String username = args[1];
        boolean removed = plugin.removeFromWhitelist(username);
        
        if (removed) {
            sender.sendMessage(ChatColor.GREEN + "Player " + username + " has been removed from the whitelist.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to remove player " + username + " from the whitelist. They may not be whitelisted.");
        }
        
        return true;
    }
    
    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("discordwhitelist.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        List<WhitelistedPlayer> players = plugin.getWhitelistedPlayers();
        
        if (args.length > 1 && args[1].equalsIgnoreCase("bedrock")) {
            // Show only Bedrock players
            players = plugin.getWhitelistedBedrockPlayers();
            
            if (players.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No Bedrock players are whitelisted.");
                return true;
            }
            
            sender.sendMessage(ChatColor.GREEN + "Whitelisted Bedrock Players (" + players.size() + "):");
            for (WhitelistedPlayer player : players) {
                sender.sendMessage(ChatColor.YELLOW + "- " + player.getUsername() + 
                                  ChatColor.GRAY + " (XUID: " + player.getXuid() + ")");
            }
        } else {
            // Show all players
            if (players.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No players are whitelisted.");
                return true;
            }
            
            // Count Java and Bedrock players
            long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
            long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
            
            sender.sendMessage(ChatColor.GREEN + "Whitelisted Players (" + players.size() + " total, " + 
                              javaCount + " Java, " + bedrockCount + " Bedrock):");
            
            // Show Java players first
            for (WhitelistedPlayer player : players) {
                if (!player.isBedrock()) {
                    sender.sendMessage(ChatColor.YELLOW + "- " + player.getUsername() + 
                                      (player.getUuid() != null ? ChatColor.GRAY + " (UUID: " + player.getUuid() + ")" : ""));
                }
            }
            
            // Then show Bedrock players
            if (bedrockCount > 0) {
                sender.sendMessage(ChatColor.AQUA + "Bedrock Players:");
                for (WhitelistedPlayer player : players) {
                    if (player.isBedrock()) {
                        sender.sendMessage(ChatColor.YELLOW + "- " + player.getUsername() + 
                                          ChatColor.GRAY + " (XUID: " + player.getXuid() + ")");
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("discordwhitelist.status")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        boolean botConnected = plugin.getDiscordBot().getJda() != null && 
                              plugin.getDiscordBot().getJda().getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
        
        sender.sendMessage(ChatColor.GREEN + "Discord Whitelister Status:");
        sender.sendMessage(ChatColor.YELLOW + "Discord Bot: " + 
                          (botConnected ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected"));
        
        List<WhitelistedPlayer> players = plugin.getWhitelistedPlayers();
        long javaCount = players.stream().filter(p -> !p.isBedrock()).count();
        long bedrockCount = players.stream().filter(WhitelistedPlayer::isBedrock).count();
        
        sender.sendMessage(ChatColor.YELLOW + "Whitelisted Players: " + ChatColor.WHITE + players.size() + 
                          " (" + javaCount + " Java, " + bedrockCount + " Bedrock)");
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("discordwhitelist.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "Discord Whitelister configuration reloaded.");
        
        return true;
    }
    
    private boolean handleGuiCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        if (!sender.hasPermission("discordwhitelist.gui")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.openWhitelistManager(player);
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "===== Discord Whitelister Help =====");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist add <username> [discord_id]" + ChatColor.WHITE + " - Add a Java player to the whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist addbedrock <gamertag> <xuid> [discord_id]" + ChatColor.WHITE + " - Add a Bedrock player to the whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist remove <username>" + ChatColor.WHITE + " - Remove a player from the whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist list [bedrock]" + ChatColor.WHITE + " - List all whitelisted players");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist status" + ChatColor.WHITE + " - Check the status of the Discord bot");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist reload" + ChatColor.WHITE + " - Reload the configuration");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist gui" + ChatColor.WHITE + " - Open the whitelist manager GUI");
        sender.sendMessage(ChatColor.YELLOW + "/discordwhitelist help" + ChatColor.WHITE + " - Show this help message");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subCommands = Arrays.asList("add", "addbedrock", "remove", "list", "status", "reload", "gui", "help");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            // Arguments for subcommands
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("remove")) {
                // Return list of whitelisted players
                return filterCompletions(
                    plugin.getWhitelistedPlayers().stream()
                        .map(WhitelistedPlayer::getUsername)
                        .collect(Collectors.toList()),
                    args[1]
                );
            } else if (subCommand.equals("list")) {
                return filterCompletions(Arrays.asList("bedrock"), args[1]);
            }
        }
        
        return completions;
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
