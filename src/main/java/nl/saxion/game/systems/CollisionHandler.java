package nl.saxion.game.systems;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.gameapp.GameApp;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

// Handles all collision detection and cleanup
public class CollisionHandler {
    private static final float DAMAGE_COOLDOWN_DURATION = 0.5f;
    private static final int ENEMY_TOUCH_DAMAGE = 1;

    private float playerDamageCooldown = 0f;

    // Damage text system for spawning damage numbers
    private DamageTextSystem damageTextSystem;
    
    // Sound manager for playing damage sound
    private SoundManager soundManager;

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

                // Use DAMAGE hitbox for bullet collision
                Rectangle enemyDamageHitbox = e.getDamageHitBox();
                float eX = enemyDamageHitbox.x;
                float eY = enemyDamageHitbox.y;
                float eW = enemyDamageHitbox.width;
                float eH = enemyDamageHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    int damage = b.getDamage();
                    e.takeDamage(damage);
                    b.destroy();

                    // Spawn damage text at enemy position
                    if (damageTextSystem != null) {
                        // Use enemy center position for damage text spawn
                        float enemyCenterX = eX + eW / 2f;
                        float enemyCenterY = eY + eH / 2f;
                        // Simple crit check: 10% chance or if damage > 15
                        boolean isCrit = GameApp.random(0f, 1f) < 0.1f || damage > 15;
                        damageTextSystem.spawnDamageText(enemyCenterX, enemyCenterY, damage, isCrit);
                    }

                    if (e.isDead()) {
                        onEnemyKilled.accept(10); // Score for killing enemy
                        if (onEnemyKilledForOrbs != null) {
                            onEnemyKilledForOrbs.accept(e); // Spawn orbs at enemy position
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
                // Player takes damage (1 tick = 1 damage instance, like Vampire Survivors)
                player.takeDamage(ENEMY_TOUCH_DAMAGE);
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

    public void reset() {
        playerDamageCooldown = 0f;
    }
}

