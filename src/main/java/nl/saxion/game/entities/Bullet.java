package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

// Arnold - bullet with movement + damage
public class Bullet {

    private float x, y;
    private float vx, vy;
    private float speed = 400f; // units per second
    private int damage;

    public Bullet(float startX, float startY, float dirX, float dirY, int damage) {
        this.x = startX;
        this.y = startY;

        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        this.vx = dirX / len;
        this.vy = dirY / len;

        this.damage = damage;
    }

    public void update(float delta) {
        x += vx * speed * delta;
        y += vy * speed * delta;
    }

    public boolean isOffScreen() {
        float w = GameApp.getWorldWidth();
        float h = GameApp.getWorldHeight();

        return x < 0 || x > w || y < 0 || y > h;
    }

    public float getX() { return x; }
    public float getY() { return y; }

    // Used later when hitting enemies
    public int getDamage() { return damage; }
}
