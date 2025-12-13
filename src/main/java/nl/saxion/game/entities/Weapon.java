package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class Weapon {

    public enum WeaponType {
        PISTOL
    }

    private WeaponType type;
    private float fireRate;        // shots per second
    private float fireCooldown;    // seconds remaining until next shot
    private int damage;            // damage per bullet

    // Bullet properties
    private float bulletSpeed;
    private float bulletWidth;
    private float bulletHeight;

    // Old constructor – still works
    public Weapon(WeaponType type, float fireRate, int damage) {
        this(type, fireRate, damage, 400f, 6f, 6f);
    }

    // New constructor with more control
    public Weapon(WeaponType type,
                  float fireRate,
                  int damage,
                  float bulletSpeed,
                  float bulletWidth,
                  float bulletHeight) {
        this.type = type;

        // safety clamps
        this.fireRate = Math.max(0.1f, fireRate);
        this.damage = Math.max(1, damage);

        this.bulletSpeed = Math.max(50f, bulletSpeed);
        this.bulletWidth = Math.max(2f, bulletWidth);
        this.bulletHeight = Math.max(2f, bulletHeight);

        this.fireCooldown = 0f;
    }

    // called every frame
    public void update(float delta) {
        if (fireCooldown > 0f) {
            fireCooldown -= delta;
            if (fireCooldown < 0f) fireCooldown = 0f;
        }
    }

    public boolean canFire() {
        return fireCooldown <= 0f;
    }

    private void startCooldown() {
        fireCooldown = 1f / Math.max(0.1f, fireRate);
    }

    public Bullet tryFire(Player player) {
        if (!canFire()) {
            return null;
        }

        // Direction from player's last movement
        float dirX = player.getLastMoveDirectionX();
        float dirY = player.getLastMoveDirectionY();

        // default: shoot upward if standing still
        if (dirX == 0 && dirY == 0) {
            dirX = 0f;
            dirY = 1f;  // Shoot up (positive Y in GameApp)
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

        Bullet bullet = new Bullet(
                bulletStartX,
                bulletStartY,
                dirX,
                dirY,
                damage,
                bulletSpeed,
                bulletWidth,
                bulletHeight
        );

        startCooldown();
        return bullet;
    }

    // =========================
    // Upgrades support (used by Player level-up)
    // =========================

    public void addDamage(int amount) {
        if (amount <= 0) return;
        damage += amount;
        damage = Math.max(1, damage);
    }

    public void multiplyFireRate(float multiplier) {
        if (multiplier <= 0f) return;
        fireRate *= multiplier;
        fireRate = GameApp.clamp(fireRate, 0.1f, 50f);

        // optional: prevent weird negative cooldown when upgrading mid-cooldown
        fireCooldown = GameApp.clamp(fireCooldown, 0f, 10f);
    }

    public void multiplyBulletSpeed(float multiplier) {
        if (multiplier <= 0f) return;
        bulletSpeed *= multiplier;
        bulletSpeed = GameApp.clamp(bulletSpeed, 50f, 2000f);
    }

    // =========================
    // Getters
    // =========================

    public int getDamage() {
        return damage;
    }

    public float getFireRate() {
        return fireRate;
    }

    public WeaponType getType() {
        return type;
    }

    public float getBulletSpeed() {
        return bulletSpeed;
    }

    public float getBulletWidth() {
        return bulletWidth;
    }

    public float getBulletHeight() {
        return bulletHeight;
    }
}
