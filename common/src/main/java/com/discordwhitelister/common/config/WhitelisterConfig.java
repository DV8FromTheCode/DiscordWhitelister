package com.discordwhitelister.common.config;

/**
 * Configuration class for Discord Whitelister
 * Contains settings that are common across all platforms
 */
public class WhitelisterConfig {
    private String botToken;
    private String guildId;
    private String channelId;
    private String storageType; // "json" or "database"
    private String databaseUrl;
    private String databaseUser;
    private String databasePassword;
    private String jsonFilePath;
    private String messageFormat;
    private String successMessage;
    private boolean requireRole;
    private String requiredRoleId;
    private String storagePath;
    
    // Default constructor
    public WhitelisterConfig() {
        this.messageFormat = "Please whitelist my Minecraft username: {username}";
        this.successMessage = "You have been whitelisted! You can now join the server.";
        this.storageType = "json";
        this.requireRole = false;
    }
    
    // Getters and setters
    public String getBotToken() {
        return botToken;
    }
    
    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }
    
    public String getGuildId() {
        return guildId;
    }
    
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public String getStorageType() {
        return storageType;
    }
    
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }
    
    public String getDatabaseUser() {
        return databaseUser;
    }
    
    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }
    
    public String getDatabasePassword() {
        return databasePassword;
    }
    
    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }
    
    public String getJsonFilePath() {
        return jsonFilePath;
    }
    
    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }
    
    public String getMessageFormat() {
        return messageFormat;
    }
    
    public void setMessageFormat(String messageFormat) {
        this.messageFormat = messageFormat;
    }
    
    public String getSuccessMessage() {
        return successMessage;
    }
    
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
    
    public boolean isRequireRole() {
        return requireRole;
    }
    
    public void setRequireRole(boolean requireRole) {
        this.requireRole = requireRole;
    }
    
    public String getRequiredRoleId() {
        return requiredRoleId;
    }
    
    public void setRequiredRoleId(String requiredRoleId) {
        this.requiredRoleId = requiredRoleId;
    }
    
    public String getStoragePath() {
        if (storageType.equalsIgnoreCase("json")) {
            return jsonFilePath;
        }
        return storagePath;
    }
    
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        // Also set the JSON file path for backward compatibility
        this.jsonFilePath = storagePath;
    }
}
