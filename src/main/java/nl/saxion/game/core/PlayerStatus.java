package nl.saxion.game.core;

import nl.saxion.gameapp.GameApp;

public class PlayerStatus {

    public final int health;
    public final int maxHealth;
    public final int score;

    public final int level;
    public final int xp;
    public final int xpToNext;

    public PlayerStatus(
            int health,
            int maxHealth,
            int score,
            int level,
            int xp,
            int xpToNext
    ) {
        this.maxHealth = Math.max(1, maxHealth);
        this.health = (int) GameApp.clamp(health, 0, this.maxHealth);
        this.score = Math.max(0, score);

        this.level = level;
        this.xp = xp;
        this.xpToNext = Math.max(1, xpToNext);
    }

    public float getHealthPercentage() {
        return (float) health / maxHealth;
    }

    public float getXPPercentage() {
        return (float) xp / xpToNext;
    }
}
