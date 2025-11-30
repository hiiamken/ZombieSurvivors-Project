package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class PlayerStatus {
    public final int health;
    public final int maxHealth;
    public final int score;

    public PlayerStatus(int health, int maxHealth, int score) {
        if (maxHealth <= 0) {
            this.maxHealth = 1; // Default to 1 to prevent division by zero
        } else {
            this.maxHealth = maxHealth;
        }
        this.health = (int) GameApp.clamp(health, 0, this.maxHealth);
        this.score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    public float getHealthPercentage() {
        if (maxHealth <= 0) {
            return 0.0f; // Prevent division by zero
        }
        return (float) health / maxHealth;
    }
}
