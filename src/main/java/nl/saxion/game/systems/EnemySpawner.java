package nl.saxion.game.systems;

import nl.saxion.game.entities.Enemy;
import nl.saxion.gameapp.GameApp;

import java.util.List;

// Handles enemy spawning with difficulty scaling
// Balanced for 10-minute survival - lots of zombies but manageable
public class EnemySpawner {
    private static final int MAX_ENEMIES = 200; // More zombies for exciting gameplay

    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 1.0f; // Fast spawns from start
    private float enemyBaseSpeed = 28f; // Slower base speed for balance
    private int enemyBaseHealth = 8; // Low health = easy to kill but lots of them

    public void update(float delta, float gameTime, float playerWorldX, float playerWorldY, float playerMoveDirX, float playerMoveDirY, List<Enemy> enemies) {
        // Max enemy limit
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // Spawn interval - fast spawns, gets faster over time
        // Starts at 1.0s, decreases to 0.3s over 10 minutes
        enemySpawnInterval = 1.0f - (gameTime * 0.00117f); // reaches 0.3 at 600s
        enemySpawnInterval = GameApp.clamp(enemySpawnInterval, 0.3f, 1.0f);

        // Spawn timer (count up)
        enemySpawnTimer += delta;
        if (enemySpawnTimer < enemySpawnInterval) {
            return;
        }

        // Time to spawn
        enemySpawnTimer = 0f;

        // Spawn multiple enemies at once - many zombies like Vampire Survivors!
        int enemiesToSpawn = 2; // Start with 2 zombies
        if (gameTime > 30f) enemiesToSpawn = 3;   // After 30 seconds
        if (gameTime > 90f) enemiesToSpawn = 4;   // After 1.5 minutes
        if (gameTime > 180f) enemiesToSpawn = 5;  // After 3 minutes
        if (gameTime > 300f) enemiesToSpawn = 6;  // After 5 minutes
        if (gameTime > 420f) enemiesToSpawn = 7;  // After 7 minutes
        if (gameTime > 540f) enemiesToSpawn = 8;  // After 9 minutes (final push!)

        for (int i = 0; i < enemiesToSpawn; i++) {
            // Check if we've reached max enemies
            if (enemies.size() >= MAX_ENEMIES) {
                break;
            }

            // Spawn enemy at world coordinates, relative to player position
            // Spawn at screen edge (outside visible area) to create feeling of zombies entering
            // With world size 960x540, half = 480x270, so 550-600 ensures spawn outside view
            float spawnDistanceMin = 520f; // Minimum distance - just outside screen edge
            float spawnDistanceMax = 600f; // Maximum distance - slightly beyond edge
            float spreadRange = 250f; // Spread range for multiple enemies

            float spawnX;
            float spawnY;

            // Determine spawn direction based on player movement
            // If player is moving, spawn behind them (opposite direction)
            // If player is not moving, spawn randomly from all edges
            float moveLength = (float) Math.sqrt(playerMoveDirX * playerMoveDirX + playerMoveDirY * playerMoveDirY);
            
            if (moveLength > 0.1f) {
                // Player is moving - spawn behind them (from the direction they came from)
                // Normalize direction
                float normalizedDirX = playerMoveDirX / moveLength;
                float normalizedDirY = playerMoveDirY / moveLength;
                
                // Spawn behind player (opposite direction)
                float behindDirX = -normalizedDirX;
                float behindDirY = -normalizedDirY;
                
                // Random distance at screen edge
                float spawnDistance = GameApp.random(spawnDistanceMin, spawnDistanceMax);
                
                // Base spawn position behind player at screen edge
                float baseSpawnX = playerWorldX + behindDirX * spawnDistance;
                float baseSpawnY = playerWorldY + behindDirY * spawnDistance;
                
                // Add perpendicular spread for multiple enemies
                // Perpendicular vector: rotate 90 degrees
                float perpX = -behindDirY;
                float perpY = behindDirX;
                
                // Random offset along perpendicular direction
                float spreadOffset = GameApp.random(-spreadRange, spreadRange);
                spawnX = baseSpawnX + perpX * spreadOffset;
                spawnY = baseSpawnY + perpY * spreadOffset;
            } else {
                // Player is not moving - spawn randomly from screen edges
                float angle = GameApp.random(0f, (float)(Math.PI * 2));
                float randomDistance = GameApp.random(spawnDistanceMin, spawnDistanceMax);
                spawnX = playerWorldX + (float)(Math.cos(angle) * randomDistance);
                spawnY = playerWorldY + (float)(Math.sin(angle) * randomDistance);
            }

            // Difficulty scaling - health grows slowly, speed grows moderately
            // Health: 1.0x → 2.0x over 10 minutes (zombies stay killable)
            float healthMultiplier = 1f + (gameTime * 0.00167f); // 2.0x at 600s
            healthMultiplier = GameApp.clamp(healthMultiplier, 1f, 2.0f);

            // Speed: 1.0x → 1.5x over 10 minutes (manageable)
            float speedMultiplier = 1f + (gameTime * 0.00083f); // 1.5x at 600s
            speedMultiplier = GameApp.clamp(speedMultiplier, 1f, 1.5f);

            float currentSpeed = enemyBaseSpeed * speedMultiplier;
            int currentHealth = (int) (enemyBaseHealth * healthMultiplier);

            // Spawn enemy
            enemies.add(new Enemy(spawnX, spawnY, currentSpeed, currentHealth));
        }
    }

    public void reset() {
        enemySpawnInterval = 1.0f;
        enemySpawnTimer = 0f;
    }

    public float getEnemyBaseSpeed() {
        return enemyBaseSpeed;
    }

    public int getEnemyBaseHealth() {
        return enemyBaseHealth;
    }
}

