package nl.saxion.game.screens;

import com.badlogic.gdx.Input;
import nl.saxion.game.core.GameState;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.PlayerStatus;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.ui.HUD;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    private Player player;
    private Weapon weapon;

    private List<Bullet> bullets;

    // Enemy system - List of all active enemies in the game
    private List<Enemy> enemies;

    // Enemy spawning system - Controls difficulty curve
    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 3f; // spawn every 3 seconds
    private float enemyBaseSpeed = 34f;
    private int enemyBaseHealth = 15;

    // Max enemies to prevent performance issues
    private static final int MAX_ENEMIES = 50;

    // Total time this run has been going (for difficulty scaling)
    private float gameTime = 0f;

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

        GameApp.addTexture("player", "assets/player/auraRambo.png");
        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");
        // you can change this path to your real enemy texture
        GameApp.addTexture("enemy", "assets/Bullet/bullet.png");

        input = new InputController();
        hud = new HUD();

        currentState = GameState.MENU;

        // Hide cursor for better game experience
        GameApp.hideCursor();

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

        // ----- STATE : MENU -----
        if (currentState == GameState.MENU) {
            handleMenuInput();
            renderMenuScreen();
            return;
        }

        // ----- STATE : GAME OVER -----
        if (currentState == GameState.GAME_OVER) {
            handleGameOverInput();
            renderGameOverScreen();
            return;
        }

        // ----- GAMEPLAY STATE -----

        // Difficulty time
        gameTime += delta;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Player + weapon
        player.update(delta, input, (int) worldW, (int) worldH);
        weapon.update(delta);

        // Shooting – Weapon handles direction + spawn position
//        if (input.isShootHeld()) {
//            Bullet newBullet = weapon.tryFire(player);
//            if (newBullet != null) {
//                bullets.add(newBullet);
//            }
//        }

        // Auto-shooting - automatically fires when cooldown is ready
        Bullet newBullet = weapon.tryFire(player);
        if (newBullet != null) {
            bullets.add(newBullet);
        }

        // Update bullets
        for (Bullet b : bullets) {
            b.update(delta);
            if (b.isOffScreen()) {
                b.destroy();
            }
        }

        // Enemies chase the player (Task 9)
        for (Enemy e : enemies) {
            e.update(delta, player.getX(), player.getY());
        }

        // Enemy spawning system - handles difficulty curve (Task 10)
        updateEnemySpawning(delta);

        // Collision detection - Bullet vs Enemy
        handleBulletEnemyCollisions();

        // Cleanup - Remove dead enemies and destroyed bullets
        removeDeadEnemies();
        removeDestroyedBullets();

        // Player Damage cooldown
        playerDamageCooldown -= delta;
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

        // ----- RENDER -----
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

    // =========================
    // PLAYER STATUS / HUD
    // =========================

    public PlayerStatus getPlayerStatus() {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();

        return new PlayerStatus(health, maxHealth, score);
    }

    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    private void renderHUD() {
        PlayerStatus status = getPlayerStatus();
        hud.render(status);
    }

    // =========================
    // ENEMY SPAWNING (TASK 10)
    // =========================

    private void updateEnemySpawning(float delta) {
        // 1. Max enemy limit
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // 2. Spawn timer (count up)
        enemySpawnTimer += delta;
        if (enemySpawnTimer < enemySpawnInterval) {
            return;
        }

        // Time to spawn
        enemySpawnTimer = 0f;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // 3. Choose edge: 0 = top, 1 = right, 2 = bottom, 3 = left
        int edge = GameApp.randomInt(0, 4);

        float spawnX;
        float spawnY;

        if (edge == 0) {
            // TOP
            spawnX = GameApp.random(0f, worldW - Enemy.SPRITE_SIZE);
            spawnY = worldH; // just above top
        } else if (edge == 1) {
            // RIGHT
            spawnX = worldW; // just outside right
            spawnY = GameApp.random(0f, worldH - Enemy.SPRITE_SIZE);
        } else if (edge == 2) {
            // BOTTOM
            spawnX = GameApp.random(0f, worldW - Enemy.SPRITE_SIZE);
            spawnY = -Enemy.SPRITE_SIZE; // just below bottom
        } else {
            // LEFT
            spawnX = -Enemy.SPRITE_SIZE; // just outside left
            spawnY = GameApp.random(0f, worldH - Enemy.SPRITE_SIZE);
        }

        // 4. Difficulty scaling: 1.0x → 3.0x
        float difficultyMultiplier = 1f + (gameTime * 0.01f);
        difficultyMultiplier = GameApp.clamp(difficultyMultiplier, 1f, 3f);

        float currentSpeed = enemyBaseSpeed * difficultyMultiplier;
        int currentHealth = (int) (enemyBaseHealth * difficultyMultiplier);

        // 5. Spawn enemy
        enemies.add(new Enemy(spawnX, spawnY, currentSpeed, currentHealth));

        // 6. Spawn interval scaling (faster spawns over time, clamped)
        if (enemySpawnInterval > 1.5f) {
            enemySpawnInterval -= delta * 0.02f;
        }
        enemySpawnInterval = GameApp.clamp(enemySpawnInterval, 0.5f, 10f);
    }

    // =========================
    // COLLISIONS & CLEANUP
    // =========================

    private void handleBulletEnemyCollisions() {
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            for (Enemy e : enemies) {
                if (e.isDead()) {
                    continue;
                }

                float eX = e.getX();
                float eY = e.getY();
                float eW = Enemy.SPRITE_SIZE;
                float eH = Enemy.SPRITE_SIZE;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    e.takeDamage(b.getDamage());
                    b.destroy();

                    if (e.isDead()) {
                        addScore(10);
                    }

                    break;
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
            if (b.isDestroyed() || b.isOffScreen()) {
                it.remove();
            }
        }
    }

    private void removeDeadEnemies() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.isDead()) {
                it.remove();
            }
        }
    }

    // =========================
    // GAME FLOW / RESET
    // =========================

    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<>();
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 10);

        enemies = new ArrayList<>();
        enemies.add(new Enemy(200, 400, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(400, 450, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(600, 350, enemyBaseSpeed, enemyBaseHealth));

        // difficulty & spawning reset
        gameTime = 0f;
        enemySpawnInterval = 3f;
        enemySpawnTimer = 0f;

        score = 0;
        playerDamageCooldown = 0f;

        GameApp.log("Game reset: new run started, player.isDead() = " + player.isDead());
    }

    // =========================
    // STATE: MENU & GAME OVER
    // =========================

    private void handleMenuInput() {
        boolean enterPressed = GameApp.isKeyJustPressed(Input.Keys.ENTER);
        if (enterPressed) {
            resetGame();
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

        GameApp.drawText("default", "ZOMBIE SURVIVORS", 260, 200, "white");
        GameApp.drawText("default", "Press ENTER to start", 220, 260, "white");

        GameApp.endSpriteRendering();
    }

    private void renderGameOverScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "GAME OVER", 280, 220, "white");

        String scoreText = "Score: " + score;
        GameApp.drawText("default", scoreText, 300, 240, "white");

        GameApp.drawText("default", "Press R to restart", 240, 300, "white");

        GameApp.endSpriteRendering();
    }
}
