package nl.saxion.game.screens;


import nl.saxion.game.MainGame;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.LevelUpOption;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.StatUpgradeType;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.entities.XPOrb;
import nl.saxion.game.systems.CollisionHandler;
import nl.saxion.game.systems.DamageTextSystem;
import nl.saxion.game.systems.EnemySpawner;
import nl.saxion.game.systems.GameRenderer;
import nl.saxion.game.systems.GameStateManager;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.systems.MapRenderer;
import nl.saxion.game.systems.ResourceLoader;
import nl.saxion.game.ui.Button;
import nl.saxion.game.ui.HUD;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.game.utils.TMXMapData;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;
    private HUD hud;

    // Cursor management
    private Cursor cursorPointer; // For click/default state
    private Cursor cursorHover;   // For hover state
    private boolean isHoveringButton = false;

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
    private DamageTextSystem damageTextSystem;

    // Game state
    private float gameTime = 0f;
    private int score = 0;
    private float playerWorldX;
    private float playerWorldY;

    // Level up menu
    private boolean isLevelUpActive = false;
    private List<LevelUpOption> levelUpOptions = new ArrayList<>();

    public PlayScreen() {
        super(640, 360); // 16:9 aspect ratio - smaller world size for zoom effect (1.33x scale)
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
        damageTextSystem = new DamageTextSystem();

        // Link damage text system to collision handler
        collisionHandler.setDamageTextSystem(damageTextSystem);

        input = new InputController(MainGame.getConfig());
        hud = new HUD();

        // Load game over fonts - adjusted size for smaller world
        GameApp.addStyledFont("gameOverTitle", "fonts/Emulogic-zrEw.ttf", 72,
                "red-500", 2f, "black", 3, 3, "red-900", true);
        GameApp.addFont("gameOverText", "fonts/PressStart2P-Regular.ttf", 16, true);
        GameApp.addStyledFont("gameOverButtonFont", "fonts/PressStart2P-Regular.ttf", 14,
                "white", 1.5f, "black", 1, 1, "gray-600", true);

        // Load level font for XP bar (pixel-perfect for HUD, smaller to fit bar height)
        GameApp.addStyledFont("levelFont", "fonts/PressStart2P-Regular.ttf", 8,
                "white", 1f, "black", 1, 1, "gray-800", true);

        // Load score font (pixel-perfect for HUD, professional styling)
        GameApp.addStyledFont("scoreFont", "fonts/PressStart2P-Regular.ttf", 10,
                "white", 1.5f, "black", 2, 2, "gray-700", true);

        // Load timer font (Press Start 2P for timer display)
        GameApp.addFont("timerFont", "fonts/PressStart2P-Regular.ttf", 14, true);

        // Load damage font (PixelOperatorMono-Bold for damage numbers - smaller size)
        GameApp.addStyledFont("damageFont", "fonts/PixelOperatorMono-Bold.ttf", 13,
                "orange-500", 0.4f, "black", 1, 1, "black", true);

        // Load game over button sprites
        if (!GameApp.hasTexture("green_long")) {
            GameApp.addTexture("green_long", "assets/ui/green_long.png");
        }
        if (!GameApp.hasTexture("green_pressed_long")) {
            GameApp.addTexture("green_pressed_long", "assets/ui/green_pressed_long.png");
        }
        if (!GameApp.hasTexture("red_long")) {
            GameApp.addTexture("red_long", "assets/ui/red_long.png");
        }
        if (!GameApp.hasTexture("red_pressed_long")) {
            GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        }

        // Load cursors
        loadCursors();

        // Start game immediately (no splash screen - menu is handled by MainMenuScreen)
        gameStateManager.setCurrentState(GameState.PLAYING);

        resetGame();
    }

    // Load cursor images (pointer.png and cursor.png)
    private void loadCursors() {
        try {
            // Load pointer cursor (for click/default state)
            String pointerPath = "assets/ui/pointer.png";
            Pixmap pointerSource = new Pixmap(Gdx.files.internal(pointerPath));
            int pointerSourceWidth = pointerSource.getWidth();
            int pointerSourceHeight = pointerSource.getHeight();

            // Resize to 32x32
            int targetSize = 32;
            Pixmap pointerPixmap = new Pixmap(targetSize, targetSize, pointerSource.getFormat());
            pointerPixmap.drawPixmap(pointerSource,
                    0, 0, pointerSourceWidth, pointerSourceHeight,
                    0, 0, targetSize, targetSize);
            cursorPointer = Gdx.graphics.newCursor(pointerPixmap, 0, 0);
            pointerPixmap.dispose();
            pointerSource.dispose();

            // Load hover cursor (for hover state)
            String cursorPath = "assets/ui/cursor.png";
            Pixmap cursorSource = new Pixmap(Gdx.files.internal(cursorPath));
            int cursorSourceWidth = cursorSource.getWidth();
            int cursorSourceHeight = cursorSource.getHeight();

            // Resize to 32x32
            Pixmap cursorPixmap = new Pixmap(targetSize, targetSize, cursorSource.getFormat());
            cursorPixmap.drawPixmap(cursorSource,
                    0, 0, cursorSourceWidth, cursorSourceHeight,
                    0, 0, targetSize, targetSize);
            cursorHover = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
            cursorPixmap.dispose();
            cursorSource.dispose();

            // Set default to pointer
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            } else {
                GameApp.showCursor();
            }
        } catch (Exception e) {
            GameApp.log("Could not load cursors: " + e.getMessage());
            GameApp.showCursor(); // Fallback to default
        }
    }

    @Override
    public void hide() {
        // Dispose fonts
        GameApp.disposeFont("gameOverTitle");
        GameApp.disposeFont("gameOverText");
        GameApp.disposeFont("gameOverButtonFont");
        GameApp.disposeFont("levelFont");
        GameApp.disposeFont("scoreFont");
        GameApp.disposeFont("timerFont");
        GameApp.disposeFont("damageFont");

        // Dispose cursors
        if (cursorPointer != null) {
            cursorPointer.dispose();
            cursorPointer = null;
        }
        if (cursorHover != null) {
            cursorHover.dispose();
            cursorHover = null;
        }

        if (resourceLoader != null) {
            resourceLoader.disposeGameResources();
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // Handle game over state
        if (gameStateManager.getCurrentState() == GameState.GAME_OVER) {
            gameStateManager.updateGameOverFade(delta);

            // Initialize buttons if not already done
            gameStateManager.initializeGameOverButtons(
                    () -> {
                        // Play Again action
                        gameStateManager.resetGameOverFade();
                        resetGame();
                    },
                    () -> {
                        // Back to Menu action
                        GameApp.switchScreen("menu");
                    }
            );

            // Get mouse position and convert to world coordinates
            float mouseX = GameApp.getMousePositionInWindowX();
            float mouseY = GameApp.getMousePositionInWindowY();
            float screenWidth = GameApp.getWorldWidth();
            float screenHeight = GameApp.getWorldHeight();

            // Convert mouse coordinates from window to world
            // Get window size and scale mouse coordinates accordingly
            float windowWidth = GameApp.getWindowWidth();
            float windowHeight = GameApp.getWindowHeight();
            float scaleX = screenWidth / windowWidth;
            float scaleY = screenHeight / windowHeight;

            float worldMouseX = mouseX * scaleX;
            float worldMouseY = (windowHeight - mouseY) * scaleY; // Flip Y and scale

            // Debug: log conversion (only if debug enabled)
            if (GameApp.isButtonJustPressed(0)) {
                DebugLogger.log("Mouse conversion: window=(%.1f, %.1f) -> world=(%.1f, %.1f), scale=(%.3f, %.3f), windowSize=(%.0f, %.0f), worldSize=(%.0f, %.0f)",
                        mouseX, mouseY, worldMouseX, worldMouseY, scaleX, scaleY, windowWidth, windowHeight, screenWidth, screenHeight);
            }

            // Show cursor for mouse interaction
            GameApp.showCursor();

            // Handle cursor switching for game over buttons
            handleGameOverCursor(worldMouseX, worldMouseY);

            // Handle input with converted coordinates
            gameStateManager.handleGameOverInput(worldMouseX, worldMouseY,
                    () -> {
                        // Play Again
                        gameStateManager.resetGameOverFade();
                        resetGame();
                    },
                    () -> {
                        // Back to Menu
                        GameApp.switchScreen("menu");
                    }
            );

            gameStateManager.renderGameOverScreen();
            return;
        }

        // ----- GAMEPLAY STATE -----

        // Handle level up menu (pause game when active)
        if (isLevelUpActive) {
            // Get mouse position for cursor switching
            float mouseX = GameApp.getMousePositionInWindowX();
            float mouseY = GameApp.getMousePositionInWindowY();
            float screenWidth = GameApp.getWorldWidth();
            float screenHeight = GameApp.getWorldHeight();
            float windowWidth = GameApp.getWindowWidth();
            float windowHeight = GameApp.getWindowHeight();
            float scaleX = screenWidth / windowWidth;
            float scaleY = screenHeight / windowHeight;
            float worldMouseX = mouseX * scaleX;
            float worldMouseY = (windowHeight - mouseY) * scaleY;

            // Handle cursor switching for level up menu (though it's keyboard-only, show cursor)
            handleLevelUpCursor(worldMouseX, worldMouseY);

            handleLevelUpInput();
            // Still render game in background
            mapRenderer.render(playerWorldX, playerWorldY);
            GameApp.startSpriteRendering();
            gameRenderer.renderPlayer();
            gameRenderer.renderEnemies(enemies);
            gameRenderer.renderBullets(bullets);
            GameApp.endSpriteRendering();
            renderPlayerHealthBar();
            renderXPOrbs();
            renderHUD();
            renderLevelUpMenu();
            return; // Skip game updates
        }

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

        // Update XP orb animation
        GameApp.updateAnimation("orb_animation");

        // Enemy spawning
        enemySpawner.update(delta, gameTime, playerWorldX, playerWorldY, enemies);

        // Collision detection
        collisionHandler.update(delta);
        // Pass wall collision checker to prevent bullets hitting enemies through walls
        CollisionChecker wallChecker = mapRenderer::checkWallCollision;
        collisionHandler.handleBulletEnemyCollisions(bullets, enemies,
                (score) -> addScore(score),
                (enemy) -> spawnXPOrbsAtEnemy(enemy),
                wallChecker);
        collisionHandler.handleEnemyPlayerCollisions(player, enemies);

        // Update damage texts
        damageTextSystem.update(delta);

        // Update XP orbs
        updateXPOrbs(delta);

        // Check for level up
        if (!isLevelUpActive && player.checkLevelUp()) {
            showLevelUpMenu();
        }

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
                gameStateManager.resetGameOverFade(); // Reset fade for smooth transition
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

        GameApp.endSpriteRendering();

        // Render damage texts (after sprites, uses its own sprite batch)
        damageTextSystem.render(playerWorldX, playerWorldY);

        // Render health bar below player (uses shape rendering)
        renderPlayerHealthBar();

        // Render XP orbs (uses shape rendering)
        renderXPOrbs();

        // Render HUD after sprite rendering (HUD uses shapes and text)
        renderHUD();
    }

    // =========================
    // PLAYER STATUS / HUD
    // =========================

    public PlayerStatus getPlayerStatus() {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();
        int level = player.getCurrentLevel();
        int currentXP = player.getCurrentXP();
        int xpToNext = player.getXPToNextLevel();
        return new PlayerStatus(health, maxHealth, score, level, currentXP, xpToNext);
    }

    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    private void renderHUD() {
        PlayerStatus status = getPlayerStatus();
        hud.render(status, gameTime);
    }

    // Render health bar below player (like Vampire Survivors)
    private void renderPlayerHealthBar() {
        if (player == null) return;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Player is always at center of viewport
        float playerScreenX = worldW / 2f;
        float playerScreenY = worldH / 2f;

        // Health bar properties - smaller and closer to player
        float barWidth = 18f;  // Narrower bar
        float barHeight = 2f;  // Thinner bar
        float barOffsetY = 6f; // Closer to player

        // Calculate health percentage
        float hpPercent = player.getHealth() / (float) player.getMaxHealth();
        hpPercent = GameApp.clamp(hpPercent, 0f, 1f);

        // Position: centered below player
        float barX = playerScreenX - barWidth / 2f;
        float barY = playerScreenY - Player.SPRITE_SIZE / 2f - barOffsetY;

        GameApp.startShapeRenderingFilled();

        // Background (empty health) - dark red/black
        GameApp.setColor(60, 20, 20, 255);
        GameApp.drawRect(barX, barY, barWidth, barHeight);

        // Fill (current health) - bright red
        if (hpPercent > 0) {
            GameApp.setColor(220, 30, 30, 255); // Bright red
            GameApp.drawRect(barX, barY, barWidth * hpPercent, barHeight);
        }

        GameApp.endShapeRendering();

        // Border outline - subtle dark border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(1f);
        GameApp.setColor(120, 120, 120, 255); // Dark gray border
        GameApp.drawRect(barX, barY, barWidth, barHeight);
        GameApp.endShapeRendering();
    }

    // =========================
    // XP ORB SYSTEM
    // =========================

    // Spawn XP orbs at enemy position when enemy dies
    private void spawnXPOrbsAtEnemy(Enemy enemy) {
        // Spawn 1-3 orbs
        int orbCount = GameApp.randomInt(1, 4);
        for (int i = 0; i < orbCount; i++) {
            float offsetX = GameApp.random(-10f, 10f);
            float offsetY = GameApp.random(-10f, 10f);
            XPOrb orb = new XPOrb(enemy.getX() + offsetX, enemy.getY() + offsetY, 10);
            xpOrbs.add(orb);
        }
    }

    // Update XP orbs (magnet, collection, expiration)
    private void updateXPOrbs(float delta) {
        java.util.Iterator<XPOrb> it = xpOrbs.iterator();
        while (it.hasNext()) {
            XPOrb orb = it.next();

            // Update orb position and magnet
            orb.update(delta, player.getX(), player.getY());

            // Check if collected
            if (orb.isCollected()) {
                player.addXP(orb.getXPValue());
                it.remove();
                continue;
            }

            // Remove expired orbs
            if (orb.isExpired()) {
                it.remove();
            }
        }
    }

    // Render XP orbs
    private void renderXPOrbs() {
        GameApp.startSpriteRendering();
        for (XPOrb orb : xpOrbs) {
            orb.render(playerWorldX, playerWorldY);
        }
        GameApp.endSpriteRendering();
    }

    // =========================
    // LEVEL UP MENU
    // =========================

    // Show level up menu with 3 random options
    private void showLevelUpMenu() {
        isLevelUpActive = true;
        levelUpOptions.clear();

        // Generate 3 random stat upgrades
        StatUpgradeType[] allUpgrades = StatUpgradeType.values();
        List<StatUpgradeType> available = new ArrayList<>();
        for (StatUpgradeType upgrade : allUpgrades) {
            available.add(upgrade);
        }

        // Pick 3 random upgrades
        for (int i = 0; i < 3 && !available.isEmpty(); i++) {
            int index = GameApp.randomInt(0, available.size());
            levelUpOptions.add(new LevelUpOption(available.get(index)));
            available.remove(index);
        }
    }

    // Render level up menu
    private void renderLevelUpMenu() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // Draw semi-transparent background
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, 200);
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();

        // Draw menu
        GameApp.startSpriteRendering();

        // Title
        float titleY = centerY + 80f;
        GameApp.drawTextCentered("default", "LEVEL UP!", centerX, titleY, "yellow-500");

        // Options
        float optionStartY = centerY - 20f;
        float optionSpacing = 40f;

        for (int i = 0; i < levelUpOptions.size(); i++) {
            LevelUpOption option = levelUpOptions.get(i);
            float optionY = optionStartY - (i * optionSpacing);
            String text = String.format("[%d] %s - %s", i + 1, option.title, option.description);
            GameApp.drawTextCentered("default", text, centerX, optionY, "white");
        }

        GameApp.endSpriteRendering();
    }

    // Handle level up menu input
    private void handleLevelUpInput() {
        // Check for number key presses (1, 2, 3)
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_1) && levelUpOptions.size() > 0) {
            applyLevelUpOption(0);
        } else if (GameApp.isKeyJustPressed(Input.Keys.NUM_2) && levelUpOptions.size() > 1) {
            applyLevelUpOption(1);
        } else if (GameApp.isKeyJustPressed(Input.Keys.NUM_3) && levelUpOptions.size() > 2) {
            applyLevelUpOption(2);
        }
    }

    // Apply selected level up option
    private void applyLevelUpOption(int index) {
        if (index < 0 || index >= levelUpOptions.size()) return;

        LevelUpOption option = levelUpOptions.get(index);
        player.applyStatUpgrade(option.stat);
        player.levelUp();

        isLevelUpActive = false;
        levelUpOptions.clear();
    }

    // Handle cursor switching for game over screen
    private void handleGameOverCursor(float worldMouseX, float worldMouseY) {
        if (cursorPointer == null || cursorHover == null) return;

        // Check if hovering over any game over button
        boolean hoveringAnyButton = false;
        List<Button> gameOverButtons = gameStateManager.getGameOverButtons();
        if (gameOverButtons != null) {
            for (Button button : gameOverButtons) {
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    hoveringAnyButton = true;
                    break;
                }
            }
        }

        // Switch cursor based on hover state and click state
        boolean isMouseDown = GameApp.isButtonPressed(0);
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseDown || isMouseJustPressed) {
            // When clicking, use pointer cursor
            Gdx.graphics.setCursor(cursorPointer);
            isHoveringButton = false;
        } else if (hoveringAnyButton) {
            // Hovering over button - use hover cursor
            if (!isHoveringButton) {
                Gdx.graphics.setCursor(cursorHover);
                isHoveringButton = true;
            }
        } else {
            // Not hovering - use pointer cursor
            if (isHoveringButton) {
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringButton = false;
            }
        }
    }

    // Handle cursor switching for level up menu (show pointer cursor, no hover needed as it's keyboard-only)
    private void handleLevelUpCursor(float worldMouseX, float worldMouseY) {
        if (cursorPointer != null) {
            Gdx.graphics.setCursor(cursorPointer);
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
        // Weapon với random damage: 5-15 (enemy health 15, chết trong 1-3 hit)
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 5, 15, 400f, 10f, 10f);

        enemies = new ArrayList<>();
        xpOrbs = new ArrayList<>();
        isLevelUpActive = false;
        levelUpOptions.clear();

        // Reset game state
        gameTime = 0f;
        score = 0;
        enemySpawner.reset();
        collisionHandler.reset();
        damageTextSystem.reset();

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
