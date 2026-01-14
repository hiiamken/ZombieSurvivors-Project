package nl.saxion.game.systems;


import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.gameapp.GameApp;
import nl.saxion.game.entities.Boss;
import nl.saxion.game.systems.EnemySpawner.StampedeZombie;


import java.util.List;

// Handles rendering of all game entities relative to viewport
public class GameRenderer {
    private float playerWorldX;
    private float playerWorldY;
    private Player player;

    public void setPlayerWorldPosition(float x, float y) {
        playerWorldX = x;
        playerWorldY = y;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void renderPlayer() {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Player always at center of viewport
        float playerScreenX = worldW / 2f;
        float playerScreenY = worldH / 2f;

        // Get current animation from player state
        String animKey = (player != null) ? player.getCurrentAnimation() : "player_idle";

        // Check if animation exists before rendering (prevent crash if animation not loaded)
        if (!GameApp.hasAnimation(animKey)) {
            // Fallback to idle if animation not available
            animKey = "player_idle";
            if (!GameApp.hasAnimation(animKey)) {
                // If even idle doesn't exist, skip rendering
                return;
            }
        }

        // Flip animations based on facing direction
        // player_idle, player_hit, and player_death default face right, so flip when facing left
        // player_run_left and player_run_right already have direction, don't flip
        boolean flipX = false;
        if (player != null) {
            if (animKey.equals("player_idle") || animKey.equals("player_hit") || animKey.equals("player_death")) {
                flipX = !player.isFacingRight();
            }
            // player_run_left and player_run_right already have correct direction
        }

        GameApp.drawAnimation(animKey,
                playerScreenX - Player.SPRITE_SIZE / 2f,
                playerScreenY - Player.SPRITE_SIZE / 2f,
                Player.SPRITE_SIZE,
                Player.SPRITE_SIZE,
                0,
                flipX,
                false
        );
    }

    public void renderEnemies(List<Enemy> enemies) {
        for (Enemy e : enemies) {
            renderEnemy(e);
        }
    }
    public void renderBosses(List<Boss> bosses) {
        if (bosses == null) {
            return;
        }

        for (Boss boss : bosses) {
            renderBoss(boss);
        }
    }

    private void renderBoss(Boss boss) {

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        float offsetX = boss.getX() - playerWorldX;
        float offsetY = boss.getY() - playerWorldY;
        float screenX = worldW / 2f + offsetX;
        float screenY = worldH / 2f + offsetY;

        float size = Boss.SPRITE_SIZE;

        if (screenX + size > 0 && screenX < worldW && screenY + size > 0 && screenY < worldH) {
            String animationKey = boss.getCurrentAnimation();
            boolean flipX = !boss.isFacingRight();

            if (GameApp.hasAnimation(animationKey)) {
                GameApp.drawAnimation(animationKey, screenX, screenY, size, size, 0, flipX, false);
            } else {
                GameApp.drawTexture("enemy", screenX, screenY, size, size);
            }
        }

    }


    private void renderEnemy(Enemy enemy) {
        // Only render if visible (soft despawn check)
        if (!enemy.isVisible()) {
            return;
        }
        
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Calculate screen position (world → screen)
        float offsetX = enemy.getX() - playerWorldX;
        float offsetY = enemy.getY() - playerWorldY;
        float screenX = worldW / 2f + offsetX;
        float screenY = worldH / 2f + offsetY;

        // Only render if in viewport
        if (screenX + Enemy.SPRITE_SIZE > 0 && screenX < worldW &&
                screenY + Enemy.SPRITE_SIZE > 0 && screenY < worldH) {

            // Get the current animation from enemy (handles hit/death/run states)
            String animationKey = enemy.getCurrentAnimation();

            // Flip sprite based on facing direction (flip when facing left)
            boolean flipX = !enemy.isFacingRight();

            if (GameApp.hasAnimation(animationKey)) {
                GameApp.drawAnimation(animationKey, screenX, screenY, Enemy.SPRITE_SIZE, Enemy.SPRITE_SIZE, 0, flipX, false);
            } else {
                // Fallback to static texture
                GameApp.drawTexture("enemy", screenX, screenY, Enemy.SPRITE_SIZE, Enemy.SPRITE_SIZE);
            }
        }
    }

    public void renderBullets(List<Bullet> bullets) {
        for (Bullet b : bullets) {
            if (!b.isDestroyed()) {
                renderBullet(b);
            }
        }
    }

    private void renderBullet(Bullet bullet) {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Calculate screen position (world → screen)
        float offsetX = bullet.getX() - playerWorldX;
        float offsetY = bullet.getY() - playerWorldY;
        float screenX = worldW / 2f + offsetX;
        float screenY = worldH / 2f + offsetY;

        // Only render if in viewport
        if (screenX + bullet.getWidth() > 0 && screenX < worldW &&
                screenY + bullet.getHeight() > 0 && screenY < worldH) {

            GameApp.drawTexture("bullet", screenX, screenY, bullet.getWidth(), bullet.getHeight());
        }
    }
    
    /**
     * Render stampede zombies - they run straight across screen
     */
    public void renderStampedeZombies(java.util.List<StampedeZombie> stampedeZombies) {
        if (stampedeZombies == null) return;
        
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();
        float spriteSize = Enemy.SPRITE_SIZE; // Same size as regular zombies
        
        for (StampedeZombie sz : stampedeZombies) {
            // Calculate screen position
            float offsetX = sz.x - playerWorldX;
            float offsetY = sz.y - playerWorldY;
            float screenX = worldW / 2f + offsetX;
            float screenY = worldH / 2f + offsetY;
            
            // Only render if in viewport
            if (screenX + spriteSize > 0 && screenX < worldW &&
                    screenY + spriteSize > 0 && screenY < worldH) {
                
                String animKey = sz.currentAnimation;
                boolean flipX = !sz.facingRight;
                
                if (GameApp.hasAnimation(animKey)) {
                    GameApp.drawAnimation(animKey, screenX, screenY, spriteSize, spriteSize, 0, flipX, false);
                } else {
                    GameApp.drawTexture("enemy", screenX, screenY, spriteSize, spriteSize);
                }
            }
        }
    }
}

