package nl.saxion.game.systems;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.gameapp.GameApp;

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

        GameApp.drawAnimation(animKey,
                playerScreenX - Player.SPRITE_SIZE / 2f,
                playerScreenY - Player.SPRITE_SIZE / 2f,
                Player.SPRITE_SIZE,
                Player.SPRITE_SIZE
        );
    }

    public void renderEnemies(List<Enemy> enemies) {
        for (Enemy e : enemies) {
            renderEnemy(e);
        }
    }

    private void renderEnemy(Enemy enemy) {
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
}

