package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import nl.saxion.game.utils.CollisionChecker;
import java.awt.Rectangle;

public class Enemy {

    // Sprite origin position (same as Player worldX/worldY)
    private float x;
    private float y;

    // Movement speed (pixels per second)
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Sprite size constant
    public static final int SPRITE_SIZE = 36; // Larger sprite for zoomed out view
    // Wall hitbox (small, for wall collision)
    public static final int HITBOX_WIDTH = 12;
    public static final int HITBOX_HEIGHT = 12;
    // Damage hitbox (smaller to reduce early damage - was 18x20, now 14x16)
    public static final int DAMAGE_HITBOX_WIDTH = 14;
    public static final int DAMAGE_HITBOX_HEIGHT = 16;

    // Wall hitbox offset: adjusted to match sprite position
    private static final float WALL_OFFSET_X = 20f;
    private static final float WALL_OFFSET_Y = 16f;

    // Damage hitbox offset: centered to cover sprite (adjusted for smaller hitbox)
    private static final float DAMAGE_OFFSET_X = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2f;
    private static final float DAMAGE_OFFSET_Y = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2f;

    // Separation constants (to prevent zombies from overlapping - like Vampire Survivors)
    private static final float SEPARATION_RADIUS = 28f;  // Minimum distance between zombies (larger for bigger sprites)
    private static final float SEPARATION_FORCE = 90f;   // Push force strength
    
    // Knockback system (like Vampire Survivors) - INCREASED for visible effect
    private float knockbackX = 0f;
    private float knockbackY = 0f;
    private static final float KNOCKBACK_STRENGTH = 250f;  // Base knockback distance (increased significantly)
    private static final float KNOCKBACK_DECAY = 5f;       // How fast knockback decays (slower decay)

    // Soft despawn zones (like Vampire Survivors) - adjusted for 960x540 world view
    public static final float ACTIVE_RADIUS = 700f;   // Active zone: update AI, move, attack
    public static final float RESPAWN_RADIUS = 550f;  // Distance to respawn enemy at (edge of screen)
    public static final float TELEPORT_RADIUS = 900f; // If enemy goes beyond this, teleport to random edge

    // Active/visible state for soft despawn
    private boolean isActive = true;   // Update AI, move, attack
    private boolean isVisible = true;  // Render on screen

    // Animation state
    private String currentAnimation = "zombie_run";
    private boolean isDying = false;
    private float deathAnimationTimer = 0f;
    private float hitAnimationTimer = 0f;
    private static final float HIT_ANIMATION_DURATION = 0.3f;
    private static final float DEATH_ANIMATION_DURATION = 1.5f;

    // Facing direction: true = facing right, false = facing left
    private boolean facingRight = true;

    // Wall hitbox: for wall collision
    private Rectangle wallHitBox;
    // Damage hitbox: for player interaction
    private Rectangle damageHitBox;

    // Zombie type: 1 = original, 3 = zombie3, 4 = zombie4
    private int zombieType = 1;

    // AI behavior type for smarter movement
    private enum AIBehavior {
        CHASE,      // Direct chase (default)
        FLANK_LEFT, // Try to go around player's left
        FLANK_RIGHT // Try to go around player's right
    }
    private AIBehavior aiBehavior = AIBehavior.CHASE;
    private float flankTimer = 0f; // Timer to periodically adjust flanking
    private float flankAngle = 0f; // Current flanking angle
    private static final float FLANK_UPDATE_INTERVAL = 1.5f; // Adjust flanking every 1.5s
    private static final float FLANK_ANGLE_MAX = 45f; // Max flanking angle in degrees

    // Constructor with random zombie type
    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this(startX, startY, speed, maxHealth, getRandomZombieType());
    }

    // Constructor with specific zombie type
    public Enemy(float startX, float startY, float speed, int maxHealth, int zombieType) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.zombieType = zombieType;

        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.isActive = true;
        this.isVisible = true;

        // Set initial animation based on zombie type
        this.currentAnimation = getAnimationName("run");
        this.previousAnimation = this.currentAnimation;

        // Randomly assign AI behavior for variety (40% chase, 30% flank left, 30% flank right)
        float behaviorRoll = (float) Math.random();
        if (behaviorRoll < 0.4f) {
            this.aiBehavior = AIBehavior.CHASE;
        } else if (behaviorRoll < 0.7f) {
            this.aiBehavior = AIBehavior.FLANK_LEFT;
        } else {
            this.aiBehavior = AIBehavior.FLANK_RIGHT;
        }
        
        // Randomize initial flank angle
        this.flankAngle = (float)(Math.random() * FLANK_ANGLE_MAX);
        this.flankTimer = (float)(Math.random() * FLANK_UPDATE_INTERVAL);

        // Wall hitbox position = sprite position + offset
        this.wallHitBox = new Rectangle((int) (x + WALL_OFFSET_X), (int) (y + WALL_OFFSET_Y), HITBOX_WIDTH, HITBOX_HEIGHT);

        // Damage hitbox position - larger to cover body and head
        this.damageHitBox = new Rectangle((int) (x + DAMAGE_OFFSET_X), (int) (y + DAMAGE_OFFSET_Y), DAMAGE_HITBOX_WIDTH, DAMAGE_HITBOX_HEIGHT);
    }

    // Random zombie type: 1, 3, or 4
    private static int getRandomZombieType() {
        int[] types = {1, 3, 4};
        int randomIndex = (int)(Math.random() * types.length);
        return types[randomIndex];
    }

    // Get animation name based on zombie type
    // Type 1: zombie_run, zombie_hit, zombie_death
    // Type 3: zombie3_run, zombie3_hit, zombie3_death
    // Type 4: zombie4_run, zombie4_hit, zombie4_death
    private String getAnimationName(String action) {
        if (zombieType == 1) {
            return "zombie_" + action;
        } else {
            return "zombie" + zombieType + "_" + action;
        }
    }
    
    // Update active/visible state based on distance to player (soft despawn)
    public void updateSoftDespawnState(float playerX, float playerY) {
        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < ACTIVE_RADIUS) {
            // Active zone: update AI, move, attack
            isActive = true;
            isVisible = true;
        } else {
            // Sleep zone or beyond: freeze but keep HP (enemy stays in memory until KILL_RADIUS)
            isActive = false;
            isVisible = false;
        }
    }
    
    // Check if enemy should be teleported (too far from player - like VS)
    public boolean shouldTeleport(float playerX, float playerY) {
        float dx = playerX - x;
        float dy = playerY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance >= TELEPORT_RADIUS;
    }
    
    // Teleport enemy to random edge position around player (like VS respawn mechanic)
    // When enemy goes too far, it reappears from a different direction
    public void teleportToRandomEdge(float playerX, float playerY) {
        // Random angle (0 to 2*PI)
        double angle = Math.random() * 2 * Math.PI;
        
        // Spawn at RESPAWN_RADIUS distance from player
        float newX = playerX + (float)(Math.cos(angle) * RESPAWN_RADIUS);
        float newY = playerY + (float)(Math.sin(angle) * RESPAWN_RADIUS);
        
        // Set new position
        this.x = newX;
        this.y = newY;
        
        // Reset to active state
        this.isActive = true;
        this.isVisible = true;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean isVisible() {
        return isVisible;
    }

    // Enemy chases player with collision detection
    public void update(float delta, float playerX, float playerY, CollisionChecker collisionChecker, java.util.List<Enemy> allEnemies) {
        // Update soft despawn state first
        updateSoftDespawnState(playerX, playerY);

        // Update animation if active, visible, or dying (optimization: skip animation tick for sleeping enemies)
        if (isActive || isVisible || isDying) {
            updateAnimationState(delta);
        }

        // Only update movement and AI if active
        if (!isActive) {
            // Update hitboxes even when not moving (for collision checks)
            wallHitBox.x = (int)(x + WALL_OFFSET_X);
            wallHitBox.y = (int)(y + WALL_OFFSET_Y);
            damageHitBox.x = (int)(x + DAMAGE_OFFSET_X);
            damageHitBox.y = (int)(y + DAMAGE_OFFSET_Y);
            return;
        }

        if (isDying || collisionChecker == null) {
            // Update hitboxes even when not moving
            wallHitBox.x = (int)(x + WALL_OFFSET_X);
            wallHitBox.y = (int)(y + WALL_OFFSET_Y);
            damageHitBox.x = (int)(x + DAMAGE_OFFSET_X);
            damageHitBox.y = (int)(y + DAMAGE_OFFSET_Y);
            return;
        }

        // Update flanking timer and angle
        flankTimer += delta;
        if (flankTimer >= FLANK_UPDATE_INTERVAL) {
            flankTimer = 0f;
            // Slightly adjust flank angle for unpredictable movement
            flankAngle = (float)(Math.random() * FLANK_ANGLE_MAX);
        }

        // Direction vector to player
        float dirX = playerX - x;
        float dirY = playerY - y;
        float dist = (float)Math.sqrt(dirX*dirX + dirY*dirY);

        if (dist > 0.001f) {
            dirX /= dist;
            dirY /= dist;
        }

        // Apply flanking behavior when not too close to player
        // Close range: direct chase, Far range: flank to surround
        if (aiBehavior != AIBehavior.CHASE && dist > 60f) {
            float angleRadians = (float) Math.toRadians(flankAngle);
            
            // Rotate direction vector based on flanking behavior
            if (aiBehavior == AIBehavior.FLANK_LEFT) {
                float newDirX = dirX * (float)Math.cos(angleRadians) - dirY * (float)Math.sin(angleRadians);
                float newDirY = dirX * (float)Math.sin(angleRadians) + dirY * (float)Math.cos(angleRadians);
                dirX = newDirX;
                dirY = newDirY;
            } else if (aiBehavior == AIBehavior.FLANK_RIGHT) {
                float newDirX = dirX * (float)Math.cos(-angleRadians) - dirY * (float)Math.sin(-angleRadians);
                float newDirY = dirX * (float)Math.sin(-angleRadians) + dirY * (float)Math.cos(-angleRadians);
                dirX = newDirX;
                dirY = newDirY;
            }
            
            // Re-normalize after rotation
            float length = (float)Math.sqrt(dirX*dirX + dirY*dirY);
            if (length > 0.001f) {
                dirX /= length;
                dirY /= length;
            }
        }

        // Update facing direction based on movement
        // Update direction whenever there's horizontal movement
        if (dirX < 0) {
            facingRight = true;

        } else if (dirX > 0) {
            facingRight = false;
        }
        // Keep current direction if dirX == 0 (moving purely vertical)

        float dx = dirX * speed * delta;
        float dy = dirY * speed * delta;

        // Normalize diagonal movement for consistent speed
        if (dx != 0 && dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float normalizedDx = dx / length;
            float normalizedDy = dy / length;
            dx = normalizedDx * speed * delta;
            dy = normalizedDy * speed * delta;
        }

        // Apply knockback first
        if (knockbackX != 0 || knockbackY != 0) {
            x += knockbackX * delta;
            y += knockbackY * delta;
            
            // Decay knockback
            knockbackX *= (1f - KNOCKBACK_DECAY * delta);
            knockbackY *= (1f - KNOCKBACK_DECAY * delta);
            
            // Stop knockback when very small
            if (Math.abs(knockbackX) < 1f) knockbackX = 0f;
            if (Math.abs(knockbackY) < 1f) knockbackY = 0f;
        }
        
        // Move freely (no wall collision - enemies can pass through walls like VS)
        x += dx;
        y += dy;

        // ===== SEPARATION: Push each other to prevent overlapping (like Vampire Survivors) =====
        if (allEnemies != null) {
            float separationX = 0f;
            float separationY = 0f;

            for (Enemy other : allEnemies) {
                // Skip self, dead, dying, or inactive enemies (separation only for active enemies)
                if (other == this || other.isDying || other.isDead() || !other.isActive()) {
                    continue;
                }

                // Calculate distance between 2 enemies (center to center)
                float centerX = x + SPRITE_SIZE / 2f;
                float centerY = y + SPRITE_SIZE / 2f;
                float otherCenterX = other.x + SPRITE_SIZE / 2f;
                float otherCenterY = other.y + SPRITE_SIZE / 2f;

                float distX = centerX - otherCenterX;
                float distY = centerY - otherCenterY;
                float distance = (float) Math.sqrt(distX * distX + distY * distY);

                // If too close, push apart
                if (distance < SEPARATION_RADIUS && distance > 0.001f) {
                    // Normalize direction
                    float normX = distX / distance;
                    float normY = distY / distance;

                    // Push strength inversely proportional to distance (closer = stronger push)
                    float pushStrength = (SEPARATION_RADIUS - distance) / SEPARATION_RADIUS;
                    separationX += normX * pushStrength * SEPARATION_FORCE * delta;
                    separationY += normY * pushStrength * SEPARATION_FORCE * delta;
                }
            }

            // Apply separation force (no wall collision - enemies pass through walls)
            x += separationX;
            y += separationY;
        }

        // Update hitboxes after movement
        wallHitBox.x = (int)(x + WALL_OFFSET_X);
        wallHitBox.y = (int)(y + WALL_OFFSET_Y);
        damageHitBox.x = (int)(x + DAMAGE_OFFSET_X);
        damageHitBox.y = (int)(y + DAMAGE_OFFSET_Y);
    }

    // Track previous animation to detect state changes
    private String previousAnimation = "zombie_run";

    private void updateAnimationState(float delta) {
        previousAnimation = currentAnimation;

        String deathAnim = getAnimationName("death");
        String hitAnim = getAnimationName("hit");
        String runAnim = getAnimationName("run");

        if (isDying) {
            currentAnimation = deathAnim;
            deathAnimationTimer += delta;

            // Reset animation when first entering death state
            if (!previousAnimation.equals(deathAnim) && GameApp.hasAnimation(deathAnim)) {
                GameApp.resetAnimation(deathAnim);
            }
        } else if (hitAnimationTimer > 0f) {
            currentAnimation = hitAnim;
            hitAnimationTimer -= delta;

            // Reset animation when first entering hit state
            if (!previousAnimation.equals(hitAnim) && GameApp.hasAnimation(hitAnim)) {
                GameApp.resetAnimation(hitAnim);
            }

            if (hitAnimationTimer <= 0f) {
                hitAnimationTimer = 0f;
            }
        } else {
            currentAnimation = runAnim;
        }
    }

    public void render() {
        String animToRender = currentAnimation;
        
        // Fallback: if hit animation doesn't exist, use run animation instead
        if (currentAnimation.endsWith("_hit") && !GameApp.hasAnimation(currentAnimation)) {
            animToRender = getAnimationName("run");
        }
        
        if (GameApp.hasAnimation(animToRender)) {
            GameApp.drawAnimation(animToRender, x, y, SPRITE_SIZE, SPRITE_SIZE);
        } else {
            GameApp.drawTexture("enemy", x, y, SPRITE_SIZE, SPRITE_SIZE);
        }
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    // Wall HitBox getter (for wall collision and bullets)
    public Rectangle getHitBox() {
        return wallHitBox;
    }

    // Damage HitBox getter (for player interaction)
    public Rectangle getDamageHitBox() {
        return damageHitBox;
    }

    public void takeDamage(int amount) {
        health -= amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);

        // Trigger hit animation
        if (!isDying && health > 0) {
            hitAnimationTimer = HIT_ANIMATION_DURATION;
        }

        // Trigger death animation if health drops to 0
        if (health <= 0 && !isDying) {
            isDying = true;
            deathAnimationTimer = 0f;
        }
    }
    
    /**
     * Apply knockback when hit by bullet (like Vampire Survivors)
     * @param bulletDirX normalized direction X of bullet
     * @param bulletDirY normalized direction Y of bullet
     * @param strength knockback strength multiplier
     */
    public void applyKnockback(float bulletDirX, float bulletDirY, float strength) {
        knockbackX = bulletDirX * KNOCKBACK_STRENGTH * strength;
        knockbackY = bulletDirY * KNOCKBACK_STRENGTH * strength;
    }
    
    /**
     * Apply knockback with default strength
     */
    public void applyKnockback(float bulletDirX, float bulletDirY) {
        applyKnockback(bulletDirX, bulletDirY, 1.0f);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isDying() {
        return isDying;
    }

    public boolean isDeathAnimationFinished() {
        if (!isDying) {
            return false;
        }

        // Check by timer (ensure animation has played long enough)
        if (deathAnimationTimer >= DEATH_ANIMATION_DURATION) {
            return true;
        }

        // Check by GameApp API (if animation is finished)
        String deathAnim = getAnimationName("death");
        if (GameApp.hasAnimation(deathAnim) && GameApp.isAnimationFinished(deathAnim)) {
            return deathAnimationTimer >= 0.3f;
        }

        return false;
    }

    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }

    public void setPosition(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        wallHitBox.x = (int) (x + WALL_OFFSET_X);
        wallHitBox.y = (int) (y + WALL_OFFSET_Y);
        damageHitBox.x = (int) (x + DAMAGE_OFFSET_X);
        damageHitBox.y = (int) (y + DAMAGE_OFFSET_Y);
    }

    public float getWidth() { return SPRITE_SIZE; }
    public float getHeight() { return SPRITE_SIZE; }

    // Get facing direction for sprite flipping
    public boolean isFacingRight() {
        return facingRight;
    }

    // Get zombie type (1, 3, or 4)
    public int getZombieType() {
        return zombieType;
    }

    /**
     * Check if enemy should be removed (death animation completed).
     */
    public boolean shouldRemove() {
        return isDying && deathAnimationTimer >= DEATH_ANIMATION_DURATION;
    }

    /**
     * Reset enemy for object pooling reuse.
     */
    public void reset(float startX, float startY, float speed, int maxHealth) {
        reset(startX, startY, speed, maxHealth, getRandomZombieType());
    }

    /**
     * Reset enemy with specific zombie type for object pooling reuse.
     */
    public void reset(float startX, float startY, float speed, int maxHealth, int zombieType) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.zombieType = zombieType;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.isActive = true;
        this.isVisible = true;
        this.isDying = false;
        this.deathAnimationTimer = 0f;
        this.hitAnimationTimer = 0f;
        this.knockbackX = 0f;
        this.knockbackY = 0f;
        this.facingRight = true;

        // Reset AI behavior
        float behaviorRoll = (float) Math.random();
        if (behaviorRoll < 0.4f) {
            this.aiBehavior = AIBehavior.CHASE;
        } else if (behaviorRoll < 0.7f) {
            this.aiBehavior = AIBehavior.FLANK_LEFT;
        } else {
            this.aiBehavior = AIBehavior.FLANK_RIGHT;
        }
        this.flankAngle = (float)(Math.random() * FLANK_ANGLE_MAX);
        this.flankTimer = (float)(Math.random() * FLANK_UPDATE_INTERVAL);

        // Reset animation
        this.currentAnimation = getAnimationName("run");
        this.previousAnimation = this.currentAnimation;

        // Update hitboxes
        this.wallHitBox.x = (int) (x + WALL_OFFSET_X);
        this.wallHitBox.y = (int) (y + WALL_OFFSET_Y);
        this.damageHitBox.x = (int) (x + DAMAGE_OFFSET_X);
        this.damageHitBox.y = (int) (y + DAMAGE_OFFSET_Y);
    }
}
