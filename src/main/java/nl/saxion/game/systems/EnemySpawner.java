package nl.saxion.game.systems;

import nl.saxion.game.entities.Enemy;
import nl.saxion.gameapp.GameApp;

import java.util.List;

// Handles enemy spawning with difficulty scaling
public class EnemySpawner {
    private static final int MAX_ENEMIES = 50;

    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 3f; // spawn every 3 seconds
    private float enemyBaseSpeed = 34f;
    private int enemyBaseHealth = 15;

    public void update(float delta, float gameTime, float playerWorldX, float playerWorldY, List<Enemy> enemies) {
        // Max enemy limit
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // Spawn timer (count up)
        enemySpawnTimer += delta;
        if (enemySpawnTimer < enemySpawnInterval) {
            return;
        }

        // Time to spawn
        enemySpawnTimer = 0f;

        // Spawn enemy at world coordinates, relative to player position
        // Spawn at a distance from player (off-screen) so enemies come from all directions
        float spawnDistance = 400f; // Distance from player to spawn enemy

        // Choose edge: 0 = top, 1 = right, 2 = bottom, 3 = left
        int edge = GameApp.randomInt(0, 4);

        float spawnX;
        float spawnY;

        // Spawn enemy ở world coordinates, offset từ player position
        if (edge == 0) {
            // TOP - spawn above player
            float offsetX = GameApp.random(-spawnDistance, spawnDistance);
            spawnX = playerWorldX + offsetX;
            spawnY = playerWorldY + spawnDistance;
        } else if (edge == 1) {
            // RIGHT - spawn to the right of player
            spawnX = playerWorldX + spawnDistance;
            float offsetY = GameApp.random(-spawnDistance, spawnDistance);
            spawnY = playerWorldY + offsetY;
        } else if (edge == 2) {
            // BOTTOM - spawn below player
            float offsetX = GameApp.random(-spawnDistance, spawnDistance);
            spawnX = playerWorldX + offsetX;
            spawnY = playerWorldY - spawnDistance;
        } else {
            // LEFT - spawn to the left of player
            spawnX = playerWorldX - spawnDistance;
            float offsetY = GameApp.random(-spawnDistance, spawnDistance);
            spawnY = playerWorldY + offsetY;
        }

        // Difficulty scaling: 1.0x → 3.0x
        float difficultyMultiplier = 1f + (gameTime * 0.01f);
        difficultyMultiplier = GameApp.clamp(difficultyMultiplier, 1f, 3f);

        float currentSpeed = enemyBaseSpeed * difficultyMultiplier;
        int currentHealth = (int) (enemyBaseHealth * difficultyMultiplier);

        // Spawn enemy
        enemies.add(new Enemy(spawnX, spawnY, currentSpeed, currentHealth));

        // Spawn interval scaling (faster spawns over time, clamped)
        if (enemySpawnInterval > 1.5f) {
            enemySpawnInterval -= delta * 0.02f;
        }
        enemySpawnInterval = GameApp.clamp(enemySpawnInterval, 0.5f, 10f);
    }

    public void reset() {
        enemySpawnInterval = 3f;
        enemySpawnTimer = 0f;
    }

    public float getEnemyBaseSpeed() {
        return enemyBaseSpeed;
    }

    public int getEnemyBaseHealth() {
        return enemyBaseHealth;
    }
}

