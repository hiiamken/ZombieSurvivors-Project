package nl.saxion.game.entities;

/**
 * Represents a passive item that the player owns.
 * Passive items provide permanent stat bonuses and can be leveled up.
 * 
 * When combined with a max level weapon, certain passive item combinations
 * can trigger weapon evolution.
 */
public class PassiveItem {
    
    private PassiveItemType type;
    private int level;
    
    public PassiveItem(PassiveItemType type) {
        this.type = type;
        this.level = 1;
    }
    
    public PassiveItem(PassiveItemType type, int level) {
        this.type = type;
        this.level = Math.min(level, type.maxLevel);
    }
    
    /**
     * Level up this passive item.
     * @return true if level up was successful, false if already at max level
     */
    public boolean levelUp() {
        if (level < type.maxLevel) {
            level++;
            return true;
        }
        return false;
    }
    
    /**
     * Check if this passive item is at max level.
     */
    public boolean isMaxLevel() {
        return level >= type.maxLevel;
    }
    
    /**
     * Get the current stat multiplier from this passive item.
     */
    public float getMultiplier() {
        return type.getMultiplierForLevel(level);
    }
    
    /**
     * Get display title for level-up menu.
     */
    public String getDisplayTitle() {
        return type.getTitleWithLevel(level);
    }
    
    /**
     * Get display title for next level (used in upgrade menu).
     */
    public String getNextLevelTitle() {
        if (level < type.maxLevel) {
            return type.getTitleWithLevel(level + 1);
        }
        return type.getTitleWithLevel(level) + " (MAX)";
    }
    
    /**
     * Get description for current level.
     */
    public String getDescription() {
        return type.getDescriptionForLevel(level);
    }
    
    /**
     * Get description for next level (used in upgrade menu).
     */
    public String getNextLevelDescription() {
        if (level < type.maxLevel) {
            return type.getDescriptionForLevel(level + 1);
        }
        return type.getDescriptionForLevel(level) + " (MAX)";
    }
    
    // Getters
    public PassiveItemType getType() {
        return type;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getMaxLevel() {
        return type.maxLevel;
    }
    
    public String getIcon() {
        return type.textureKey;
    }
    
    public String getTextureKey() {
        return type.textureKey;
    }
}
