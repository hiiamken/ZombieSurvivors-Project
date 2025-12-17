package nl.saxion.game.entities;

// Health number floating text (green color for healing)
public class HealthText {
    public int value;
    public float x;
    public float y;
    public float scale;
    public float alpha;
    public float velocityY;
    public float lifeTime;
    public float maxLifeTime;
    public boolean isActive;

    public HealthText() {
        reset();
    }

    public void reset() {
        value = 0;
        x = 0f;
        y = 0f;
        scale = 1.0f;
        alpha = 1.0f;
        velocityY = 0f;
        lifeTime = 0f;
        maxLifeTime = 0.8f;
        isActive = false;
    }

    public void activate(int healAmount, float playerX, float playerY) {
        this.value = healAmount;
        // Random offset to avoid overlap
        this.x = playerX + (float)(Math.random() * 8 - 4); // -4 to +4
        this.y = playerY + (float)(Math.random() * 12 - 6); // -6 to +6
        this.scale = 1.2f;
        this.alpha = 1.0f;
        this.velocityY = 40f;
        this.lifeTime = 0f;
        this.maxLifeTime = 0.8f;
        this.isActive = true;
    }

    public void update(float delta) {
        if (!isActive) return;

        lifeTime += delta;

        // Move up
        y += velocityY * delta;
        // Easing: slow down over time
        velocityY *= 0.9f;

        // Scale: lerp from initial scale to 1.0
        float targetScale = 1.0f;
        scale = lerp(scale, targetScale, 0.2f);

        // Alpha: fade after 0.1s
        if (lifeTime < 0.1f) {
            alpha = 1.0f;
        } else {
            float lifeRatio = 1.0f - ((lifeTime - 0.1f) / (maxLifeTime - 0.1f));
            alpha = Math.max(0f, lifeRatio);
        }

        // Deactivate when lifetime expires
        if (lifeTime >= maxLifeTime) {
            isActive = false;
        }
    }

    public float getRenderX() {
        return x;
    }

    public float getRenderY() {
        return y;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
