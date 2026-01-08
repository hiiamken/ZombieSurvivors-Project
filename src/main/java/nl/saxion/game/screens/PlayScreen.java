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
import nl.saxion.game.systems.SoundManager;
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
    private SoundManager soundManager;

    // Game state
    private float gameTime = 0f;
    private int score = 0;
    private float playerWorldX;
    private float playerWorldY;

    // Level up menu
    private boolean isLevelUpActive = false;
    private List<LevelUpOption> levelUpOptions = new ArrayList<>();
    
    // Ingame music delay
    private float ingameMusicDelayTimer = 0f;
    private static final float INGAME_MUSIC_DELAY = 1.2f;
    private boolean ingameMusicStarted = false;
    
    // Game over overlay
    private boolean isGameOver = false;
    private float gameOverFadeTimer = 0f;
    private static final float GAME_OVER_FADE_DURATION = 1.0f;
    private List<Button> gameOverButtons;
    private boolean gameOverButtonsInitialized = false;
    private float gameOverPressDelay = 0.15f;
    private float gameOverPressTimer = 0f;
    private Runnable gameOverPendingAction = null;
    private Button gameOverPressedButton = null;
    private boolean isHoveringGameOverButton = false;

    public PlayScreen() {
        super(640, 360); // 16:9 aspect ratio - smaller world size for zoom effect (1.33x scale)
    }

    @Override
    public void show() {
        // Initialize systems
        resourceLoader = new ResourceLoader();
        resourceLoader.loadGameResources();
        
        // Get SoundManager from ResourceLoader
        soundManager = resourceLoader.getSoundManager();

        Map<Integer, TMXMapData> tmxMapDataByRoomIndex = resourceLoader.loadTMXMaps();
        mapRenderer = new MapRenderer(tmxMapDataByRoomIndex);
        enemySpawner = new EnemySpawner();
        collisionHandler = new CollisionHandler();
        gameRenderer = new GameRenderer();
        gameStateManager = new GameStateManager();
        damageTextSystem = new DamageTextSystem();

        // Link damage text system to collision handler
        collisionHandler.setDamageTextSystem(damageTextSystem);
        
        // Link sound manager to collision handler for damage sounds
        if (soundManager != null) {
            collisionHandler.setSoundManager(soundManager);
        }
        
        // GameOverScreen is now a separate ScalableGameScreen, no need to initialize here
        
        // Stop menu music and start ingame music with delay
        // Delay allows clickbutton sound to play before music starts
        if (soundManager != null) {
            soundManager.stopMusic(); // Stop menu music
            // Reset delay timer for ingame music
            ingameMusicDelayTimer = 0f;
            ingameMusicStarted = false;
        }

        input = new InputController(MainGame.getConfig());
        hud = new HUD();

        // Load game over fonts - adjusted size for smaller world
        GameApp.addStyledFont("gameOverTitle", "fonts/Emulogic-zrEw.ttf", 72,
                "red-500", 2f, "black", 3, 3, "red-900", true);
        GameApp.addFont("gameOverText", "fonts/PressStart2P-Regular.ttf", 16, true);
        // Register custom button text colors for game over screen
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 47, 87, 83); // #2f5753
        }
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 79, 29, 76); // #4f1d4c
        }
        
        // Font size 22 for GameOverScreen
        GameApp.addStyledFont("gameOverButtonFont", "fonts/upheavtt.ttf", 19,
                "gray-200", 2f, "black", 2, 2, "gray-600", true);

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

        // Load game over background image
        if (!GameApp.hasTexture("gameover_bg")) {
            GameApp.addTexture("gameover_bg", "assets/ui/gameover.png");
        }

        // Load game over title image
        if (!GameApp.hasTexture("gameover_title")) {
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
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
        // Reset game over state when leaving screen
        isGameOver = false;
        gameOverFadeTimer = 0f;
        gameOverButtonsInitialized = false;
        gameOverPressTimer = 0f;
        gameOverPendingAction = null;
        gameOverPressedButton = null;
        isHoveringGameOverButton = false;
        if (gameOverButtons != null) {
            gameOverButtons.clear();
        }
        
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

        // Stop ingame music when leaving gameplay
        if (soundManager != null) {
            soundManager.stopIngameMusic();
        }
        
        if (resourceLoader != null) {
            resourceLoader.disposeGameResources();
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        
        // Handle F11 key to toggle fullscreen
        if (GameApp.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F11)) {
            toggleFullscreen();
        }

        GameApp.clearScreen("black");


        // ----- GAMEPLAY STATE -----
        
        // Handle ingame music delay (start music after delay to allow click sound)
        if (!ingameMusicStarted && soundManager != null) {
            ingameMusicDelayTimer += delta;
            if (ingameMusicDelayTimer >= INGAME_MUSIC_DELAY) {
                soundManager.playIngameMusic(true);
                ingameMusicStarted = true;
            }
        }

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
            java.util.List<Bullet> newBullets = weapon.tryFire(player, soundManager);
            if (newBullets != null) {
                bullets.addAll(newBullets);
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

        // Update zombie animations - Type 1
        GameApp.updateAnimation("zombie_idle");
        GameApp.updateAnimation("zombie_run");
        GameApp.updateAnimation("zombie_hit");
        GameApp.updateAnimation("zombie_death");

        // Update zombie animations - Type 3
        GameApp.updateAnimation("zombie3_idle");
        GameApp.updateAnimation("zombie3_run");
        GameApp.updateAnimation("zombie3_hit");
        GameApp.updateAnimation("zombie3_death");

        // Update zombie animations - Type 4
        GameApp.updateAnimation("zombie4_idle");
        GameApp.updateAnimation("zombie4_run");
        GameApp.updateAnimation("zombie4_hit");
        GameApp.updateAnimation("zombie4_death");

        // Update XP orb animation
        GameApp.updateAnimation("orb_animation");

        // Enemy spawning (spawn behind player like Vampire Survivors)
        float playerMoveDirX = player.getLastMoveDirectionX();
        float playerMoveDirY = player.getLastMoveDirectionY();
        enemySpawner.update(delta, gameTime, playerWorldX, playerWorldY, playerMoveDirX, playerMoveDirY, enemies);

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

        // Cleanup: remove dead enemies and enemies too far (soft despawn cleanup)
        collisionHandler.removeDeadOrFarEnemies(enemies, playerWorldX, playerWorldY);
        collisionHandler.removeDestroyedBullets(bullets);

        // Player death check - wait for death animation to finish
        // Guard: only trigger game over once (avoid multiple triggers)
        if (player.isDying() && !isGameOver) {
            // Only transition to game over after death animation completes
            if (player.isDeathAnimationFinished()) {
                GameApp.log("Death animation finished - showing game over overlay");
                
                // Stop ingame music smoothly and play game over sound
                if (soundManager != null) {
                    // Fade out ingame music smoothly before stopping
                    soundManager.setIngameMusicVolumeTemporary(0.0f);
                    // Small delay to allow music fade, then stop and play gameover sound
                    soundManager.stopIngameMusic();
                    soundManager.playSound("gameover", 0.3f); // Volume at 0.3f (30%)
                }
                
                // Initialize game over overlay
                isGameOver = true;
                gameOverFadeTimer = 0f;
                initializeGameOverButtons();
            }
        }
        
        // Pause game updates when game over (but still render)
        if (isGameOver) {
            // Update fade timer
            if (gameOverFadeTimer < GAME_OVER_FADE_DURATION) {
                gameOverFadeTimer += delta;
            }
            
            // Update button press delay timer
            if (gameOverPendingAction != null && gameOverPressedButton != null) {
                gameOverPressTimer += delta;
                if (gameOverPressTimer >= gameOverPressDelay) {
                    Runnable action = gameOverPendingAction;
                    gameOverPendingAction = null;
                    gameOverPressedButton = null;
                    gameOverPressTimer = 0f;
                    action.run();
                }
            }
            
            // Handle game over input
            handleGameOverInput();
        }

        // ----- RENDER -----
        // Render map background first (always render, even when game over)
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
        
        // Render game over overlay if game over
        if (isGameOver) {
            renderGameOverOverlay();
        }
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
                // Play pickup item sound at 10% volume
                if (soundManager != null) {
                    soundManager.playSound("pickupitem", 0.1f);
                }
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
        // Check if animation is available - if yes use sprite rendering, if no use shape rendering
        if (GameApp.hasAnimation("orb_animation")) {
            GameApp.startSpriteRendering();
            for (XPOrb orb : xpOrbs) {
                orb.renderWithAnimation(playerWorldX, playerWorldY);
            }
            GameApp.endSpriteRendering();
        } else {
            // Fallback: use shape rendering for circles
            GameApp.startShapeRenderingFilled();
            for (XPOrb orb : xpOrbs) {
                orb.renderWithCircle(playerWorldX, playerWorldY);
            }
            GameApp.endShapeRendering();
        }
    }

    // =========================
    // LEVEL UP MENU
    // =========================

    // Show level up menu with 3 random options
    private void showLevelUpMenu() {
        isLevelUpActive = true;
        levelUpOptions.clear();

        // Play level up sound
        if (soundManager != null) {
            soundManager.playSound("levelup", 1.0f);
        }
        
        // Reduce ingame music volume to 30% when level up menu is open
        if (soundManager != null) {
            soundManager.setIngameMusicVolumeTemporary(0.3f);
        }

        // Generate 3 random stat upgrades (exclude maxed upgrades)
        StatUpgradeType[] allUpgrades = StatUpgradeType.values();
        List<StatUpgradeType> available = new ArrayList<>();
        for (StatUpgradeType upgrade : allUpgrades) {
            // Only add if not maxed
            if (!player.isUpgradeMaxed(upgrade)) {
                available.add(upgrade);
            }
        }

        // If no upgrades available (all maxed), skip menu and just level up
        if (available.isEmpty()) {
            player.levelUp();
            isLevelUpActive = false;
            // Restore music volume
            if (soundManager != null) {
                soundManager.restoreIngameMusicVolume();
            }
            return;
        }

        // Pick 3 random upgrades (or as many as available)
        int optionsToPick = Math.min(3, available.size());
        for (int i = 0; i < optionsToPick && !available.isEmpty(); i++) {
            int index = GameApp.randomInt(0, available.size());
            StatUpgradeType selected = available.get(index);
            int currentLevel = player.getUpgradeLevel(selected);
            levelUpOptions.add(new LevelUpOption(selected, currentLevel));
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
        
        // Restore ingame music volume to normal after closing level up menu
        if (soundManager != null) {
            soundManager.restoreIngameMusicVolume();
        }
    }

    // Handle cursor switching for game over screen

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
        // Reset game state to PLAYING (important for Play Again button)
        gameStateManager.setCurrentState(GameState.PLAYING);
        
        // Reset game over overlay state
        isGameOver = false;
        gameOverFadeTimer = 0f;
        gameOverButtonsInitialized = false;
        gameOverPressTimer = 0f;
        gameOverPendingAction = null;
        gameOverPressedButton = null;
        isHoveringGameOverButton = false;
        if (gameOverButtons != null) {
            gameOverButtons.clear();
        }
        
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 10; // Increased base health

        player = new Player(startX, startY, speed, maxHealth, null);
        
        // Set health text callback for regen display
        player.setHealthTextCallback((amount, x, y) -> {
            damageTextSystem.spawnHealthText(x, y, amount);
        });

        bullets = new ArrayList<>();
        // Weapon với random damage: 5-15 (enemy health 15, chết trong 1-3 hit)
        // Increased fire rate from 1.5 to 2.5 shots per second for faster shooting
        // Fire rate 3.5 = fast shooting, damage 6-12, bullet speed 450
        // With 2 bullets per shot, effective DPS is very good for clearing hordes
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 3.5f, 6, 12, 450f, 10f, 10f);

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
        
        // Reset ingame music delay timer
        ingameMusicDelayTimer = 0f;
        ingameMusicStarted = false;

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
    
    // =========================
    // GAME OVER OVERLAY
    // =========================
    
    /**
     * Initialize game over buttons.
     */
    private void initializeGameOverButtons() {
        if (gameOverButtonsInitialized) return;
        
        if (gameOverButtons == null) {
            gameOverButtons = new ArrayList<>();
        }
        gameOverButtons.clear();
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Load button textures if not loaded
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
        
        // Calculate button size from texture dimensions
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        
        float buttonWidth = texW / 2f;
        float buttonHeight = texH / 2f;
        float buttonSpacing = 20f;
        
        // Play Again button (green)
        float playAgainY = centerY - 80;
        Button playAgainButton = new Button(centerX - buttonWidth / 2, playAgainY, buttonWidth, buttonHeight, "");
        playAgainButton.setOnClick(() -> {
            // Handled by handleGameOverInput with delay
        });
        if (GameApp.hasTexture("green_long")) {
            playAgainButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        gameOverButtons.add(playAgainButton);
        
        // Back to Menu button (red)
        float backToMenuY = playAgainY - buttonHeight - buttonSpacing;
        Button backToMenuButton = new Button(centerX - buttonWidth / 2, backToMenuY, buttonWidth, buttonHeight, "");
        backToMenuButton.setOnClick(() -> {
            // Handled by handleGameOverInput with delay
        });
        if (GameApp.hasTexture("red_long")) {
            backToMenuButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        gameOverButtons.add(backToMenuButton);
        
        gameOverButtonsInitialized = true;
    }
    
    /**
     * Handle input for game over overlay.
     */
    private void handleGameOverInput() {
        // Get mouse position in world coordinates
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        if (gameOverButtons == null || gameOverButtons.isEmpty()) return;
        
        // Check if hovering over any button for cursor switching
        boolean hoveringAnyButton = false;
        for (Button button : gameOverButtons) {
            if (button.containsPoint(worldMouseX, worldMouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }
        
        // Switch cursor based on hover state
        if (cursorPointer != null && cursorHover != null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
            
            if (isMouseDown || isMouseJustPressed) {
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringGameOverButton = false;
            } else if (hoveringAnyButton) {
                if (!isHoveringGameOverButton) {
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringGameOverButton = true;
                }
            } else {
                if (isHoveringGameOverButton) {
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringGameOverButton = false;
                }
            }
        }
        
        // Update buttons with world mouse position
        for (Button button : gameOverButtons) {
            button.update(worldMouseX, worldMouseY);
        }
        
        // Handle mouse click
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed && gameOverPendingAction == null) {
            for (int i = 0; i < gameOverButtons.size(); i++) {
                Button button = gameOverButtons.get(i);
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    // Play button click sound
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    // Store button and action for delayed execution
                    gameOverPressedButton = button;
                    button.setPressed(true);
                    
                    // Create delayed action based on button
                    if (i == 0) {
                        // Play Again button
                        gameOverPendingAction = () -> {
                            GameApp.switchScreen("play");
                        };
                    } else if (i == 1) {
                        // Back to Menu button
                        gameOverPendingAction = () -> {
                            GameApp.switchScreen("menu");
                        };
                    }
                    
                    gameOverPressTimer = 0f;
                    break;
                }
            }
        }
        
        // Update button pressed states
        if (gameOverPendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : gameOverButtons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        } else if (gameOverPressedButton != null) {
            gameOverPressedButton.setPressed(true);
        }
    }
    
    /**
     * Render game over overlay (dark screen + title + buttons).
     */
    private void renderGameOverOverlay() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Calculate fade alpha (0 to 1)
        float fadeAlpha = Math.min(gameOverFadeTimer / GAME_OVER_FADE_DURATION, 1.0f);
        
        // Draw dark overlay (semi-transparent black to darken the gameplay)
        // Make it very light so gameplay is clearly visible behind (subtle darkening effect)
        GameApp.enableTransparency(); // Enable transparency for semi-transparent overlay
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, (int)(fadeAlpha * 90)); // ~12% opacity - gameplay should be very clearly visible
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // Render buttons first (they manage their own sprite batch)
        if (gameOverButtons != null) {
            for (Button button : gameOverButtons) {
                button.render();
            }
        }
        
        // Render title and text in sprite batch
        GameApp.startSpriteRendering();
        
        // Draw "GAME OVER" title image
        renderGameOverTitle(centerX, centerY);
        
        // Draw score text
        String scoreText = String.format("SCORE: %,d", score);
        float titleY = centerY + 90;
        float titleHeight = 200f;
        try {
            if (GameApp.hasTexture("gameover_title")) {
                titleHeight = GameApp.getTextureHeight("gameover_title");
            }
        } catch (Exception e) {
            // Use default
        }
        float scoreTextHeight = GameApp.getTextHeight("gameOverText", scoreText);
        float scoreY = titleY - titleHeight / 2 - scoreTextHeight * 2.2f;
        
        // Load font and color if not loaded
        // Fonts should be loaded by ResourceLoader or already exist
        // Just use them if available
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 34, 139, 34); // #228b22
        }
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 79, 29, 76); // #4f1d4c
        }
        if (!GameApp.hasFont("gameOverButtonFont")) {
            // Font will be loaded by ResourceLoader or already exists
            // Just check if it exists, don't create it here
        }
        
        GameApp.drawTextCentered("gameOverText", scoreText, centerX, scoreY, "white");
        
        // Draw button text labels
        renderGameOverButtonText(centerX, centerY);
        
        GameApp.endSpriteRendering();
    }
    
    /**
     * Draw game over title image.
     */
    private void renderGameOverTitle(float centerX, float centerY) {
        if (!GameApp.hasTexture("gameover_title")) {
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
            if (!GameApp.hasTexture("gameover_title")) {
                return;
            }
        }
        
        float screenWidth = GameApp.getWorldWidth();
        float titleWidth = 600f;
        float titleHeight = 200f;
        
        try {
            int texWidth = GameApp.getTextureWidth("gameover_title");
            int texHeight = GameApp.getTextureHeight("gameover_title");
            if (texWidth > 0 && texHeight > 0) {
                float targetWidth = screenWidth * 0.6f;
                float aspectRatio = (float)texHeight / texWidth;
                titleWidth = targetWidth;
                titleHeight = targetWidth * aspectRatio;
            }
        } catch (Exception e) {
            // Use default
        }
        
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = centerY - 60f;
        
        GameApp.drawTexture("gameover_title", titleX, titleY, titleWidth, titleHeight);
    }
    
    /**
     * Draw button text labels.
     */
    private void renderGameOverButtonText(float centerX, float centerY) {
        if (gameOverButtons == null || gameOverButtons.size() < 2) return;
        
        // Play Again button text
        Button playAgainButton = gameOverButtons.get(0);
        float playAgainCenterX = playAgainButton.getX() + playAgainButton.getWidth() / 2;
        float playAgainCenterY = playAgainButton.getY() + playAgainButton.getHeight() / 2;
        float playAgainTextHeight = GameApp.getTextHeight("gameOverButtonFont", "PLAY AGAIN");
        float playAgainAdjustedY = playAgainCenterY + playAgainTextHeight * 0.15f;
        GameApp.drawTextCentered("gameOverButtonFont", "PLAY AGAIN", playAgainCenterX, playAgainAdjustedY, "gameover_play_again_color");
        
        // Back to Menu button text
        Button backToMenuButton = gameOverButtons.get(1);
        float backToMenuCenterX = backToMenuButton.getX() + backToMenuButton.getWidth() / 2;
        float backToMenuCenterY = backToMenuButton.getY() + backToMenuButton.getHeight() / 2;
        float backToMenuTextHeight = GameApp.getTextHeight("gameOverButtonFont", "BACK TO MENU");
        float backToMenuAdjustedY = backToMenuCenterY + backToMenuTextHeight * 0.15f;
        GameApp.drawTextCentered("gameOverButtonFont", "BACK TO MENU", backToMenuCenterX, backToMenuAdjustedY, "gameover_back_menu_color");
    }
    
    // Toggle fullscreen and update config
    private void toggleFullscreen() {
        nl.saxion.game.config.GameConfig config = nl.saxion.game.config.ConfigManager.loadConfig();
        config.fullscreen = !config.fullscreen;
        nl.saxion.game.config.ConfigManager.saveConfig(config);
        
        if (config.fullscreen) {
            com.badlogic.gdx.Gdx.graphics.setFullscreenMode(com.badlogic.gdx.Gdx.graphics.getDisplayMode());
        } else {
            com.badlogic.gdx.Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
}
