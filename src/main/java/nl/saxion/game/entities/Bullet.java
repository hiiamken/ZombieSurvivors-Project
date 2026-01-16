package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

/**
 * Bullet with movement, damage, pierce capability, and rendering.
 * 
 * PIERCE SYSTEM:
 * - pierceCount = 0: Destroy on first hit (normal)
 * - pierceCount > 0: Can pass through N enemies before destroying
 * - pierceCount = -1: Infinite pierce (evolved weapons)
 */
public class Bullet {

    private float x, y;
    private float vx, vy;
    private float speed;      // units per second
    private int damage;

    // Size
    private float width;
    private float height;

    // Track spawn position to calculate travel distance
    private float spawnX, spawnY;
    private static final float MAX_TRAVEL_DISTANCE = 2000f; // Max distance bullet can travel

    private Rectangle hitBox;

    private boolean destroyed = false;

    // Pierce system
    private int pierceCount;         // How many enemies can be pierced (-1 = infinite)
    private int pierceRemaining;     // How many more enemies can be hit
    private Set<Integer> hitEnemyIds; // Track which enemies were already hit (prevent double-hit)

    // Evolved bullet flag (for special rendering)
    private boolean isEvolved = false;

    // Defaults
    private static final float DEFAULT_SPEED = 400f;
    private static final float DEFAULT_SIZE  = 10f;
    private static final String TEXTURE_KEY  = "bullet";
    private static final String EVOLVED_TEXTURE_KEY = "bullet"; // Can use different texture for evolved

    // Old-style constructor (kept for compatibility - no pierce)
    public Bullet(float startX, float startY, float dirX, float dirY, int damage) {
        this(startX, startY, dirX, dirY, damage, DEFAULT_SPEED, DEFAULT_SIZE, DEFAULT_SIZE, 0);
    }

    // Constructor with speed & size (no pierce)
    public Bullet(float startX,
                  float startY,
                  float dirX,
                  float dirY,
                  int damage,
                  float speed,
                  float width,
                  float height) {
        this(startX, startY, dirX, dirY, damage, speed, width, height, 0);
    }

    // Full constructor with pierce support
    public Bullet(float startX,
                  float startY,
                  float dirX,
                  float dirY,
                  int damage,
                  float speed,
                  float width,
                  float height,
                  int pierceCount) {

        this.x = startX;
        this.y = startY;
        this.spawnX = startX;
        this.spawnY = startY;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.pierceCount = pierceCount;
        this.pierceRemaining = pierceCount;
        this.hitEnemyIds = new HashSet<>();

        // Normalize direction
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        this.vx = dirX / len;
        this.vy = dirY / len;

        this.hitBox = new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    public void update(float delta) {
        x += vx * speed * delta;
        y += vy * speed * delta;

        hitBox.x = (int) x;
        hitBox.y = (int) y;
    }

    public boolean isOffScreen() {
        float distance = GameApp.distance(x, y, spawnX, spawnY);
        return distance > MAX_TRAVEL_DISTANCE;
    }

    public void render() {
        String textureKey = isEvolved ? EVOLVED_TEXTURE_KEY : TEXTURE_KEY;
        
        if (isEvolved) {
            // Evolved bullets have a purple/pink tint
            // GameApp doesn't have direct tint support, so we draw normally
            // Could use different sprite later
            GameApp.drawTexture(textureKey, x, y, width * 1.2f, height * 1.2f);
        } else {
            GameApp.drawTexture(textureKey, x, y, width, height);
        }
    }

    // ============================================
    // PIERCE SYSTEM
    // ============================================

    /**
     * Called when bullet hits an enemy.
     * @param enemyId unique ID of the enemy hit
     * @return true if bullet should continue, false if it should be destroyed
     */
    public boolean onHitEnemy(int enemyId) {
        // Check if already hit this enemy
        if (hitEnemyIds.contains(enemyId)) {
            return true; // Already hit, don't count, continue
        }
        
        // Mark enemy as hit
        hitEnemyIds.add(enemyId);
        
        // Check pierce
        if (pierceCount == -1) {
            // Infinite pierce
            return true;
        } else if (pierceRemaining > 0) {
            // Can pierce more enemies
            pierceRemaining--;
            return true;
        } else {
            // No pierce left, destroy
            return false;
        }
    }

    /**
     * Check if this bullet has already hit a specific enemy.
     */
    public boolean hasHitEnemy(int enemyId) {
        return hitEnemyIds.contains(enemyId);
    }

    /**
     * Check if bullet can pierce (has pierce remaining or infinite).
     */
    public boolean canPierce() {
        return pierceCount == -1 || pierceRemaining > 0;
    }

    /**
     * Get remaining pierce count.
     */
    public int getPierceRemaining() {
        if (pierceCount == -1) return Integer.MAX_VALUE;
        return pierceRemaining;
    }

    // ============================================
    // EVOLVED BULLET
    // ============================================

    /**
     * Set whether this is an evolved bullet (for special rendering).
     */
    public void setEvolved(boolean evolved) {
        this.isEvolved = evolved;
    }

    /**
     * Check if this is an evolved bullet.
     */
    public boolean isEvolved() {
        return isEvolved;
    }

    // ============================================
    // GETTERS
    // ============================================

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public int getDamage() { return damage; }
    public float getDirX() { return vx; }  // vx is normalized direction
    public float getDirY() { return vy; }  // vy is normalized direction

    public Rectangle getHitBox() {
        return hitBox;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Reset bullet for object pooling reuse.
     */
    public void reset(float startX, float startY, float dirX, float dirY,
                      int damage, float speed, float width, float height, int pierceCount) {
        this.x = startX;
        this.y = startY;
        this.spawnX = startX;
        this.spawnY = startY;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.pierceCount = pierceCount;
        this.pierceRemaining = pierceCount;
        this.destroyed = false;
        this.isEvolved = false;
        this.hitEnemyIds.clear();

        // Normalize direction
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        this.vx = dirX / len;
        this.vy = dirY / len;

        this.hitBox.x = (int) x;
        this.hitBox.y = (int) y;
        this.hitBox.width = (int) width;
        this.hitBox.height = (int) height;
    }
}
