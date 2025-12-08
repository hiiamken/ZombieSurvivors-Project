package nl.saxion.game.entities;


import nl.saxion.gameapp.GameApp;
import nl.saxion.game.systems.InputController;
import java.awt.Image;
import java.awt.Rectangle;

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

    public static final int SPRITE_SIZE = 24;
    public static final int HITBOX_SIZE = 16;

    private float targetShootDirX = 0f; //Target shoot direction X
    private float targetShootDirY = -1f; //Target shoot direction Y
    private float smoothShootDirX = 0f; //Smooth shoot direction X
    private float smoothShootDirY = -1f; //Smooth shoot direction X

    static final float SHOOT_DIRECTION_SMOOTHING = 12f;

    // HitBox

    private Rectangle hitbox;

    public Player(float startX, float startY, float speed, int maxHealth, Image sprite) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.sprite = sprite;

        // Hitbox centered in sprite, smaller than sprite for fair collision
        float hitboxOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2f;
        hitbox = new Rectangle((int) (x + hitboxOffset), (int) (y + hitboxOffset), HITBOX_SIZE, HITBOX_SIZE);
    }

    // movement
    public void update(float delta, InputController input, int worldWidth, int worldHeight) {

        float dirX = 0f;
        float dirY = 0f;


        if (input.isMoveUp()) {
            dirY += 1f;
            y = y + speed * delta; // W
        }
        if (input.isMoveDown()) {
            dirY -= 1f;
            y = y - speed * delta;  // S
        }
        if (input.isMoveLeft()) {
            dirX -= 1f;
            x = x - speed * delta; // A
        }
        if (input.isMoveRight()) {
            dirX += 1f;
            x = x + speed * delta;  // D
        }

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


        float maxX = worldWidth - SPRITE_SIZE;
        float maxY = worldHeight - SPRITE_SIZE;

        x = GameApp.clamp(x, 0, maxX);
        y = GameApp.clamp(y, 0, maxY);

        // Update hitbox position (centered in sprite)
        float hitboxOffset = (SPRITE_SIZE - HITBOX_SIZE) / 2f;
        hitbox.x = (int) (x + hitboxOffset);
        hitbox.y = (int) (y + hitboxOffset);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
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
        health -= amount;

        health = (int) GameApp.clamp(health, 0, maxHealth);

        // Temp debug
        System.out.println("Player took " + amount + " damage. HP: " + health + "/" + maxHealth);
    }

    public void heal(int amount) {
        health += amount;

        health = (int) GameApp.clamp(health, 0, maxHealth);

        System.out.println("Player healed " + amount + ". HP: " + health + "/" + maxHealth);
    }

    public boolean isDead() {
        return health <= 0;
    }

    // HitBox getter
    public Rectangle getHitBox() {
        return  hitbox;
    }


    // render
    public void render() {
        GameApp.drawTexture("player", x, y, SPRITE_SIZE, SPRITE_SIZE);
    }
}

