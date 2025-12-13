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
    public static final int SPRITE_SIZE = 24;
    // Wall hitbox (small, for wall collision)
    public static final int HITBOX_WIDTH = 8;
    public static final int HITBOX_HEIGHT = 8;
    // Damage hitbox (larger, covers body and head for player-enemy collision)
    public static final int DAMAGE_HITBOX_WIDTH = 12;
    public static final int DAMAGE_HITBOX_HEIGHT = 14;

    // Wall hitbox offset: adjusted to match sprite position
    private static final float WALL_OFFSET_X = 20f;
    private static final float WALL_OFFSET_Y = 16f;

    // Damage hitbox offset: centered to cover sprite
    private static final float DAMAGE_OFFSET_X = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2f;
    private static final float DAMAGE_OFFSET_Y = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2f;

    // Collision constants (for smooth movement near walls)
    private static final float EPSILON = 2.0f;
    private static final float SLIDE_MARGIN = 1.0f;

    // Separation constants (to prevent zombies from overlapping - like Vampire Survivors)
    private static final float SEPARATION_RADIUS = 20f;  // Minimum distance between zombies
    private static final float SEPARATION_FORCE = 80f;   // Push force strength


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

    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        // Wall hitbox position = sprite position + offset
        this.wallHitBox = new Rectangle((int) (x + WALL_OFFSET_X), (int) (y + WALL_OFFSET_Y), HITBOX_WIDTH, HITBOX_HEIGHT);

        // Damage hitbox position - larger to cover body and head
        this.damageHitBox = new Rectangle((int) (x + DAMAGE_OFFSET_X), (int) (y + DAMAGE_OFFSET_Y), DAMAGE_HITBOX_WIDTH, DAMAGE_HITBOX_HEIGHT);
    }

    // Enemy chases player with collision detection
    public void update(float delta, float playerX, float playerY, CollisionChecker collisionChecker, java.util.List<Enemy> allEnemies) {

        updateAnimationState(delta);

        if (isDying || collisionChecker == null) {
            // Update hitboxes even when not moving
            wallHitBox.x = (int)(x + WALL_OFFSET_X);
            wallHitBox.y = (int)(y + WALL_OFFSET_Y);
            damageHitBox.x = (int)(x + DAMAGE_OFFSET_X);
            damageHitBox.y = (int)(y + DAMAGE_OFFSET_Y);
            return;
        }

        // Direction vector to player
        float dirX = playerX - x;
        float dirY = playerY - y;
        float dist = (float)Math.sqrt(dirX*dirX + dirY*dirY);

        if (dist > 0.001f) {
            dirX /= dist;
            dirY /= dist;
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
        float offsetX = WALL_OFFSET_X;
        float offsetY = WALL_OFFSET_Y;

        // Save original position
        float originalX = x;
        float originalY = y;

        // Normalize diagonal movement for consistent speed
        if (dx != 0 && dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float normalizedDx = dx / length;
            float normalizedDy = dy / length;
            dx = normalizedDx * speed * delta;
            dy = normalizedDy * speed * delta;
        }

        // Move X first (standard 2D game approach)
        if (dx != 0 && collisionChecker != null) {
            float newX = originalX + dx;
            float hitboxWorldX = newX + offsetX;
            float hitboxWorldY = originalY + offsetY;

            // When moving horizontal, shrink height to avoid friction with ceiling/floor
            float checkWidth = (float)HITBOX_WIDTH - EPSILON;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON - SLIDE_MARGIN;
            float checkX = hitboxWorldX + EPSILON / 2f;
            float checkY = hitboxWorldY + (EPSILON + SLIDE_MARGIN) / 2f;

            boolean collisionX = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionX) {
                x = newX;
            }
            // Enemy doesn't need corner correction - it chases player and finds path naturally
        } else if (dx != 0) {
            x = originalX + dx;
        }

        // Move Y (using updated X position)
        if (dy != 0 && collisionChecker != null) {
            float newY = originalY + dy;
            float hitboxWorldX = x + offsetX;
            float hitboxWorldY = newY + offsetY;

            // When moving vertical, shrink width to avoid friction with left/right walls
            float checkWidth = (float)HITBOX_WIDTH - EPSILON - SLIDE_MARGIN;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON;
            float checkX = hitboxWorldX + (EPSILON + SLIDE_MARGIN) / 2f;
            float checkY = hitboxWorldY + EPSILON / 2f;

            boolean collisionY = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionY) {
                y = newY;
            }
        } else if (dy != 0) {
            y = originalY + dy;
        }

        // ===== SEPARATION: Push each other to prevent overlapping (like Vampire Survivors) =====
        if (allEnemies != null) {
            float separationX = 0f;
            float separationY = 0f;

            for (Enemy other : allEnemies) {
                if (other == this || other.isDying || other.isDead()) {
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

            // Apply separation force (with wall collision check)
            if (separationX != 0 || separationY != 0) {
                float newX = x + separationX;
                float newY = y + separationY;

                // Check wall collision for separation X
                if (separationX != 0) {
                    float hitboxWorldX = newX + offsetX;
                    float hitboxWorldY = y + offsetY;
                    if (!collisionChecker.checkCollision(hitboxWorldX, hitboxWorldY, HITBOX_WIDTH - EPSILON, HITBOX_HEIGHT - EPSILON)) {
                        x = newX;
                    }
                }

                // Check wall collision for separation Y
                if (separationY != 0) {
                    float hitboxWorldX = x + offsetX;
                    float hitboxWorldY = newY + offsetY;
                    if (!collisionChecker.checkCollision(hitboxWorldX, hitboxWorldY, HITBOX_WIDTH - EPSILON, HITBOX_HEIGHT - EPSILON)) {
                        y = newY;
                    }
                }
            }
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

        if (isDying) {
            currentAnimation = "zombie_death";
            deathAnimationTimer += delta;

            // Reset animation when first entering death state
            if (!previousAnimation.equals("zombie_death") && GameApp.hasAnimation("zombie_death")) {
                GameApp.resetAnimation("zombie_death");
            }
        } else if (hitAnimationTimer > 0f) {
            currentAnimation = "zombie_hit";
            hitAnimationTimer -= delta;

            // Reset animation when first entering hit state
            if (!previousAnimation.equals("zombie_hit") && GameApp.hasAnimation("zombie_hit")) {
                GameApp.resetAnimation("zombie_hit");
            }

            if (hitAnimationTimer <= 0f) {
                hitAnimationTimer = 0f;
            }
        } else {
            currentAnimation = "zombie_run";
        }
    }

    public void render() {
        if (GameApp.hasAnimation(currentAnimation)) {
            GameApp.drawAnimation(currentAnimation, x, y, SPRITE_SIZE, SPRITE_SIZE);
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
        if (GameApp.hasAnimation("zombie_death") && GameApp.isAnimationFinished("zombie_death")) {
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

}
