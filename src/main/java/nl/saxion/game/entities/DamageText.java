package nl.saxion.game.entities;

// Damage number floating text (Vampire Survivors style)
public class DamageText {
    public int value;
    public float x;
    public float y;
    public float scale;
    public float alpha;
    public float velocityY;
    public float lifeTime;
    public float maxLifeTime;
    public boolean isCrit;
    public boolean isActive;

    // Crit shake effect
    private float shakeTimer = 0f;
    private float shakeOffsetX = 0f;

    public DamageText() {
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
        isCrit = false;
        isActive = false;
        shakeTimer = 0f;
        shakeOffsetX = 0f;
    }

    public void activate(int damage, float enemyX, float enemyY, boolean crit) {
        this.value = damage;
        // Random offset to avoid overlap
        this.x = enemyX + (float)(Math.random() * 8 - 4); // -4 to +4
        this.y = enemyY + (float)(Math.random() * 12 - 6); // -6 to +6
        this.isCrit = crit;
        this.scale = crit ? 1.4f : 1.2f;
        this.alpha = 1.0f;
        this.velocityY = 40f;
        this.lifeTime = 0f;
        this.maxLifeTime = 0.8f;
        this.isActive = true;
        this.shakeTimer = 0f;
        this.shakeOffsetX = 0f;
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

        // Crit shake effect (first 2 frames worth of time)
        if (isCrit && shakeTimer < 0.033f) { // ~2 frames at 60fps
            shakeOffsetX = (float)(Math.random() * 2 - 1); // -1 to +1
            shakeTimer += delta;
        } else {
            shakeOffsetX = 0f;
        }

        // Deactivate when lifetime expires
        if (lifeTime >= maxLifeTime) {
            isActive = false;
        }
    }

    public float getRenderX() {
        return x + shakeOffsetX;
    }

    public float getRenderY() {
        return y;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
