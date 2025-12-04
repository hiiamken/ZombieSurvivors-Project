package nl.saxion.game.screens;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import nl.saxion.game.core.GameState;
import nl.saxion.game.entities.PlayerStatus;

import java.awt.*;
import java.awt.event.KeyEvent;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.ui.HUD;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import nl.saxion.game.entities.Player;


import java.security.Key;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    private Player player;

    private List<Bullet> bullets;

    private Weapon weapon;

    // Enemy system - List of all active enemies in the game
    private List<Enemy> enemies;

    // Enemy spawning system - Controls difficulty curve
    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 3f; // spawn every 3 seconds
    private float enemyBaseSpeed = 60f;
    private int enemyBaseHealth = 15;

    private float playerDamageCooldown = 0f;
    private static final float DAMAGE_COOLDOWN_DURATION = 0.5f;
    private static final int ENEMY_TOUCH_DAMAGE = 1;

    private int score = 0;

    private GameState currentState = GameState.MENU;

    private HUD hud;

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {

        GameApp.log("PlayScreen loaded");

        // Текстуры грузим один раз
        GameApp.addTexture("player", "assets/player/auraRambo.png");
        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");
        GameApp.addTexture("enemy", "assets/Bullet/bullet.png");

        input = new InputController();

        hud = new HUD();

        currentState = GameState.MENU;

        resetGame();
    }


    @Override
    public void hide() {

        GameApp.log("PlayScreen hidden");
        GameApp.disposeTexture("player");

        GameApp.disposeTexture("bullet");

        GameApp.disposeTexture("enemy");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // STATE : MENU

        if (currentState == GameState.MENU) {
            handleMenuInput();
            renderMenuScreen();
            return;
        }
        // STATE : GAME OVER
        if (currentState == GameState.GAME_OVER) {
            handleGameOverInput();
            renderGameOverScreen();
            return;
        }

        // --- UPDATE ---
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        player.update(delta, input, (int) worldW, (int) worldH);
        weapon.update(delta);

        // Shooting – Weapon handles direction + spawn position
        if (input.isShootHeld()) {
            Bullet newBullet = weapon.tryFire(player);
            if (newBullet != null) {
                bullets.add(newBullet);
            }
        }

        // Update bullets and remove off-screen ones
        for (Bullet b : bullets) {
            b.update(delta);

            if(b.isOffScreen()) {
                b.destroy();
            }
        }

        for (Enemy e : enemies) {
            e.update(delta);
        }

        // Enemy spawning system - handles difficulty curve
        updateEnemySpawning(delta);

        // Collision detection - Bullet vs Enemy
        handleBulletEnemyCollisions();

        // Cleanup - Remove dead enemies and destroyed bullets
        removeDeadEnemies();
        removeDestroyedBullets();

        // Player Damage cooldown
        playerDamageCooldown = playerDamageCooldown - delta;
        if (playerDamageCooldown < 0f) {
            playerDamageCooldown = 0f;
        }

        // Enemy and player collision
        handleEnemyPlayerCollisions();

        // Player death check
        if (player.isDead()) {
            GameApp.log("Player died!");
            currentState = GameState.GAME_OVER;
        }

        // --- RENDER ---
        GameApp.startSpriteRendering();

        player.render();

        for (Enemy e : enemies) {
            e.render();
        }

        for (Bullet b : bullets) {
            b.render();
        }

        renderHUD();

        GameApp.endSpriteRendering();

    }


    // Used by HUD/UI to draw HP, score, etc.
    // UI should only READ this, not modify Player. etc. here

    public PlayerStatus getPlayerStatus() {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();

        return new PlayerStatus(health, maxHealth, score);
    }
    // Use this when enemy dies and u need to add score
    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }
    private void renderHUD() {
        PlayerStatus status = getPlayerStatus();

        hud.render(status);
    }

    // ENEMY SYSTEM METHODS

    private void updateEnemySpawning(float delta) {
        enemySpawnTimer -= delta;

        if (enemySpawnTimer <= 0f) {
            enemySpawnTimer = enemySpawnInterval;

            float worldW = GameApp.getWorldWidth();
            float worldH = GameApp.getWorldHeight();

            // Spawn a new enemy at a random X at the top of the world
            float spawnX = GameApp.random(0f, worldW - Enemy.SPRITE_SIZE);
            float spawnY = worldH;

            enemies.add(new Enemy(spawnX, spawnY, enemyBaseSpeed, enemyBaseHealth));

            // Very simple difficulty scaling: slightly reduce spawn interval over time
            // makes the game progressively harder as time passes
            if (enemySpawnInterval > 1.5f) {
                enemySpawnInterval -= delta * 0.02f;
            }
        }
    }

    private void handleBulletEnemyCollisions() {
        for (Bullet b : bullets) {
            // Skip bullets that are already destroyed
            if (b.isDestroyed()) {
                continue;
            }

            // Get bullet position and size for collision detection
            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            for (Enemy e : enemies) {
                // Check if bullet hitbox intersects with enemy hitbox
                float eX = e.getX();
                float eY = e.getY();
                float eW = Enemy.SPRITE_SIZE;
                float eH = Enemy.SPRITE_SIZE;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    // Apply damage to enemy
                    e.takeDamage(b.getDamage());

                    // Destroy the bullet (it hit something)
                    b.destroy();

                    // Optional: This is where a score system would be notified
                    // Future enhancement: addScore(10); when enemy dies

                    break; // This bullet already hit an enemy, no need to check others
                }
            }
        }
    }

    private void handleEnemyPlayerCollisions() {
        float pX = player.getX();
        float pY = player.getY();
        float pW = Player.SPRITE_SIZE;
        float pH = Player.SPRITE_SIZE;

        for (Enemy e : enemies) {
            float eX = e.getX();
            float eY = e.getY();
            float eW = Enemy.SPRITE_SIZE;
            float eH = Enemy.SPRITE_SIZE;

            boolean overlap = GameApp.rectOverlap(pX, pY, pW, pH, eX, eY, eW, eH);
            if (overlap) {
                if (playerDamageCooldown <= 0f) {
                    player.takeDamage(ENEMY_TOUCH_DAMAGE);
                    playerDamageCooldown = DAMAGE_COOLDOWN_DURATION;
                }
            }
        }
    }

    private void removeDestroyedBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            // Remove bullets that are destroyed or off-screen
            if (b.isDestroyed() || b.isOffScreen()) {
                it.remove();
            }
        }
    }

    private void removeDeadEnemies() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            // Remove enemies that are dead
            if (e.isDead()) {
                it.remove();
            }
        }
    }
    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 200f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<Bullet>();

        weapon = new Weapon(Weapon.WeaponType.PISTOL, 5.0f, 10);

        enemies = new ArrayList<Enemy>();
        enemies.add(new Enemy(200,400, enemyBaseSpeed,enemyBaseHealth));
        enemies.add(new Enemy(400,450, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(600,350,enemyBaseSpeed, enemyBaseHealth));

        enemySpawnInterval = 3f;
        enemySpawnTimer = enemySpawnInterval;

        score = 0;

        playerDamageCooldown = 0f;

        GameApp.log("Game reset: new run started, player.isDead() = " + player.isDead());
    }
    private void handleMenuInput() {
        boolean enterPressed = GameApp.isKeyJustPressed(Input.Keys.ENTER);
        if (enterPressed) {
            currentState = GameState.PLAYING;
        }
    }
    private void handleGameOverInput() {
        boolean rPressed = GameApp.isKeyJustPressed(Input.Keys.R);

        if (rPressed) {
            resetGame();
            currentState = GameState.PLAYING;
        }
    }
    private void renderMenuScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "ZOMBIE SURVIVORS", 260,200, "white");
        GameApp.drawText("default", "Press ENTER to start", 220,260, "white");

        GameApp.endSpriteRendering();
    }

    private void renderGameOverScreen() {
        GameApp.startSpriteRendering();
        GameApp.drawText("default", "GAME OVER", 280,220,"white");

        String scoreText = "Score: " + score;
        GameApp.drawText("default", scoreText, 300,240, "white");

        GameApp.drawText("default", "Press R to restart", 240,300,"white");

        GameApp.endSpriteRendering();

    }
    // Later: getPlayerStatus(), HUD, etc.
}
