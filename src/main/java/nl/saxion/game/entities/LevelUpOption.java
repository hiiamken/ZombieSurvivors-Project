package nl.saxion.game.entities;

/**
 * Represents a level-up option that can be selected by the player.
 * 
 * Supports three types of upgrades:
 * 1. STAT - Basic stat upgrades (speed, damage, health, etc.)
 * 2. WEAPON - Weapon level upgrades (bullet count, fire rate, pierce, etc.)
 * 3. PASSIVE - Passive item upgrades (Spinach, Armor, Wings, etc.)
 * 4. EVOLUTION - Weapon evolution (when weapon MAX + all passives MAX)
 */
public class LevelUpOption {

    public enum Type { 
        STAT,           // Basic stat upgrade
        WEAPON,         // Weapon level upgrade
        PASSIVE,        // Passive item upgrade/acquisition
        EVOLUTION,      // Weapon evolution
        BONUS_POINTS,   // Post-evolution: random points per level
        BONUS_HEALTH    // Post-evolution: +25 HP per level
    }

    public final Type type;
    public final String title;
    public final String description;
    public final String previewText;
    public final String icon;
    public final int[] themeRGB;
    public final String themeTextColor;
    
    // For STAT type
    public final StatUpgradeType stat;
    public final int currentLevel;
    public final int nextLevel;
    
    // For PASSIVE type
    public final PassiveItemType passiveItem;
    public final int passiveCurrentLevel;
    public final int passiveNextLevel;
    
    // For WEAPON type
    public final int weaponCurrentLevel;
    public final int weaponNextLevel;

    // ============================================
    // CONSTRUCTORS
    // ============================================

    /**
     * Constructor for STAT upgrade option (legacy support).
     */
    public LevelUpOption(StatUpgradeType stat, int currentLevel) {
        this.type = Type.STAT;
        this.stat = stat;
        this.currentLevel = currentLevel;
        this.nextLevel = currentLevel + 1;
        this.title = stat.getTitleWithLevel(nextLevel);
        this.description = stat.getDescriptionForLevel(nextLevel);
        this.previewText = buildStatPreviewText(stat, currentLevel, nextLevel);
        this.icon = getStatIcon(stat);
        this.themeRGB = getStatThemeRGB(stat);
        this.themeTextColor = getStatThemeTextColor(stat);
        
        // Not used for STAT
        this.passiveItem = null;
        this.passiveCurrentLevel = 0;
        this.passiveNextLevel = 0;
        this.weaponCurrentLevel = 0;
        this.weaponNextLevel = 0;
    }

    /**
     * Constructor for WEAPON upgrade option.
     */
    public LevelUpOption(int weaponCurrentLevel) {
        this.type = Type.WEAPON;
        this.weaponCurrentLevel = weaponCurrentLevel;
        this.weaponNextLevel = weaponCurrentLevel + 1;
        this.title = WeaponUpgrade.getTitleForLevel(weaponNextLevel);
        this.description = WeaponUpgrade.getDescriptionForLevel(weaponNextLevel);
        this.previewText = WeaponUpgrade.getPreviewTextForLevel(weaponCurrentLevel, weaponNextLevel);
        this.icon = "piston_icon"; // Use piston texture for weapon
        this.themeRGB = new int[]{255, 165, 0}; // Orange
        this.themeTextColor = "orange-500";
        
        // Not used for WEAPON
        this.stat = null;
        this.currentLevel = 0;
        this.nextLevel = 0;
        this.passiveItem = null;
        this.passiveCurrentLevel = 0;
        this.passiveNextLevel = 0;
    }

    /**
     * Constructor for PASSIVE item upgrade option.
     */
    public LevelUpOption(PassiveItemType passiveItem, int passiveCurrentLevel) {
        this.type = Type.PASSIVE;
        this.passiveItem = passiveItem;
        this.passiveCurrentLevel = passiveCurrentLevel;
        this.passiveNextLevel = passiveCurrentLevel + 1;
        
        if (passiveCurrentLevel == 0) {
            // New acquisition
            this.title = "NEW: " + passiveItem.displayName;
            this.description = passiveItem.baseDescription;
        } else {
            // Level up
            this.title = passiveItem.getTitleWithLevel(passiveNextLevel);
            this.description = passiveItem.getDescriptionForLevel(passiveNextLevel);
        }
        
        this.previewText = buildPassivePreviewText(passiveItem, passiveCurrentLevel, passiveNextLevel);
        this.icon = passiveItem.textureKey; // Use texture key for rendering
        this.themeRGB = passiveItem.getThemeRGB();
        this.themeTextColor = passiveItem.getThemeTextColor();
        
        // Not used for PASSIVE
        this.stat = null;
        this.currentLevel = 0;
        this.nextLevel = 0;
        this.weaponCurrentLevel = 0;
        this.weaponNextLevel = 0;
    }

    /**
     * Constructor for EVOLUTION option.
     */
    public static LevelUpOption createEvolutionOption() {
        return new LevelUpOption(Type.EVOLUTION);
    }
    
    /**
     * Create BONUS_POINTS option (post-evolution: random points per level).
     */
    public static LevelUpOption createBonusPointsOption() {
        return new LevelUpOption(Type.BONUS_POINTS);
    }
    
    /**
     * Create BONUS_HEALTH option (post-evolution: +25 HP per level).
     */
    public static LevelUpOption createBonusHealthOption() {
        return new LevelUpOption(Type.BONUS_HEALTH);
    }
    
    // Private constructor for special types (evolution, bonus)
    private LevelUpOption(Type specialType) {
        this.type = specialType;
        
        switch (specialType) {
            case EVOLUTION:
                this.title = WeaponUpgrade.EVOLUTION_NAME;
                this.description = "Ultimate form!";
                this.previewText = "EVOLVE YOUR WEAPON!";
                this.icon = "pistonevo"; // Use evolved piston texture
                this.themeRGB = new int[]{148, 0, 211}; // Purple
                this.themeTextColor = "purple-500";
                break;
            case BONUS_POINTS:
                this.title = "Random Points";
                this.description = "Get random points each level up";
                this.previewText = "+50-200 points per level!";
                this.icon = "star"; // Use star.png
                this.themeRGB = new int[]{255, 215, 0}; // Gold
                this.themeTextColor = "yellow-500";
                break;
            case BONUS_HEALTH:
                this.title = "+25 Health";
                this.description = "Gain 25 HP each level up";
                this.previewText = "+25 HP per level!";
                this.icon = "chicken"; // Use chicken.png
                this.themeRGB = new int[]{255, 99, 71}; // Tomato/Red
                this.themeTextColor = "red-400";
                break;
            default:
                this.title = "Unknown";
                this.description = "";
                this.previewText = "";
                this.icon = "";
                this.themeRGB = new int[]{255, 255, 255};
                this.themeTextColor = "white";
        }
        
        // Not used for special types
        this.stat = null;
        this.currentLevel = 0;
        this.nextLevel = 0;
        this.passiveItem = null;
        this.passiveCurrentLevel = 0;
        this.passiveNextLevel = 0;
        this.weaponCurrentLevel = (specialType == Type.EVOLUTION) ? WeaponUpgrade.WEAPON_MAX_LEVEL : 0;
        this.weaponNextLevel = (specialType == Type.EVOLUTION) ? WeaponUpgrade.WEAPON_MAX_LEVEL : 0;
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String buildStatPreviewText(StatUpgradeType stat, int curLevel, int nextLevel) {
        String n = (stat == null ? "" : stat.name().toUpperCase());
        
        if (n.contains("HEALTH") || n.contains("HP")) {
            int cur = 10 + curLevel * 2;
            int nw = 10 + nextLevel * 2;
            return "Current: " + cur + " → New: " + nw;
        }
        if (n.contains("SPEED") || n.contains("MOVE")) {
            int cur = 100 + curLevel * 10;
            int nw = 100 + nextLevel * 10;
            return "Current: +" + (cur - 100) + "% → New: +" + (nw - 100) + "%";
        }
        if (n.contains("DAMAGE") || n.contains("DMG")) {
            int cur = 100 + curLevel * 5;
            int nw = 100 + nextLevel * 5;
            return "Current: +" + (cur - 100) + "% → New: +" + (nw - 100) + "%";
        }
        if (n.contains("XP") || n.contains("EXP") || n.contains("MAGNET")) {
            int cur = 100 + curLevel * 25;
            int nw = 100 + nextLevel * 25;
            return "Current: +" + (cur - 100) + "% → New: +" + (nw - 100) + "%";
        }
        if (n.contains("REGEN")) {
            float cur = curLevel * 0.1f;
            float nw = nextLevel * 0.1f;
            return String.format("Current: +%.1f HP/s → New: +%.1f HP/s", cur, nw);
        }
        
        return "Level " + curLevel + " → Level " + nextLevel;
    }

    private String buildPassivePreviewText(PassiveItemType type, int curLevel, int nextLevel) {
        if (curLevel == 0) {
            return "NEW! " + type.getDescriptionForLevel(1);
        }
        
        return switch (type) {
            case POWER_HERB -> String.format("+%d%% dmg → +%d%% dmg", curLevel * 10, nextLevel * 10);
            case IRON_SHIELD -> String.format("-%d%% taken → -%d%% taken", curLevel * 5, nextLevel * 5);
            case SWIFT_BOOTS -> String.format("+%d%% spd → +%d%% spd", curLevel * 10, nextLevel * 10);
            case LUCKY_COIN -> String.format("+%d%% crit → +%d%% crit", curLevel * 5, nextLevel * 5);
            case MAGNET_STONE -> String.format("+%d%% range → +%d%% range", curLevel * 20, nextLevel * 20);
            case LIFE_ESSENCE -> String.format("+%.1f HP/s → +%.1f HP/s", curLevel * 0.2f, nextLevel * 0.2f);
            case VITALITY_CORE -> String.format("+%d%% HP → +%d%% HP", curLevel * 20, nextLevel * 20);
        };
    }

    private String getStatIcon(StatUpgradeType type) {
        // Use ASCII characters instead of emojis (not supported by default font)
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return "[X]";
        if (n.contains("SPEED") || n.contains("MOVE")) return "[>]";
        if (n.contains("HEALTH") || n.contains("HP")) return "[+]";
        if (n.contains("REGEN")) return "[R]";
        if (n.contains("XP") || n.contains("EXP") || n.contains("MAGNET")) return "[M]";
        return "[*]";
    }

    private int[] getStatThemeRGB(StatUpgradeType type) {
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return new int[]{231, 76, 60};
        if (n.contains("SPEED") || n.contains("MOVE")) return new int[]{52, 152, 219};
        if (n.contains("HEALTH") || n.contains("HP")) return new int[]{46, 204, 113};
        if (n.contains("REGEN")) return new int[]{39, 174, 96};
        if (n.contains("XP") || n.contains("EXP") || n.contains("MAGNET")) return new int[]{155, 89, 182};
        return new int[]{255, 255, 255};
    }

    private String getStatThemeTextColor(StatUpgradeType type) {
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return "red-500";
        if (n.contains("SPEED") || n.contains("MOVE")) return "blue-500";
        if (n.contains("HEALTH") || n.contains("HP")) return "green-500";
        if (n.contains("REGEN")) return "green-400";
        if (n.contains("XP") || n.contains("EXP") || n.contains("MAGNET")) return "purple-500";
        return "white";
    }

    // ============================================
    // TYPE CHECKERS
    // ============================================

    public boolean isStatUpgrade() {
        return type == Type.STAT;
    }

    public boolean isWeaponUpgrade() {
        return type == Type.WEAPON;
    }

    public boolean isPassiveUpgrade() {
        return type == Type.PASSIVE;
    }

    public boolean isEvolution() {
        return type == Type.EVOLUTION;
    }
}
