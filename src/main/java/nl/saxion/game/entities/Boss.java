package nl.saxion.game.entities;

/**
 * MiniBoss entity - spawns at the end of each round (every 60 seconds)
 * Has hit animation when damaged for better visual feedback
 * Uses dedicated Boss sprite sheets (Boss_Idle, Boss_run, Boss_Hit)
 */
public class Boss {

    private float x;
    private float y;

    private int health;
    private int maxHealth;

    private boolean isAlive;
    private boolean isDying;

    private float speed;
    private float size;

    public static final float SPRITE_SIZE = 48f; // Larger than zombie (36f)

    // Hit animation system
    private boolean isHit = false;
    private float hitTimer = 0f;
    private static final float HIT_DURATION = 0.15f; // Duration of hit animation
    
    // Death animation tracking
    private float deathAnimTimer = 0f;
    private static final float DEATH_ANIM_DURATION = 0.8f; // Duration before removal
    
    // Knockback system (less than regular zombies - boss is tanky)
    private float knockbackX = 0f;
    private float knockbackY = 0f;
    private static final float KNOCKBACK_STRENGTH = 30f;  // Less than regular zombies (80)
    private static final float KNOCKBACK_DECAY = 10f;     // Faster decay for boss

    public Boss(float startX, float startY, int hp) {
        x = startX;
        y = startY;

        maxHealth = hp;
        health = hp;

        isAlive = true;
        isDying = false;

        size = 48f;
        speed = 45f; // Slightly slower but tankier
        
        // Use Boss animations
        this.currentAnimation = "boss_run";
    }
    
    private boolean facingRight = true;

    public boolean isFacingRight() {
        return facingRight;
    }

    public void update(float delta, float playerX, float playerY) {
        // Handle death state
        if (!isAlive || isDying) {
            state = BossState.DEATH;
            currentAnimation = "boss_death";
            deathAnimTimer += delta;
            return;
        }

        // Handle hit animation timer
        if (isHit) {
            hitTimer -= delta;
            if (hitTimer <= 0f) {
                isHit = false;
                hitTimer = 0f;
            }
        }

        float dx = playerX - x;
        float dy = playerY - y;

        facingRight = dx >= 0;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Attack cooldown
        attackCooldown -= delta;
        if (attackCooldown < 0f) {
            attackCooldown = 0f;
        }

        // If currently in hit state, show hit animation but still allow movement
        if (isHit) {
            state = BossState.HIT;
            currentAnimation = "boss_hit";
            
            // Still move during hit, but slower
            if (distance > 0.001f) {
                float nx = dx / distance;
                float ny = dy / distance;
                x += nx * speed * 0.5f * delta; // Half speed when hit
                y += ny * speed * 0.5f * delta;
            }
            return;
        }

        // If close enough -> ATTACK
        if (distance < 70f) {
            state = BossState.ATTACK;
            currentAnimation = "boss_run"; // Keep run animation, attack is handled by collision

            if (attackCooldown == 0f) {
                attackCooldown = 0.6f;
            }
            return;
        }

        // Otherwise -> RUN
        state = BossState.RUN;
        currentAnimation = "boss_run";

        // Apply knockback first
        if (knockbackX != 0 || knockbackY != 0) {
            x += knockbackX * delta;
            y += knockbackY * delta;
            
            // Decay knockback faster for boss
            knockbackX *= (1f - KNOCKBACK_DECAY * delta);
            knockbackY *= (1f - KNOCKBACK_DECAY * delta);
            
            if (Math.abs(knockbackX) < 1f) knockbackX = 0f;
            if (Math.abs(knockbackY) < 1f) knockbackY = 0f;
        }
        
        // Movement toward player
        if (distance > 0.001f) {
            float nx = dx / distance;
            float ny = dy / distance;

            x += nx * speed * delta;
            y += ny * speed * delta;
        }
    }

    public void takeDamage(int damage) {
        if (!isAlive) {
            return;
        }

        health -= damage;
        
        // Trigger hit animation
        isHit = true;
        hitTimer = HIT_DURATION;

        if (health <= 0) {
            health = 0;
            isAlive = false;
            isDying = true;
            deathAnimTimer = 0f;
        }
    }
    
    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getSize() { return size; }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public boolean isAlive() { return isAlive; }
    public boolean isDying() { return isDying; }
    
    /**
     * Check if death animation has finished
     */
    public boolean isDeathAnimationFinished() {
        return isDying && deathAnimTimer >= DEATH_ANIM_DURATION;
    }
    
    /**
     * Check if boss is currently in hit state
     */
    public boolean isHit() {
        return isHit;
    }

    public enum BossState {
        IDLE,
        RUN,
        ATTACK,
        HIT,
        DEATH
    }

    private BossState state = BossState.RUN;
    private String currentAnimation = "boss_run";
    private float attackCooldown = 0f;

    private boolean rewardsGiven = false;

    public boolean isRewardsGiven() {
        return rewardsGiven;
    }

    public void setRewardsGiven(boolean value) {
        rewardsGiven = value;
    }
    
    /**
     * Apply knockback when hit by bullet (less than regular zombies)
     * @param bulletDirX normalized direction X of bullet
     * @param bulletDirY normalized direction Y of bullet
     * @param strength knockback strength multiplier
     */
    public void applyKnockback(float bulletDirX, float bulletDirY, float strength) {
        knockbackX = bulletDirX * KNOCKBACK_STRENGTH * strength;
        knockbackY = bulletDirY * KNOCKBACK_STRENGTH * strength;
    }
    
    /**
     * Apply knockback with default strength
     */
    public void applyKnockback(float bulletDirX, float bulletDirY) {
        applyKnockback(bulletDirX, bulletDirY, 1.0f);
    }
}
