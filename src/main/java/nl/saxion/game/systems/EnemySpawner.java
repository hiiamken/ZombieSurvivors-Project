package nl.saxion.game.systems;

import nl.saxion.game.entities.Enemy;
import nl.saxion.gameapp.GameApp;

import java.util.List;

// Handles enemy spawning with difficulty scaling
public class EnemySpawner {
    private static final int MAX_ENEMIES = 150; // Increased for more enemies like Vampire Survivors

    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 1.5f; // spawn every 1.5 seconds (faster spawns)
    private float enemyBaseSpeed = 34f;
    private int enemyBaseHealth = 15;

    public void update(float delta, float gameTime, float playerWorldX, float playerWorldY, List<Enemy> enemies) {
        // Max enemy limit
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // Spawn interval scaling based on gameTime (faster spawns over time)
        enemySpawnInterval = 1.5f - (gameTime * 0.03f);
        enemySpawnInterval = GameApp.clamp(enemySpawnInterval, 0.3f, 1.5f);

        // Spawn timer (count up)
        enemySpawnTimer += delta;
        if (enemySpawnTimer < enemySpawnInterval) {
            return;
        }

        // Time to spawn
        enemySpawnTimer = 0f;

        // Spawn multiple enemies at once for more intensity (like Vampire Survivors)
        int enemiesToSpawn = 1;
        if (gameTime > 30f) enemiesToSpawn = 2; // After 30 seconds, spawn 2 at a time
        if (gameTime > 60f) enemiesToSpawn = 3; // After 60 seconds, spawn 3 at a time

        for (int i = 0; i < enemiesToSpawn; i++) {
            // Check if we've reached max enemies
            if (enemies.size() >= MAX_ENEMIES) {
                break;
            }

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
        }
    }

    public void reset() {
        enemySpawnInterval = 1.5f;
        enemySpawnTimer = 0f;
    }

    public float getEnemyBaseSpeed() {
        return enemyBaseSpeed;
    }

    public int getEnemyBaseHealth() {
        return enemyBaseHealth;
    }
}

