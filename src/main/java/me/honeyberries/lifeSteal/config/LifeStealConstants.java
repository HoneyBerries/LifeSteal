package me.honeyberries.lifeSteal.config;

/**
 * Constants used throughout the LifeSteal plugin.
 * Centralizes magic numbers and strings for better maintainability.
 */
public final class LifeStealConstants {
    
    // Prevent instantiation
    private LifeStealConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // Health constants
    public static final double HEALTH_POINTS_PER_HEART = 2.0;
    public static final double MIN_HEALTH_EPSILON = 0.01; // For floating point comparisons
    public static final double DEFAULT_MIN_HEALTH = 1.0;
    public static final double DEFAULT_MAX_HEALTH = 0.0; // 0 means disabled
    
    // Recipe keys
    public static final String HEART_RECIPE_KEY = "custom_heart_recipe";
    public static final String REVIVAL_RECIPE_KEY = "custom_revival_recipe";
    
    // Persistent data keys
    public static final String HEART_ID = "heart";
    public static final String REVIVAL_ID = "revival";
    public static final String ELIMINATED_KEY = "eliminated";
    
    // Default item materials
    public static final String DEFAULT_HEART_ITEM = "NETHER_STAR";
    public static final String DEFAULT_REVIVAL_ITEM = "BEACON";
    
    // Default item names
    public static final String DEFAULT_HEART_NAME = "Heart";
    public static final String DEFAULT_REVIVAL_NAME = "Revival Beacon";
    
    // Sound volumes
    public static final float SOUND_VOLUME = 1.0f;
    public static final float SOUND_PITCH = 1.0f;
    
    // GUI titles
    public static final String REVIVAL_GUI_TITLE = "Select Player to Revive";
    
    // Scheduler delays
    public static final long RECIPE_DISCOVERY_INITIAL_DELAY = 1L;
    public static final long RECIPE_DISCOVERY_REPEAT_INTERVAL = 1L;
}
