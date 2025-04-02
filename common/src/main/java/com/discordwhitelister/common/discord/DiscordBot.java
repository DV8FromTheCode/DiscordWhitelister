package com.discordwhitelister.common.discord;

import com.discordwhitelister.common.config.WhitelisterConfig;
import com.discordwhitelister.common.lookup.MinecraftPlayerLookup;
import com.discordwhitelister.common.storage.WhitelistStorage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discord bot implementation for whitelist management
 */
public class DiscordBot extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordBot.class);
    private final WhitelisterConfig config;
    private final WhitelistStorage storage;
    private final MinecraftPlayerLookup playerLookup;
    private JDA jda;
    
    // Pattern for Bedrock whitelist requests
    private static final Pattern BEDROCK_PATTERN = Pattern.compile("bedrock\\s+(.+)\\s+xuid:([0-9]+)");
    
    public DiscordBot(WhitelisterConfig config, WhitelistStorage storage, MinecraftPlayerLookup playerLookup) {
        this.config = config;
        this.storage = storage;
        this.playerLookup = playerLookup;
    }
    
    /**
     * Start the Discord bot
     * 
     * @return CompletableFuture that completes when the bot is ready
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            JDABuilder builder = JDABuilder.createDefault(config.getBotToken())
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this);
            
            jda = builder.build();
            
            // Complete the future when the bot is ready
            jda.awaitReady();
            LOGGER.info("Discord bot started successfully");
            future.complete(null);
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Stop the Discord bot
     */
    public void stop() {
        if (jda != null) {
            jda.shutdown();
            LOGGER.info("Discord bot stopped");
        }
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from bots
        if (event.getAuthor().isBot()) {
            return;
        }
        
        // Check if the message is in the configured guild and channel
        if (!isTargetChannel(event)) {
            return;
        }
        
        String content = event.getMessage().getContentRaw();
        
        // Check for Bedrock whitelist request first
        Matcher bedrockMatcher = BEDROCK_PATTERN.matcher(content);
        if (bedrockMatcher.find()) {
            String username = bedrockMatcher.group(1).trim();
            String xuid = bedrockMatcher.group(2).trim();
            processBedrockWhitelistRequest(event, username, xuid);
            return;
        }
        
        // Check for Java Edition whitelist request
        String messageFormat = config.getMessageFormat().replace("{username}", "(.+)");
        Pattern pattern = Pattern.compile(messageFormat);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.matches()) {
            String username = matcher.group(1).trim();
            processWhitelistRequest(event, username);
        } else {
            // If message doesn't match any pattern, provide help
            if (content.toLowerCase().contains("help") || content.toLowerCase().contains("whitelist")) {
                sendHelpMessage(event);
            }
        }
    }
    
    private void sendHelpMessage(MessageReceivedEvent event) {
        StringBuilder help = new StringBuilder("**Discord Whitelister Help**\n\n");
        help.append("To whitelist your Java Edition account:\n");
        help.append("`").append(config.getMessageFormat().replace("{username}", "YourMinecraftUsername")).append("`\n\n");
        help.append("To whitelist your Bedrock Edition account:\n");
        help.append("`bedrock YourGamertag xuid:1234567890`\n");
        help.append("(Replace YourGamertag with your Bedrock username and the number with your XUID)\n\n");
        help.append("You can find your XUID using websites like https://cxkes.me/xbox/xuid or https://www.cxkes.me/xbox/xuid");
        
        event.getMessage().reply(help.toString()).queue();
    }
    
    private boolean isTargetChannel(MessageReceivedEvent event) {
        String guildId = config.getGuildId();
        String channelId = config.getChannelId();
        
        if (guildId == null || channelId == null) {
            return false;
        }
        
        return event.getGuild().getId().equals(guildId) && 
               event.getChannel().getId().equals(channelId);
    }
    
    private void processWhitelistRequest(MessageReceivedEvent event, String username) {
        // Check if the user has the required role if enabled
        if (config.isRequireRole() && !hasRequiredRole(event.getMember())) {
            event.getMessage().reply("You don't have the required role to use this command.").queue();
            return;
        }
        
        // Check if the username is valid
        if (!isValidMinecraftUsername(username)) {
            event.getMessage().reply("Invalid Minecraft username. Usernames must be 3-16 characters and contain only letters, numbers, and underscores.").queue();
            return;
        }
        
        // Check if already whitelisted
        if (storage.isWhitelisted(username)) {
            event.getMessage().reply("This username is already whitelisted.").queue();
            return;
        }
        
        // Look up UUID if possible
        playerLookup.lookupPlayer(username).thenAccept(playerInfo -> {
            if (playerInfo != null) {
                // Add to whitelist with UUID
                boolean added = storage.addPlayer(playerInfo.username(), playerInfo.uuid(), event.getAuthor().getId());
                if (added) {
                    event.getMessage().reply(config.getSuccessMessage()).queue();
                    LOGGER.info("Added player {} ({}) to whitelist, requested by Discord user {}", 
                                playerInfo.username(), playerInfo.uuid(), event.getAuthor().getId());
                } else {
                    event.getMessage().reply("Failed to add you to the whitelist. Please try again later.").queue();
                }
            } else {
                // Add to whitelist without UUID (offline mode)
                boolean added = storage.addPlayer(username, null, event.getAuthor().getId());
                if (added) {
                    event.getMessage().reply(config.getSuccessMessage() + " (Note: UUID lookup failed, added in offline mode)").queue();
                    LOGGER.info("Added player {} to whitelist (offline mode), requested by Discord user {}", 
                                username, event.getAuthor().getId());
                } else {
                    event.getMessage().reply("Failed to add you to the whitelist. Please try again later.").queue();
                }
            }
        }).exceptionally(ex -> {
            event.getMessage().reply("An error occurred while processing your request. Please try again later.").queue();
            LOGGER.error("Error processing whitelist request", ex);
            return null;
        });
    }
    
    private void processBedrockWhitelistRequest(MessageReceivedEvent event, String username, String xuid) {
        // Check if the user has the required role if enabled
        if (config.isRequireRole() && !hasRequiredRole(event.getMember())) {
            event.getMessage().reply("You don't have the required role to use this command.").queue();
            return;
        }
        
        // Check if the username is valid (Bedrock usernames can have spaces)
        if (username.length() < 1 || username.length() > 16) {
            event.getMessage().reply("Invalid Bedrock gamertag. Gamertags must be 1-16 characters.").queue();
            return;
        }
        
        // Check if XUID is valid (should be a numeric string)
        if (!xuid.matches("^[0-9]+$")) {
            event.getMessage().reply("Invalid XUID format. XUID should be a numeric value.").queue();
            return;
        }
        
        // Check if already whitelisted
        if (storage.isWhitelisted(username) || storage.isBedrockWhitelisted(xuid)) {
            event.getMessage().reply("This Bedrock account is already whitelisted.").queue();
            return;
        }
        
        // Add to whitelist
        boolean added = storage.addBedrockPlayer(username, xuid, event.getAuthor().getId());
        if (added) {
            event.getMessage().reply("Your Bedrock account has been whitelisted! You can now join the server.").queue();
            LOGGER.info("Added Bedrock player {} (XUID: {}) to whitelist, requested by Discord user {}", 
                        username, xuid, event.getAuthor().getId());
        } else {
            event.getMessage().reply("Failed to add your Bedrock account to the whitelist. Please try again later.").queue();
        }
    }
    
    private boolean hasRequiredRole(Member member) {
        if (member == null) {
            return false;
        }
        
        String requiredRoleId = config.getRequiredRoleId();
        if (requiredRoleId == null || requiredRoleId.isEmpty()) {
            return true;
        }
        
        return member.getRoles().stream()
            .anyMatch(role -> role.getId().equals(requiredRoleId));
    }
    
    private boolean isValidMinecraftUsername(String username) {
        // Minecraft usernames are 3-16 characters and can only contain letters, numbers, and underscores
        return username.matches("^[a-zA-Z0-9_]{3,16}$");
    }
    
    /**
     * Get the JDA instance
     * 
     * @return JDA instance
     */
    public JDA getJda() {
        return jda;
    }
}
