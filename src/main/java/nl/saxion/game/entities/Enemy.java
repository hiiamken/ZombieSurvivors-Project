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
    public static final int SPRITE_SIZE = 100;

    // Hitbox for collisions with bullets
    private Rectangle hitBox;


    public Enemy(float startX, float startY, float speed, int maxHealth) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        // Initialize hitbox for collision detection
        this.hitBox = new Rectangle((int) x, (int) y, SPRITE_SIZE, SPRITE_SIZE);
    }


    public void update(float delta) {
        // Simple zombie behaviour: move downward
        y -= speed * delta;

        // Sync hitbox to current position
        hitBox.x = (int) x;
        hitBox.y = (int) y;
    }


    public void render() {
        GameApp.drawTexture("enemy", x, y, SPRITE_SIZE, SPRITE_SIZE);
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    // HEALTH SYSTEM


    public void takeDamage(int amount) {
        health -= amount;

        // Ensure health never goes below 0
        health = (int) GameApp.clamp(health, 0, maxHealth);
    }

    public boolean isDead() {
        return health <= 0;
    }

    // Getters for position
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}

