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
        x += dirX * speed * delta;
        y += dirY * speed * delta;

        // Update hitbox position (centered in sprite)
        float hitboxOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2f;
        hitBox.x = (int) (x + hitboxOffset);
        hitBox.y = (int) (y + hitboxOffset);
    }

    public void render() {
        GameApp.drawTexture("enemy", x, y, SPRITE_SIZE, SPRITE_SIZE);
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    public void takeDamage(int amount) {
        health -= amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
