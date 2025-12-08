package nl.saxion.game.entities;


import nl.saxion.gameapp.GameApp;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.utils.CollisionChecker;
import java.awt.Image;
import java.awt.Rectangle;

public class Player {
    // Position - WORLD COORDINATES
    private float worldX;
    private float worldY;

    // Speed
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Sprite
    private Image sprite;

    public static final int SPRITE_SIZE = 24;
    public static final int HITBOX_WIDTH = 12;
    public static final int HITBOX_HEIGHT = 16;

    private float targetShootDirX = 0f; //Target shoot direction X
    private float targetShootDirY = -1f; //Target shoot direction Y
    private float smoothShootDirX = 0f; //Smooth shoot direction X
    private float smoothShootDirY = -1f; //Smooth shoot direction X

    static final float SHOOT_DIRECTION_SMOOTHING = 12f;

    // HitBox (world coordinates)
    private Rectangle hitbox;

    public Player(float startWorldX, float startWorldY, float speed, int maxHealth, Image sprite) {
        this.worldX = startWorldX;
        this.worldY = startWorldY;
        this.speed = speed;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.sprite = sprite;

        // Hitbox nhỏ hơn sprite, centered in sprite (world coordinates)
        int offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        hitbox = new Rectangle((int) (worldX + offsetX), (int) (worldY + offsetY), HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    // movement
    public void update(float delta, InputController input, int worldWidth, int worldHeight, CollisionChecker collisionChecker) {

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
        if (dx != 0 && collisionChecker != null) {
            float newWorldX = worldX + dx;
            float hitboxWorldX = newWorldX + offsetX;
            float hitboxWorldY = worldY + offsetY;
            boolean collX = collisionChecker.checkCollision(hitboxWorldX, hitboxWorldY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT);
            if (!collX) {
                worldX = newWorldX;
            } else {
                float stepSize = Math.abs(dx) / 4f;
                float stepX = (dx > 0) ? stepSize : -stepSize;
                float testX = worldX;
                for (int i = 0; i < 4; i++) {
                    float testWorldX = testX + stepX;
                    float testHitboxX = testWorldX + offsetX;
                    if (!collisionChecker.checkCollision(testHitboxX, worldY + offsetY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT)) {
                        testX = testWorldX;
                    } else {
                        break;
                    }
                }
                worldX = testX;
            }
        } else if (dx != 0) {
            worldX = worldX + dx;
        }
        if (dy != 0 && collisionChecker != null) {
            float newWorldY = worldY + dy;
            float hitboxWorldX = worldX + offsetX;
            float hitboxWorldY = newWorldY + offsetY;
            boolean collY = collisionChecker.checkCollision(hitboxWorldX, hitboxWorldY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT);
            if (!collY) {
                worldY = newWorldY;
            } else {

                float stepSize = Math.abs(dy) / 4f;
                float stepY = (dy > 0) ? stepSize : -stepSize;
                float testY = worldY;
                for (int i = 0; i < 4; i++) {
                    float testWorldY = testY + stepY;
                    float testHitboxY = testWorldY + offsetY;

                    if (!collisionChecker.checkCollision(worldX + offsetX, testHitboxY, (float)HITBOX_WIDTH, (float)HITBOX_HEIGHT)) {
                        testY = testWorldY;
                    } else {
                        break;
                    }
                }
                worldY = testY;
            }
        } else if (dy != 0) {
            worldY = worldY + dy;
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

        if (worldWidth < Integer.MAX_VALUE / 2 && worldHeight < Integer.MAX_VALUE / 2) {
            float maxX = worldWidth - SPRITE_SIZE;
            float maxY = worldHeight - SPRITE_SIZE;
            worldX = GameApp.clamp(worldX, 0, maxX);
            worldY = GameApp.clamp(worldY, 0, maxY);
        }

        // Update hitbox position (centered in sprite, world coordinates)
        hitbox.x = (int) (worldX + offsetX);
        hitbox.y = (int) (worldY + offsetY);
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
        // Update hitbox position (world coordinates)
        int offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        hitbox.x = (int) (worldX + offsetX);
        hitbox.y = (int) (worldY + offsetY);
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
        GameApp.drawTexture("player", worldX, worldY, SPRITE_SIZE, SPRITE_SIZE);
    }
}
