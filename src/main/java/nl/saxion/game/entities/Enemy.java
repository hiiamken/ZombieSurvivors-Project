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

    // Hitbox for collisions with bullets
    private Rectangle hitBox;

    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.hitBox = new Rectangle((int) x, (int) y, SPRITE_SIZE, SPRITE_SIZE);
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

        // Sync hitbox
        hitBox.x = (int) x;
        hitBox.y = (int) y;
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
