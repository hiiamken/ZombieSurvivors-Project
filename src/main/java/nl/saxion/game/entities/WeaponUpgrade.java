package nl.saxion.game.entities;

/**
 * Contains weapon upgrade definitions for each level.
 * Weapon max level is 8, with each level providing specific improvements.
 * 
 * PISTOL EVOLUTION PATH:
 * Level 1: Base (2 bullets, normal damage)
 * Level 2: +1 bullet (3 total)
 * Level 3: +15% fire rate
 * Level 4: +1 bullet (4 total), +10% damage
 * Level 5: Bullets pierce 1 enemy
 * Level 6: +1 bullet (5 total), +15% fire rate
 * Level 7: Bullets pierce +1 enemy (2 total)
 * Level 8 (MAX): +2 bullets (7 total), +25% damage
 * 
 * EVOLUTION (requires weapon MAX + ALL passive items MAX):
 * DEATH SPIRAL - 8 bullets in rotating pattern, auto-target, infinite pierce, +150% damage
 */
public class WeaponUpgrade {
    
    public static final int WEAPON_MAX_LEVEL = 8;
    
    /**
     * Get bullet count bonus for weapon level.
     */
    public static int getBulletCountForLevel(int level) {
        return switch (level) {
            case 1 -> 2;  // Base: 2 bullets
            case 2 -> 3;  // +1 bullet
            case 3 -> 3;  // No change
            case 4 -> 4;  // +1 bullet
            case 5 -> 4;  // No change
            case 6 -> 5;  // +1 bullet
            case 7 -> 5;  // No change
            case 8 -> 7;  // +2 bullets
            default -> 2;
        };
    }
    
    /**
     * Get fire rate multiplier for weapon level.
     */
    public static float getFireRateMultiplierForLevel(int level) {
        return switch (level) {
            case 1, 2 -> 1.0f;     // Base
            case 3 -> 1.15f;       // +15%
            case 4, 5 -> 1.15f;    // Keep +15%
            case 6 -> 1.30f;       // +15% more = +30% total
            case 7, 8 -> 1.30f;    // Keep +30%
            default -> 1.0f;
        };
    }
    
    /**
     * Get damage multiplier for weapon level.
     */
    public static float getDamageMultiplierForLevel(int level) {
        return switch (level) {
            case 1, 2, 3 -> 1.0f;  // Base
            case 4 -> 1.10f;       // +10%
            case 5, 6, 7 -> 1.10f; // Keep +10%
            case 8 -> 1.35f;       // +25% more = +35% total
            default -> 1.0f;
        };
    }
    
    /**
     * Get pierce count (how many enemies bullet can pass through).
     * 0 = no pierce (destroy on first hit)
     */
    public static int getPierceCountForLevel(int level) {
        return switch (level) {
            case 1, 2, 3, 4 -> 0;  // No pierce
            case 5 -> 1;           // Pierce 1 enemy
            case 6 -> 1;           // Keep pierce 1
            case 7, 8 -> 2;        // Pierce 2 enemies
            default -> 0;
        };
    }
    
    /**
     * Get upgrade title for level.
     */
    public static String getTitleForLevel(int level) {
        return "Pistol";
    }
    
    /**
     * Get upgrade description for level.
     */
    public static String getDescriptionForLevel(int level) {
        return switch (level) {
            case 1 -> "Base weapon";
            case 2 -> "+1 bullet (3 total)";
            case 3 -> "+15% fire rate";
            case 4 -> "+1 bullet, +10% damage";
            case 5 -> "Bullets pierce 1 enemy";
            case 6 -> "+1 bullet, +15% fire rate";
            case 7 -> "Bullets pierce +1 enemy";
            case 8 -> "+2 bullets, +25% damage (MAX)";
            default -> "Unknown";
        };
    }
    
    /**
     * Get preview text showing stat change for upgrade menu.
     */
    public static String getPreviewTextForLevel(int currentLevel, int nextLevel) {
        int curBullets = getBulletCountForLevel(currentLevel);
        int newBullets = getBulletCountForLevel(nextLevel);
        float curFireRate = getFireRateMultiplierForLevel(currentLevel);
        float newFireRate = getFireRateMultiplierForLevel(nextLevel);
        float curDamage = getDamageMultiplierForLevel(currentLevel);
        float newDamage = getDamageMultiplierForLevel(nextLevel);
        int curPierce = getPierceCountForLevel(currentLevel);
        int newPierce = getPierceCountForLevel(nextLevel);
        
        StringBuilder sb = new StringBuilder();
        
        if (newBullets != curBullets) {
            sb.append("Bullets: ").append(curBullets).append(" → ").append(newBullets).append("  ");
        }
        if (Math.abs(newFireRate - curFireRate) > 0.01f) {
            sb.append("Rate: +").append((int)((curFireRate - 1) * 100)).append("% → +")
              .append((int)((newFireRate - 1) * 100)).append("%  ");
        }
        if (Math.abs(newDamage - curDamage) > 0.01f) {
            sb.append("Dmg: +").append((int)((curDamage - 1) * 100)).append("% → +")
              .append((int)((newDamage - 1) * 100)).append("%  ");
        }
        if (newPierce != curPierce) {
            sb.append("Pierce: ").append(curPierce).append(" → ").append(newPierce);
        }
        
        return sb.toString().trim();
    }
    
    // ============================================
    // EVOLUTION STATS (DEATH SPIRAL)
    // ============================================
    
    public static final String EVOLUTION_NAME = "Death Spiral";
    public static final String EVOLUTION_DESCRIPTION = "8 rotating bullets, auto-target, infinite pierce";
    public static final String EVOLUTION_ICON = "💀";
    
    /**
     * Get evolved weapon bullet count.
     */
    public static int getEvolvedBulletCount() {
        return 8;
    }
    
    /**
     * Get evolved weapon fire rate multiplier.
     */
    public static float getEvolvedFireRateMultiplier() {
        return 2.5f; // Very fast firing
    }
    
    /**
     * Get evolved weapon damage multiplier.
     */
    public static float getEvolvedDamageMultiplier() {
        return 2.5f; // +150% damage (2.5x)
    }
    
    /**
     * Get evolved weapon pierce count (-1 = infinite).
     */
    public static int getEvolvedPierceCount() {
        return -1; // Infinite pierce
    }
    
    /**
     * Check if weapon is evolved.
     */
    public static boolean isEvolvedBulletPattern() {
        return true; // Evolved weapon shoots in rotating pattern
    }
}
