package nl.saxion.game.screens;

import nl.saxion.game.MainGame;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.entities.XPOrb;
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

import java.awt.Rectangle;
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
    private List<XPOrb> xpOrbs;

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

        // MENU
        if (gameStateManager.getCurrentState() == GameState.MENU) {
            gameStateManager.handleMenuInput(this::resetGame);
            gameStateManager.renderMenuScreen();
            return;
        }

        // GAME OVER
        if (gameStateManager.getCurrentState() == GameState.GAME_OVER) {
            gameStateManager.handleGameOverInput(this::resetGame);
            gameStateManager.renderGameOverScreen();
            return;
        }

        // ----- GAMEPLAY -----
        gameTime += delta;

        CollisionChecker collisionChecker = mapRenderer::checkWallCollision;

        // Player
        player.update(delta, input, Integer.MAX_VALUE, Integer.MAX_VALUE, collisionChecker);

        playerWorldX = player.getX();
        playerWorldY = player.getY();
        gameRenderer.setPlayerWorldPosition(playerWorldX, playerWorldY);

        // Weapon shooting
        weapon.update(delta);
        if (!player.isDying()) {
            Bullet newBullet = weapon.tryFire(player);
            if (newBullet != null) bullets.add(newBullet);
        }

        // Bullets
        for (Bullet b : bullets) {
            if (b.isDestroyed()) continue;

            b.update(delta);

            if (mapRenderer.checkWallCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight())) {
                b.destroy();
            }
            if (b.isOffScreen()) {
                b.destroy();
            }
        }

        // Enemies
        for (Enemy e : enemies) {
            e.update(delta, player.getX(), player.getY(), collisionChecker, enemies);
        }

        // XP Orbs update + collect
        for (XPOrb orb : xpOrbs) {
            orb.update(delta, playerWorldX, playerWorldY);
            if (orb.isCollected()) {
                // If your Player doesn't have addXP yet, tell me and I’ll patch Player.java too
                player.addXP(orb.getValue());
            }
        }
        xpOrbs.removeIf(XPOrb::isCollected);

        // Animations
        GameApp.updateAnimation("player_idle");
        GameApp.updateAnimation("player_run_left");
        GameApp.updateAnimation("player_run_right");
        GameApp.updateAnimation("player_hit");
        GameApp.updateAnimation("player_death");

        GameApp.updateAnimation("zombie_idle");
        GameApp.updateAnimation("zombie_run");
        GameApp.updateAnimation("zombie_hit");
        GameApp.updateAnimation("zombie_death");

        // Spawning
        enemySpawner.update(delta, gameTime, playerWorldX, playerWorldY, enemies);

        // Collisions
        collisionHandler.update(delta);

        // ✅ Spawn 1–3 XP orbs when enemy dies
        collisionHandler.handleBulletEnemyCollisions(
                bullets,
                enemies,
                enemy -> {
                    score += 10;

                    int count = GameApp.randomInt(1, 4); // 1..3
                    for (int i = 0; i < count; i++) {
                        xpOrbs.add(new XPOrb(enemy.getX(), enemy.getY(), 5));
                    }
                }
        );

        collisionHandler.handleEnemyPlayerCollisions(player, enemies);

        // Cleanup
        collisionHandler.removeDeadEnemies(enemies);
        collisionHandler.removeDestroyedBullets(bullets);

        // Death -> Game Over (after animation)
        if (player.isDying() && player.isDeathAnimationFinished()) {
            GameApp.log("Death animation finished - showing game over");
            gameStateManager.setCurrentState(GameState.GAME_OVER);
            gameStateManager.setScore(score);
        }

        // ----- RENDER -----
        mapRenderer.render(playerWorldX, playerWorldY);

        GameApp.startSpriteRendering();

        gameRenderer.renderPlayer();
        gameRenderer.renderEnemies(enemies);
        gameRenderer.renderBullets(bullets);

        // Render XP orbs (world -> screen)
        for (XPOrb orb : xpOrbs) {
            float sx = GameApp.getWorldWidth() / 2f + (orb.getX() - playerWorldX);
            float sy = GameApp.getWorldHeight() / 2f + (orb.getY() - playerWorldY);
            orb.render(sx, sy);
        }

        renderHUD();

        GameApp.endSpriteRendering();
    }

    // =========================
    // PLAYER STATUS / HUD
    // =========================
    public PlayerStatus getPlayerStatus() {
        return new PlayerStatus(
                player.getHealth(),
                player.getMaxHealth(),
                score,
                player.getLevel(),
                player.getXP(),
                player.getXPToNext()
        );
    }

    private void renderHUD() {
        hud.render(getPlayerStatus());
    }

    // =========================
    // RESET
    // =========================
    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<>();
        enemies = new ArrayList<>();
        xpOrbs = new ArrayList<>();

        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 10, 400f, 10f, 10f);

        gameTime = 0f;
        score = 0;
        enemySpawner.reset();
        collisionHandler.reset();

        playerWorldX = MapRenderer.getMapTileWidth() / 2f;
        playerWorldY = MapRenderer.getMapTileHeight() / 2f;

        // Adjust spawn if inside wall
        if (mapRenderer != null) {
            TMXMapData spawnMapData = mapRenderer.getTMXDataForPosition(playerWorldX, playerWorldY);
            if (spawnMapData != null) {
                Rectangle testHitbox = new Rectangle((int) playerWorldX, (int) playerWorldY, 16, 16);
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

        player.setPosition(playerWorldX, playerWorldY);

        gameRenderer.setPlayer(player);

        // Spawn a few enemies
        float enemyBaseSpeed = enemySpawner.getEnemyBaseSpeed();
        int enemyBaseHealth = enemySpawner.getEnemyBaseHealth();

        enemies.add(new Enemy(playerWorldX + 200, playerWorldY + 150, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 400, playerWorldY + 200, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 600, playerWorldY + 100, enemyBaseSpeed, enemyBaseHealth));

        GameApp.log("Game reset: new run started, player.isDead() = " + player.isDead());
        GameApp.log("Player starting at world position: (" + playerWorldX + ", " + playerWorldY + ")");
    }
}
