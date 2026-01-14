package nl.saxion.game.systems;

import nl.saxion.game.entities.Enemy;
import nl.saxion.gameapp.GameApp;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Handles enemy spawning with Vampire Survivors-style wave system.
 * 
 * WAVE SYSTEM:
 * - Each wave (minute) has minimum and maximum enemy counts
 * - If below minimum, spawn rate increases dramatically
 * - If at 300+ enemies, stop spawning (only bosses can spawn)
 * - Enemies spawn at screen edge and despawn if too far from player
 * 
 * STAMPEDE SYSTEM (from minute 2+):
 * - Random chance to spawn a horde that runs straight across screen
 * - They push other zombies toward player
 * - Different directions: horizontal, vertical, diagonal
 */
public class EnemySpawner {
    // === ENEMY LIMITS (Vampire Survivors style) ===
    private static final int HARD_MAX_ENEMIES = 500; // At 500+, stop spawning normal enemies (increased for massive hordes)
    private static final int SOFT_MAX_ENEMIES = 600; // Absolute maximum with stampedes
    
    // === WAVE CONFIGURATION ===
    private int currentWave = 0; // Wave = minute number
    private float waveTimer = 0f;
    private static final float WAVE_DURATION = 60f; // 1 wave = 1 minute
    
    // Wave minimum/maximum (scales per wave)
    private int waveMinimum = 50;  // If below this, spawn faster (increased)
    private int waveMaximum = 300; // Soft cap for this wave (increased for massive hordes)
    
    // === SPAWN TIMERS ===
    private float enemySpawnTimer = 0f;
    private float baseSpawnInterval = 0.6f;
    private float currentSpawnInterval = 0.6f;
    
    // === BASE STATS ===
    private float enemyBaseSpeed = 30f; // Reduced from 35 for easier gameplay
    private int enemyBaseHealth = 12;
    
    // === STAMPEDE SYSTEM ===
    private List<StampedeZombie> stampedeZombies = new ArrayList<>();
    private float stampedeCooldown = 0f; // Cooldown between stampedes
    private static final float STAMPEDE_MIN_COOLDOWN = 8f;  // Minimum 8 seconds between stampedes
    private static final float STAMPEDE_MAX_COOLDOWN = 20f; // Maximum 20 seconds between stampedes
    private float nextStampedeTime = 0f; // When next stampede can occur
    
    // === SPAWN AREA (rectangular around player) ===
    private static final float SPAWN_DISTANCE_MIN = 450f; // Just outside screen
    private static final float SPAWN_DISTANCE_MAX = 700f; // Up to 1.5x screen distance
    private static final float DESPAWN_DISTANCE = 800f;   // Despawn if beyond this
    
    /**
     * Inner class for Stampede Zombies - they run straight across screen
     */
    public static class StampedeZombie {
        public float x, y;
        public float dirX, dirY; // Fixed direction (doesn't chase player)
        public float speed;
        public int health;
        public int maxHealth;
        public boolean isDead = false;
        public boolean isDying = false;
        public float deathTimer = 0f;
        public int zombieType;
        public String currentAnimation;
        public boolean facingRight = true;
        
        public StampedeZombie(float x, float y, float dirX, float dirY, float speed, int health) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.speed = speed;
            this.health = health;
            this.maxHealth = health;
            // Random zombie type
            int[] types = {1, 3, 4};
            this.zombieType = types[(int)(Math.random() * 3)];
            this.currentAnimation = getAnimationName("run");
            this.facingRight = dirX > 0;
        }
        
        private String getAnimationName(String action) {
            if (zombieType == 1) return "zombie_" + action;
            return "zombie" + zombieType + "_" + action;
        }
        
        public void update(float delta) {
            if (isDying) {
                deathTimer += delta;
                currentAnimation = getAnimationName("death");
                return;
            }
            
            // Move in fixed direction (doesn't chase player)
            x += dirX * speed * delta;
            y += dirY * speed * delta;
            currentAnimation = getAnimationName("run");
        }
        
        public void takeDamage(int damage) {
            health -= damage;
            if (health <= 0) {
                health = 0;
                isDead = true;
                isDying = true;
                deathTimer = 0f;
            }
        }
        
        public boolean isDeathAnimationFinished() {
            return isDying && deathTimer >= 1.0f;
        }
        
        // Check if zombie has left the screen area
        public boolean isOutOfBounds(float playerX, float playerY) {
            float dx = x - playerX;
            float dy = y - playerY;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            return dist > DESPAWN_DISTANCE;
        }
    }
    
    public void update(float delta, float gameTime, float playerWorldX, float playerWorldY, 
                       float playerMoveDirX, float playerMoveDirY, List<Enemy> enemies) {
        
        // === UPDATE WAVE ===
        int newWave = (int)(gameTime / WAVE_DURATION);
        if (newWave > currentWave) {
            currentWave = newWave;
            onNewWave(currentWave);
        }
        
        // === UPDATE STAMPEDE ZOMBIES ===
        updateStampedeZombies(delta, playerWorldX, playerWorldY);
        
        // === TRY SPAWN STAMPEDE (from minute 2+) ===
        if (gameTime >= 120f) { // After 2 minutes
            trySpawnStampede(gameTime, playerWorldX, playerWorldY);
        }
        
        // === SPAWN NORMAL ENEMIES ===
        spawnNormalEnemies(delta, gameTime, playerWorldX, playerWorldY, 
                          playerMoveDirX, playerMoveDirY, enemies);
    }
    
    private void onNewWave(int wave) {
        // Update wave minimum/maximum based on wave number
        // Wave 1: min 30, max 150
        // Wave 5: min 80, max 250
        // Wave 10: min 150, max 350
        waveMinimum = 30 + (wave * 12);
        waveMaximum = 150 + (wave * 25);
        waveMaximum = Math.min(waveMaximum, HARD_MAX_ENEMIES);
        
        GameApp.log("Wave " + wave + " started! Min: " + waveMinimum + ", Max: " + waveMaximum);
    }
    
    private void spawnNormalEnemies(float delta, float gameTime, float playerWorldX, float playerWorldY,
                                     float playerMoveDirX, float playerMoveDirY, List<Enemy> enemies) {
        int currentEnemyCount = enemies.size();
        
        // Hard cap: At 500+ enemies, stop spawning (increased for massive hordes)
        if (currentEnemyCount >= 500) {
            return;
        }
        
        // EXPONENTIAL SCALING from minute 4 onwards (240 seconds)
        // This creates MASSIVE hordes for 20k-100k kills per game
        float exponentialMultiplier = 1.0f;
        if (gameTime >= 240f) { // After minute 4
            float minutesPast4 = (gameTime - 240f) / 60f;
            // Exponential growth: 1.5^minutes past minute 4
            exponentialMultiplier = (float) Math.pow(1.8f, minutesPast4);
            exponentialMultiplier = Math.min(exponentialMultiplier, 15f); // Cap at 15x
        }
        
        // Dynamic wave minimum based on time
        int dynamicWaveMinimum = (int)(waveMinimum * exponentialMultiplier);
        dynamicWaveMinimum = Math.min(dynamicWaveMinimum, 400);
        
        // Calculate spawn interval based on enemy count vs minimum
        if (currentEnemyCount < dynamicWaveMinimum) {
            // Below minimum - spawn MUCH faster to fill quota
            float deficit = (float)(dynamicWaveMinimum - currentEnemyCount) / dynamicWaveMinimum;
            currentSpawnInterval = baseSpawnInterval * (0.1f + (1f - deficit) * 0.4f);
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.05f); // Minimum 0.05s (very fast!)
        } else {
            // Above minimum - faster spawn rate over time
            currentSpawnInterval = baseSpawnInterval - (gameTime * 0.001f);
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.1f);
        }
        
        // Apply exponential multiplier to spawn rate
        currentSpawnInterval = currentSpawnInterval / Math.max(1f, exponentialMultiplier * 0.3f);
        currentSpawnInterval = Math.max(currentSpawnInterval, 0.03f); // Absolute minimum
        
        // Spawn timer
        enemySpawnTimer += delta;
        if (enemySpawnTimer < currentSpawnInterval) {
            return;
        }
        enemySpawnTimer = 0f;
        
        // Calculate how many to spawn - MUCH more after minute 4
        int enemiesToSpawn;
        if (currentEnemyCount < dynamicWaveMinimum) {
            // Below minimum - spawn more at once
            int deficit = dynamicWaveMinimum - currentEnemyCount;
            enemiesToSpawn = Math.min(deficit / 3 + 5, 25); // Spawn 5-25 based on deficit
        } else {
            // Normal spawning based on wave + exponential
            enemiesToSpawn = (int)((3 + currentWave) * Math.max(1f, exponentialMultiplier * 0.5f));
            enemiesToSpawn = Math.min(enemiesToSpawn, 30);
        }
        
        for (int i = 0; i < enemiesToSpawn; i++) {
            if (enemies.size() >= HARD_MAX_ENEMIES) break;
            
            // From minute 8+ (480s), spawn from ALL 4 edges for overwhelming waves
            float[] spawnPos;
            if (gameTime >= 480f) {
                spawnPos = getSpawnPositionFromAllEdges(playerWorldX, playerWorldY);
            } else {
                spawnPos = getRandomSpawnPosition(playerWorldX, playerWorldY);
            }
            float spawnX = spawnPos[0];
            float spawnY = spawnPos[1];
            
            // Stats scaling
            float healthMult = 1f + (gameTime * 0.002f);
            float speedMult = 1f + (gameTime * 0.001f);
            healthMult = GameApp.clamp(healthMult, 1f, 2.5f);
            speedMult = GameApp.clamp(speedMult, 1f, 1.6f);
            
            float speed = enemyBaseSpeed * speedMult;
            int health = (int)(enemyBaseHealth * healthMult);
            
            enemies.add(new Enemy(spawnX, spawnY, speed, health));
        }
    }
    
    private float[] getRandomSpawnPosition(float playerX, float playerY) {
        // Spawn in rectangular area around player (like Vampire Survivors)
        // From just outside screen to 1.5x screen distance
        
        float spawnX, spawnY;
        int side = (int)(Math.random() * 4); // 0=top, 1=bottom, 2=left, 3=right
        
        float distance = GameApp.random(SPAWN_DISTANCE_MIN, SPAWN_DISTANCE_MAX);
        float spread = GameApp.random(-400f, 400f);
        
        switch (side) {
            case 0: // Top
                spawnX = playerX + spread;
                spawnY = playerY - distance;
                break;
            case 1: // Bottom
                spawnX = playerX + spread;
                spawnY = playerY + distance;
                break;
            case 2: // Left
                spawnX = playerX - distance;
                spawnY = playerY + spread;
                break;
            default: // Right
                spawnX = playerX + distance;
                spawnY = playerY + spread;
                break;
        }
        
        return new float[]{spawnX, spawnY};
    }
    
    /**
     * Get spawn position from ALL 4 edges simultaneously (for minute 8+ waves)
     * This creates overwhelming hordes from every direction
     */
    private float[] getSpawnPositionFromAllEdges(float playerX, float playerY) {
        float spawnX, spawnY;
        
        // Spawn from screen edges (closer than normal spawn for overwhelming effect)
        float edgeDistance = 380f + GameApp.random(0f, 100f); // Closer to screen edge
        
        // Random position along ANY edge
        int edge = (int)(Math.random() * 4);
        float edgePos = GameApp.random(-500f, 500f); // Position along the edge
        
        switch (edge) {
            case 0: // Top edge
                spawnX = playerX + edgePos;
                spawnY = playerY - edgeDistance;
                break;
            case 1: // Bottom edge
                spawnX = playerX + edgePos;
                spawnY = playerY + edgeDistance;
                break;
            case 2: // Left edge
                spawnX = playerX - edgeDistance;
                spawnY = playerY + edgePos;
                break;
            default: // Right edge
                spawnX = playerX + edgeDistance;
                spawnY = playerY + edgePos;
                break;
        }
        
        return new float[]{spawnX, spawnY};
    }
    
    // ==========================================
    // STAMPEDE SYSTEM
    // ==========================================
    
    private void updateStampedeZombies(float delta, float playerX, float playerY) {
        Iterator<StampedeZombie> it = stampedeZombies.iterator();
        while (it.hasNext()) {
            StampedeZombie sz = it.next();
            sz.update(delta);
            
            // Remove if death animation finished or out of bounds
            if (sz.isDeathAnimationFinished() || sz.isOutOfBounds(playerX, playerY)) {
                it.remove();
            }
        }
    }
    
    private void trySpawnStampede(float gameTime, float playerX, float playerY) {
        // Check cooldown
        if (gameTime < nextStampedeTime) {
            return;
        }
        
        // Random chance to spawn (higher chance as game progresses)
        float spawnChance = 0.003f + (gameTime * 0.000005f); // ~0.3% base, increases over time
        if (Math.random() > spawnChance) {
            return;
        }
        
        // Don't spawn too many stampede zombies
        if (stampedeZombies.size() > 30) {
            return;
        }
        
        // Set next stampede time
        float cooldown = GameApp.random(STAMPEDE_MIN_COOLDOWN, STAMPEDE_MAX_COOLDOWN);
        nextStampedeTime = gameTime + cooldown;
        
        // Spawn stampede horde
        spawnStampedeHorde(playerX, playerY, gameTime);
        
        GameApp.log("STAMPEDE spawned at " + gameTime + "s! Next possible at " + nextStampedeTime + "s");
    }
    
    private void spawnStampedeHorde(float playerX, float playerY, float gameTime) {
        // Horde size: 25-40 zombies - MUCH denser horde!
        int hordeSize = 25 + (int)(Math.random() * 16);
        
        // Pick random direction toward player (8 directions: N, S, E, W, NE, NW, SE, SW)
        int dirType = (int)(Math.random() * 8);
        
        float dirX, dirY;
        float startX, startY;
        
        float spawnDist = SPAWN_DISTANCE_MAX;
        float diag = 0.707f; // sqrt(2)/2 for diagonal directions
        
        switch (dirType) {
            case 0: // From West (Left to Right)
                dirX = 1; dirY = 0;
                startX = playerX - spawnDist;
                startY = playerY;
                break;
            case 1: // From East (Right to Left)
                dirX = -1; dirY = 0;
                startX = playerX + spawnDist;
                startY = playerY;
                break;
            case 2: // From North (Top to Bottom)
                dirX = 0; dirY = 1;
                startX = playerX;
                startY = playerY - spawnDist;
                break;
            case 3: // From South (Bottom to Top)
                dirX = 0; dirY = -1;
                startX = playerX;
                startY = playerY + spawnDist;
                break;
            case 4: // From NorthWest (NW to SE)
                dirX = diag; dirY = diag;
                startX = playerX - spawnDist * diag;
                startY = playerY - spawnDist * diag;
                break;
            case 5: // From NorthEast (NE to SW)
                dirX = -diag; dirY = diag;
                startX = playerX + spawnDist * diag;
                startY = playerY - spawnDist * diag;
                break;
            case 6: // From SouthWest (SW to NE)
                dirX = diag; dirY = -diag;
                startX = playerX - spawnDist * diag;
                startY = playerY + spawnDist * diag;
                break;
            default: // From SouthEast (SE to NW)
                dirX = -diag; dirY = -diag;
                startX = playerX + spawnDist * diag;
                startY = playerY + spawnDist * diag;
                break;
        }
        
        // Speed: 3x faster than normal zombies - VERY FAST!
        float speed = enemyBaseSpeed * 3.0f; // Triple speed - extremely fast!
        int health = (int)(6 + gameTime * 0.01f); // Lower health than normal
        
        // Spawn horde in TIGHT ellipse formation - compact and dense
        // Smaller ellipse = tighter pack of zombies rushing together
        float ellipseWidth = 100f;  // Perpendicular spread (tighter)
        float ellipseDepth = 50f;   // Along movement direction (very narrow)
        
        for (int i = 0; i < hordeSize; i++) {
            // Use ellipse distribution - angle around ellipse
            float angle = (float)(Math.random() * Math.PI * 2);
            float radiusRatio = (float)Math.sqrt(Math.random()); // Square root for uniform distribution
            
            // Calculate ellipse offsets (perpendicular and along movement)
            float ellipseX = (float)(Math.cos(angle) * ellipseWidth * radiusRatio);
            float ellipseY = (float)(Math.sin(angle) * ellipseDepth * radiusRatio);
            
            // Calculate perpendicular direction to movement
            float perpX = -dirY;
            float perpY = dirX;
            
            // Position in world: start + perpendicular offset + depth offset along direction
            float spawnX = startX + perpX * ellipseX + dirX * ellipseY;
            float spawnY = startY + perpY * ellipseX + dirY * ellipseY;
            
            StampedeZombie sz = new StampedeZombie(spawnX, spawnY, dirX, dirY, speed, health);
            stampedeZombies.add(sz);
        }
    }
    
    // ==========================================
    // GETTERS FOR PLAYSCREEN
    // ==========================================
    
    public List<StampedeZombie> getStampedeZombies() {
        return stampedeZombies;
    }
    
    public void reset() {
        enemySpawnTimer = 0f;
        currentSpawnInterval = baseSpawnInterval;
        currentWave = 0;
        waveMinimum = 30;
        waveMaximum = 150;
        stampedeZombies.clear();
        nextStampedeTime = 0f;
    }

    public float getEnemyBaseSpeed() {
        return enemyBaseSpeed;
    }

    public int getEnemyBaseHealth() {
        return enemyBaseHealth;
    }
}

