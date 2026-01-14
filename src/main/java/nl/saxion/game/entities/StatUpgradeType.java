package nl.saxion.game.entities;

public enum StatUpgradeType {
    SPEED("Swift Boots", 5), // Max 5 levels, +10% per level (different from PassiveItemType.SWIFT_BOOTS)
    DAMAGE("Damage Boost", 5), // Max 5 levels, +5% per level
    MAX_HEALTH("Max Health", 3), // Max 3 levels, +10% per level
    HEALTH_REGEN("Health Regen", 5), // Max 5 levels, +0.1 HP/s per level
    XP_MAGNET("XP Magnet", 2); // Max 2 levels, +25% per level

    public final String title;
    public final int maxLevel;

    StatUpgradeType(String title, int maxLevel) {
        this.title = title;
        this.maxLevel = maxLevel;
    }
    
    // Get title with level indicator
    public String getTitleWithLevel(int level) {
        return title + " Lv" + level;
    }
    
    // Get description for specific level
    public String getDescriptionForLevel(int level) {
        return switch (this) {
            case SPEED -> String.format("+%d%% movement speed", level * 10);
            case DAMAGE -> String.format("+%d%% damage", level * 5);
            case MAX_HEALTH -> String.format("+%d%% max health", level * 10);
            case HEALTH_REGEN -> String.format("+%.1f HP/sec", level * 0.1f);
            case XP_MAGNET -> String.format("+%d%% pickup range", level * 25);
        };
    }
}
