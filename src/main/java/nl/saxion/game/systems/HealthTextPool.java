package nl.saxion.game.systems;

import nl.saxion.game.entities.HealthText;

import java.util.ArrayList;
import java.util.List;

// Object pool for health texts (performance optimization)
public class HealthTextPool {
    private static final int POOL_SIZE = 50;
    private List<HealthText> pool;
    private List<HealthText> active;

    public HealthTextPool() {
        pool = new ArrayList<>();
        active = new ArrayList<>();

        // Pre-allocate pool objects
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.add(new HealthText());
        }
    }

    // Get a health text from pool
    public HealthText obtain() {
        HealthText text;
        if (pool.isEmpty()) {
            // Pool exhausted, create new one (shouldn't happen often)
            text = new HealthText();
        } else {
            text = pool.remove(pool.size() - 1);
        }
        active.add(text);
        return text;
    }

    // Return health text to pool when done
    public void free(HealthText text) {
        if (active.remove(text)) {
            text.reset();
            pool.add(text);
        }
    }

    // Update all active health texts and free expired ones
    public void update(float delta) {
        List<HealthText> toFree = new ArrayList<>();
        for (HealthText text : active) {
            text.update(delta);
            if (!text.isActive) {
                toFree.add(text);
            }
        }
        for (HealthText text : toFree) {
            free(text);
        }
    }

    // Get all active health texts for rendering
    public List<HealthText> getActive() {
        return active;
    }

    // Clear all active texts (for game reset)
    public void clear() {
        for (HealthText text : new ArrayList<>(active)) {
            free(text);
        }
    }
}
