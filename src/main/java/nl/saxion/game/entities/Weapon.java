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
            dirY = 1f;
        }

        float playerX = player.getX();
        float playerY = player.getY();
        float playerSize = Player.SPRITE_SIZE;

        // Hitbox offset
        float hitboxOffsetX = (Player.SPRITE_SIZE - Player.HITBOX_WIDTH) / 2f;
        float hitboxOffsetY = (Player.SPRITE_SIZE - Player.HITBOX_HEIGHT) / 2f;

        // Hitbox center
        float hitboxCenterX = playerX + hitboxOffsetX + Player.HITBOX_WIDTH / 2f;
        float hitboxCenterY = playerY + hitboxOffsetY + Player.HITBOX_HEIGHT / 2f;

        // Spawn bullet
        float bulletStartX = hitboxCenterX - bulletWidth / 2f;
        float bulletStartY = hitboxCenterY - bulletHeight / 2f;

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
