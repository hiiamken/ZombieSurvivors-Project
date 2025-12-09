package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import nl.saxion.game.utils.CollisionChecker;
import java.awt.Rectangle;

public class Enemy {

    // Position
    private float x;
    private float y;

    // Movement speed (pixels per second)
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Sprite size constant
    public static final int SPRITE_SIZE = 24;
    public static final int HITBOX_WIDTH = 12;
    public static final int HITBOX_HEIGHT = 16;

    // Animation state
    private String currentAnimation = "zombie_run"; // running animation
    private boolean isDying = false;
    private boolean deathAnimationStarted = false;
    private float deathAnimationTimer = 0f;
    private float hitAnimationTimer = 0f;
    private static final float HIT_ANIMATION_DURATION = 0.3f; // Show hit animation for 0.3 seconds
    private static final float DEATH_ANIMATION_DURATION = 1.5f; // Death animation duration (approximate)

    // Hitbox for collisions with bullets
    private Rectangle hitBox;

    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        // Hitbox nhỏ hơn sprite, centered in sprite
        int offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        this.hitBox = new Rectangle((int) (x + offsetX), (int) (y + offsetY), HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    // ✅ Enemy chases player with smooth sliding collision (like player)
    public void update(float delta, float playerX, float playerY, CollisionChecker collisionChecker) {

        // Update animation state
        updateAnimationState(delta);

        // Vector from enemy to player
        float dx = playerX - x;
        float dy = playerY - y;

        // Distance using GameApp utility
        float distance = GameApp.distance(x, y, playerX, playerY);

        float dirX;
        float dirY;

        if (distance > 0f) {
            dirX = dx / distance;
            dirY = dy / distance;
        } else {
            // Already at player position
            dirX = 0f;
            dirY = 0f;
        }

        // Move toward player (với smooth sliding collision như player)
        if (!isDying && collisionChecker != null) {
            float moveX = dirX * speed * delta;
            float moveY = dirY * speed * delta;

            // Offset for hitbox centering
            float offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2f;
            float offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2f;

            // Move X FIRST
            if (moveX != 0) {
                float newX = x + moveX;
                float hitboxX = newX + offsetX;
                float hitboxY = y + offsetY;

                boolean collX = collisionChecker.checkCollision(hitboxX, hitboxY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT);
                if (!collX) {
                    x = newX;
                } else {
                    // Collision detected - try moving in smaller steps to get closer to wall
                    float stepSize = Math.abs(moveX) / 4f;
                    float stepX = (moveX > 0) ? stepSize : -stepSize;
                    float testX = x;
                    for (int i = 0; i < 4; i++) {
                        float testWorldX = testX + stepX;
                        float testHitboxX = testWorldX + offsetX;
                        if (!collisionChecker.checkCollision(testHitboxX, hitboxY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT)) {
                            testX = testWorldX;
                        } else {
                            break;  // Stop when hitting wall
                        }
                    }
                    x = testX;
                }
            }

            // Move Y SECOND
            if (moveY != 0) {
                float newY = y + moveY;
                float hitboxX = x + offsetX;
                float hitboxY = newY + offsetY;

                boolean collY = collisionChecker.checkCollision(hitboxX, hitboxY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT);
                if (!collY) {
                    y = newY;
                } else {
                    // Collision detected - try moving in smaller steps to get closer to wall
                    float stepSize = Math.abs(moveY) / 4f;
                    float stepY = (moveY > 0) ? stepSize : -stepSize;
                    float testY = y;
                    for (int i = 0; i < 4; i++) {
                        float testWorldY = testY + stepY;
                        float testHitboxY = testWorldY + offsetY;
                        if (!collisionChecker.checkCollision(hitboxX, testHitboxY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT)) {
                            testY = testWorldY;
                        } else {
                            break;  //  Stop when hitting wall
                        }
                    }
                    y = testY;
                }
            }
        }

        // Update hitbox position (centered in sprite)
        float offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2f;
        float offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2f;
        hitBox.x = (int) (x + offsetX);
        hitBox.y = (int) (y + offsetY);
    }

    private void updateAnimationState(float delta) {
        if (isDying) {
            currentAnimation = "zombie_death";
            // Track death animation timer
            deathAnimationTimer += delta;

            if (GameApp.hasAnimation("zombie_death") && GameApp.isAnimationFinished("zombie_death")) {
            }
        } else if (hitAnimationTimer > 0f) {
            // Show hit animation
            currentAnimation = "zombie_hit";
            hitAnimationTimer -= delta;
            // Reset hit animation when timer ends
            if (hitAnimationTimer <= 0f) {
                hitAnimationTimer = 0f;
                GameApp.resetAnimation("zombie_hit");
            }
        } else {
            // run animation by default
            currentAnimation = "zombie_run";
        }
    }

    public void render() {
        if (isDying && !deathAnimationStarted && GameApp.hasAnimation("zombie_death")) {
            GameApp.resetAnimation("zombie_death");
            deathAnimationStarted = true;
        }
        if (GameApp.hasAnimation(currentAnimation)) {
            GameApp.drawAnimation(currentAnimation, x, y, SPRITE_SIZE, SPRITE_SIZE);
        } else {
            // Fallback to static texture
            GameApp.drawTexture("enemy", x, y, SPRITE_SIZE, SPRITE_SIZE);
        }
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    public void takeDamage(int amount) {
        health -= amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);

        // Trigger hit animation
        if (!isDying && health > 0) {
            hitAnimationTimer = HIT_ANIMATION_DURATION;
            GameApp.resetAnimation("zombie_hit");
        }

        // Trigger death animation if health drops to 0
        if (health <= 0 && !isDying) {
            isDying = true;
            deathAnimationTimer = 0f; // Reset timer
            deathAnimationStarted = false;
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

        if (deathAnimationTimer >= DEATH_ANIMATION_DURATION) {
            return true;
        }

        if (GameApp.hasAnimation("zombie_death") && GameApp.isAnimationFinished("zombie_death")) {

            return deathAnimationTimer >= 0.3f;
        }

        return false;
    }

    public float getX() { return x; }
    public float getY() { return y; }

    public void setPosition(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        // Update hitbox position
        int offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        hitBox.x = (int) (x + offsetX);
        hitBox.y = (int) (y + offsetY);
    }

    public float getWidth() { return SPRITE_SIZE; }
    public float getHeight() { return SPRITE_SIZE; }
}
