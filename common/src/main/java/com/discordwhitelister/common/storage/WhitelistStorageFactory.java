package com.discordwhitelister.common.storage;

import com.discordwhitelister.common.config.WhitelisterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating whitelist storage implementations
 */
public class WhitelistStorageFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistStorageFactory.class);
    
    /**
     * Create a whitelist storage implementation based on the configuration
     * 
     * @param config The whitelist configuration
     * @return The appropriate whitelist storage implementation
     */
    public static WhitelistStorage createStorage(WhitelisterConfig config) {
        String storageType = config.getStorageType();
        
        if (storageType == null || storageType.isEmpty()) {
            LOGGER.warn("No storage type specified, defaulting to JSON");
            storageType = "json";
        }
        
        WhitelistStorage storage;
        
        switch (storageType.toLowerCase()) {
            case "database":
                LOGGER.info("Using database storage for whitelist");
                storage = new DatabaseWhitelistStorage(config);
                break;
            case "json":
            default:
                LOGGER.info("Using JSON storage for whitelist");
                storage = new JsonWhitelistStorage(config);
                break;
        }
        
        // Initialize the storage
        storage.initialize();
        
        return storage;
    }
}
