package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
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
    public static final int HITBOX_SIZE = 16;

    // Animation state
    private String currentAnimation = "zombie_run"; // running animation
    private boolean isDying = false;
    private float deathAnimationTimer = 0f;
    private float hitAnimationTimer = 0f;
    private static final float HIT_ANIMATION_DURATION = 0.3f; // Show hit animation for 0.3 seconds
    private static final float DEATH_ANIMATION_DURATION = 1.0f; // Death animation duration (approximate)

    // Hitbox for collisions with bullets
    private Rectangle hitBox;

    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        // Hitbox centered in sprite, smaller than sprite for fair collision
        float hitboxOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2f;
        this.hitBox = new Rectangle((int) (x + hitboxOffset), (int) (y + hitboxOffset), HITBOX_SIZE, HITBOX_SIZE);
    }

    // ✅ NEW: enemy chases player
    public void update(float delta, float playerX, float playerY) {

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

        // Move toward player
        if (!isDying) {
            x += dirX * speed * delta;
            y += dirY * speed * delta;
        }

        // Update hitbox position (centered in sprite)
        float hitboxOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2f;
        hitBox.x = (int) (x + hitboxOffset);
        hitBox.y = (int) (y + hitboxOffset);
    }

    private void updateAnimationState(float delta) {
        if (isDying) {
            currentAnimation = "zombie_death";
            // Track death animation timer
            deathAnimationTimer += delta;
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
            GameApp.resetAnimation("zombie_death");
        }
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isDying() {
        return isDying;
    }

    public boolean isDeathAnimationFinished() {
        // Death animation finished if dying and timer exceeds duration
        return isDying && deathAnimationTimer >= DEATH_ANIMATION_DURATION;
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
