package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import java.util.ArrayList;
import java.util.List;

public class Weapon {

    public enum WeaponType {
        PISTOL
    }

    private WeaponType type;
    private float fireRate;        // shots per second
    private float fireCooldown;    // seconds remaining until next shot
    private int minDamage;        // minimum damage per bullet
    private int maxDamage;        // maximum damage per bullet

    // Bullet properties
    private float bulletSpeed;
    private float bulletWidth;
    private float bulletHeight;

    // Multi-bullet settings (default: 2 bullets in same direction)
    private int bulletCount = 2;

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
        this.fireRate = fireRate;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.bulletSpeed = bulletSpeed;
        this.bulletWidth = bulletWidth;
        this.bulletHeight = bulletHeight;
        this.fireCooldown = 0f;
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

    // called every frame
    public void update(float delta) {
        if (fireCooldown > 0f) {
            fireCooldown -= delta;
        }
    }

    public boolean canFire() {
        return fireCooldown <= 0f;
    }

    private void startCooldown() {
        fireCooldown = 1f / fireRate;
    }

    public List<Bullet> tryFire(Player player) {
        return tryFire(player, null);
    }
    
    public List<Bullet> tryFire(Player player, nl.saxion.game.systems.SoundManager soundManager) {
        if (!canFire()) {
            return null;
        }

        List<Bullet> bullets = new ArrayList<>();

        // Direction from player's last movement
        float dirX = player.getLastMoveDirectionX();
        float dirY = player.getLastMoveDirectionY();

        // default: shoot upward if standing still
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

        // Fire multiple bullets in the same direction (straight line)
        for (int i = 0; i < bulletCount; i++) {
            // Random damage within range
            int baseDamage = GameApp.randomInt(minDamage, maxDamage + 1);
            // Apply damage multiplier from player upgrades
            int finalDamage = (int) (baseDamage * player.getDamageMultiplier());

            // Offset each bullet slightly along the firing direction
            // Creates a "train" of bullets going same direction
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
                    bulletHeight
            );
            bullets.add(bullet);
        }

        // Play shooting sound at 10% volume (only once)
        if (soundManager != null) {
            soundManager.playSound("shooting", 0.1f);
        }

        startCooldown();
        return bullets;
    }

    // Set bullet count for multi-shot
    public void setBulletCount(int count) {
        this.bulletCount = Math.max(1, count);
    }

    public int getBulletCount() {
        return bulletCount;
    }

    // Get average damage for display/calculation
    public int getAverageDamage() {
        return (minDamage + maxDamage) / 2;
    }

    public int getMinDamage() {
        return minDamage;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    public float getFireRate() {
        return fireRate;
    }

    public WeaponType getType() {
        return type;
    }
}
