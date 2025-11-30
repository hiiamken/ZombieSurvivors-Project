package nl.saxion.game.entities;

public class PlayerStatus {
    public final int health;
    public final int maxHealth;
    public final int score;

    public PlayerStatus(int health, int maxHealth, int score) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.score = score;
    }
}
