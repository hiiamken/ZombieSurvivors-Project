package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.awt.Rectangle;

/**
 * Breakable object that can be destroyed when shot by bullets.
 * Uses spritesheet object.png with 64x64 frame size.
 * Row 0: 3 idle frames (col 0-2), 4 break frames (col 3-6)
 * Requires 2-10 hits (random) to break completely.
 */
public class BreakableObject {

    // World position
    private float x;
    private float y;

    // Sprite and hitbox dimensions
    public static final int SPRITE_SIZE = 64;  // Frame size in spritesheet
    public static final int RENDER_SIZE = 80;  // Render size (slightly larger for better visibility)
    public static final int HITBOX_SIZE = 70;  // Hitbox slightly smaller than render size

    // Offset to center hitbox within rendered sprite
    private static final float HITBOX_OFFSET = (RENDER_SIZE - HITBOX_SIZE) / 2f;

    // Hitbox for bullet collision detection
    private Rectangle hitbox;

    // Health system - requires multiple hits to break
    private int health;
    private int maxHealth;

    // State flags
    private boolean isBroken = false;
    private boolean isBreaking = false;
    private float breakAnimationTimer = 0f;
    private static final float BREAK_ANIMATION_DURATION = 0.6f; // 4 frames x 0.15s

    // Hit flash effect - renders bright white when hit
    private boolean isFlashing = false;
    private float flashTimer = 0f;
    private static final float FLASH_DURATION = 0.15f; // Flash white for 0.15 seconds (visible)

    // Current animation name
    private String currentAnimation = "barrel_idle";

    /**
     * Creates a breakable object at the specified world position.
     * Health is randomized between 2-10 hits required to break.
     * @param x World X position
     * @param y World Y position
     */
    public BreakableObject(float x, float y) {
        this.x = x;
        this.y = y;
        this.hitbox = new Rectangle(
            (int)(x + HITBOX_OFFSET), 
            (int)(y + HITBOX_OFFSET), 
            HITBOX_SIZE, 
            HITBOX_SIZE
        );
        
        // Random health between 2-10 hits required
        this.maxHealth = GameApp.randomInt(2, 11); // 2 to 10 inclusive
        this.health = maxHealth;
    }

    /**
     * Updates the object state (animation timer, breaking state, flash effect).
     * @param delta Time since last frame in seconds
     */
    public void update(float delta) {
        // Update flash timer
        if (isFlashing) {
            flashTimer -= delta;
            if (flashTimer <= 0f) {
                isFlashing = false;
                flashTimer = 0f;
            }
        }

        // Update break animation
        if (isBreaking) {
            breakAnimationTimer += delta;
            currentAnimation = "barrel_break";

            // Finish break animation
            if (breakAnimationTimer >= BREAK_ANIMATION_DURATION) {
                isBroken = true;
                isBreaking = false;
            }
        }
    }

    /**
     * Renders the object with animation at screen position relative to player.
     * Renders with bright flash effect when recently hit (draws sprite twice with additive effect).
     * @param playerWorldX Player's X position in world coordinates
     * @param playerWorldY Player's Y position in world coordinates
     */
    public void render(float playerWorldX, float playerWorldY) {
        // Don't render if completely broken
        if (isBroken) return;

        // Calculate screen position from world position (player is at screen center)
        float screenX = GameApp.getWorldWidth() / 2f + (x - playerWorldX);
        float screenY = GameApp.getWorldHeight() / 2f + (y - playerWorldY);

        // Draw animation with flash effect
        if (GameApp.hasAnimation(currentAnimation)) {
            // Normal rendering first
            GameApp.drawAnimation(currentAnimation, screenX, screenY, RENDER_SIZE, RENDER_SIZE);
            
            if (isFlashing) {
                // Flash effect: draw sprite again with bright tint to create white flash
                SpriteBatch batch = GameApp.getSpriteBatch();
                Color oldColor = batch.getColor().cpy(); // Save current color
                // Use bright color (values > 1 create overbright/white effect)
                batch.setColor(3f, 3f, 3f, 1f);
                GameApp.drawAnimation(currentAnimation, screenX, screenY, RENDER_SIZE, RENDER_SIZE);
                batch.setColor(oldColor); // Restore original color
            }
        } else {
            // Fallback: log warning if animation not loaded
            GameApp.log("Warning: Animation '" + currentAnimation + "' not found for BreakableObject");
        }
    }

    /**
     * Called when hit by a bullet - reduces health and triggers flash effect.
     * Starts break animation when health reaches 0.
     * @return true if object was destroyed (health reached 0), false otherwise
     */
    public boolean takeDamage() {
        if (isBreaking || isBroken) {
            return false;
        }

        // Reduce health
        health--;
        
        // Trigger flash effect
        isFlashing = true;
        flashTimer = FLASH_DURATION;

        // Check if destroyed
        if (health <= 0) {
            startBreaking();
            return true;
        }

        return false;
    }

    /**
     * Starts the break animation when health reaches 0.
     */
    private void startBreaking() {
        isBreaking = true;
        breakAnimationTimer = 0f;
        currentAnimation = "barrel_break";

        // Reset animation to play from start
        if (GameApp.hasAnimation("barrel_break")) {
            GameApp.resetAnimation("barrel_break");
        }
    }

    /**
     * Legacy method - now calls takeDamage() internally.
     * Kept for backward compatibility.
     */
    public void breakObject() {
        // Force break by setting health to 0
        health = 0;
        startBreaking();
    }

    /**
     * Checks if the object is completely broken and should be removed.
     * @return true if object is fully broken
     */
    public boolean isBroken() {
        return isBroken;
    }

    /**
     * Checks if the object is currently playing break animation.
     * @return true if breaking animation is in progress
     */
    public boolean isBreaking() {
        return isBreaking;
    }

    /**
     * Checks if the object can be shot (not broken and not currently breaking).
     * @return true if object can receive bullet hits
     */
    public boolean canBeShot() {
        return !isBroken && !isBreaking;
    }

    /**
     * Gets the hitbox for collision detection.
     * @return Rectangle hitbox
     */
    public Rectangle getHitbox() {
        return hitbox;
    }

    // Position getters
    public float getX() { return x; }
    public float getY() { return y; }

    /**
     * Gets center X position (useful for spawning items).
     * @return Center X coordinate
     */
    public float getCenterX() {
        return x + RENDER_SIZE / 2f;
    }

    /**
     * Gets center Y position (useful for spawning items).
     * @return Center Y coordinate
     */
    public float getCenterY() {
        return y + RENDER_SIZE / 2f;
    }

    /**
     * Checks if object is currently flashing (hit effect).
     * @return true if flashing
     */
    public boolean isFlashing() {
        return isFlashing;
    }

    /**
     * Gets current health of the object.
     * @return Current health
     */
    public int getHealth() {
        return health;
    }

    /**
     * Gets maximum health of the object.
     * @return Maximum health (2-10)
     */
    public int getMaxHealth() {
        return maxHealth;
    }
}
