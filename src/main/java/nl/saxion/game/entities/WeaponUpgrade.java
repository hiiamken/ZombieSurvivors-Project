package nl.saxion.game.entities;

/**
 * Contains weapon upgrade definitions for each level.
 * Weapon max level is 10, with each level providing specific improvements.
 * 
 * BALANCED PISTOL EVOLUTION PATH (for Vampire Survivors feel):
 * Level 1: Base (2 bullets, normal damage)
 * Level 2: +1 bullet (3 total), +10% damage
 * Level 3: ★ MULTI-SHOT FRONT - Shoots 3 bullets forward in spread pattern
 * Level 4: +1 bullet (4 total), +15% damage
 * Level 5: +20% fire rate
 * Level 6: Bullets pierce 1 enemy, +10% fire rate
 * Level 7: +1 bullet (5 total), +20% damage
 * Level 8: ★ MULTI-SHOT BACK - Shoots 3 bullets backward
 * Level 9: Bullets pierce +1 enemy (2 total), +15% fire rate
 * Level 10 (MAX): +2 bullets (7 total), +30% damage, pierce +1 (3 total)
 * 
 * EVOLUTION (requires weapon MAX level 10 + ALL passive items MAX):
 * DEATH SPIRAL - 8 bullets in rotating pattern, auto-target, infinite pierce, slower fire rate, 10% lifesteal
 */
public class WeaponUpgrade {
    
    public static final int WEAPON_MAX_LEVEL = 10;
    
    /**
     * Get bullet count bonus for weapon level.
     */
    public static int getBulletCountForLevel(int level) {
        return switch (level) {
            case 1 -> 2;   // Base: 2 bullets
            case 2 -> 3;   // +1 bullet
            case 3 -> 3;   // No change
            case 4 -> 4;   // +1 bullet
            case 5 -> 4;   // Multi-shot front (no base bullet change)
            case 6 -> 4;   // No change
            case 7 -> 5;   // +1 bullet
            case 8 -> 5;   // Multi-shot back (no base bullet change)
            case 9 -> 5;   // No change
            case 10 -> 7;  // +2 bullets (MAX)
            default -> 2;
        };
    }
    
    /**
     * Get fire rate multiplier for weapon level.
     * Smoother progression for satisfying upgrades
     */
    public static float getFireRateMultiplierForLevel(int level) {
        return switch (level) {
            case 1, 2, 3, 4 -> 1.0f; // Base (no fire rate boost until level 5)
            case 5 -> 1.20f;        // +20% fire rate
            case 6 -> 1.30f;        // +10% more = +30% total
            case 7 -> 1.30f;        // Keep +30%
            case 8 -> 1.30f;        // Keep +30%
            case 9 -> 1.45f;        // +15% more = +45% total
            case 10 -> 1.50f;       // +5% more = +50% total (fast!)
            default -> 1.0f;
        };
    }
    
    /**
     * Get damage multiplier for weapon level.
     * Progressive scaling to handle late-game zombie hordes
     */
    public static float getDamageMultiplierForLevel(int level) {
        return switch (level) {
            case 1 -> 1.0f;         // Base
            case 2 -> 1.10f;        // +10%
            case 3 -> 1.10f;        // Keep +10%
            case 4 -> 1.25f;        // +15% more = +25% total
            case 5 -> 1.25f;        // Keep +25%
            case 6 -> 1.25f;        // Keep +25%
            case 7 -> 1.45f;        // +20% more = +45% total
            case 8 -> 1.45f;        // Keep +45%
            case 9 -> 1.45f;        // Keep +45%
            case 10 -> 1.75f;       // +30% more = +75% total (powerful!)
            default -> 1.0f;
        };
    }
    
    /**
     * Get pierce count (how many enemies bullet can pass through).
     * 0 = no pierce (destroy on first hit)
     * Critical for clearing zombie hordes in late game
     */
    public static int getPierceCountForLevel(int level) {
        return switch (level) {
            case 1, 2, 3, 4, 5 -> 0;  // No pierce
            case 6, 7, 8 -> 1;         // Pierce 1 enemy
            case 9 -> 2;               // Pierce 2 enemies
            case 10 -> 3;              // Pierce 3 enemies (crowd control!)
            default -> 0;
        };
    }
    
    /**
     * Check if this level has multi-shot front (3 bullets forward in spread).
     */
    public static boolean hasMultiShotFront(int level) {
        return level >= 3; // Now unlocks at level 3
    }
    
    /**
     * Check if this level has multi-shot back (3 bullets backward).
     */
    public static boolean hasMultiShotBack(int level) {
        return level >= 8;
    }
    
    /**
     * Get number of front spread bullets (0 if not unlocked).
     */
    public static int getFrontSpreadBulletCount(int level) {
        return hasMultiShotFront(level) ? 3 : 0;
    }
    
    /**
     * Get number of back bullets (0 if not unlocked).
     */
    public static int getBackBulletCount(int level) {
        return hasMultiShotBack(level) ? 3 : 0;
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
            case 2 -> "+1 bullet, +10% damage";
            case 3 -> "MULTI-SHOT: +3 front bullets";
            case 4 -> "+1 bullet, +15% damage";
            case 5 -> "+20% fire rate";
            case 6 -> "Pierce 1 enemy, +10% fire rate";
            case 7 -> "+1 bullet, +20% damage";
            case 8 -> "MULTI-SHOT: +3 back bullets";
            case 9 -> "Pierce +1, +15% fire rate";
            case 10 -> "+2 bullets, +30% damage, Pierce +1 (MAX)";
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
    // Nerfed: Slower fire rate, but has 10% lifesteal
    // ============================================
    
    public static final String EVOLUTION_NAME = "Death Spiral";
    public static final String EVOLUTION_DESCRIPTION = "16 rotating bullets, infinite pierce, 10% lifesteal";
    public static final String EVOLUTION_ICON = "💀";
    
    /**
     * Get evolved weapon bullet count.
     * BUFFED: 16 bullets (was 8) to match full upgrade multi-shot power
     */
    public static int getEvolvedBulletCount() {
        return 16; // More bullets for devastating damage (was 8)
    }
    
    /**
     * Get evolved weapon fire rate multiplier.
     * BUFFED: Faster fire rate to compete with full upgrade
     */
    public static float getEvolvedFireRateMultiplier() {
        return 2.0f; // Faster fire rate (was 1.5f)
    }
    
    /**
     * Get evolved weapon damage multiplier.
     * BUFFED: Higher damage per bullet to shred minibosses
     */
    public static float getEvolvedDamageMultiplier() {
        return 4.5f; // +350% damage (4.5x) - much more devastating!
    }
    
    /**
     * Get evolved weapon pierce count (-1 = infinite).
     */
    public static int getEvolvedPierceCount() {
        return -1; // Infinite pierce
    }
    
    /**
     * Get evolved weapon lifesteal percentage.
     * @return Lifesteal as decimal (0.10 = 10%)
     */
    public static float getEvolvedLifestealPercent() {
        return 0.10f; // 10% lifesteal
    }
    
    /**
     * Check if weapon is evolved.
     */
    public static boolean isEvolvedBulletPattern() {
        return true; // Evolved weapon shoots in rotating pattern
    }
}
