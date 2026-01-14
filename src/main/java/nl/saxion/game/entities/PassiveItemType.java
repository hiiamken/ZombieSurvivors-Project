package nl.saxion.game.entities;

/**
 * Enum for passive item types in the game.
 * Each passive item provides different stat bonuses when collected.
 * All passive items have max level 5.
 * 
 * Uses texture keys for rendering (no emojis - not supported by default font).
 */
public enum PassiveItemType {
    // Damage boost - Power Herb
    POWER_HERB("Power Herb", 5, "+10% damage per level", "passive_powerherb"),
    
    // Damage reduction - Iron Shield
    IRON_SHIELD("Iron Shield", 5, "-5% damage taken per level", "passive_ironshield"),
    
    // Movement speed - Swift Boots
    SWIFT_BOOTS("Swift Boots", 5, "+10% movement speed per level", "passive_swiftboots"),
    
    // Critical chance - Lucky Coin
    LUCKY_COIN("Lucky Coin", 5, "+5% critical chance per level", "passive_luckycoin"),
    
    // XP pickup range - Magnet Stone
    MAGNET_STONE("Magnet Stone", 5, "+20% pickup range per level", "passive_magnetstone"),
    
    // Health regeneration - Life Essence
    LIFE_ESSENCE("Life Essence", 5, "+0.2 HP/sec per level", "passive_lifeessence"),
    
    // Max health increase - Vitality Core
    VITALITY_CORE("Vitality Core", 5, "+20% max HP per level", "passive_vitalitycore");

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
            case POWER_HERB -> String.format("+%d%% damage", level * 10);
            case IRON_SHIELD -> String.format("-%d%% damage taken", level * 5);
            case SWIFT_BOOTS -> String.format("+%d%% movement speed", level * 10);
            case LUCKY_COIN -> String.format("+%d%% critical chance", level * 5);
            case MAGNET_STONE -> String.format("+%d%% pickup range", level * 20);
            case LIFE_ESSENCE -> String.format("+%.1f HP/sec", level * 0.2f);
            case VITALITY_CORE -> String.format("+%d%% max HP", level * 20);
        };
    }

    /**
     * Get the stat multiplier for a specific level.
     * Returns the multiplier to apply to the base stat.
     */
    public float getMultiplierForLevel(int level) {
        return switch (this) {
            case POWER_HERB -> 1f + (level * 0.10f);     // +10% per level
            case IRON_SHIELD -> 1f - (level * 0.05f);       // -5% per level (damage reduction)
            case SWIFT_BOOTS -> 1f + (level * 0.10f);       // +10% per level
            case LUCKY_COIN -> level * 0.05f;             // +5% per level (additive crit chance)
            case MAGNET_STONE -> 1f + (level * 0.20f);  // +20% per level
            case LIFE_ESSENCE -> level * 0.2f;           // +0.2 HP/s per level (additive)
            case VITALITY_CORE -> 1f + (level * 0.20f); // +20% per level
        };
    }

    /**
     * Get RGB color for UI theming.
     */
    public int[] getThemeRGB() {
        return switch (this) {
            case POWER_HERB -> new int[]{231, 76, 60};    // Red (damage)
            case IRON_SHIELD -> new int[]{149, 165, 166};    // Gray (defense)
            case SWIFT_BOOTS -> new int[]{52, 152, 219};     // Blue (speed)
            case LUCKY_COIN -> new int[]{46, 204, 113};    // Green (luck)
            case MAGNET_STONE -> new int[]{155, 89, 182}; // Purple (magnet)
            case LIFE_ESSENCE -> new int[]{241, 148, 138}; // Pink (healing)
            case VITALITY_CORE -> new int[]{241, 196, 15}; // Yellow (health)
        };
    }

    /**
     * Get text color name for UI.
     */
    public String getThemeTextColor() {
        return switch (this) {
            case POWER_HERB -> "red-500";
            case IRON_SHIELD -> "gray-400";
            case SWIFT_BOOTS -> "blue-500";
            case LUCKY_COIN -> "green-500";
            case MAGNET_STONE -> "purple-500";
            case LIFE_ESSENCE -> "pink-500";
            case VITALITY_CORE -> "yellow-500";
        };
    }
    
    /**
     * Get texture key for rendering the icon.
     */
    public String getTextureKey() {
        return textureKey;
    }
}
