package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class XPOrb {

    private float x, y;
    private final int value;

    private boolean collected = false;

    // Magnet settings
    private static final float MAGNET_RANGE = 140f;     // start pulling if within this distance
    private static final float COLLECT_RANGE = 18f;     // collect if very close
    private static final float MAGNET_SPEED = 220f;     // pull speed

    public XPOrb(float x, float y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public void update(float delta, float playerWorldX, float playerWorldY) {
        if (collected) return;

        float dist = GameApp.distance(x, y, playerWorldX, playerWorldY);

        // Collect
        if (dist <= COLLECT_RANGE) {
            collected = true;
            return;
        }

        // Magnet pull
        if (dist <= MAGNET_RANGE && dist > 0.001f) {
            float dirX = (playerWorldX - x) / dist;
            float dirY = (playerWorldY - y) / dist;

            x += dirX * MAGNET_SPEED * delta;
            y += dirY * MAGNET_SPEED * delta;
        }
    }

    public void render(float screenX, float screenY) {
        // Draw a small orb texture (make sure you loaded "xp_orb")
        GameApp.drawTexture("xp_orb", screenX - 6, screenY - 6, 12, 12);
    }

    public boolean isCollected() {
        return collected;
    }

    public int getValue() {
        return value;
    }

    // ✅ These fix your "Cannot resolve method getX/getY" error
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
