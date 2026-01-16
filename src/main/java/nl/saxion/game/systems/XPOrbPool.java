package nl.saxion.game.systems;

import nl.saxion.game.entities.OrbType;
import nl.saxion.game.entities.XPOrb;

import java.util.ArrayList;
import java.util.List;

/**
 * Object pool for XP orbs (performance optimization).
 * Reduces garbage collection by reusing orb objects instead of creating new ones.
 */
public class XPOrbPool {
    private static final int INITIAL_POOL_SIZE = 200;
    private static final int MAX_POOL_SIZE = 1000;
    
    private final List<XPOrb> pool;
    private final List<XPOrb> active;
    
    public XPOrbPool() {
        pool = new ArrayList<>(INITIAL_POOL_SIZE);
        active = new ArrayList<>(INITIAL_POOL_SIZE);
        
        // Pre-allocate pool objects
        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            pool.add(new XPOrb(0, 0, OrbType.BLUE));
        }
    }
    
    /**
     * Obtain an XP orb from the pool and initialize it.
     */
    public XPOrb obtain(float x, float y, OrbType orbType) {
        XPOrb orb;
        if (pool.isEmpty()) {
            // Pool exhausted, create new one
            orb = new XPOrb(x, y, orbType);
        } else {
            orb = pool.remove(pool.size() - 1);
            orb.reset(x, y, orbType);
        }
        active.add(orb);
        return orb;
    }
    
    /**
     * Obtain an XP orb with legacy int xpValue (defaults to BLUE).
     */
    public XPOrb obtain(float x, float y, int xpValue) {
        XPOrb orb;
        if (pool.isEmpty()) {
            orb = new XPOrb(x, y, xpValue);
        } else {
            orb = pool.remove(pool.size() - 1);
            orb.reset(x, y, OrbType.BLUE);
        }
        active.add(orb);
        return orb;
    }
    
    /**
     * Return orb to pool when done.
     */
    public void free(XPOrb orb) {
        if (active.remove(orb)) {
            if (pool.size() < MAX_POOL_SIZE) {
                pool.add(orb);
            }
        }
    }
    
    /**
     * Free all collected orbs.
     */
    public void freeCollected() {
        List<XPOrb> toFree = new ArrayList<>();
        for (XPOrb orb : active) {
            if (orb.isCollected()) {
                toFree.add(orb);
            }
        }
        for (XPOrb orb : toFree) {
            free(orb);
        }
    }
    
    /**
     * Get all active orbs.
     */
    public List<XPOrb> getActive() {
        return active;
    }
    
    /**
     * Clear all orbs (for game reset).
     */
    public void clear() {
        for (XPOrb orb : new ArrayList<>(active)) {
            free(orb);
        }
    }
    
    /**
     * Get pool statistics for debugging.
     */
    public String getStats() {
        return String.format("XPOrbPool: %d active, %d pooled", active.size(), pool.size());
    }
}
