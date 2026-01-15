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
    private int waveMinimum = 15;  // Start with fewer zombies (was 80)
    private int waveMaximum = 400; // Soft cap for this wave
    
    // === SPAWN TIMERS ===
    private float enemySpawnTimer = 0f;
    private float baseSpawnInterval = 0.5f; // Slower base spawn rate for easier start (was 0.15f)
    private float currentSpawnInterval = 0.5f;
    
    // === BASE STATS (balanced for quick kills) ===
    private float enemyBaseSpeed = 35f; // Slightly faster zombies
    private int enemyBaseHealth = 25; // Balanced: dies in ~1-2 seconds with basic weapon
    
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
        // Update wave minimum/maximum based on wave number - EXPONENTIAL SCALING
        // Wave 1: min 80, max 200
        // Wave 5: min 200, max 400
        // Wave 10: min 400, max 500
        // Using exponential formula: base * (1.25 ^ wave)
        float expMultiplier = (float) Math.pow(1.25f, wave);
        waveMinimum = (int)(80 * expMultiplier);
        waveMaximum = (int)(200 * expMultiplier);
        waveMinimum = Math.min(waveMinimum, HARD_MAX_ENEMIES - 100);
        waveMaximum = Math.min(waveMaximum, HARD_MAX_ENEMIES);
        
        GameApp.log("Wave " + wave + " started! Min: " + waveMinimum + ", Max: " + waveMaximum + " (exp: " + expMultiplier + "x)");
    }
    
    private void spawnNormalEnemies(float delta, float gameTime, float playerWorldX, float playerWorldY,
                                     float playerMoveDirX, float playerMoveDirY, List<Enemy> enemies) {
        int currentEnemyCount = enemies.size();
        
        // Hard cap: At 500+ enemies, stop spawning (increased for massive hordes)
        if (currentEnemyCount >= 500) {
            return;
        }
        
        // === GRADUAL DIFFICULTY SCALING ===
        // Minutes 0-1: Very easy (few zombies)
        // Minutes 1-2: Easy (gradual increase)
        // Minutes 2+: Normal scaling with exponential growth
        
        float minutes = gameTime / 60f;
        float earlyGameMultiplier = 1.0f;
        float exponentialMultiplier = 1.0f;
        
        if (gameTime < 60f) {
            // First minute: Very slow start (20% to 50% spawn rate)
            earlyGameMultiplier = 0.2f + (gameTime / 60f) * 0.3f; // 0.2 -> 0.5
        } else if (gameTime < 120f) {
            // Second minute: Gradual ramp up (50% to 100%)
            earlyGameMultiplier = 0.5f + ((gameTime - 60f) / 60f) * 0.5f; // 0.5 -> 1.0
        } else if (gameTime < 300f) {
            // Minutes 2-5: Normal exponential growth
            float minutesPast2 = (gameTime - 120f) / 60f;
            exponentialMultiplier = (float) Math.pow(1.8f, minutesPast2);
            exponentialMultiplier = Math.min(exponentialMultiplier, 20f);
        } else {
            // After minute 5: AGGRESSIVE spawning for higher difficulty
            float minutesPast2 = (gameTime - 120f) / 60f;
            exponentialMultiplier = (float) Math.pow(2.2f, minutesPast2); // Much faster growth
            exponentialMultiplier = Math.min(exponentialMultiplier, 35f); // Higher cap
        }
        
        // Dynamic wave minimum based on time and multipliers
        int dynamicWaveMinimum = (int)(waveMinimum * earlyGameMultiplier * exponentialMultiplier);
        dynamicWaveMinimum = Math.max(dynamicWaveMinimum, 5); // At least 5 zombies
        dynamicWaveMinimum = Math.min(dynamicWaveMinimum, 350);
        
        // Calculate spawn interval based on enemy count vs minimum
        if (currentEnemyCount < dynamicWaveMinimum) {
            // Below minimum - spawn faster to fill quota
            float deficit = (float)(dynamicWaveMinimum - currentEnemyCount) / Math.max(1, dynamicWaveMinimum);
            currentSpawnInterval = baseSpawnInterval * (0.3f + (1f - deficit) * 0.5f) / earlyGameMultiplier;
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.1f); // Not too fast early game
        } else {
            // Above minimum - slower spawn rate
            currentSpawnInterval = baseSpawnInterval / earlyGameMultiplier;
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.15f);
        }
        
        // Apply exponential multiplier to spawn rate (only after minute 2)
        if (gameTime >= 120f) {
            currentSpawnInterval = currentSpawnInterval / Math.max(1f, exponentialMultiplier * 0.4f);
            currentSpawnInterval = Math.max(currentSpawnInterval, 0.05f); // Faster spawn late game
        }
        
        // Spawn timer
        enemySpawnTimer += delta;
        if (enemySpawnTimer < currentSpawnInterval) {
            return;
        }
        enemySpawnTimer = 0f;
        
        // Calculate how many to spawn - gradual increase
        int enemiesToSpawn;
        if (currentEnemyCount < dynamicWaveMinimum) {
            // Below minimum - spawn to fill quota
            int deficit = dynamicWaveMinimum - currentEnemyCount;
            if (gameTime < 60f) {
                // First minute: spawn 1-3 at a time
                enemiesToSpawn = Math.min(deficit / 3 + 1, 3);
            } else if (gameTime < 120f) {
                // Second minute: spawn 2-8 at a time
                enemiesToSpawn = Math.min(deficit / 2 + 2, 8);
            } else {
                // After minute 2: spawn more aggressively
                enemiesToSpawn = Math.min(deficit / 2 + 10, 50);
            }
        } else {
            // Normal spawning - scales with wave and time
            if (gameTime < 120f) {
                enemiesToSpawn = 1 + (int)(minutes); // 1-2 zombies at a time early
            } else {
                enemiesToSpawn = (int)((5 + currentWave * 2) * Math.max(1f, exponentialMultiplier * 0.5f));
                enemiesToSpawn = Math.min(enemiesToSpawn, 60);
            }
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
            
            // Stats scaling - EASY early game, ramps up after minute 3
            // First 3 minutes: reduced health for easy leveling
            // After minute 3: exponential health scaling kicks in
            float healthMult;
            float speedMult;
            
            if (minutes < 1f) {
                // First minute: 50% health, normal speed
                healthMult = 0.5f;
                speedMult = 1f;
            } else if (minutes < 2f) {
                // Second minute: 60% health, normal speed
                healthMult = 0.6f;
                speedMult = 1f;
            } else if (minutes < 3f) {
                // Third minute: 75% health, slight speed increase
                healthMult = 0.75f;
                speedMult = 1.05f;
            } else {
                // After minute 3: exponential scaling kicks in
                float minutesPast3 = minutes - 3f;
                healthMult = (float) Math.pow(1.1f, minutesPast3); // 10% per minute after minute 3
                healthMult = Math.min(healthMult, 6f); // Cap at 6x
                // Speed increases 5% per minute for more challenge (was 3%)
                speedMult = 1f + (minutesPast3 * 0.05f); // 5% per minute after minute 3
                speedMult = Math.min(speedMult, 1.8f); // Cap at 1.8x speed (was 1.4x)
            }
            
            float speed = enemyBaseSpeed * speedMult;
            int health = (int)(enemyBaseHealth * healthMult);
            health = Math.max(health, 10); // Minimum 10 HP
            
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
        
        // Don't spawn too many stampede zombies (higher limit after minute 5)
        int maxStampedeZombies = (gameTime >= 300f) ? 150 : 50;
        if (stampedeZombies.size() > maxStampedeZombies) {
            return;
        }
        
        // Set next stampede time
        float cooldown = GameApp.random(STAMPEDE_MIN_COOLDOWN, STAMPEDE_MAX_COOLDOWN);
        nextStampedeTime = gameTime + cooldown;
        
        // After minute 5 (300s): spawn multiple stampedes at once (2-5 hordes)
        int numHordes = 1;
        if (gameTime >= 300f) {
            numHordes = 2 + (int)(Math.random() * 4); // 2-5 hordes
            GameApp.log("=== MULTI-STAMPEDE! Spawning " + numHordes + " hordes ===");
        }
        
        for (int h = 0; h < numHordes; h++) {
            spawnStampedeHorde(playerX, playerY, gameTime);
        }
        
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
        
        // Speed: 5x faster than normal zombies - EXTREMELY FAST! (was 2.5x, now 2x faster = 5x)
        float speed = enemyBaseSpeed * 5f; // Very fast stampede zombies
        // Stampede health - lower than normal zombies (they're fast obstacles)
        float minutes = gameTime / 60f;
        float healthExpMult = (float) Math.pow(1.05f, minutes); // 5% per minute (gentle)
        int health = (int)((15 + minutes * 2f) * healthExpMult); // Base 15 HP, scales slowly
        
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
        waveMinimum = 80;
        waveMaximum = 400;
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

