package nl.saxion.game.core;

import nl.saxion.gameapp.GameApp;

public class PlayerStatus {

    // =====================
    // HEALTH
    // =====================
    public final int health;
    public final int maxHealth;

    // =====================
    // SCORE
    // =====================
    public final int score;

    // =====================
    // KILL COUNT
    // =====================
    public final int killCount;

    // =====================
    // XP / LEVEL
    // =====================
    public final int level;
    public final int currentXP;
    public final int xpToNext;

    // 🔒 OLD constructor — DO NOT REMOVE
    public PlayerStatus(int health, int maxHealth, int score) {
        this(
                health,
                maxHealth,
                score,
                0,
                1,
                0,
                100
        );
    }

    // ✅ Constructor with killCount (used by XP system)
    public PlayerStatus(
            int health,
            int maxHealth,
            int score,
            int killCount,
            int level,
            int currentXP,
            int xpToNext
    ) {
        this.maxHealth = Math.max(1, maxHealth);
        this.health = (int) GameApp.clamp(health, 0, this.maxHealth);
        this.score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
        this.killCount = Math.max(0, killCount);

        this.level = Math.max(1, level);
        this.currentXP = Math.max(0, currentXP);
        this.xpToNext = Math.max(1, xpToNext);
    }

    public float getHealthPercentage() {
        return (float) health / maxHealth;
    }

    public float getXPPercentage() {
        return (float) currentXP / xpToNext;
    }
}
