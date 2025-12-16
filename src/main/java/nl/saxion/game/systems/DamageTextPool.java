package nl.saxion.game.systems;

import nl.saxion.game.entities.DamageText;

import java.util.ArrayList;
import java.util.List;

// Object pool for damage texts (performance optimization)
public class DamageTextPool {
    private static final int POOL_SIZE = 150;
    private List<DamageText> pool;
    private List<DamageText> active;

    public DamageTextPool() {
        pool = new ArrayList<>();
        active = new ArrayList<>();

        // Pre-allocate pool objects
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.add(new DamageText());
        }
    }

    // Get a damage text from pool
    public DamageText obtain() {
        DamageText text;
        if (pool.isEmpty()) {
            // Pool exhausted, create new one (shouldn't happen often)
            text = new DamageText();
        } else {
            text = pool.remove(pool.size() - 1);
        }
        active.add(text);
        return text;
    }

    // Return damage text to pool when done
    public void free(DamageText text) {
        if (active.remove(text)) {
            text.reset();
            pool.add(text);
        }
    }

    // Update all active damage texts and free expired ones
    public void update(float delta) {
        List<DamageText> toFree = new ArrayList<>();
        for (DamageText text : active) {
            text.update(delta);
            if (!text.isActive) {
                toFree.add(text);
            }
        }
        for (DamageText text : toFree) {
            free(text);
        }
    }

    // Get all active damage texts for rendering
    public List<DamageText> getActive() {
        return active;
    }

    // Clear all active texts (for game reset)
    public void clear() {
        for (DamageText text : new ArrayList<>(active)) {
            free(text);
        }
    }
}
