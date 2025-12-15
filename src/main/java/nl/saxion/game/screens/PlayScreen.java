package nl.saxion.game.screens;

import com.badlogic.gdx.Input;
import nl.saxion.game.MainGame;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.*;
import nl.saxion.game.systems.*;
import nl.saxion.game.ui.HUD;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.game.utils.TMXMapData;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;
    private HUD hud;

    private Player player;
    private Weapon weapon;
    private List<Bullet> bullets;
    private List<Enemy> enemies;

    // ✅ XP + LEVEL FIELDS
    private final List<XPOrb> xpOrbs = new ArrayList<>();
    private boolean levelUpActive = false;
    private final List<LevelUpOption> levelOptions = new ArrayList<>();

    // ✅ SAFE: prevents spawning XP multiple times for same enemy
    private final Set<Enemy> xpDropped = new HashSet<>();

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

        // ✅ LEVEL-UP MENU (PAUSES GAME)
        if (levelUpActive) {
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("default", "LEVEL UP!", 400, 400, "white");

            GameApp.drawTextCentered("default", "Press 1, 2 or 3 to choose", 400, 370, "white");


            for (int i = 0; i < 3; i++) {
                LevelUpOption opt = levelOptions.get(i);
                GameApp.drawText("default",
                        "[" + (i + 1) + "] " + opt.title + " - " + opt.description,
                        250, 350 - i * 30, "white");
            }
            GameApp.endSpriteRendering();

            if (GameApp.isKeyJustPressed(Input.Keys.NUM_1)) applyUpgrade(0);
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_2)) applyUpgrade(1);
            if (GameApp.isKeyJustPressed(Input.Keys.NUM_3)) applyUpgrade(2);
            return;
        }

        // ----- GAMEPLAY -----
        gameTime += delta;

        CollisionChecker collisionChecker = mapRenderer::checkWallCollision;
        player.update(delta, input, Integer.MAX_VALUE, Integer.MAX_VALUE, collisionChecker);

        playerWorldX = player.getX();
        playerWorldY = player.getY();
        gameRenderer.setPlayerWorldPosition(playerWorldX, playerWorldY);

        weapon.update(delta);
        if (!player.isDying()) {
            Bullet newBullet = weapon.tryFire(player);
            if (newBullet != null) bullets.add(newBullet);
        }

        for (Bullet b : bullets) {
            if (b.isDestroyed()) continue;
            b.update(delta);

            if (mapRenderer.checkWallCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight())) b.destroy();
            if (b.isOffScreen()) b.destroy();
        }

        for (Enemy e : enemies) {
            e.update(delta, player.getX(), player.getY(), collisionChecker, enemies);
        }

        // animations
        GameApp.updateAnimation("player_idle");
        GameApp.updateAnimation("player_run_left");
        GameApp.updateAnimation("player_run_right");
        GameApp.updateAnimation("player_hit");
        GameApp.updateAnimation("player_death");

        GameApp.updateAnimation("zombie_idle");
        GameApp.updateAnimation("zombie_run");
        GameApp.updateAnimation("zombie_hit");
        GameApp.updateAnimation("zombie_death");

        enemySpawner.update(delta, gameTime, playerWorldX, playerWorldY, enemies);

        // collisions
        collisionHandler.update(delta);
        CollisionChecker wallChecker = mapRenderer::checkWallCollision;
        collisionHandler.handleBulletEnemyCollisions(bullets, enemies, this::addScore, wallChecker);
        collisionHandler.handleEnemyPlayerCollisions(player, enemies);

        // ✅ SAFE XP ORB SPAWN HOOK (only once per enemy)
        for (Enemy e : enemies) {
            if (e.isDead() && !xpDropped.contains(e)) {
                xpDropped.add(e);

                int count = GameApp.randomInt(1, 4);
                for (int i = 0; i < count; i++) {
                    xpOrbs.add(new XPOrb(
                            e.getX() + GameApp.random(-10, 10),
                            e.getY() + GameApp.random(-10, 10),
                            10
                    ));
                }
                addScore(10);
            }
        }

        // cleanup
        collisionHandler.removeDeadEnemies(enemies);
        collisionHandler.removeDestroyedBullets(bullets);

        // ✅ XP UPDATE LOOP
        for (int i = xpOrbs.size() - 1; i >= 0; i--) {
            XPOrb orb = xpOrbs.get(i);
            orb.update(delta, player.getX(), player.getY());

            if (orb.isCollected()) {
                player.addXP(orb.getXPValue());
                xpOrbs.remove(i);
            } else if (orb.isExpired()) {
                xpOrbs.remove(i);
            }
        }

        // ✅ LEVEL-UP TRIGGER
        if (!levelUpActive && player.checkLevelUp()) {
            levelUpActive = true;
            levelOptions.clear();

            int len = StatUpgradeType.values().length;
            levelOptions.add(new LevelUpOption(StatUpgradeType.values()[GameApp.randomInt(0, len)]));
            levelOptions.add(new LevelUpOption(StatUpgradeType.values()[GameApp.randomInt(0, len)]));
            levelOptions.add(new LevelUpOption(StatUpgradeType.values()[GameApp.randomInt(0, len)]));
        }

        // death -> game over
        if (player.isDying() && player.isDeathAnimationFinished()) {
            gameStateManager.setCurrentState(GameState.GAME_OVER);
            gameStateManager.setScore(score);
        }

        // ----- RENDER (FIXED ORDER) -----
        mapRenderer.render(playerWorldX, playerWorldY);

        // 1) SPRITES (player/enemy/bullets)
        GameApp.startSpriteRendering();
        gameRenderer.renderPlayer();
        gameRenderer.renderEnemies(enemies);
        gameRenderer.renderBullets(bullets);
        GameApp.endSpriteRendering();

        // 2) SHAPES (XP orbs + XP bar)
        GameApp.startShapeRenderingFilled();
        for (XPOrb orb : xpOrbs) {
            orb.render(playerWorldX, playerWorldY);
        }
        hud.renderXPBarOnly(getPlayerStatus()); // ✅ needs HUD change below
        GameApp.endShapeRendering();

        // 3) TEXT (HP + score + level text)
        GameApp.startSpriteRendering();
        hud.renderTextOnly(getPlayerStatus());  // ✅ needs HUD change below
        GameApp.endSpriteRendering();
    }

    private void applyUpgrade(int index) {
        player.applyStatUpgrade(levelOptions.get(index).stat);
        player.levelUp();
        levelUpActive = false;
    }

    public PlayerStatus getPlayerStatus() {
        return new PlayerStatus(
                player.getHealth(),
                player.getMaxHealth(),
                score,
                player.getCurrentLevel(),
                player.getCurrentXP(),
                player.getXPToNextLevel()
        );
    }

    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<>();
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 10, 400f, 10f, 10f);

        enemies = new ArrayList<>();

        gameTime = 0f;
        score = 0;
        enemySpawner.reset();
        collisionHandler.reset();

        // ✅ RESET XP SYSTEM STATE
        xpOrbs.clear();
        xpDropped.clear();
        levelUpActive = false;
        levelOptions.clear();

        playerWorldX = MapRenderer.getMapTileWidth() / 2f;
        playerWorldY = MapRenderer.getMapTileHeight() / 2f;

        if (mapRenderer != null) {
            Rectangle testHitbox = new Rectangle((int) playerWorldX, (int) playerWorldY, 16, 16);
            if (mapRenderer.checkWallCollision(testHitbox.x, testHitbox.y, testHitbox.width, testHitbox.height)) {
                for (int offset = 50; offset < 300; offset += 50) {
                    for (int dx = -offset; dx <= offset; dx += 50) {
                        for (int dy = -offset; dy <= offset; dy += 50) {
                            float testX = playerWorldX + dx;
                            float testY = playerWorldY + dy;
                            if (!mapRenderer.checkWallCollision(testX, testY, 16, 16)) {
                                playerWorldX = testX;
                                playerWorldY = testY;
                                break;
                            }
                        }
                    }
                    if (!mapRenderer.checkWallCollision(playerWorldX, playerWorldY, 16, 16)) break;
                }
            }
        }

        player.setPosition(playerWorldX, playerWorldY);
        gameRenderer.setPlayer(player);

        float enemyBaseSpeed = enemySpawner.getEnemyBaseSpeed();
        int enemyBaseHealth = enemySpawner.getEnemyBaseHealth();
        enemies.add(new Enemy(playerWorldX + 200, playerWorldY + 150, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 400, playerWorldY + 200, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 600, playerWorldY + 100, enemyBaseSpeed, enemyBaseHealth));
    }
}
