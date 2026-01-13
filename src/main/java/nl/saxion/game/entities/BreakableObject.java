package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Breakable object that can be destroyed when shot by bullets.
 * Uses spritesheet object.png with 64x64 frame size.
 * Supports multiple object types: Barrel, Box, Rock, Sign, Mushroom, Chest.
 * Each type uses different rows in the spritesheet.
 * Requires 2-10 hits (random) to break completely.
 */
public class BreakableObject {

    /**
     * Enum defining all object types available in the sprite sheet.
     * Each type uses ONE row with: cols 0-2 for idle, cols 3-6 for break animation.
     */
    public enum ObjectType {
        BARREL(1, "barrel"),      // Row 1: cols 0-2 idle, cols 3-6 break
        BOX(3, "box"),            // Row 3: cols 0-2 idle, cols 3-6 break  
        ROCK(5, "rock"),          // Row 5: cols 0-2 idle, cols 3-6 break
        SIGN(7, "sign"),          // Row 7: cols 0-2 idle, cols 3-6 break
        MUSHROOM(9, "mushroom"),  // Row 9: cols 0-2 idle, cols 3-6 break
        CHEST(11, "chest");       // Row 11: cols 0-2 idle, cols 3-6 break

        public final int row;           // Row for both idle and break animation
        public final String name;       // Animation name prefix

        ObjectType(int row, String name) {
            this.row = row;
            this.name = name;
        }

        /**
         * Get idle animation name for this object type.
         */
        public String getIdleAnimationName() {
            return name + "_idle";
        }

        /**
         * Get break animation name for this object type.
         */
        public String getBreakAnimationName() {
            return name + "_break";
        }

        /**
         * Get a random object type.
         */
        public static ObjectType getRandomType() {
            ObjectType[] types = values();
            return types[GameApp.randomInt(0, types.length)];
        }
    }

    // World position
    private float x;
    private float y;

    // Object type determines which sprite to use
    private ObjectType objectType;

    // Sprite and hitbox dimensions
    public static final int SPRITE_SIZE = 64;  // Frame size in spritesheet
    public static final int RENDER_SIZE = 80;  // Render size (slightly larger for better visibility)
    
    // IMPORTANT: Hitbox much smaller than render - bullet must hit center!
    public static final int HITBOX_SIZE = 32;  // Small hitbox so bullet must hit center
    
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

    // Hit effect particles
    private List<HitParticle> hitParticles = new ArrayList<>();

    // Current animation name (depends on object type)
    private String currentAnimation;

    /**
     * Inner class for hit effect particles.
     * Creates small particles that fly out when object is hit.
     */
    private static class HitParticle {
        float x, y;
        float vx, vy;
        float lifetime;
        float size;
        Color color;

        HitParticle(float x, float y) {
            this.x = x;
            this.y = y;
            // Random velocity direction
            float angle = GameApp.random(0f, 360f) * (float) Math.PI / 180f;
            float speed = GameApp.random(50f, 150f);
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.lifetime = GameApp.random(0.2f, 0.4f);
            this.size = GameApp.random(3f, 6f);
            // Brown/tan color for wood particles
            float r = GameApp.random(0.6f, 0.9f);
            float g = GameApp.random(0.4f, 0.6f);
            float b = GameApp.random(0.2f, 0.4f);
            this.color = new Color(r, g, b, 1f);
        }

        void update(float delta) {
            x += vx * delta;
            y += vy * delta;
            vy -= 200f * delta; // Gravity
            lifetime -= delta;
            // Fade out
            if (lifetime < 0.1f) {
                color.a = lifetime / 0.1f;
            }
        }

        boolean isAlive() {
            return lifetime > 0;
        }
    }

    /**
     * Creates a breakable object at the specified world position with random type.
     * Health is randomized between 2-10 hits required to break.
     * @param x World X position
     * @param y World Y position
     */
    public BreakableObject(float x, float y) {
        this(x, y, ObjectType.getRandomType());
    }

    /**
     * Creates a breakable object at the specified world position with specific type.
     * Health is randomized between 2-10 hits required to break.
     * @param x World X position
     * @param y World Y position
     * @param type The type of object to create
     */
    public BreakableObject(float x, float y, ObjectType type) {
        this.x = x;
        this.y = y;
        this.objectType = type;
        this.currentAnimation = type.getIdleAnimationName();
        
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
     * Updates the object state (animation timer, breaking state, flash effect, particles).
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

        // Update hit particles
        hitParticles.removeIf(p -> {
            p.update(delta);
            return !p.isAlive();
        });

        // Update break animation
        if (isBreaking) {
            breakAnimationTimer += delta;
            currentAnimation = objectType.getBreakAnimationName();

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
            // Fallback: use barrel animation if specific type not loaded
            String fallbackAnim = isBreaking ? "barrel_break" : "barrel_idle";
            if (GameApp.hasAnimation(fallbackAnim)) {
                GameApp.drawAnimation(fallbackAnim, screenX, screenY, RENDER_SIZE, RENDER_SIZE);
            } else {
                // Last resort: draw colored rectangle
                GameApp.log("Warning: Animation '" + currentAnimation + "' not found for BreakableObject");
            }
        }
    }

    /**
     * Renders hit particles for this object.
     * Should be called during shape rendering phase.
     * @param playerWorldX Player's X position in world coordinates
     * @param playerWorldY Player's Y position in world coordinates
     */
    public void renderParticles(float playerWorldX, float playerWorldY) {
        for (HitParticle p : hitParticles) {
            float screenX = GameApp.getWorldWidth() / 2f + (p.x - playerWorldX);
            float screenY = GameApp.getWorldHeight() / 2f + (p.y - playerWorldY);
            
            // Draw particle as small square
            GameApp.setColor((int)(p.color.r * 255), (int)(p.color.g * 255), 
                           (int)(p.color.b * 255), (int)(p.color.a * 255));
            GameApp.drawRect(screenX - p.size/2, screenY - p.size/2, p.size, p.size);
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

        // Spawn hit particles (3-6 particles per hit)
        int particleCount = GameApp.randomInt(3, 7);
        float centerX = getCenterX();
        float centerY = getCenterY();
        for (int i = 0; i < particleCount; i++) {
            hitParticles.add(new HitParticle(centerX, centerY));
        }

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
        currentAnimation = objectType.getBreakAnimationName();

        // Reset animation to play from start
        if (GameApp.hasAnimation(currentAnimation)) {
            GameApp.resetAnimation(currentAnimation);
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

    /**
     * Gets the object type.
     * @return ObjectType enum value
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Checks if this object has active hit particles to render.
     * @return true if there are particles to render
     */
    public boolean hasParticles() {
        return !hitParticles.isEmpty();
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
