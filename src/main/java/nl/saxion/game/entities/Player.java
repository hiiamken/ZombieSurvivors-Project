package nl.saxion.game.entities;

import nl.saxion.game.systems.InputController;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.gameapp.GameApp;

import java.awt.Rectangle;

public class Player {

    public static final int SPRITE_SIZE = 24;
    public static final int DAMAGE_HITBOX_WIDTH = 18;
    public static final int DAMAGE_HITBOX_HEIGHT = 20;

    private float x, y;
    private float speed;

    private int health;
    private int maxHealth;

    // XP SYSTEM
    private int level = 1;
    private int xp = 0;
    private int xpToNext = 20;

    // Level-up state
    private boolean levelUpPending = false;
    private Upgrade[] currentChoices = null;

    private Rectangle damageHitBox;

    private String currentAnimation = "player_idle";
    private boolean dying = false;
    private float deathTimer = 0f;

    private float lastMoveX = 0;
    private float lastMoveY = 1;

    public Player(float x, float y, float speed, int maxHealth, Object unused) {
        this.x = x;
        this.y = y;
        this.speed = speed;

        this.maxHealth = Math.max(1, maxHealth);
        this.health = this.maxHealth;

        damageHitBox = new Rectangle(
                (int) x,
                (int) y,
                DAMAGE_HITBOX_WIDTH,
                DAMAGE_HITBOX_HEIGHT
        );
    }

    public void update(float delta, InputController input, int w, int h, CollisionChecker checker) {
        if (dying) {
            deathTimer += delta;
            return;
        }

        // If level-up is pending, we usually PAUSE movement in PlayScreen.
        // But even if you forget to pause, player won't break.
        float dx = input.getMoveX();
        float dy = input.getMoveY();

        if (dx != 0 || dy != 0) {
            lastMoveX = dx;
            lastMoveY = dy;
        }

        x += dx * speed * delta;
        y += dy * speed * delta;

        damageHitBox.x = (int) x;
        damageHitBox.y = (int) y;

        if (dx != 0) currentAnimation = dx < 0 ? "player_run_left" : "player_run_right";
        else currentAnimation = "player_idle";
    }

    // =========================
    // XP + LEVELING
    // =========================

    public void addXP(int amount) {
        if (amount <= 0) return;

        xp += amount;

        // Queue exactly one "level up screen" at a time
        while (xp >= xpToNext) {
            xp -= xpToNext;
            level++;
            xpToNext = (int) Math.max(10, xpToNext * 1.5f);

            // If a level-up is already waiting, we just keep stacking levels silently
            // but we still want ONE upgrade screen.
            if (!levelUpPending) {
                levelUpPending = true;
                currentChoices = roll3Upgrades();
            }
        }
    }

    public boolean isLevelUpPending() {
        return levelUpPending;
    }

    public Upgrade[] getLevelUpChoices() {
        return currentChoices;
    }

    // Call this from PlayScreen when player selects 1/2/3
    public void pickUpgrade(int index, Weapon weapon) {
        if (!levelUpPending || currentChoices == null) return;
        if (index < 0 || index >= currentChoices.length) return;

        Upgrade chosen = currentChoices[index];
        applyUpgrade(chosen, weapon);

        // clear pending
        levelUpPending = false;
        currentChoices = null;
    }

    private Upgrade[] roll3Upgrades() {
        Upgrade[] all = Upgrade.values();
        Upgrade[] picked = new Upgrade[3];

        // pick 3 distinct
        int a = GameApp.randomInt(0, all.length);
        int b = GameApp.randomInt(0, all.length);
        while (b == a) b = GameApp.randomInt(0, all.length);

        int c = GameApp.randomInt(0, all.length);
        while (c == a || c == b) c = GameApp.randomInt(0, all.length);

        picked[0] = all[a];
        picked[1] = all[b];
        picked[2] = all[c];
        return picked;
    }

    private void applyUpgrade(Upgrade up, Weapon weapon) {
        switch (up) {
            case MOVE_SPEED_UP:
                speed *= 1.10f; // +10%
                break;

            case MAX_HP_UP:
                maxHealth += 1;
                health += 1; // also heal 1
                health = (int) GameApp.clamp(health, 0, maxHealth);
                break;

            case HEAL_NOW:
                health += 2;
                health = (int) GameApp.clamp(health, 0, maxHealth);
                break;

            case DAMAGE_UP:
                if (weapon != null) weapon.addDamage(2);
                break;

            case FIRE_RATE_UP:
                if (weapon != null) weapon.multiplyFireRate(1.15f); // +15%
                break;

            case BULLET_SPEED_UP:
                if (weapon != null) weapon.multiplyBulletSpeed(1.15f); // +15%
                break;
        }
    }

    public enum Upgrade {
        MOVE_SPEED_UP("Move Speed +10%"),
        MAX_HP_UP("Max HP +1"),
        HEAL_NOW("Heal +2"),
        DAMAGE_UP("Damage +2"),
        FIRE_RATE_UP("Fire Rate +15%"),
        BULLET_SPEED_UP("Bullet Speed +15%");

        public final String label;
        Upgrade(String label) { this.label = label; }
    }

    // =========================
    // DAMAGE / DEATH
    // =========================

    public void takeDamage(int dmg) {
        if (dying) return;

        health -= dmg;
        health = (int) GameApp.clamp(health, 0, maxHealth);

        if (health <= 0) {
            dying = true;
            deathTimer = 0f;
            if (GameApp.hasAnimation("player_death")) {
                GameApp.resetAnimation("player_death");
            }
        }
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isDying() {
        return dying;
    }

    public boolean isDeathAnimationFinished() {
        // Prefer real animation finish if available
        if (GameApp.hasAnimation("player_death")) {
            return GameApp.isAnimationFinished("player_death");
        }
        // Fallback timer
        return deathTimer > 1.5f;
    }

    // =========================
    // GETTERS / HELPERS
    // =========================

    public Rectangle getDamageHitBox() {
        return damageHitBox;
    }

    public String getCurrentAnimation() {
        return dying ? "player_death" : currentAnimation;
    }

    public float getX() { return x; }
    public float getY() { return y; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        damageHitBox.x = (int) x;
        damageHitBox.y = (int) y;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public int getLevel() { return level; }
    public int getXP() { return xp; }
    public int getXPToNext() { return xpToNext; }

    public float getLastMoveDirectionX() { return lastMoveX; }
    public float getLastMoveDirectionY() { return lastMoveY; }
}
