package nl.saxion.game.systems;

import nl.saxion.game.entities.Bullet;

import java.util.ArrayList;
import java.util.List;

/**
 * Object pool for bullets (performance optimization).
 * Reduces garbage collection by reusing bullet objects instead of creating new ones.
 */
public class BulletPool {
    private static final int INITIAL_POOL_SIZE = 100;
    private static final int MAX_POOL_SIZE = 500;
    
    private final List<Bullet> pool;
    private final List<Bullet> active;
    
    public BulletPool() {
        pool = new ArrayList<>(INITIAL_POOL_SIZE);
        active = new ArrayList<>(INITIAL_POOL_SIZE);
        
        // Pre-allocate pool objects
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            pool.add(createBullet());
        }
    }
    
    private Bullet createBullet() {
        // Create a default bullet that will be reset when obtained
        return new Bullet(0, 0, 1, 0, 0);
    }
    
    /**
     * Obtain a bullet from the pool and initialize it.
     */
    public Bullet obtain(float startX, float startY, float dirX, float dirY, 
                         int damage, float speed, float width, float height, int pierceCount) {
        Bullet bullet;
        if (pool.isEmpty()) {
            // Pool exhausted, create new one
            bullet = new Bullet(startX, startY, dirX, dirY, damage, speed, width, height, pierceCount);
        } else {
            bullet = pool.remove(pool.size() - 1);
            bullet.reset(startX, startY, dirX, dirY, damage, speed, width, height, pierceCount);
        }
        active.add(bullet);
        return bullet;
    }
    
    /**
     * Obtain a bullet with default speed and size.
     */
    public Bullet obtain(float startX, float startY, float dirX, float dirY, int damage) {
        return obtain(startX, startY, dirX, dirY, damage, 400f, 10f, 10f, 0);
    }
    
    /**
     * Return bullet to pool when done.
     */
    public void free(Bullet bullet) {
        if (active.remove(bullet)) {
            if (pool.size() < MAX_POOL_SIZE) {
                pool.add(bullet);
            }
            // If pool is full, let GC handle it
        }
    }
    
    /**
     * Free all destroyed or off-screen bullets.
     */
    public void freeDestroyed() {
        List<Bullet> toFree = new ArrayList<>();
        for (Bullet bullet : active) {
            if (bullet.isDestroyed() || bullet.isOffScreen()) {
                toFree.add(bullet);
            }
        }
        for (Bullet bullet : toFree) {
            free(bullet);
        }
    }
    
    /**
     * Get all active bullets.
     */
    public List<Bullet> getActive() {
        return active;
    }
    
    /**
     * Clear all bullets (for game reset).
     */
    public void clear() {
        for (Bullet bullet : new ArrayList<>(active)) {
            free(bullet);
        }
    }
    
    /**
     * Get pool statistics for debugging.
     */
    public String getStats() {
        return String.format("BulletPool: %d active, %d pooled", active.size(), pool.size());
    }
}
