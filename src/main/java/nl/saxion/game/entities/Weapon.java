package nl.saxion.game.entities;

// Basic weapon with fire-rate & damage
public class Weapon {

    public enum WeaponType {
        PISTOL
    }

    private WeaponType type;
    private float fireRate;        // shots per second
    private float fireCooldown;    // seconds remaining until next shot
    private int damage;            // damage per bullet

    public Weapon(WeaponType type, float fireRate, int damage) {
        this.type = type;
        this.fireRate = fireRate;
        this.damage = damage;
        this.fireCooldown = 0f;
    }

    // called every frame
    public void update(float delta) {
        if (fireCooldown > 0f) {
            fireCooldown -= delta;
        }
    }

    // can we fire a new bullet now?
    public boolean canFire() {
        return fireCooldown <= 0f;
    }

    // when a bullet is fired, start cooldown again
    public void onFire() {
        // cooldown = time between shots
        fireCooldown = 1f / fireRate;
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
