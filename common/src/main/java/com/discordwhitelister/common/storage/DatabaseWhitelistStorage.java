package com.discordwhitelister.common.storage;

import com.discordwhitelister.common.config.WhitelisterConfig;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database implementation of whitelist storage
 */
public class DatabaseWhitelistStorage implements WhitelistStorage, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseWhitelistStorage.class);
    private final WhitelisterConfig config;
    private Connection connection;
    
    public DatabaseWhitelistStorage(WhitelisterConfig config) {
        this.config = config;
    }
    
    @Override
    public void initialize() {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish connection
            connection = DriverManager.getConnection(
                config.getDatabaseUrl(),
                config.getDatabaseUser(),
                config.getDatabasePassword()
            );
            
            // Create table if it doesn't exist
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS whitelist (" +
                    "username VARCHAR(16) PRIMARY KEY, " +
                    "uuid VARCHAR(36), " +
                    "xuid VARCHAR(20), " +
                    "discord_id VARCHAR(20) NOT NULL, " +
                    "whitelisted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "is_bedrock BOOLEAN DEFAULT FALSE" +
                    ")"
                );
            }
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.error("Failed to initialize database connection", e);
        }
    }
    
    @Override
    public void save() {
        // No-op for database storage as changes are saved immediately
    }
    
    @Override
    public boolean addPlayer(String username, UUID uuid, String discordId) {
        if (isWhitelisted(username)) {
            return false;
        }
        
        try {
            String sql = "INSERT INTO whitelist (username, uuid, discord_id, is_bedrock) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, uuid != null ? uuid.toString() : null);
                pstmt.setString(3, discordId);
                pstmt.setBoolean(4, false);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to add player to whitelist", e);
            return false;
        }
    }
    
    @Override
    public boolean addBedrockPlayer(String username, String xuid, String discordId) {
        if (isWhitelisted(username) || isBedrockWhitelisted(xuid)) {
            return false;
        }
        
        try {
            String sql = "INSERT INTO whitelist (username, xuid, discord_id, is_bedrock) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, xuid);
                pstmt.setString(3, discordId);
                pstmt.setBoolean(4, true);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to add Bedrock player to whitelist", e);
            return false;
        }
    }
    
    @Override
    public boolean removePlayer(String username) {
        try {
            String sql = "DELETE FROM whitelist WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to remove player from whitelist", e);
            return false;
        }
    }
    
    @Override
    public boolean isWhitelisted(String username) {
        try {
            String sql = "SELECT COUNT(*) FROM whitelist WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if player is whitelisted", e);
        }
        
        return false;
    }
    
    @Override
    public boolean isBedrockWhitelisted(String xuid) {
        try {
            String sql = "SELECT COUNT(*) FROM whitelist WHERE xuid = ? AND is_bedrock = TRUE";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, xuid);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if Bedrock player is whitelisted", e);
        }
        
        return false;
    }
    
    @Override
    public List<WhitelistedPlayer> getAllPlayers() {
        List<WhitelistedPlayer> players = new ArrayList<>();
        
        try {
            String sql = "SELECT username, uuid, xuid, discord_id, whitelisted_at, is_bedrock FROM whitelist";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String username = rs.getString("username");
                    String uuidStr = rs.getString("uuid");
                    UUID uuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
                    String xuid = rs.getString("xuid");
                    String discordId = rs.getString("discord_id");
                    Timestamp timestamp = rs.getTimestamp("whitelisted_at");
                    Instant whitelistedAt = timestamp != null ? timestamp.toInstant() : Instant.now();
                    boolean isBedrock = rs.getBoolean("is_bedrock");
                    
                    players.add(new WhitelistedPlayer(username, uuid, xuid, discordId, whitelistedAt, isBedrock));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get all whitelisted players", e);
        }
        
        return players;
    }
    
    @Override
    public List<WhitelistedPlayer> getAllBedrockPlayers() {
        List<WhitelistedPlayer> allPlayers = getAllPlayers();
        return allPlayers.stream()
            .filter(WhitelistedPlayer::isBedrock)
            .collect(Collectors.toList());
    }
    
    /**
     * Close the database connection
     */
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("Failed to close database connection", e);
            }
        }
    }
}
