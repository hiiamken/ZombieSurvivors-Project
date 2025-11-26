package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class Bullet {

    private float x, y;      // position
    private float vx, vy;    // direction (normalized)
    private float speed = 400f; // units per second

    public Bullet(float startX, float startY, float dirX, float dirY) {
        this.x = startX;
        this.y = startY;

        // normalize direction so diagonal isn't faster
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

        return x < 0 || x > w || y < 0 || y > h;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
