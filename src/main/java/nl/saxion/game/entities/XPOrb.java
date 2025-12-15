package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class XPOrb {

    private float x, y;
    private int xpValue;
    private float lifetime = 10f;
    private boolean collected = false;

    private float magnetRange = 100f;
    private float magnetSpeed = 150f;
    private float pulseTime = 0f;

    public XPOrb(float x, float y, int xpValue) {
        this.x = x;
        this.y = y;
        this.xpValue = xpValue;
    }

    public void update(float delta, float playerX, float playerY) {
        lifetime -= delta;
        pulseTime += delta;

        if (collected) return;

        float dist = GameApp.distance(x, y, playerX, playerY);

        if (dist < magnetRange && dist > 0) {
            float dx = (playerX - x) / dist;
            float dy = (playerY - y) / dist;
            x += dx * magnetSpeed * delta;
            y += dy * magnetSpeed * delta;
        }

        if (dist < 20f) {
            collected = true;
        }
    }

    public void render(float playerWorldX, float playerWorldY) {
        float screenX = GameApp.getWorldWidth() / 2f + (x - playerWorldX);
        float screenY = GameApp.getWorldHeight() / 2f + (y - playerWorldY);

        float scale = 1f + 0.15f * (float)Math.sin(pulseTime * 5f);
        float size = 8f * scale;

        // Draw animation from sprite sheet (row 9, cols 19-22)
        if (GameApp.hasAnimation("orb_animation")) {
            GameApp.drawAnimation("orb_animation", screenX - size/2, screenY - size/2, size, size);
        } else {
            // Fallback to circle if animation not loaded
            GameApp.setColor(0, 200, 255, 255);
            GameApp.drawCircle(screenX, screenY, size/2);
        }
    }

    public boolean isCollected() { return collected; }
    public boolean isExpired() { return lifetime <= 0f; }
    public int getXPValue() { return xpValue; }
}
