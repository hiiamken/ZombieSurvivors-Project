package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

/**
 * Healing item (chicken) that drops from breakable objects.
 * Similar to XPOrb but restores player health instead of giving XP.
 * Features magnet system that pulls item toward player when in range.
 * Compatible with Orb Magnet passive item (ATTRACTORB).
 */
public class HealingItem {

    private float x, y;
    private int healAmount;
    private float lifetime = 120f; // 2 minutes (120 seconds) before expiration
    private boolean collected = false;

    // Magnet system - same as XPOrb
    private float baseMagnetRange = 60f;  // Slightly smaller than orbs by default
    private float magnetSpeed = 120f;

    // Render size (smaller, no pulsing effect)
    private static final float BASE_SIZE = 24f;

    /**
     * Creates a healing item at the specified position.
     * @param x World X position
     * @param y World Y position
     * @param healAmount Amount of health to restore (typically 10-30)
     */
    public HealingItem(float x, float y, int healAmount) {
        this.x = x;
        this.y = y;
        this.healAmount = healAmount;
    }

    /**
     * Updates the healing item's position and magnet effect.
     * @param delta Time since last frame
     * @param playerX Player's world X position
     * @param playerY Player's world Y position
     * @param magnetBonusRange Bonus magnet range from ATTRACTORB passive item
     */
    public void update(float delta, float playerX, float playerY, float magnetBonusRange) {
        lifetime -= delta;

        if (collected) return;

        // Calculate effective magnet range (base + bonus from passive item)
        float effectiveMagnetRange = baseMagnetRange + magnetBonusRange;
        
        float dist = GameApp.distance(x, y, playerX, playerY);

        // Magnet effect - pull toward player when in range
        if (dist < effectiveMagnetRange && dist > 0) {
            float dx = (playerX - x) / dist;
            float dy = (playerY - y) / dist;
            
            // Speed increases as item gets closer
            float speedMultiplier = 1f + (1f - dist / effectiveMagnetRange) * 0.5f;
            x += dx * magnetSpeed * speedMultiplier * delta;
            y += dy * magnetSpeed * speedMultiplier * delta;
        }

        // Collect when very close to player
        if (dist < 25f) {
            collected = true;
        }
    }

    /**
     * Overload for backward compatibility without magnet bonus.
     */
    public void update(float delta, float playerX, float playerY) {
        update(delta, playerX, playerY, 0f);
    }

    /**
     * Renders the healing item (chicken texture) - static, no animation.
     * Requires sprite rendering to be active.
     * @param playerWorldX Player's world X position
     * @param playerWorldY Player's world Y position
     */
    public void render(float playerWorldX, float playerWorldY) {
        if (collected) return;

        float screenX = GameApp.getWorldWidth() / 2f + (x - playerWorldX);
        float screenY = GameApp.getWorldHeight() / 2f + (y - playerWorldY);

        // Fixed size, no pulsing effect
        float size = BASE_SIZE;

        // Draw chicken texture
        if (GameApp.hasTexture("chicken_item")) {
            GameApp.drawTexture("chicken_item", 
                screenX - size / 2, 
                screenY - size / 2, 
                size, size);
        } else {
            // Fallback: draw a red circle with cross (health symbol)
            // This requires shape rendering, so it's a last resort
            GameApp.setColor(255, 100, 100, 255);
            GameApp.drawCircle(screenX, screenY, size / 2);
        }
    }

    /**
     * Renders a glow effect around the item.
     * Currently disabled for cleaner visuals.
     * @param playerWorldX Player's world X position
     * @param playerWorldY Player's world Y position
     */
    public void renderGlow(float playerWorldX, float playerWorldY) {
        // Glow effect disabled - chicken renders without background
    }

    /**
     * Checks if the item has been collected.
     * @return true if collected
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Checks if the item has expired.
     * @return true if expired
     */
    public boolean isExpired() {
        return lifetime <= 0f;
    }

    /**
     * Gets the health amount this item restores.
     * @return Heal amount
     */
    public int getHealAmount() {
        return healAmount;
    }

    /**
     * Gets the X position.
     * @return World X position
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the Y position.
     * @return World Y position
     */
    public float getY() {
        return y;
    }
}
