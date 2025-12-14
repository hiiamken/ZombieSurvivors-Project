package nl.saxion.game.entities;


import nl.saxion.gameapp.GameApp;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.utils.CollisionChecker;
import java.awt.Image;
import java.awt.Rectangle;

public class Player {
    // Animation states
    public enum AnimationState {
        IDLE, RUNNING_LEFT, RUNNING_RIGHT, HIT, DEAD
    }

    // Position - WORLD COORDINATES
    private float worldX;
    private float worldY;

    // Speed
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Animation
    private AnimationState animationState = AnimationState.IDLE;
    private float hitAnimationTimer = 0f;
    private boolean facingRight = true;
    private static final float HIT_ANIMATION_DURATION = 0.3f;

    // Death state
    private boolean isDying = false;

    public static final int SPRITE_SIZE = 24;
    // Wall hitbox (small, for wall collision)
    public static final int HITBOX_WIDTH = 8;
    public static final int HITBOX_HEIGHT = 8;
    // Damage hitbox (larger, covers body and head for player-enemy collision)
    public static final int DAMAGE_HITBOX_WIDTH = 12;
    public static final int DAMAGE_HITBOX_HEIGHT = 14;

    private float targetShootDirX = 0f;
    private float targetShootDirY = -1f;
    private float smoothShootDirX = 0f;
    private float smoothShootDirY = -1f;

    static final float SHOOT_DIRECTION_SMOOTHING = 12f;

    // Corner Correction constants
    private static final float CORNER_CHECK_DIST = 6.0f;  // Distance to check for gaps beside wall
    private static final float NUDGE_SPEED = 60.0f;       // Speed to auto-slide around corners

    // HitBox (world coordinates)
    // wallHitbox: for wall collision
    private Rectangle wallHitbox;
    // damageHitbox: for enemy interaction
    private Rectangle damageHitbox;

    public Player(float startWorldX, float startWorldY, float speed, int maxHealth, Image sprite) {
        this.worldX = startWorldX;
        this.worldY = startWorldY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        // Wall hitbox: smaller than sprite, centered (world coordinates)
        int wallOffsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int wallOffsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        wallHitbox = new Rectangle((int) (worldX + wallOffsetX), (int) (worldY + wallOffsetY), HITBOX_WIDTH, HITBOX_HEIGHT);

        // Damage hitbox: larger to cover body and head
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox = new Rectangle((int) (worldX + damageOffsetX), (int) (worldY + damageOffsetY), DAMAGE_HITBOX_WIDTH, DAMAGE_HITBOX_HEIGHT);
    }

    // Movement update
    public void update(float delta, InputController input, int worldWidth, int worldHeight, CollisionChecker collisionChecker) {
        // Don't update movement if dying - just update animation state
        if (isDying) {
            animationState = AnimationState.DEAD;
            return;
        }

        // Update hit animation timer
        if (hitAnimationTimer > 0) {
            hitAnimationTimer -= delta;
        }

        float dirX = 0f;
        float dirY = 0f;
        if (input.isMoveUp()) dirY += 1f;
        if (input.isMoveDown()) dirY -= 1f;
        if (input.isMoveLeft()) dirX -= 1f;
        if (input.isMoveRight()) dirX += 1f;
        float dx = dirX * speed * delta;
        float dy = dirY * speed * delta;
        float offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2f;
        float offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2f;

        // Save original position
        float originalWorldX = worldX;
        float originalWorldY = worldY;

        // Normalize diagonal movement to maintain consistent speed
        if (dx != 0 && dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float normalizedDx = dx / length;
            float normalizedDy = dy / length;
            dx = normalizedDx * speed * delta;
            dy = normalizedDy * speed * delta;
        }

        // Simple collision check with epsilon for smooth movement near walls
        final float EPSILON = 2.0f;
        final float SLIDE_MARGIN = 1.0f;

        // Move X first (standard 2D game approach)
        if (dx != 0 && collisionChecker != null) {
            float newWorldX = originalWorldX + dx;
            float hitboxWorldX = newWorldX + offsetX;
            float hitboxWorldY = originalWorldY + offsetY;

            // When moving horizontal (X), keep width standard but shrink HEIGHT
            // to avoid friction with ceiling or floor
            float checkWidth = (float)HITBOX_WIDTH - EPSILON;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON - SLIDE_MARGIN;
            float checkX = hitboxWorldX + EPSILON / 2f;
            float checkY = hitboxWorldY + (EPSILON + SLIDE_MARGIN) / 2f;

            boolean collisionX = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionX) {
                worldX = newWorldX;
            } else {
                // === CORNER CORRECTION X ===
                // Moving horizontal and hit wall -> Check if can slide up/down
                boolean nudgeUp = !collisionChecker.checkCollision(checkX, checkY + CORNER_CHECK_DIST, checkWidth, checkHeight);
                boolean nudgeDown = !collisionChecker.checkCollision(checkX, checkY - CORNER_CHECK_DIST, checkWidth, checkHeight);

                // Smart Nudge: Prioritize player's intended direction
                if (nudgeUp && nudgeDown) {
                    // Both directions clear -> Prioritize based on Y input
                    if (dirY > 0) {
                        worldY += NUDGE_SPEED * delta;
                    } else if (dirY < 0) {
                        worldY -= NUDGE_SPEED * delta;
                    } else {
                        // No Y input -> Binary search to find safe position
                        doBinarySearchX(dx, originalWorldX, checkY, checkWidth, checkHeight, offsetX, collisionChecker, EPSILON);
                    }
                } else if (nudgeUp && dirY >= 0) {
                    worldY += NUDGE_SPEED * delta;
                } else if (nudgeDown && dirY <= 0) {
                    worldY -= NUDGE_SPEED * delta;
                } else {
                    // Real collision -> Binary search to stop at safe position
                    doBinarySearchX(dx, originalWorldX, checkY, checkWidth, checkHeight, offsetX, collisionChecker, EPSILON);
                }
            }
        } else if (dx != 0) {
            worldX = originalWorldX + dx;
        }

        // Move Y (using updated worldX position) - standard 2D approach
        if (dy != 0 && collisionChecker != null) {
            float newWorldY = originalWorldY + dy;
            float hitboxWorldX = worldX + offsetX;
            float hitboxWorldY = newWorldY + offsetY;

            // When moving vertical (Y), keep height standard but shrink WIDTH
            // to avoid friction with left/right walls
            float checkWidth = (float)HITBOX_WIDTH - EPSILON - SLIDE_MARGIN;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON;
            float checkX = hitboxWorldX + (EPSILON + SLIDE_MARGIN) / 2f;
            float checkY = hitboxWorldY + EPSILON / 2f;

            boolean collisionY = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionY) {
                worldY = newWorldY;
            } else {
                // === CORNER CORRECTION Y ===
                // Moving vertical and hit wall -> Check if can slide left/right
                boolean nudgeRight = !collisionChecker.checkCollision(checkX + CORNER_CHECK_DIST, checkY, checkWidth, checkHeight);
                boolean nudgeLeft = !collisionChecker.checkCollision(checkX - CORNER_CHECK_DIST, checkY, checkWidth, checkHeight);

                // Smart Nudge: Prioritize player's intended direction
                if (nudgeRight && nudgeLeft) {
                    if (dirX > 0) {
                        worldX += NUDGE_SPEED * delta;
                    } else if (dirX < 0) {
                        worldX -= NUDGE_SPEED * delta;
                    } else {
                        doBinarySearchY(dy, originalWorldY, checkX, checkWidth, checkHeight, offsetY, collisionChecker, EPSILON);
                    }
                } else if (nudgeRight && dirX >= 0) {
                    worldX += NUDGE_SPEED * delta;
                } else if (nudgeLeft && dirX <= 0) {
                    worldX -= NUDGE_SPEED * delta;
                } else {
                    doBinarySearchY(dy, originalWorldY, checkX, checkWidth, checkHeight, offsetY, collisionChecker, EPSILON);
                }
            }
        } else if (dy != 0) {
            worldY = originalWorldY + dy;
        }

        // Update shooting direction
        updateShootingDirection(dirX, dirY, delta);

        // Clamp world bounds
        clampPosition(worldWidth, worldHeight);

        // Update wall hitbox position
        wallHitbox.x = (int) (worldX + offsetX);
        wallHitbox.y = (int) (worldY + offsetY);

        // Update damage hitbox position (larger, covers body and head)
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox.x = (int) (worldX + damageOffsetX);
        damageHitbox.y = (int) (worldY + damageOffsetY);

        // Update animation state
        updateAnimationState(dirX, dirY);
    }

    // Determine which animation to play based on state
    private void updateAnimationState(float dirX, float dirY) {
        // Dead state has highest priority
        if (isDead()) {
            animationState = AnimationState.DEAD;
            return;
        }

        // Track facing direction - only update when actually moving horizontally
        // This preserves facing direction when standing still or being hit
        if (dirX < 0) {
            facingRight = false;
        } else if (dirX > 0) {
            facingRight = true;
        }
        // If dirX == 0, keep current facingRight value (don't reset)

        // Hit animation plays while timer is active
        if (hitAnimationTimer > 0) {
            animationState = AnimationState.HIT;
            return;
        }

        // Running or idle based on movement
        boolean isMoving = (dirX != 0 || dirY != 0);
        if (isMoving) {
            animationState = facingRight ? AnimationState.RUNNING_RIGHT : AnimationState.RUNNING_LEFT;
        } else {
            animationState = AnimationState.IDLE;
        }
    }

    public float getX() {
        return worldX;
    }

    public float getY() {
        return worldY;
    }

    public void setPosition(float newWorldX, float newWorldY) {
        this.worldX = newWorldX;
        this.worldY = newWorldY;
        // Update wall hitbox position
        int wallOffsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int wallOffsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        wallHitbox.x = (int) (worldX + wallOffsetX);
        wallHitbox.y = (int) (worldY + wallOffsetY);

        // Update damage hitbox position
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox.x = (int) (worldX + damageOffsetX);
        damageHitbox.y = (int) (worldY + damageOffsetY);
    }

    public float getLastMoveDirectionX() {
        return smoothShootDirX;
    }

    public float getLastMoveDirectionY() {
        return smoothShootDirY;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }


    // --- HEALTH SYSTEM ---

    public void takeDamage(int amount) {
        // Don't take damage if already dying
        if (isDying) return;

        health -= amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);

        if (health <= 0) {
            // Start death sequence
            isDying = true;
            GameApp.resetAnimation("player_death");
            System.out.println("Player died! Starting death animation...");
        } else {
            // Trigger hit animation and reset to first frame
            hitAnimationTimer = HIT_ANIMATION_DURATION;
            GameApp.resetAnimation("player_hit");
            System.out.println("Player took " + amount + " damage. HP: " + health + "/" + maxHealth);
        }
    }

    public void heal(int amount) {
        health += amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);
        System.out.println("Player healed " + amount + ". HP: " + health + "/" + maxHealth);
    }

    public boolean isDead() {
        return health <= 0;
    }

    // Check if player is in dying state (health <= 0)
    public boolean isDying() {
        return isDying;
    }

    // Check if death animation has finished playing
    public boolean isDeathAnimationFinished() {
        if (!isDying) return false;
        return GameApp.isAnimationFinished("player_death");
    }

    // Get current animation key for rendering
    public String getCurrentAnimation() {
        switch (animationState) {
            case DEAD:
                return "player_death";
            case HIT:
                return "player_hit";
            case RUNNING_LEFT:
                return "player_run_left";
            case RUNNING_RIGHT:
                return "player_run_right";
            case IDLE:
            default:
                return "player_idle";
        }
    }

    // Get facing direction for sprite flipping
    public boolean isFacingRight() {
        return facingRight;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }

    // Wall HitBox getter (for wall collision)
    public Rectangle getHitBox() {
        return wallHitbox;
    }

    // Damage HitBox getter (for enemy interaction)
    public Rectangle getDamageHitBox() {
        return damageHitbox;
    }


    // Render (now uses animations)
    public void render() {
        String animKey = getCurrentAnimation();
        GameApp.drawAnimation(animKey, worldX, worldY, SPRITE_SIZE, SPRITE_SIZE);
    }

    // Helper: Binary search X to find closest safe position
    private void doBinarySearchX(float dx, float originalX, float checkY, float w, float h, float offsetX, CollisionChecker checker, float epsilon) {
        float minX = dx > 0 ? originalX : originalX + dx;
        float maxX = dx > 0 ? originalX + dx : originalX;
        float bestX = originalX;

        for (int i = 0; i < 8; i++) {
            float midX = (minX + maxX) / 2f;
            float midCheckX = (midX + offsetX) + epsilon / 2f;
            if (!checker.checkCollision(midCheckX, checkY, w, h)) {
                bestX = midX;
                if (dx > 0) minX = midX; else maxX = midX;
            } else {
                if (dx > 0) maxX = midX; else minX = midX;
            }
        }
        worldX = bestX;
    }

    // Helper: Binary search Y to find closest safe position
    private void doBinarySearchY(float dy, float originalY, float checkX, float w, float h, float offsetY, CollisionChecker checker, float epsilon) {
        float minY = dy > 0 ? originalY : originalY + dy;
        float maxY = dy > 0 ? originalY + dy : originalY;
        float bestY = originalY;

        for (int i = 0; i < 8; i++) {
            float midY = (minY + maxY) / 2f;
            float midCheckY = (midY + offsetY) + epsilon / 2f;
            if (!checker.checkCollision(checkX, midCheckY, w, h)) {
                bestY = midY;
                if (dy > 0) minY = midY; else maxY = midY;
            } else {
                if (dy > 0) maxY = midY; else minY = midY;
            }
        }
        worldY = bestY;
    }

    private void updateShootingDirection(float dirX, float dirY, float delta) {
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (length > 0) {
            targetShootDirX = dirX / length;
            targetShootDirY = dirY / length;
        }

        if (dirY == 0 && dirX != 0) {
            targetShootDirY = 0f;
            targetShootDirX = (dirX > 0) ? 1f : -1f;
        }

        if (dirX == 0 && dirY != 0) {
            targetShootDirX = 0f;
            targetShootDirY = (dirY > 0) ? 1f : -1f;
        }

        if (targetShootDirY == 0f && targetShootDirX != 0f) {
            smoothShootDirX = targetShootDirX;
            smoothShootDirY = 0f;
        } else if (targetShootDirX == 0f && targetShootDirY != 0f) {
            smoothShootDirX  = 0f;
            smoothShootDirY = targetShootDirY;
        } else {
            float lerpFactor = 1f - (float) Math.exp(-SHOOT_DIRECTION_SMOOTHING * delta);
            smoothShootDirX = smoothShootDirX + (targetShootDirX - smoothShootDirX) * lerpFactor;
            smoothShootDirY = smoothShootDirY + (targetShootDirY - smoothShootDirY) * lerpFactor;
        }

        float smoothedLength = (float) Math.sqrt(smoothShootDirX * smoothShootDirX + smoothShootDirY * smoothShootDirY);
        if (smoothedLength > 0.001f) {
            smoothShootDirX = smoothShootDirX / smoothedLength;
            smoothShootDirY = smoothShootDirY / smoothedLength;
        }
    }

    private void clampPosition(int worldWidth, int worldHeight) {
        if (worldWidth < Integer.MAX_VALUE / 2 && worldHeight < Integer.MAX_VALUE / 2) {
            float maxX = worldWidth - SPRITE_SIZE;
            float maxY = worldHeight - SPRITE_SIZE;
            worldX = GameApp.clamp(worldX, 0, maxX);
            worldY = GameApp.clamp(worldY, 0, maxY);
        }
    }
}
