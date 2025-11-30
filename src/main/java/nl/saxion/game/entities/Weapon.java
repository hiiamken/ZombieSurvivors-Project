package nl.saxion.game.entities;

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
        this(type, fireRate, damage, 400f, 8f, 8f);
    }

    // New constructor with more control
    public Weapon(WeaponType type,
                  float fireRate,
                  int damage,
                  float bulletSpeed,
                  float bulletWidth,
                  float bulletHeight) {
        this.type = type;
        this.fireRate = fireRate;
        this.damage = damage;
        this.bulletSpeed = bulletSpeed;
        this.bulletWidth = bulletWidth;
        this.bulletHeight = bulletHeight;
        this.fireCooldown = 0f;
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

    /**
     * Tries to fire a bullet from the given player.
     * Returns a new Bullet if fired, or null if still on cooldown.
     */
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
            dirY = -1f;
        }

        float playerX = player.getX();
        float playerY = player.getY();
        float playerSize = Player.SPRITE_SIZE;

        float bulletStartX = playerX + playerSize / 2f - bulletWidth / 2f;
        float bulletStartY = playerY + playerSize / 2f;

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

    public int getDamage() {
        return damage;
    }

    public float getFireRate() {
        return fireRate;
    }

    public WeaponType getType() {
        return type;
    }
}
