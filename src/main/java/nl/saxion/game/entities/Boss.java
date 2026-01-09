package nl.saxion.game.entities;

public class Boss {

    private float x;
    private float y;

    private int health;
    private int maxHealth;

    private boolean isAlive;
    private boolean isDying;

    private float speed;
    private float size;

    public static final float SPRITE_SIZE = 48f;


    public Boss(float startX, float startY, int hp) {
        x = startX;
        y = startY;

        maxHealth = hp;
        health = hp;

        isAlive = true;
        isDying = false;

        size = 48f;
        speed = 55f;
    }
    private boolean facingRight = true;

    public boolean isFacingRight() {
        return facingRight;
    }



    public void update(float delta, float playerX, float playerY) {
        if (!isAlive) {
            state = BossState.DEATH;
            currentAnimation = "boss_death";
            return;
        }

        float dx = playerX - x;
        float dy = playerY - y;

        facingRight = dx >= 0;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // cooldown
        attackCooldown -= delta;
        if (attackCooldown < 0f) {
            attackCooldown = 0f;
        }

        // State switching
        if (isDying) {
            state = BossState.DEATH;
            currentAnimation = "boss_death";
            return;
        }

        // If close enough -> ATTACK
        if (distance < 70f) {
            state = BossState.ATTACK;
            currentAnimation = "boss_attack";

            // (опционально) можно запускать кулдаун, чтобы "атака" была не всегда
            if (attackCooldown == 0f) {
                attackCooldown = 0.6f;
            }

            // В атаке можем не двигаться или двигаться медленно — пока просто не двигаемся
            return;
        }

        // Otherwise -> RUN
        state = BossState.RUN;
        currentAnimation = "boss_run";

        // movement toward player
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
        if (health <= 0) {
            health = 0;
            isAlive = false;
            isDying = true;
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

    public enum BossState {
        IDLE,
        RUN,
        ATTACK,
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


}
