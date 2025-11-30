package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

// Bullet with movement, damage and rendering
public class Bullet {

    private float x, y;
    private float vx, vy;
    private float speed;      // units per second
    private int damage;

    // Size
    private float width;
    private float height;

    // Defaults
    private static final float DEFAULT_SPEED = 400f;
    private static final float DEFAULT_SIZE  = 8f;
    private static final String TEXTURE_KEY  = "bullet";

    // Old-style constructor (kept for compatibility if needed)
    public Bullet(float startX, float startY, float dirX, float dirY, int damage) {
        this(startX, startY, dirX, dirY, damage, DEFAULT_SPEED, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    // New constructor with speed & size
    public Bullet(float startX,
                  float startY,
                  float dirX,
                  float dirY,
                  int damage,
                  float speed,
                  float width,
                  float height) {

        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;

        // normalize direction
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        this.vx = dirX / len;
        this.vy = dirY / len;
    }

    public void update(float delta) {
        x += vx * speed * delta;
        y += vy * speed * delta;
    }

    public boolean isOffScreen() {
        float w = GameApp.getWorldWidth();
        float h = GameApp.getWorldHeight();

        return x + width < 0 || x > w || y + height < 0 || y > h;
    }

    public void render() {
        GameApp.drawTexture(TEXTURE_KEY, x, y, width, height);
    }

    public float getX() { return x; }
    public float getY() { return y; }

    public int getDamage() { return damage; }
}
