package com.discordwhitelister.common.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading and working with the DiscordWhitelister logo
 * across different platforms
 */
public class LogoUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoUtil.class);
    private static final String LOGO_PATH = "logo.png";
    private static BufferedImage logoImage;
    
    /**
     * Load the logo image from resources
     * 
     * @return The logo as a BufferedImage, or null if loading failed
     */
    public static BufferedImage getLogoImage() {
        if (logoImage == null) {
            try (InputStream is = LogoUtil.class.getClassLoader().getResourceAsStream(LOGO_PATH)) {
                if (is != null) {
                    logoImage = ImageIO.read(is);
                    LOGGER.info("Successfully loaded DiscordWhitelister logo");
                } else {
                    LOGGER.error("Could not find logo resource at: {}", LOGO_PATH);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load logo image", e);
            }
        }
        return logoImage;
    }
    
    /**
     * Check if the logo is available
     * 
     * @return true if the logo was successfully loaded
     */
    public static boolean isLogoAvailable() {
        return getLogoImage() != null;
    }
    
    /**
     * Get the resource path to the logo
     * 
     * @return The path to the logo resource
     */
    public static String getLogoPath() {
        return LOGO_PATH;
    }
}
