package assets.xp;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.gameapp.GameApp;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

// Handles all collision detection and cleanup
public class CollisionHandler {
    private static final float DAMAGE_COOLDOWN_DURATION = 0.5f;
    private static final int ENEMY_TOUCH_DAMAGE = 1;

    private float playerDamageCooldown = 0f;

    public void update(float delta) {
        playerDamageCooldown -= delta;
        if (playerDamageCooldown < 0f) {
            playerDamageCooldown = 0f;
        }
    }

    // ✅ FIXED: callback gives you the Enemy (so enemy.getX()/getY() works)
    public void handleBulletEnemyCollisions(List<Bullet> bullets,
                                            List<Enemy> enemies,
                                            Consumer<Enemy> onEnemyKilled) {
        for (Bullet b : bullets) {
            if (b.isDestroyed()) continue;

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            for (Enemy e : enemies) {
                if (e.isDead() || e.isDying()) continue;

                Rectangle enemyDamageHitbox = e.getDamageHitBox();
                float eX = enemyDamageHitbox.x;
                float eY = enemyDamageHitbox.y;
                float eW = enemyDamageHitbox.width;
                float eH = enemyDamageHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    e.takeDamage(b.getDamage());
                    b.destroy();

                    if (e.isDead()) {
                        onEnemyKilled.accept(e); // ✅ pass the actual enemy
                    }
                    break;
                }
            }
        }
    }

    public void handleEnemyPlayerCollisions(Player player, List<Enemy> enemies) {
        Rectangle playerDamageHitbox = player.getDamageHitBox();
        float pX = playerDamageHitbox.x;
        float pY = playerDamageHitbox.y;
        float pW = playerDamageHitbox.width;
        float pH = playerDamageHitbox.height;

        for (Enemy e : enemies) {
            if (e.isDying()) continue;

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
            if (e.isDead() && e.isDeathAnimationFinished()) {
                it.remove();
            }
        }
    }

    public void reset() {
        playerDamageCooldown = 0f;
    }
}
