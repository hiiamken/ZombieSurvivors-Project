package nl.saxion.game.screens;


import nl.saxion.game.MainGame;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.systems.CollisionHandler;
import nl.saxion.game.systems.EnemySpawner;
import nl.saxion.game.systems.GameRenderer;
import nl.saxion.game.systems.GameStateManager;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.systems.MapRenderer;
import nl.saxion.game.systems.ResourceLoader;
import nl.saxion.game.ui.HUD;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.game.utils.TMXMapData;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;
    private HUD hud;

    private Player player;
    private Weapon weapon;
    private List<Bullet> bullets;
    private List<Enemy> enemies;

    // Systems
    private ResourceLoader resourceLoader;
    private MapRenderer mapRenderer;
    private EnemySpawner enemySpawner;
    private CollisionHandler collisionHandler;
    private GameRenderer gameRenderer;
    private GameStateManager gameStateManager;

    // Game state
    private float gameTime = 0f;
    private int score = 0;
    private float playerWorldX;
    private float playerWorldY;

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {
        // Initialize systems
        resourceLoader = new ResourceLoader();
        resourceLoader.loadGameResources();

        Map<Integer, TMXMapData> tmxMapDataByRoomIndex = resourceLoader.loadTMXMaps();
        mapRenderer = new MapRenderer(tmxMapDataByRoomIndex);
        enemySpawner = new EnemySpawner();
        collisionHandler = new CollisionHandler();
        gameRenderer = new GameRenderer();
        gameStateManager = new GameStateManager();

        input = new InputController(MainGame.getConfig());
        hud = new HUD();

        gameStateManager.setCurrentState(GameState.MENU);

        resetGame();
    }

    @Override
    public void hide() {
        if (resourceLoader != null) {
            resourceLoader.disposeGameResources();
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // Handle menu state
        if (gameStateManager.getCurrentState() == GameState.MENU) {
            gameStateManager.handleMenuInput(this::resetGame);
            gameStateManager.renderMenuScreen();
            return;
        }

        // Handle game over state
        if (gameStateManager.getCurrentState() == GameState.GAME_OVER) {
            gameStateManager.handleGameOverInput(this::resetGame);
            gameStateManager.renderGameOverScreen();
            return;
        }

        // ----- GAMEPLAY STATE -----

        // Update game time for difficulty scaling
        gameTime += delta;

        // Update player
        CollisionChecker collisionChecker = mapRenderer::checkWallCollision;
        player.update(delta, input, Integer.MAX_VALUE, Integer.MAX_VALUE, collisionChecker);

        // Update player world position
        playerWorldX = player.getX();
        playerWorldY = player.getY();
        gameRenderer.setPlayerWorldPosition(playerWorldX, playerWorldY);

        // Update weapon and shooting (only if player is alive)
        weapon.update(delta);
        if (!player.isDying()) {
            Bullet newBullet = weapon.tryFire(player);
            if (newBullet != null) {
                bullets.add(newBullet);
            }
        }

        // Update bullets
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            b.update(delta);

            // Check wall collision
            if (mapRenderer.checkWallCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight())) {
                b.destroy();
            }

            if (b.isOffScreen()) {
                b.destroy();
            }
        }

        // Update enemies
        for (Enemy e : enemies) {
            e.update(delta, player.getX(), player.getY(), collisionChecker, enemies);
        }

        // Update player animations
        GameApp.updateAnimation("player_idle");
        GameApp.updateAnimation("player_run_left");
        GameApp.updateAnimation("player_run_right");
        GameApp.updateAnimation("player_hit");
        GameApp.updateAnimation("player_death");

        // Update zombie animations
        GameApp.updateAnimation("zombie_idle");
        GameApp.updateAnimation("zombie_run");
        GameApp.updateAnimation("zombie_hit");
        GameApp.updateAnimation("zombie_death");

        // Enemy spawning
        enemySpawner.update(delta, gameTime, playerWorldX, playerWorldY, enemies);

        // Collision detection
        collisionHandler.update(delta);
        // Pass wall collision checker to prevent bullets hitting enemies through walls
        CollisionChecker wallChecker = mapRenderer::checkWallCollision;
        collisionHandler.handleBulletEnemyCollisions(bullets, enemies, this::addScore, wallChecker);
        collisionHandler.handleEnemyPlayerCollisions(player, enemies);

        // Cleanup
        collisionHandler.removeDeadEnemies(enemies);
        collisionHandler.removeDestroyedBullets(bullets);

        // Player death check - wait for death animation to finish
        if (player.isDying()) {
            // Only transition to game over after death animation completes
            if (player.isDeathAnimationFinished()) {
                GameApp.log("Death animation finished - showing game over");
                gameStateManager.setCurrentState(GameState.GAME_OVER);
                gameStateManager.setScore(score);
            }
        }

        // ----- RENDER -----
        // Render map background first
        mapRenderer.render(playerWorldX, playerWorldY);

        GameApp.startSpriteRendering();

        // Render entities
        gameRenderer.renderPlayer();
        gameRenderer.renderEnemies(enemies);
        gameRenderer.renderBullets(bullets);

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
    // GAME FLOW / RESET
    // =========================

    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<>();
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 10, 400f, 10f, 10f);

        enemies = new ArrayList<>();

        // Reset game state
        gameTime = 0f;
        score = 0;
        enemySpawner.reset();
        collisionHandler.reset();

        // Set initial player world position
        playerWorldX = MapRenderer.getMapTileWidth() / 2f; // 480
        playerWorldY = MapRenderer.getMapTileHeight() / 2f; // 320

        // Check and adjust if spawn position has wall
        if (mapRenderer != null) {
            TMXMapData spawnMapData = mapRenderer.getTMXDataForPosition(playerWorldX, playerWorldY);
            if (spawnMapData != null) {
                Rectangle testHitbox = new Rectangle((int)playerWorldX, (int)playerWorldY, 16, 16);
                if (mapRenderer.checkWallCollision(testHitbox.x, testHitbox.y, testHitbox.width, testHitbox.height)) {

                    for (int offset = 50; offset < 300; offset += 50) {

                        for (int dx = -offset; dx <= offset; dx += 50) {
                            for (int dy = -offset; dy <= offset; dy += 50) {
                                float testX = playerWorldX + dx;
                                float testY = playerWorldY + dy;
                                if (testX >= 0 && testX < MapRenderer.getMapTileWidth() &&
                                        testY >= 0 && testY < MapRenderer.getMapTileHeight()) {
                                    if (!mapRenderer.checkWallCollision(testX, testY, 16, 16)) {
                                        playerWorldX = testX;
                                        playerWorldY = testY;
                                        GameApp.log("Adjusted player spawn to safe position: (" + playerWorldX + ", " + playerWorldY + ")");
                                        break;
                                    }
                                }
                            }
                        }
                        if (!mapRenderer.checkWallCollision(playerWorldX, playerWorldY, 16, 16)) {
                            break;
                        }
                    }
                }
            }
        }

        // Set player position
        player.setPosition(playerWorldX, playerWorldY);

        // Pass player reference to renderer
        gameRenderer.setPlayer(player);

        // Reset enemies - spawn a few around the player
        enemies.clear();
        float enemyBaseSpeed = enemySpawner.getEnemyBaseSpeed();
        int enemyBaseHealth = enemySpawner.getEnemyBaseHealth();
        enemies.add(new Enemy(playerWorldX + 200, playerWorldY + 150, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 400, playerWorldY + 200, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 600, playerWorldY + 100, enemyBaseSpeed, enemyBaseHealth));

        GameApp.log("Game reset: new run started, player.isDead() = " + player.isDead());
        GameApp.log("Player starting at world position: (" + playerWorldX + ", " + playerWorldY + ")");
    }
}
