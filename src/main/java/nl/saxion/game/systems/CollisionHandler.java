package nl.saxion.game.systems;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.gameapp.GameApp;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

// Handles all collision detection and cleanup
public class CollisionHandler {
    private static final float DAMAGE_COOLDOWN_DURATION = 0.5f;
    private static final int ENEMY_TOUCH_DAMAGE = 1;

    private float playerDamageCooldown = 0f;

    public void update(float delta) {
        // Player Damage cooldown
        playerDamageCooldown -= delta;
        if (playerDamageCooldown < 0f) {
            playerDamageCooldown = 0f;
        }
    }

    public void handleBulletEnemyCollisions(List<Bullet> bullets, List<Enemy> enemies, java.util.function.Consumer<Integer> onEnemyKilled) {
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            for (Enemy e : enemies) {
                // Skip enemies that are dead or already dying
                if (e.isDead() || e.isDying()) {
                    continue;
                }

                // Use DAMAGE hitbox (larger, covers body) for bullet collision
                Rectangle enemyDamageHitbox = e.getDamageHitBox();
                float eX = enemyDamageHitbox.x;
                float eY = enemyDamageHitbox.y;
                float eW = enemyDamageHitbox.width;
                float eH = enemyDamageHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    e.takeDamage(b.getDamage());
                    b.destroy();

                    if (e.isDead()) {
                        onEnemyKilled.accept(10); // Score for killing enemy
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
            // Skip enemies that are dying
            if (e.isDying()) {
                continue;
            }

            // Use damage hitbox for enemy-player interaction
            Rectangle enemyDamageHitbox = e.getDamageHitBox();
            float eX = enemyDamageHitbox.x;
            float eY = enemyDamageHitbox.y;
            float eW = enemyDamageHitbox.width;
            float eH = enemyDamageHitbox.height;

            boolean overlap = GameApp.rectOverlap(pX, pY, pW, pH, eX, eY, eW, eH);
            if (overlap) {
                if (playerDamageCooldown <= 0f) {
                    player.takeDamage(ENEMY_TOUCH_DAMAGE);
                    playerDamageCooldown = DAMAGE_COOLDOWN_DURATION;
                }
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
            // Remove enemy
            if (e.isDead() && e.isDeathAnimationFinished()) {
                it.remove();
            }
        }
    }

    public void reset() {
        playerDamageCooldown = 0f;
    }
}

