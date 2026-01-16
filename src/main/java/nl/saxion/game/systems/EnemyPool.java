package nl.saxion.game.systems;

import nl.saxion.game.entities.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Object pool for enemies (performance optimization).
 * Reduces garbage collection by reusing enemy objects instead of creating new ones.
 */
public class EnemyPool {
    private static final int INITIAL_POOL_SIZE = 100;
    private static final int MAX_POOL_SIZE = 500;
    
    private final List<Enemy> pool;
    private final List<Enemy> active;
    
    public EnemyPool() {
        pool = new ArrayList<>(INITIAL_POOL_SIZE);
        active = new ArrayList<>(INITIAL_POOL_SIZE);
        
        // Pre-allocate pool objects with default values
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            pool.add(new Enemy(0, 0, 50f, 10));
        }
    }
    
    /**
     * Obtain an enemy from the pool and initialize it.
     */
    public Enemy obtain(float startX, float startY, float speed, int maxHealth) {
        Enemy enemy;
        if (pool.isEmpty()) {
            // Pool exhausted, create new one
            enemy = new Enemy(startX, startY, speed, maxHealth);
        } else {
            enemy = pool.remove(pool.size() - 1);
            enemy.reset(startX, startY, speed, maxHealth);
        }
        active.add(enemy);
        return enemy;
    }
    
    /**
     * Obtain an enemy with specific zombie type.
     */
    public Enemy obtain(float startX, float startY, float speed, int maxHealth, int zombieType) {
        Enemy enemy;
        if (pool.isEmpty()) {
            enemy = new Enemy(startX, startY, speed, maxHealth, zombieType);
        } else {
            enemy = pool.remove(pool.size() - 1);
            enemy.reset(startX, startY, speed, maxHealth, zombieType);
        }
        active.add(enemy);
        return enemy;
    }
    
    /**
     * Return enemy to pool when done.
     */
    public void free(Enemy enemy) {
        if (active.remove(enemy)) {
            if (pool.size() < MAX_POOL_SIZE) {
                pool.add(enemy);
            }
        }
    }
    
    /**
     * Free all dead enemies (death animation completed).
     */
    public void freeDead() {
        List<Enemy> toFree = new ArrayList<>();
        for (Enemy enemy : active) {
            if (enemy.shouldRemove()) {
                toFree.add(enemy);
            }
        }
        for (Enemy enemy : toFree) {
            free(enemy);
        }
    }
    
    /**
     * Get all active enemies.
     */
    public List<Enemy> getActive() {
        return active;
    }
    
    /**
     * Clear all enemies (for game reset).
     */
    public void clear() {
        for (Enemy enemy : new ArrayList<>(active)) {
            free(enemy);
        }
    }
    
    /**
     * Get pool statistics for debugging.
     */
    public String getStats() {
        return String.format("EnemyPool: %d active, %d pooled", active.size(), pool.size());
    }
}
