package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;
import nl.saxion.game.systems.InputController;
import java.awt.Image;

public class Player {
    // Position
    private float x;
    private float y;

    // Speed
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Sprite
    private Image sprite;

    public static final int SPRITE_SIZE = 16;

    public Player(float startX, float startY, float speed, int maxHealth, Image sprite) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.sprite = sprite;
    }

    // movement
    public void update(float delta, InputController input, int worldWidth, int worldHeight) {
        if (input.isMoveUp()) {
            y = y + speed * delta;  // W
        }
        if (input.isMoveDown()) {
            y = y - speed * delta;  // S
        }
        if (input.isMoveLeft()) {
            x = x - speed * delta;  // A
        }
        if (input.isMoveRight()) {
            x = x + speed * delta;  // D
        }

        // ограничения по краям
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        float maxX = worldWidth - SPRITE_SIZE;
        float maxY = worldHeight - SPRITE_SIZE;

        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    // --- HEALTH SYSTEM ---

    public void takeDamage(int amount) {
        health -= amount;

        if (health < 0) {
            health = 0;
        }

        // Temp debug
        System.out.println("Player took " + amount + " damage. HP: " + health + "/" + maxHealth);
    }

    public void heal(int amount) {
        health += amount;

        if (health > maxHealth) {
            health = maxHealth;
        }

        // временный debug
        System.out.println("Player healed " + amount + ". HP: " + health + "/" + maxHealth);
    }

    public boolean isDead() {
        return health <= 0;
    }


    // render
    public void render() {
        GameApp.drawTexture("player", x, y, SPRITE_SIZE, SPRITE_SIZE);
    }
}

