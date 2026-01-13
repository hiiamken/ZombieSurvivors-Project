package nl.saxion.game.entities;

/**
 * Enum for passive item types in the game.
 * Each passive item provides different stat bonuses when collected.
 * All passive items have max level 5.
 * 
 * Uses texture keys for rendering (no emojis - not supported by default font).
 */
public enum PassiveItemType {
    // Damage boost - Spinach
    SPINACH("Spinach", 5, "+10% damage per level", "passive_spinach"),
    
    // Damage reduction - Armor
    ARMOR("Armor", 5, "-5% damage taken per level", "passive_armor"),
    
    // Movement speed - Wings
    WINGS("Wings", 5, "+10% movement speed per level", "passive_wings"),
    
    // Critical chance - Clover (NEW)
    CLOVER("Clover", 5, "+5% critical chance per level", "passive_clover"),
    
    // XP pickup range - Attractorb (NEW)
    ATTRACTORB("Attractorb", 5, "+20% pickup range per level", "passive_attractorb"),
    
    // Health regeneration - Pummarola
    PUMMAROLA("Pummarola", 5, "+0.2 HP/sec per level", "passive_pummarola"),
    
    // Max health increase - Hollow Heart
    HOLLOW_HEART("Hollow Heart", 5, "+20% max HP per level", "passive_hollowheart");

    public final String displayName;
    public final int maxLevel;
    public final String baseDescription;
    public final String textureKey; // Texture key for rendering icon

    PassiveItemType(String displayName, int maxLevel, String baseDescription, String textureKey) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.baseDescription = baseDescription;
        this.textureKey = textureKey;
    }

    /**
     * Get the title with level indicator for level-up menu.
     */
    public String getTitleWithLevel(int level) {
        return displayName + " Lv" + level;
    }

    /**
     * Get description for a specific level.
     */
    public String getDescriptionForLevel(int level) {
        return switch (this) {
            case SPINACH -> String.format("+%d%% damage", level * 10);
            case ARMOR -> String.format("-%d%% damage taken", level * 5);
            case WINGS -> String.format("+%d%% movement speed", level * 10);
            case CLOVER -> String.format("+%d%% critical chance", level * 5);
            case ATTRACTORB -> String.format("+%d%% pickup range", level * 20);
            case PUMMAROLA -> String.format("+%.1f HP/sec", level * 0.2f);
            case HOLLOW_HEART -> String.format("+%d%% max HP", level * 20);
        };
    }

    /**
     * Get the stat multiplier for a specific level.
     * Returns the multiplier to apply to the base stat.
     */
    public float getMultiplierForLevel(int level) {
        return switch (this) {
            case SPINACH -> 1f + (level * 0.10f);     // +10% per level
            case ARMOR -> 1f - (level * 0.05f);       // -5% per level (damage reduction)
            case WINGS -> 1f + (level * 0.10f);       // +10% per level
            case CLOVER -> level * 0.05f;             // +5% per level (additive crit chance)
            case ATTRACTORB -> 1f + (level * 0.20f);  // +20% per level
            case PUMMAROLA -> level * 0.2f;           // +0.2 HP/s per level (additive)
            case HOLLOW_HEART -> 1f + (level * 0.20f); // +20% per level
        };
    }

    /**
     * Get RGB color for UI theming.
     */
    public int[] getThemeRGB() {
        return switch (this) {
            case SPINACH -> new int[]{231, 76, 60};    // Red (damage)
            case ARMOR -> new int[]{149, 165, 166};    // Gray (defense)
            case WINGS -> new int[]{52, 152, 219};     // Blue (speed)
            case CLOVER -> new int[]{46, 204, 113};    // Green (luck)
            case ATTRACTORB -> new int[]{155, 89, 182}; // Purple (magnet)
            case PUMMAROLA -> new int[]{241, 148, 138}; // Pink (healing)
            case HOLLOW_HEART -> new int[]{241, 196, 15}; // Yellow (health)
        };
    }

    /**
     * Get text color name for UI.
     */
    public String getThemeTextColor() {
        return switch (this) {
            case SPINACH -> "red-500";
            case ARMOR -> "gray-400";
            case WINGS -> "blue-500";
            case CLOVER -> "green-500";
            case ATTRACTORB -> "purple-500";
            case PUMMAROLA -> "pink-500";
            case HOLLOW_HEART -> "yellow-500";
        };
    }
    
    /**
     * Get texture key for rendering the icon.
     */
    public String getTextureKey() {
        return textureKey;
    }
}
