package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import java.util.ArrayList;
import java.util.List;

/**
 * Weapon class with level system and evolution support.
 * 
 * WEAPON LEVEL SYSTEM (Max Level 8):
 * - Level 1: Base (2 bullets)
 * - Level 2: +1 bullet (3 total)
 * - Level 3: +15% fire rate
 * - Level 4: +1 bullet (4 total), +10% damage
 * - Level 5: Bullets pierce 1 enemy
 * - Level 6: +1 bullet (5 total), +15% fire rate
 * - Level 7: Bullets pierce +1 enemy (2 total)
 * - Level 8 (MAX): +2 bullets (7 total), +25% damage
 * 
 * EVOLUTION (requires weapon MAX + ALL passive items MAX):
 * DEATH SPIRAL - 8 bullets in rotating pattern, auto-target, infinite pierce, +150% damage
 */
public class Weapon {

    public enum WeaponType {
        PISTOL,
        DEATH_SPIRAL // Evolved form of PISTOL
    }

    private WeaponType type;
    private float baseFireRate;    // Base shots per second
    private float fireCooldown;    // Seconds remaining until next shot
    private int baseMinDamage;     // Base minimum damage per bullet
    private int baseMaxDamage;     // Base maximum damage per bullet

    // Bullet properties
    private float bulletSpeed;
    private float bulletWidth;
    private float bulletHeight;

    // Level system
    private int level = 1;
    private boolean isEvolved = false;

    // Evolved weapon rotation angle (for Death Spiral)
    private float evolvedRotationAngle = 0f;
    private static final float EVOLVED_ROTATION_SPEED = 180f; // degrees per second

    // Old constructor – still works (uses damage as both min and max)
    public Weapon(WeaponType type, float fireRate, int damage) {
        this(type, fireRate, damage, damage, 400f, 6f, 6f);
    }

    // Constructor with damage range
    public Weapon(WeaponType type,
                  float fireRate,
                  int minDamage,
                  int maxDamage,
                  float bulletSpeed,
                  float bulletWidth,
                  float bulletHeight) {
        this.type = type;
        this.baseFireRate = fireRate;
        this.baseMinDamage = minDamage;
        this.baseMaxDamage = maxDamage;
        this.bulletSpeed = bulletSpeed;
        this.bulletWidth = bulletWidth;
        this.bulletHeight = bulletHeight;
        this.fireCooldown = 0f;
        this.level = 1;
        this.isEvolved = false;
    }

    // Legacy constructor for compatibility (uses damage as both min and max)
    public Weapon(WeaponType type,
                  float fireRate,
                  int damage,
                  float bulletSpeed,
                  float bulletWidth,
                  float bulletHeight) {
        this(type, fireRate, damage, damage, bulletSpeed, bulletWidth, bulletHeight);
    }

    // Called every frame
    public void update(float delta) {
        if (fireCooldown > 0f) {
            fireCooldown -= delta;
        }

        // Update evolved weapon rotation
        if (isEvolved) {
            evolvedRotationAngle += EVOLVED_ROTATION_SPEED * delta;
            if (evolvedRotationAngle >= 360f) {
                evolvedRotationAngle -= 360f;
            }
        }
    }

    public boolean canFire() {
        return fireCooldown <= 0f;
    }

    private void startCooldown() {
        float effectiveFireRate = getEffectiveFireRate();
        fireCooldown = 1f / effectiveFireRate;
    }

    /**
     * Get the effective fire rate after level bonuses.
     */
    public float getEffectiveFireRate() {
        if (isEvolved) {
            return baseFireRate * WeaponUpgrade.getEvolvedFireRateMultiplier();
        }
        return baseFireRate * WeaponUpgrade.getFireRateMultiplierForLevel(level);
    }

    /**
     * Get the effective damage multiplier from weapon level.
     */
    public float getWeaponDamageMultiplier() {
        if (isEvolved) {
            return WeaponUpgrade.getEvolvedDamageMultiplier();
        }
        return WeaponUpgrade.getDamageMultiplierForLevel(level);
    }

    /**
     * Get the current bullet count based on level.
     */
    public int getEffectiveBulletCount() {
        if (isEvolved) {
            return WeaponUpgrade.getEvolvedBulletCount();
        }
        return WeaponUpgrade.getBulletCountForLevel(level);
    }

    /**
     * Get the current pierce count based on level.
     * -1 = infinite pierce
     */
    public int getPierceCount() {
        if (isEvolved) {
            return WeaponUpgrade.getEvolvedPierceCount();
        }
        return WeaponUpgrade.getPierceCountForLevel(level);
    }

    public List<Bullet> tryFire(Player player) {
        return tryFire(player, null);
    }
    
    public List<Bullet> tryFire(Player player, nl.saxion.game.systems.SoundManager soundManager) {
        if (!canFire()) {
            return null;
        }

        List<Bullet> bullets = new ArrayList<>();

        // Get effective values based on level
        int bulletCount = getEffectiveBulletCount();
        int pierceCount = getPierceCount();
        float weaponDamageMult = getWeaponDamageMultiplier();

        // Direction from player's last movement
        float dirX = player.getLastMoveDirectionX();
        float dirY = player.getLastMoveDirectionY();

        // Default: shoot upward if standing still
        if (dirX == 0 && dirY == 0) {
            dirX = 0f;
            dirY = 1f;
        }

        // Player position is top-left of sprite
        float playerX = player.getX();
        float playerY = player.getY();

        float damageOffsetX = (Player.SPRITE_SIZE - Player.DAMAGE_HITBOX_WIDTH) / 2f - 12f;
        float damageOffsetY = (Player.SPRITE_SIZE - Player.DAMAGE_HITBOX_HEIGHT) / 2f - 12f;

        float damageHitboxCenterX = playerX + damageOffsetX + Player.DAMAGE_HITBOX_WIDTH / 2f;
        float damageHitboxCenterY = playerY + damageOffsetY + Player.DAMAGE_HITBOX_HEIGHT / 2f;

        float bulletStartX = damageHitboxCenterX - bulletWidth / 2f;
        float bulletStartY = damageHitboxCenterY - bulletHeight / 2f;

        if (isEvolved) {
            // DEATH SPIRAL: Fire bullets in rotating pattern
            bullets.addAll(fireEvolvedPattern(player, bulletStartX, bulletStartY, 
                bulletCount, pierceCount, weaponDamageMult));
        } else {
            // Normal firing: Fire multiple bullets in the same direction
            for (int i = 0; i < bulletCount; i++) {
                // Random damage within range, then apply multipliers
                int baseDamage = GameApp.randomInt(baseMinDamage, baseMaxDamage + 1);
                // Apply weapon level damage multiplier AND player damage multiplier
                int finalDamage = (int) (baseDamage * weaponDamageMult * player.getDamageMultiplier());

                // Offset each bullet slightly along the firing direction
                float offsetDistance = i * 12f; // 12 pixels apart
                float offsetX = bulletStartX + dirX * offsetDistance;
                float offsetY = bulletStartY + dirY * offsetDistance;

                Bullet bullet = new Bullet(
                        offsetX,
                        offsetY,
                        dirX,
                        dirY,
                        finalDamage,
                        bulletSpeed,
                        bulletWidth,
                        bulletHeight,
                        pierceCount
                );
                bullets.add(bullet);
            }
            
            // === MULTI-SHOT FRONT (Level 5+): 3 bullets in spread pattern ===
            if (WeaponUpgrade.hasMultiShotFront(level)) {
                int frontBullets = WeaponUpgrade.getFrontSpreadBulletCount(level);
                float spreadAngle = 25f; // degrees between each bullet
                float startAngle = -spreadAngle * (frontBullets - 1) / 2f;
                
                for (int i = 0; i < frontBullets; i++) {
                    float angle = startAngle + i * spreadAngle;
                    float radians = (float) Math.toRadians(angle);
                    
                    // Rotate the direction vector
                    float cos = (float) Math.cos(radians);
                    float sin = (float) Math.sin(radians);
                    float spreadDirX = dirX * cos - dirY * sin;
                    float spreadDirY = dirX * sin + dirY * cos;
                    
                    int baseDamage = GameApp.randomInt(baseMinDamage, baseMaxDamage + 1);
                    int finalDamage = (int) (baseDamage * weaponDamageMult * player.getDamageMultiplier() * 0.7f);
                    
                    Bullet bullet = new Bullet(
                            bulletStartX,
                            bulletStartY,
                            spreadDirX,
                            spreadDirY,
                            finalDamage,
                            bulletSpeed,
                            bulletWidth,
                            bulletHeight,
                            pierceCount
                    );
                    bullets.add(bullet);
                }
            }
            
            // === MULTI-SHOT BACK (Level 8+): 3 bullets backward ===
            if (WeaponUpgrade.hasMultiShotBack(level)) {
                int backBullets = WeaponUpgrade.getBackBulletCount(level);
                float spreadAngle = 20f; // degrees between each bullet
                float startAngle = -spreadAngle * (backBullets - 1) / 2f;
                
                // Reverse direction for backward bullets
                float backDirX = -dirX;
                float backDirY = -dirY;
                
                for (int i = 0; i < backBullets; i++) {
                    float angle = startAngle + i * spreadAngle;
                    float radians = (float) Math.toRadians(angle);
                    
                    // Rotate the backward direction vector
                    float cos = (float) Math.cos(radians);
                    float sin = (float) Math.sin(radians);
                    float spreadDirX = backDirX * cos - backDirY * sin;
                    float spreadDirY = backDirX * sin + backDirY * cos;
                    
                    int baseDamage = GameApp.randomInt(baseMinDamage, baseMaxDamage + 1);
                    int finalDamage = (int) (baseDamage * weaponDamageMult * player.getDamageMultiplier() * 0.6f);
                    
                    Bullet bullet = new Bullet(
                            bulletStartX,
                            bulletStartY,
                            spreadDirX,
                            spreadDirY,
                            finalDamage,
                            bulletSpeed * 0.9f, // Back bullets slightly slower
                            bulletWidth,
                            bulletHeight,
                            pierceCount
                    );
                    bullets.add(bullet);
                }
            }
        }

        // Play shooting sound at 10% volume (only once)
        if (soundManager != null) {
            soundManager.playSound("shooting", 0.1f);
        }

        startCooldown();
        return bullets;
    }

    /**
     * Fire evolved weapon pattern (Death Spiral - rotating bullets).
     */
    private List<Bullet> fireEvolvedPattern(Player player, float centerX, float centerY,
            int bulletCount, int pierceCount, float weaponDamageMult) {
        List<Bullet> bullets = new ArrayList<>();
        
        // Fire bullets in a circular pattern, rotating over time
        float angleStep = 360f / bulletCount;
        
        for (int i = 0; i < bulletCount; i++) {
            float angle = evolvedRotationAngle + (i * angleStep);
            float radians = (float) Math.toRadians(angle);
            
            float dirX = (float) Math.cos(radians);
            float dirY = (float) Math.sin(radians);
            
            // Random damage within range, then apply multipliers
            int baseDamage = GameApp.randomInt(baseMinDamage, baseMaxDamage + 1);
            int finalDamage = (int) (baseDamage * weaponDamageMult * player.getDamageMultiplier());
            
            // Bullets start slightly offset from center in their direction
            float startOffset = 15f;
            float bulletX = centerX + dirX * startOffset;
            float bulletY = centerY + dirY * startOffset;
            
            Bullet bullet = new Bullet(
                    bulletX,
                    bulletY,
                    dirX,
                    dirY,
                    finalDamage,
                    bulletSpeed * 1.2f, // Evolved bullets are slightly faster
                    bulletWidth,
                    bulletHeight,
                    pierceCount
            );
            
            // Mark as evolved bullet for special rendering
            bullet.setEvolved(true);
            bullets.add(bullet);
        }
        
        return bullets;
    }

    // ============================================
    // LEVEL SYSTEM
    // ============================================

    /**
     * Level up the weapon.
     * @return true if level up successful, false if already at max
     */
    public boolean levelUp() {
        if (level < WeaponUpgrade.WEAPON_MAX_LEVEL) {
            level++;
            GameApp.log("Weapon leveled up to " + level + "! " + getUpgradeDescription());
            return true;
        }
        return false;
    }

    /**
     * Check if weapon is at max level.
     */
    public boolean isMaxLevel() {
        return level >= WeaponUpgrade.WEAPON_MAX_LEVEL;
    }

    /**
     * Get current level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get max level.
     */
    public int getMaxLevel() {
        return WeaponUpgrade.WEAPON_MAX_LEVEL;
    }

    /**
     * Get upgrade title for current level.
     */
    public String getUpgradeTitle() {
        if (isEvolved) {
            return WeaponUpgrade.EVOLUTION_NAME;
        }
        return WeaponUpgrade.getTitleForLevel(level);
    }

    /**
     * Get upgrade title for next level (used in upgrade menu).
     */
    public String getNextLevelTitle() {
        if (level < WeaponUpgrade.WEAPON_MAX_LEVEL) {
            return WeaponUpgrade.getTitleForLevel(level + 1);
        }
        return getUpgradeTitle() + " (MAX)";
    }

    /**
     * Get description for current level upgrades.
     */
    public String getUpgradeDescription() {
        if (isEvolved) {
            return WeaponUpgrade.EVOLUTION_DESCRIPTION;
        }
        return WeaponUpgrade.getDescriptionForLevel(level);
    }

    /**
     * Get description for next level upgrade.
     */
    public String getNextLevelDescription() {
        if (level < WeaponUpgrade.WEAPON_MAX_LEVEL) {
            return WeaponUpgrade.getDescriptionForLevel(level + 1);
        }
        return "MAX LEVEL";
    }

    /**
     * Get preview text for upgrade menu.
     */
    public String getPreviewText() {
        if (level < WeaponUpgrade.WEAPON_MAX_LEVEL) {
            return WeaponUpgrade.getPreviewTextForLevel(level, level + 1);
        }
        return "Weapon at maximum level";
    }

    // ============================================
    // EVOLUTION SYSTEM
    // ============================================

    /**
     * Evolve the weapon into Death Spiral.
     * Requires: weapon at max level AND all passive items at max level.
     */
    public void evolve() {
        if (!isEvolved && isMaxLevel()) {
            isEvolved = true;
            type = WeaponType.DEATH_SPIRAL;
            GameApp.log("⚡ WEAPON EVOLVED into " + WeaponUpgrade.EVOLUTION_NAME + "! ⚡");
        }
    }

    /**
     * Check if weapon is evolved.
     */
    public boolean isEvolved() {
        return isEvolved;
    }

    /**
     * Check if weapon can evolve (requirements met).
     * @param allPassiveItemsMaxed whether all passive items are at max level
     */
    public boolean canEvolve(boolean allPassiveItemsMaxed) {
        return !isEvolved && isMaxLevel() && allPassiveItemsMaxed;
    }

    // ============================================
    // GETTERS (for compatibility)
    // ============================================

    // Set bullet count (legacy support - now handled by level system)
    @Deprecated
    public void setBulletCount(int count) {
        // No longer directly settable - controlled by level
        GameApp.log("Warning: setBulletCount is deprecated. Bullet count is now controlled by weapon level.");
    }

    public int getBulletCount() {
        return getEffectiveBulletCount();
    }

    // Get average damage for display/calculation
    public int getAverageDamage() {
        int avgBase = (baseMinDamage + baseMaxDamage) / 2;
        return (int) (avgBase * getWeaponDamageMultiplier());
    }

    public int getMinDamage() {
        return (int) (baseMinDamage * getWeaponDamageMultiplier());
    }

    public int getMaxDamage() {
        return (int) (baseMaxDamage * getWeaponDamageMultiplier());
    }

    public float getFireRate() {
        return getEffectiveFireRate();
    }

    public WeaponType getType() {
        return type;
    }

    /**
     * Get weapon icon for display.
     */
    public String getIcon() {
        if (isEvolved) {
            return WeaponUpgrade.EVOLUTION_ICON;
        }
        return "🔫"; // Default pistol icon
    }

    /**
     * Get theme RGB color for UI.
     */
    public int[] getThemeRGB() {
        if (isEvolved) {
            return new int[]{148, 0, 211}; // Purple for evolved
        }
        return new int[]{255, 165, 0}; // Orange for normal
    }

    /**
     * Get theme text color for UI.
     */
    public String getThemeTextColor() {
        if (isEvolved) {
            return "purple-500";
        }
        return "orange-500";
    }
}
