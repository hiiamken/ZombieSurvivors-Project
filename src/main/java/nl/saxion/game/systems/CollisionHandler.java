package nl.saxion.game.systems;

import nl.saxion.game.entities.*;
import nl.saxion.game.entities.WeaponUpgrade;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.gameapp.GameApp;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

// Handles all collision detection and cleanup
public class CollisionHandler {
    private static final float DAMAGE_COOLDOWN_DURATION = 0.6f;
    private static final float ENEMY_BASE_DAMAGE = 0.5f;  // Base damage for enemies (reduced - fast zombies should deal less)
    private static final int BOSS_BASE_DAMAGE = 3;   // Base damage for bosses (increased)

    private float playerDamageCooldown = 0f;
    
    // Game time for exponential damage scaling
    private float gameElapsedTime = 0f;
    
    /**
     * Set the elapsed game time for damage scaling calculations.
     * @param elapsedTime Time in seconds since game started
     */
    public void setGameElapsedTime(float elapsedTime) {
        this.gameElapsedTime = elapsedTime;
    }
    
    /**
     * Calculate enemy touch damage with exponential scaling.
     * Damage scales: base * (1.1 ^ minutes) - 10% increase per minute
     */
    private int getScaledEnemyDamage() {
        float minutes = gameElapsedTime / 60f;
        float damageMultiplier = (float) Math.pow(1.08f, minutes); // 8% per minute (reduced from 10%)
        damageMultiplier = Math.min(damageMultiplier, 6f); // Cap at 6x damage (reduced from 10x)
        float scaledDamage = ENEMY_BASE_DAMAGE * damageMultiplier;
        return Math.max(1, Math.round(scaledDamage));
    }
    
    /**
     * Calculate boss touch damage with exponential scaling.
     * Damage scales: base * (1.15 ^ minutes) - 15% increase per minute (boss is more dangerous)
     */
    private int getScaledBossDamage() {
        float minutes = gameElapsedTime / 60f;
        float damageMultiplier = (float) Math.pow(1.15f, minutes);
        damageMultiplier = Math.min(damageMultiplier, 15f); // Cap at 15x damage
        return Math.max(1, (int)(BOSS_BASE_DAMAGE * damageMultiplier));
    }

    // Damage text system for spawning damage numbers
    private DamageTextSystem damageTextSystem;
    
    // Sound manager for playing damage sound
    private SoundManager soundManager;
    
    // Player reference for lifesteal
    private Player playerRef;

    public void update(float delta) {
        // Player Damage cooldown
        playerDamageCooldown -= delta;
        if (playerDamageCooldown < 0f) {
            playerDamageCooldown = 0f;
        }
    }

    public void handleBulletEnemyCollisions(List<Bullet> bullets, List<Enemy> enemies, java.util.function.Consumer<Integer> onEnemyKilled, java.util.function.Consumer<Enemy> onEnemyKilledForOrbs, CollisionChecker wallCollisionChecker) {
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            // Check wall collision first - bullet can't hit enemy through wall
            if (wallCollisionChecker != null && wallCollisionChecker.checkCollision(bX, bY, bW, bH)) {
                b.destroy();
                continue;
            }

            for (Enemy e : enemies) {
                // Skip enemies that are dead, dying, or not active (soft despawn)
                if (e.isDead() || e.isDying() || !e.isActive()) {
                    continue;
                }
                
                // Skip enemies already hit by this bullet (for pierce system)
                int enemyId = System.identityHashCode(e);
                if (b.hasHitEnemy(enemyId)) {
                    continue;
                }

                // Use DAMAGE hitbox for bullet collision
                Rectangle enemyDamageHitbox = e.getDamageHitBox();
                float eX = enemyDamageHitbox.x;
                float eY = enemyDamageHitbox.y;
                float eW = enemyDamageHitbox.width;
                float eH = enemyDamageHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    // Critical hit: 25% chance - deals 1.5x damage and shows yellow text
                    boolean isCrit = GameApp.random(0f, 1f) < 0.25f;
                    int baseDamage = b.getDamage();
                    int damage = isCrit ? (int)(baseDamage * 1.5f) : baseDamage;
                    
                    e.takeDamage(damage);
                    
                    // Apply knockback (like Vampire Survivors)
                    float bulletDirX = b.getDirX();
                    float bulletDirY = b.getDirY();
                    e.applyKnockback(bulletDirX, bulletDirY);
                    
                    // Lifesteal for evolved bullets (10% of damage dealt)
                    if (b.isEvolved() && playerRef != null) {
                        float lifestealPercent = WeaponUpgrade.getEvolvedLifestealPercent();
                        int healAmount = (int)(damage * lifestealPercent);
                        if (healAmount > 0) {
                            playerRef.heal(healAmount);
                        }
                    }

                    // Spawn damage text at enemy position
                    if (damageTextSystem != null) {
                        // Use enemy center position for damage text spawn
                        float enemyCenterX = eX + eW / 2f;
                        float enemyCenterY = eY + eH / 2f;
                        // Crit shows yellow text, normal shows orange/red
                        damageTextSystem.spawnDamageText(enemyCenterX, enemyCenterY, damage, isCrit);
                    }

                    if (e.isDead()) {
                        int randomScore = (int) GameApp.random(5, 15); // Random score 5-15 per zombie
                        onEnemyKilled.accept(randomScore);
                        if (onEnemyKilledForOrbs != null) {
                            onEnemyKilledForOrbs.accept(e); // Spawn orbs at enemy position
                        }
                    }

                    // Pierce system: check if bullet should continue or be destroyed
                    boolean shouldContinue = b.onHitEnemy(enemyId);
                    if (!shouldContinue) {
                        b.destroy();
                        break; // Bullet destroyed, stop checking enemies
                    }
                    // If bullet pierces, continue checking other enemies (don't break)
                }
            }
        }
    }

    public void handleBulletBossCollisions(
            List<Bullet> bullets,
            List<Boss> bosses,
            java.util.function.Consumer<Integer> onBossKilled,
            java.util.function.Consumer<Boss> onBossKilledForOrbs,
            CollisionChecker wallCollisionChecker
    ) {
        if (bosses == null) {
            return;
        }

        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            // Wall collision first (bullet can't hit through wall)
            if (wallCollisionChecker != null && wallCollisionChecker.checkCollision(bX, bY, bW, bH)) {
                b.destroy();
                continue;
            }

            java.util.Iterator<Boss> bossIt = bosses.iterator();
            while (bossIt.hasNext()) {
                Boss boss = bossIt.next();


                float bx = boss.getX();
                float by = boss.getY();
                float bw = Boss.SPRITE_SIZE;
                float bh = Boss.SPRITE_SIZE;

                if (GameApp.rectOverlap(bX, bY, bW, bH, bx, by, bw, bh)) {
                    int damage = b.getDamage();
                    boss.takeDamage(damage);
                    
                    // Apply knockback (less than regular zombies - boss is tanky)
                    float bulletDirX = b.getDirX();
                    float bulletDirY = b.getDirY();
                    boss.applyKnockback(bulletDirX, bulletDirY);
                    
                    b.destroy();

                    // Damage text
                    if (damageTextSystem != null) {
                        float centerX = bx + bw / 2f;
                        float centerY = by + bh / 2f;
                        // Critical hit: 25% chance - shows yellow text
                        boolean isCrit = GameApp.random(0f, 1f) < 0.25f;
                        damageTextSystem.spawnDamageText(centerX, centerY, damage, isCrit);
                    }

                    // Boss killed
                    if (!boss.isAlive()) {
                        if (onBossKilled != null) {
                            int randomBossScore = (int) GameApp.random(150, 250); // Random score 150-250 per boss
                            onBossKilled.accept(randomBossScore);
                        }
                        if (onBossKilledForOrbs != null) {
                            onBossKilledForOrbs.accept(boss);
                        }
                    }


                    break;
                }
            }
        }
    }


    public void handleEnemyPlayerCollisions(Player player, List<Enemy> enemies) {
        // Use damage hitbox instead of wall hitbox for player-enemy interaction
        Rectangle playerDamageHitbox = player.getDamageHitBox();
        float pX = playerDamageHitbox.x;
        float pY = playerDamageHitbox.y;
        float pW = playerDamageHitbox.width;
        float pH = playerDamageHitbox.height;

        for (Enemy e : enemies) {
            // Skip enemies that are dying or not active (soft despawn)
            if (e.isDying() || !e.isActive()) {
                continue;
            }

            // Use damage hitbox for enemy-player interaction
            Rectangle enemyDamageHitbox = e.getDamageHitBox();
            float eX = enemyDamageHitbox.x;
            float eY = enemyDamageHitbox.y;
            float eW = enemyDamageHitbox.width;
            float eH = enemyDamageHitbox.height;

            boolean overlap = GameApp.rectOverlap(pX, pY, pW, pH, eX, eY, eW, eH);
            if (overlap && playerDamageCooldown <= 0f) {
                // Player takes damage with exponential scaling over time
                int scaledDamage = getScaledEnemyDamage();
                player.takeDamage(scaledDamage);
                playerDamageCooldown = DAMAGE_COOLDOWN_DURATION;
                
                // Play damage sound (only if player is not dying)
                if (soundManager != null && !player.isDying()) {
                    soundManager.playSound("damaged", 0.9f);
                }
                
                // Break after damage to avoid multiple damage instances in same frame
                break;
            }
        }
    }
    public void handleBossPlayerCollisions(Player player, List<Boss> bosses) {
        if (bosses == null) {
            return;
        }

        Rectangle playerDamageHitbox = player.getDamageHitBox();
        float pX = playerDamageHitbox.x;
        float pY = playerDamageHitbox.y;
        float pW = playerDamageHitbox.width;
        float pH = playerDamageHitbox.height;

        for (Boss boss : bosses) {
            if (!boss.isAlive() || boss.isDying()) {
                continue;
            }

            float bx = boss.getX();
            float by = boss.getY();
            float bw = Boss.SPRITE_SIZE;
            float bh = Boss.SPRITE_SIZE;

            boolean overlap = GameApp.rectOverlap(pX, pY, pW, pH, bx, by, bw, bh);
            if (overlap && playerDamageCooldown <= 0f) {
                // Boss deals more damage with exponential scaling
                int scaledDamage = getScaledBossDamage();
                player.takeDamage(scaledDamage);
                playerDamageCooldown = DAMAGE_COOLDOWN_DURATION;

                if (soundManager != null && !player.isDying()) {
                    soundManager.playSound("damaged", 0.9f);
                }
                break;
            }
        }
    }


    public void removeDestroyedBullets(List<Bullet> bullets) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            if (b.isDestroyed() || b.isOffScreen()) {
                it.remove();
            }
        }
    }

    public void removeDeadEnemies(List<Enemy> enemies) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            // Remove enemy after death animation finishes
            if (e.isDead() && e.isDeathAnimationFinished()) {
                it.remove();
            }
        }
    }
    
    // Remove dead enemies and teleport enemies too far from player (like VS)
    public void removeDeadOrFarEnemies(List<Enemy> enemies, float playerX, float playerY) {
        for (Enemy e : enemies) {
            // Remove dead enemy after death animation
            if (e.isDead() && e.isDeathAnimationFinished()) {
                continue; // Will be handled by iterator removal below
            }
            
            // Teleport enemy if too far away (like VS - respawn at random edge)
            if (!e.isDead() && !e.isDying() && e.shouldTeleport(playerX, playerY)) {
                e.teleportToRandomEdge(playerX, playerY);
            }
        }
        
        // Remove dead enemies after death animation
        enemies.removeIf(e -> e.isDead() && e.isDeathAnimationFinished());
    }

    public void setDamageTextSystem(DamageTextSystem system) {
        this.damageTextSystem = system;
    }
    
    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }
    
    public void setPlayer(Player player) {
        this.playerRef = player;
    }

    /**
     * Handles collision between bullets and breakable objects.
     * When bullet hits an object, the object starts break animation and spawns items.
     * 
     * @param bullets List of active bullets
     * @param breakableObjects List of breakable objects in the world
     * @param onObjectBroken Callback triggered when object is broken (for item spawning)
     * @param wallCollisionChecker Wall collision checker to prevent hits through walls
     */
    public void handleBulletBreakableObjectCollisions(
            List<Bullet> bullets,
            List<BreakableObject> breakableObjects,
            Consumer<BreakableObject> onObjectBroken,
            CollisionChecker wallCollisionChecker
    ) {
        if (breakableObjects == null || breakableObjects.isEmpty()) {
            return;
        }

        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            // Check wall collision first
            if (wallCollisionChecker != null && wallCollisionChecker.checkCollision(bX, bY, bW, bH)) {
                b.destroy();
                continue;
            }

            for (BreakableObject obj : breakableObjects) {
                // Only check objects that can be shot (not broken and not breaking)
                if (!obj.canBeShot()) {
                    continue;
                }

                Rectangle objHitbox = obj.getHitbox();
                float oX = objHitbox.x;
                float oY = objHitbox.y;
                float oW = objHitbox.width;
                float oH = objHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, oX, oY, oW, oH)) {
                    // Bullet hit object -> deal damage and check if destroyed
                    boolean wasDestroyed = obj.takeDamage();
                    b.destroy();

                    // Only trigger callback when object is fully destroyed (health reached 0)
                    if (wasDestroyed && onObjectBroken != null) {
                        onObjectBroken.accept(obj);
                    }

                    break; // Each bullet can only hit one object
                }
            }
        }
    }

    /**
     * Removes completely broken objects from the list.
     * @param breakableObjects List to clean up
     */
    public void removeBrokenObjects(List<BreakableObject> breakableObjects) {
        if (breakableObjects == null) return;
        breakableObjects.removeIf(BreakableObject::isBroken);
    }

    public void reset() {
        playerDamageCooldown = 0f;
    }
}

