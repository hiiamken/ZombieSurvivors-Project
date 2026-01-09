package nl.saxion.game.screens;


import nl.saxion.game.MainGame;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerData;
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
import nl.saxion.game.systems.LeaderboardManager;
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
import nl.saxion.game.entities.Boss;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayScreen extends ScalableGameScreen {

    // Static flag to track if returning from settings screen (to preserve pause state)
    private static boolean returningFromSettings = false;
    private static boolean wasPausedBeforeSettings = false;
    
    // Static game state preservation for settings return
    private static Player savedPlayer = null;
    private static Weapon savedWeapon = null;
    private static List<Bullet> savedBullets = null;
    private static List<Enemy> savedEnemies = null;
    private static List<Boss> savedBosses = null;
    private static List<XPOrb> savedXpOrbs = null;

    private static float savedGameTime = 0f;
    private static int savedScore = 0;
    private static float savedPlayerWorldX = 0f;
    private static float savedPlayerWorldY = 0f;
    private static boolean savedIngameMusicStarted = false;
    private static boolean savedBossSpawned = false;

    // Boss system
    private List<Boss> bosses;
    private boolean bossSpawned = false;
    private static final float BOSS_SPAWN_TIME = 10f;
    
    /**
     * Set flag to indicate returning from settings.
     */
    public static void setReturningFromSettings(boolean value) {
        returningFromSettings = value;
    }
    
    /**
     * Set the paused state before going to settings.
     */
    public static void setWasPausedBeforeSettings(boolean value) {
        wasPausedBeforeSettings = value;
    }
    
    /**
     * Save current game state before going to settings.
     */
    private void saveGameState() {
        savedPlayer = player;
        savedWeapon = weapon;
        savedBullets = bullets;
        savedEnemies = enemies;
        savedXpOrbs = xpOrbs;
        savedGameTime = gameTime;
        savedScore = score;
        savedPlayerWorldX = playerWorldX;
        savedPlayerWorldY = playerWorldY;
        savedIngameMusicStarted = ingameMusicStarted;
        savedBosses = bosses;
        savedBossSpawned = bossSpawned;

    }
    
    /**
     * Restore game state after returning from settings.
     */
    private void restoreGameState() {
        if (savedPlayer != null) {
            player = savedPlayer;
            weapon = savedWeapon;
            bullets = savedBullets;
            enemies = savedEnemies;
            bosses = savedBosses;
            bossSpawned = savedBossSpawned;
            xpOrbs = savedXpOrbs;
            gameTime = savedGameTime;
            score = savedScore;
            playerWorldX = savedPlayerWorldX;
            playerWorldY = savedPlayerWorldY;
            ingameMusicStarted = savedIngameMusicStarted;
            
            // Pass player reference to renderer
            if (gameRenderer != null) {
                gameRenderer.setPlayer(player);
                gameRenderer.setPlayerWorldPosition(playerWorldX, playerWorldY);
            }
        }
        
        // Clear saved state
        savedPlayer = null;
        savedWeapon = null;
        savedBullets = null;
        savedEnemies = null;
        savedXpOrbs = null;
        savedBosses = null;

    }

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
    private static final float GAME_DURATION = 900f; // 10 minutes countdown
    private float gameTime = GAME_DURATION;
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
    private boolean scoreSaved = false; // Flag to prevent saving score twice
    private List<Button> gameOverButtons;
    private boolean gameOverButtonsInitialized = false;
    private float gameOverPressDelay = 0.15f;
    private float gameOverPressTimer = 0f;
    private Runnable gameOverPendingAction = null;
    private Button gameOverPressedButton = null;
    private boolean isHoveringGameOverButton = false;
    
    // Pause menu overlay
    private boolean isPaused = false;
    private float pauseFadeTimer = 0f;
    private static final float PAUSE_FADE_DURATION = 0.3f;
    private List<Button> pauseButtons;
    private boolean pauseButtonsInitialized = false;
    private float pausePressDelay = 0.15f;
    private float pausePressTimer = 0f;
    private Runnable pausePendingAction = null;
    private Button pausePressedButton = null;
    private boolean isHoveringPauseButton = false;

    public PlayScreen() {
        super(960, 540); // 16:9 aspect ratio - larger world size for zoomed out view (can see more of the map)
    }

    @Override
    public void show() {
        // Check if returning from settings - preserve game state
        if (returningFromSettings && savedPlayer != null) {
            returningFromSettings = false;
            
            // Re-initialize systems (resources need to be reloaded)
            resourceLoader = new ResourceLoader();
            resourceLoader.loadGameResources();
            soundManager = resourceLoader.getSoundManager();
            
            Map<Integer, TMXMapData> tmxMapDataByRoomIndex = resourceLoader.loadTMXMaps();
            mapRenderer = new MapRenderer(tmxMapDataByRoomIndex);
            enemySpawner = new EnemySpawner();
            collisionHandler = new CollisionHandler();
            gameRenderer = new GameRenderer();
            gameStateManager = new GameStateManager();
            damageTextSystem = new DamageTextSystem();
            
            collisionHandler.setDamageTextSystem(damageTextSystem);
            if (soundManager != null) {
                collisionHandler.setSoundManager(soundManager);
            }
            
            input = new InputController(MainGame.getConfig());
            hud = new HUD();
            
            // Load fonts and textures
            loadFontsAndTextures();
            
            // Load cursors
            loadCursors();
            
            // Restore game state
            restoreGameState();
            
            // Set game state to PLAYING
            gameStateManager.setCurrentState(GameState.PLAYING);
            
            // Restore pause state
            if (wasPausedBeforeSettings) {
                isPaused = true;
                pauseButtonsInitialized = false;
                initializePauseButtons();
                
                // Keep music at reduced volume since we're still paused
                if (soundManager != null) {
                    // Start ingame music again at reduced volume
                    soundManager.playIngameMusic(true);
                    soundManager.setIngameMusicVolumeTemporary(0.3f);
                }
            }
            wasPausedBeforeSettings = false;
            
            return; // Skip full initialization
        }
        
        // Reset flags if not returning from settings
        returningFromSettings = false;
        wasPausedBeforeSettings = false;
        
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
        // Register unified button text colors for all menus
        // GREEN button text - dark gray for best contrast on bright green
        if (!GameApp.hasColor("button_green_text")) {
            GameApp.addColor("button_green_text", 25, 50, 25); // Dark green-gray
        }
        // ORANGE button text - dark brown for best contrast on orange
        if (!GameApp.hasColor("button_orange_text")) {
            GameApp.addColor("button_orange_text", 70, 30, 10); // Dark brown
        }
        // RED button text - dark maroon for best contrast on red/pink
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30); // Dark maroon
        }
        
        // Legacy colors for game over screen (use unified colors)
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 25, 50, 25); // Same as green
        }
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 60, 15, 30); // Same as red
        }
        
        // Font size 22 for GameOverScreen
        GameApp.addStyledFont("gameOverButtonFont", "fonts/upheavtt.ttf", 19,
                "gray-200", 2f, "black", 2, 2, "gray-600", true);

        // Load level font for XP bar (scaled up for 960x540 world view)
        GameApp.addStyledFont("levelFont", "fonts/PressStart2P-Regular.ttf", 12,
                "white", 1f, "black", 1, 1, "gray-800", true);

        // Load score font (scaled up for better visibility)
        GameApp.addStyledFont("scoreFont", "fonts/PressStart2P-Regular.ttf", 14,
                "white", 1.5f, "black", 2, 2, "gray-700", true);

        // Load timer font (larger for zoomed out view)
        GameApp.addFont("timerFont", "fonts/PressStart2P-Regular.ttf", 20, true);

        // Load damage font (larger for better visibility when zoomed out)
        GameApp.addStyledFont("damageFont", "fonts/PixelOperatorMono-Bold.ttf", 18,
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
        
        // Load orange button sprites for pause menu settings button
        if (!GameApp.hasTexture("orange_long")) {
            GameApp.addTexture("orange_long", "assets/ui/orange_long.png");
        }
        if (!GameApp.hasTexture("orange_pressed_long")) {
            GameApp.addTexture("orange_pressed_long", "assets/ui/orange_pressed_long.png");
        }
        
        // Pause menu uses unified button colors
        if (!GameApp.hasColor("pause_resume_color")) {
            GameApp.addColor("pause_resume_color", 25, 50, 25); // Same as button_green_text
        }
        if (!GameApp.hasColor("pause_settings_color")) {
            GameApp.addColor("pause_settings_color", 70, 30, 10); // Same as button_orange_text
        }
        if (!GameApp.hasColor("pause_quit_color")) {
            GameApp.addColor("pause_quit_color", 60, 15, 30); // Same as button_red_text
        }
        
        // Button font for PlayScreen (640x360 world, half-size buttons)
        // Size 18 for better readability
        GameApp.addStyledFont("buttonFontSmall", "fonts/upheavtt.ttf", 20,
                "white", 0f, "black", 1, 1, "gray-700", true);

        // Load game over background image
        if (!GameApp.hasTexture("gameover_bg")) {
            GameApp.addTexture("gameover_bg", "assets/ui/gameover.png");
        }

        // Load game over title image
        if (!GameApp.hasTexture("gameover_title")) {
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
        }
        
        // Load pause title image
        if (!GameApp.hasTexture("pause_title")) {
            GameApp.addTexture("pause_title", "assets/ui/pause.png");
        }

        // Load cursors
        loadCursors();

        // Start game immediately (no splash screen - menu is handled by MainMenuScreen)
        gameStateManager.setCurrentState(GameState.PLAYING);

        resetGame();
    }

    /**
     * Load fonts and textures needed for the play screen.
     */
    private void loadFontsAndTextures() {
        // Load game over fonts - adjusted size for smaller world
        GameApp.addStyledFont("gameOverTitle", "fonts/Emulogic-zrEw.ttf", 72,
                "red-500", 2f, "black", 3, 3, "red-900", true);
        GameApp.addFont("gameOverText", "fonts/PressStart2P-Regular.ttf", 16, true);
        
        // Register custom button text colors for game over screen
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 47, 87, 83);
        }
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 79, 29, 76);
        }
        
        // Font for buttons
        GameApp.addStyledFont("gameOverButtonFont", "fonts/upheavtt.ttf", 19,
                "gray-200", 2f, "black", 2, 2, "gray-600", true);

        // Load level font for XP bar (scaled up for 960x540 world view)
        GameApp.addStyledFont("levelFont", "fonts/PressStart2P-Regular.ttf", 12,
                "white", 1f, "black", 1, 1, "gray-800", true);

        // Load score font (scaled up for better visibility)
        GameApp.addStyledFont("scoreFont", "fonts/PressStart2P-Regular.ttf", 14,
                "white", 1.5f, "black", 2, 2, "gray-700", true);

        // Load timer font (larger for zoomed out view)
        GameApp.addFont("timerFont", "fonts/PressStart2P-Regular.ttf", 20, true);

        // Load damage font (larger for better visibility when zoomed out)
        GameApp.addStyledFont("damageFont", "fonts/PixelOperatorMono-Bold.ttf", 18,
                "orange-500", 0.4f, "black", 1, 1, "black", true);

        // Load button sprites
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
        if (!GameApp.hasTexture("orange_long")) {
            GameApp.addTexture("orange_long", "assets/ui/orange_long.png");
        }
        if (!GameApp.hasTexture("orange_pressed_long")) {
            GameApp.addTexture("orange_pressed_long", "assets/ui/orange_pressed_long.png");
        }
        
        // Unified button colors for all menus
        if (!GameApp.hasColor("button_green_text")) {
            GameApp.addColor("button_green_text", 25, 50, 25);
        }
        if (!GameApp.hasColor("button_orange_text")) {
            GameApp.addColor("button_orange_text", 70, 30, 10);
        }
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        if (!GameApp.hasColor("pause_resume_color")) {
            GameApp.addColor("pause_resume_color", 25, 50, 25);
        }
        if (!GameApp.hasColor("pause_settings_color")) {
            GameApp.addColor("pause_settings_color", 70, 30, 10);
        }
        if (!GameApp.hasColor("pause_quit_color")) {
            GameApp.addColor("pause_quit_color", 60, 15, 30);
        }
        
        // Button font for PlayScreen (640x360 world, half-size buttons)
        GameApp.addStyledFont("buttonFontSmall", "fonts/upheavtt.ttf", 20,
                "white", 0f, "black", 1, 1, "gray-700", true);

        // Load game over background image
        if (!GameApp.hasTexture("gameover_bg")) {
            GameApp.addTexture("gameover_bg", "assets/ui/gameover.png");
        }
        if (!GameApp.hasTexture("gameover_title")) {
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
        }
        
        // Load pause title image
        if (!GameApp.hasTexture("pause_title")) {
            GameApp.addTexture("pause_title", "assets/ui/pause.png");
        }
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
        
        // Reset pause state when leaving screen
        isPaused = false;
        pauseFadeTimer = 0f;
        pauseButtonsInitialized = false;
        pausePressTimer = 0f;
        pausePendingAction = null;
        pausePressedButton = null;
        isHoveringPauseButton = false;
        if (pauseButtons != null) {
            pauseButtons.clear();
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
        
        // Handle ESC key to toggle pause menu (only when not game over and not level up)
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE) && !isGameOver && !isLevelUpActive) {
            togglePause();
        }

        GameApp.clearScreen("black");


        // ----- GAMEPLAY STATE -----
        
        // Handle ingame music delay (start music after delay to allow click sound)
        if (!ingameMusicStarted && soundManager != null && !isPaused) {
            ingameMusicDelayTimer += delta;
            if (ingameMusicDelayTimer >= INGAME_MUSIC_DELAY) {
                soundManager.playIngameMusic(true);
                ingameMusicStarted = true;
            }
        }
        
        // Handle pause menu (pause game when active)
        if (isPaused) {
            // Update pause fade timer
            if (pauseFadeTimer < PAUSE_FADE_DURATION) {
                pauseFadeTimer += delta;
            }
            
            // Update button press delay timer
            if (pausePendingAction != null && pausePressedButton != null) {
                pausePressTimer += delta;
                if (pausePressTimer >= pausePressDelay) {
                    Runnable action = pausePendingAction;
                    pausePendingAction = null;
                    pausePressedButton = null;
                    pausePressTimer = 0f;
                    action.run();
                }
            }
            
            // Handle pause menu input
            handlePauseInput();
            
            // Still render game in background (frozen)
            mapRenderer.render(playerWorldX, playerWorldY);
            GameApp.startSpriteRendering();
            gameRenderer.renderPlayer();
            gameRenderer.renderEnemies(enemies);
            gameRenderer.renderBosses(bosses);
            gameRenderer.renderBullets(bullets);
            GameApp.endSpriteRendering();
            renderPlayerHealthBar();
            renderXPOrbs();
            renderHUD();
            
            // Render pause overlay on top
            renderPauseOverlay();
            return; // Skip game updates
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
            gameRenderer.renderBosses(bosses);
            gameRenderer.renderBullets(bullets);
            GameApp.endSpriteRendering();
            renderPlayerHealthBar();
            renderXPOrbs();
            renderHUD();
            renderLevelUpMenu();
            return; // Skip game updates
        }

        // Update countdown timer (only if not game over)
        if (!isGameOver && gameTime > 0) {
            gameTime -= delta;
            
            // Check if time ran out
            if (gameTime <= 0) {
                gameTime = 0;
                // Trigger game over when time runs out
                if (!player.isDying()) {
                    GameApp.log("Time ran out - showing game over overlay");
                    if (soundManager != null) {
                        soundManager.stopIngameMusic();
                        soundManager.playSound("gameover");
                    }
                    isGameOver = true;
                    gameOverFadeTimer = 0f;
                    initializeGameOverButtons();
                    
                    // Save score to leaderboard
                    saveScoreToLeaderboard();
                }
            }
        }

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
        for (Boss boss : bosses) {
            boss.update(delta, player.getX(), player.getY());
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

        // Update boss animations
        GameApp.updateAnimation("boss_idle");
        GameApp.updateAnimation("boss_run");
        GameApp.updateAnimation("boss_attack");
        GameApp.updateAnimation("boss_death");


        // Update XP orb animation
        GameApp.updateAnimation("orb_animation");

        // Enemy spawning (spawn behind player like Vampire Survivors)
        float playerMoveDirX = player.getLastMoveDirectionX();
        float playerMoveDirY = player.getLastMoveDirectionY();
        // Pass elapsed time (not countdown) for difficulty scaling
        float elapsedTime = GAME_DURATION - gameTime;

        if (!bossSpawned && elapsedTime >= BOSS_SPAWN_TIME) {
            spawnBossNearPlayer(playerWorldX, playerWorldY, playerMoveDirX, playerMoveDirY);
        }
        enemySpawner.update(delta, elapsedTime, playerWorldX, playerWorldY, playerMoveDirX, playerMoveDirY, enemies);

        // Collision detection
        collisionHandler.update(delta);
        // Pass wall collision checker to prevent bullets hitting enemies through walls
        CollisionChecker wallChecker = mapRenderer::checkWallCollision;
        collisionHandler.handleBulletEnemyCollisions(bullets, enemies,
                (score) -> addScore(score),
                (enemy) -> spawnXPOrbsAtEnemy(enemy),
                wallChecker);
        collisionHandler.handleEnemyPlayerCollisions(player, enemies);

        collisionHandler.handleBulletBossCollisions(
                bullets,
                bosses,
                (Integer s) -> addScore(s),
                (Boss boss) -> spawnXPOrbsAtBoss(boss),
                wallChecker
        );

        collisionHandler.handleBossPlayerCollisions(player, bosses);


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
        bosses.removeIf(boss -> !boss.isAlive());


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
                
                // Save score to leaderboard
                saveScoreToLeaderboard();
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
        gameRenderer.renderBosses(bosses);
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

        // Health bar properties - scaled for larger sprite (36px)
        float barWidth = 28f;  // Wider bar for larger sprite
        float barHeight = 3f;  // Slightly thicker bar
        float barOffsetY = 10f; // Offset below player

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
    private void spawnXPOrbsAtBoss(Boss boss) {
        int orbCount = GameApp.randomInt(12, 21); // много, потому что босс
        for (int i = 0; i < orbCount; i++) {
            float offsetX = GameApp.random(-30f, 30f);
            float offsetY = GameApp.random(-30f, 30f);
            XPOrb orb = new XPOrb(boss.getX() + offsetX, boss.getY() + offsetY, 10);
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
    // SCORE SAVING
    // =========================
    
    /**
     * Save the current score to the leaderboard.
     * Called when the game ends (player dies or time runs out).
     */
    private void saveScoreToLeaderboard() {
        // Only save once per game
        if (scoreSaved) {
            GameApp.log("Score already saved for this game session");
            return;
        }
        
        // Check if we have player data
        PlayerData currentPlayer = PlayerData.getCurrentPlayer();
        if (currentPlayer == null) {
            GameApp.log("No player data found - score not saved to leaderboard");
            return;
        }
        
        // Calculate survival time (how long they survived)
        float survivalTime = GAME_DURATION - gameTime; // Time played before dying/timeout
        
        // Save to leaderboard
        LeaderboardManager.addEntry(currentPlayer, score, survivalTime);
        scoreSaved = true;
        
        GameApp.log(String.format("Score saved: %s - %d points, survived %.1f seconds", 
            currentPlayer.getUsername(), score, survivalTime));
    }

    // =========================
    // GAME FLOW / RESET
    // =========================

    private void resetGame() {
        // Reset game state to PLAYING (important for Play Again button)
        gameStateManager.setCurrentState(GameState.PLAYING);
        
        // Reset score saved flag for new game
        scoreSaved = false;
        
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
        
        // Reset pause overlay state
        isPaused = false;
        pauseFadeTimer = 0f;
        pauseButtonsInitialized = false;
        pausePressTimer = 0f;
        pausePendingAction = null;
        pausePressedButton = null;
        isHoveringPauseButton = false;
        if (pauseButtons != null) {
            pauseButtons.clear();
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
        // Bullet size increased from 10 to 14 for better visibility with zoomed out view
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 3.5f, 6, 12, 450f, 14f, 14f);

        enemies = new ArrayList<>();
        bosses = new ArrayList<>();
        xpOrbs = new ArrayList<>();

        isLevelUpActive = false;
        bossSpawned = false;
        levelUpOptions.clear();

        // Reset game state
        gameTime = GAME_DURATION;
        score = 0;
        enemySpawner.reset();
        collisionHandler.reset();
        damageTextSystem.reset();
        
        // Reset ingame music delay timer
        ingameMusicDelayTimer = 0f;
        ingameMusicStarted = false;

        // Set initial player world position - RANDOM ROOM each game
        // Pick a random room from 0-15 (4x4 grid)
        int randomRoomIndex = GameApp.randomInt(0, 16); // 0 to 15
        int roomRow = randomRoomIndex / 4; // 0-3
        int roomCol = randomRoomIndex % 4; // 0-3
        
        // Calculate world position at center of the random room
        playerWorldX = roomCol * MapRenderer.getMapTileWidth() + MapRenderer.getMapTileWidth() / 2f;
        playerWorldY = roomRow * MapRenderer.getMapTileHeight() + MapRenderer.getMapTileHeight() / 2f;
        
        GameApp.log("Starting in random room " + randomRoomIndex + " (row=" + roomRow + ", col=" + roomCol + ")");

        // Check and adjust if spawn position has wall
        if (mapRenderer != null) {
            TMXMapData spawnMapData = mapRenderer.getTMXDataForPosition(playerWorldX, playerWorldY);
            if (spawnMapData != null) {
                Rectangle testHitbox = new Rectangle((int)playerWorldX, (int)playerWorldY, 16, 16);
                if (mapRenderer.checkWallCollision(testHitbox.x, testHitbox.y, testHitbox.width, testHitbox.height)) {
                    // Calculate bounds for the current room
                    float roomMinX = roomCol * MapRenderer.getMapTileWidth();
                    float roomMaxX = roomMinX + MapRenderer.getMapTileWidth();
                    float roomMinY = roomRow * MapRenderer.getMapTileHeight();
                    float roomMaxY = roomMinY + MapRenderer.getMapTileHeight();

                    for (int offset = 50; offset < 300; offset += 50) {

                        for (int dx = -offset; dx <= offset; dx += 50) {
                            for (int dy = -offset; dy <= offset; dy += 50) {
                                float testX = playerWorldX + dx;
                                float testY = playerWorldY + dy;
                                // Stay within the current room bounds
                                if (testX >= roomMinX && testX < roomMaxX &&
                                        testY >= roomMinY && testY < roomMaxY) {
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

        // Reset enemies - spawn a few at screen edges (outside visible area)
        enemies.clear();
        float enemyBaseSpeed = enemySpawner.getEnemyBaseSpeed();
        int enemyBaseHealth = enemySpawner.getEnemyBaseHealth();
        // Spawn at screen edges (550-600 distance) to create feeling of zombies entering from outside
        enemies.add(new Enemy(playerWorldX + 550, playerWorldY + 100, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX - 520, playerWorldY - 80, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 100, playerWorldY + 530, enemyBaseSpeed, enemyBaseHealth));

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
        
        // Draw score and stats text
        float titleY = centerY + 90;
        float titleHeight = 200f;
        try {
            if (GameApp.hasTexture("gameover_title")) {
                titleHeight = GameApp.getTextureHeight("gameover_title");
            }
        } catch (Exception e) {
            // Use default
        }
        
        // Calculate survival time
        float survivalTime = GAME_DURATION - gameTime;
        int survivalMinutes = (int) survivalTime / 60;
        int survivalSeconds = (int) survivalTime % 60;
        String survivalTimeStr = String.format("%02d:%02d", survivalMinutes, survivalSeconds);
        
        // Get player name for display
        String playerName = "PLAYER";
        PlayerData currentPlayer = PlayerData.getCurrentPlayer();
        if (currentPlayer != null) {
            playerName = currentPlayer.getUsername().toUpperCase();
        }
        
        // Get rank
        int rank = LeaderboardManager.getRank(score, survivalTime);
        String rankText = "#" + rank;
        
        // Load font and color if not loaded
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 34, 139, 34);
        }
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 79, 29, 76);
        }
        
        // Display stats below title
        float statsStartY = titleY - titleHeight / 2 - 20f;
        float statsSpacing = 25f;
        
        // Player name
        GameApp.drawTextCentered("gameOverText", playerName, centerX, statsStartY, "yellow-400");
        
        // Score
        String scoreText = String.format("SCORE: %,d", score);
        GameApp.drawTextCentered("gameOverText", scoreText, centerX, statsStartY - statsSpacing, "white");
        
        // Survival time
        String timeText = "TIME: " + survivalTimeStr;
        GameApp.drawTextCentered("gameOverText", timeText, centerX, statsStartY - statsSpacing * 2, "green-400");
        
        // Rank
        String rankDisplayText = "RANK: " + rankText;
        String rankColor = "white";
        if (rank == 1) rankColor = "yellow-400";
        else if (rank == 2) rankColor = "gray-300";
        else if (rank == 3) rankColor = "orange-400";
        GameApp.drawTextCentered("gameOverText", rankDisplayText, centerX, statsStartY - statsSpacing * 3, rankColor);
        
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
     * Using unified colors for all buttons.
     */
    private void renderGameOverButtonText(float centerX, float centerY) {
        if (gameOverButtons == null || gameOverButtons.size() < 2) return;
        
        String fontName = "buttonFontSmall"; // Small font for 640x360 world
        
        // Play Again button text (green button)
        Button playAgainButton = gameOverButtons.get(0);
        float playAgainCenterX = playAgainButton.getX() + playAgainButton.getWidth() / 2;
        float playAgainCenterY = playAgainButton.getY() + playAgainButton.getHeight() / 2;
        float playAgainTextHeight = GameApp.getTextHeight(fontName, "PLAY AGAIN");
        float playAgainAdjustedY = playAgainCenterY + playAgainTextHeight * 0.15f;
        GameApp.drawTextCentered(fontName, "PLAY AGAIN", playAgainCenterX, playAgainAdjustedY, "button_green_text");
        
        // Back to Menu button text (red button)
        Button backToMenuButton = gameOverButtons.get(1);
        float backToMenuCenterX = backToMenuButton.getX() + backToMenuButton.getWidth() / 2;
        float backToMenuCenterY = backToMenuButton.getY() + backToMenuButton.getHeight() / 2;
        float backToMenuTextHeight = GameApp.getTextHeight(fontName, "BACK TO MENU");
        float backToMenuAdjustedY = backToMenuCenterY + backToMenuTextHeight * 0.15f;
        GameApp.drawTextCentered(fontName, "BACK TO MENU", backToMenuCenterX, backToMenuAdjustedY, "button_red_text");
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
    
    // =========================
    // PAUSE MENU
    // =========================
    
    /**
     * Toggle pause state.
     */
    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            // Entering pause
            pauseFadeTimer = 0f;
            initializePauseButtons();
            
            // Reduce ingame music volume when paused
            if (soundManager != null) {
                soundManager.setIngameMusicVolumeTemporary(0.3f);
            }
        } else {
            // Resuming game
            pauseFadeTimer = 0f;
            pausePendingAction = null;
            pausePressedButton = null;
            pausePressTimer = 0f;
            
            // Restore ingame music volume
            if (soundManager != null) {
                soundManager.restoreIngameMusicVolume();
            }
        }
    }
    
    /**
     * Initialize pause menu buttons.
     */
    private void initializePauseButtons() {
        if (pauseButtonsInitialized) return;
        
        if (pauseButtons == null) {
            pauseButtons = new ArrayList<>();
        }
        pauseButtons.clear();
        
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
        if (!GameApp.hasTexture("orange_long")) {
            GameApp.addTexture("orange_long", "assets/ui/orange_long.png");
        }
        if (!GameApp.hasTexture("orange_pressed_long")) {
            GameApp.addTexture("orange_pressed_long", "assets/ui/orange_pressed_long.png");
        }
        if (!GameApp.hasTexture("red_long")) {
            GameApp.addTexture("red_long", "assets/ui/red_long.png");
        }
        if (!GameApp.hasTexture("red_pressed_long")) {
            GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        }
        
        // Load pause title texture if not loaded
        if (!GameApp.hasTexture("pause_title")) {
            GameApp.addTexture("pause_title", "assets/ui/pause.png");
        }
        
        // Calculate button size from texture dimensions
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        
        float buttonWidth = texW / 2f;
        float buttonHeight = texH / 2f;
        float buttonSpacing = 8f; // Reduced spacing for closer buttons
        
        // Position buttons below center to leave room for title above
        // Resume button starts at centerY - 30, then Settings and Quit below it
        float startY = centerY - 30f;
        
        // Resume button (green) - top
        float resumeY = startY;
        Button resumeButton = new Button(centerX - buttonWidth / 2, resumeY, buttonWidth, buttonHeight, "");
        resumeButton.setOnClick(() -> {
            // Handled by handlePauseInput with delay
        });
        if (GameApp.hasTexture("green_long")) {
            resumeButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        pauseButtons.add(resumeButton);
        
        // Settings button (orange) - middle
        float settingsY = resumeY - buttonHeight - buttonSpacing;
        Button settingsButton = new Button(centerX - buttonWidth / 2, settingsY, buttonWidth, buttonHeight, "");
        settingsButton.setOnClick(() -> {
            // Handled by handlePauseInput with delay
        });
        if (GameApp.hasTexture("orange_long")) {
            settingsButton.setSprites("orange_long", "orange_long", "orange_long", "orange_pressed_long");
        }
        pauseButtons.add(settingsButton);
        
        // Quit button (red) - bottom
        float quitY = settingsY - buttonHeight - buttonSpacing;
        Button quitButton = new Button(centerX - buttonWidth / 2, quitY, buttonWidth, buttonHeight, "");
        quitButton.setOnClick(() -> {
            // Handled by handlePauseInput with delay
        });
        if (GameApp.hasTexture("red_long")) {
            quitButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        pauseButtons.add(quitButton);
        
        pauseButtonsInitialized = true;
    }
    
    /**
     * Handle input for pause menu.
     */
    private void handlePauseInput() {
        // Get mouse position in world coordinates
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        if (pauseButtons == null || pauseButtons.isEmpty()) return;
        
        // Check if hovering over any button for cursor switching
        boolean hoveringAnyButton = false;
        for (Button button : pauseButtons) {
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
                isHoveringPauseButton = false;
            } else if (hoveringAnyButton) {
                if (!isHoveringPauseButton) {
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringPauseButton = true;
                }
            } else {
                if (isHoveringPauseButton) {
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringPauseButton = false;
                }
            }
        }
        
        // Update buttons with world mouse position
        for (Button button : pauseButtons) {
            button.update(worldMouseX, worldMouseY);
        }
        
        // Handle mouse click
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed && pausePendingAction == null) {
            for (int i = 0; i < pauseButtons.size(); i++) {
                Button button = pauseButtons.get(i);
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    // Play button click sound
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    // Store button and action for delayed execution
                    pausePressedButton = button;
                    button.setPressed(true);
                    
                    // Create delayed action based on button
                    if (i == 0) {
                        // Resume button
                        pausePendingAction = () -> {
                            togglePause(); // Resume game
                        };
                    } else if (i == 1) {
                        // Settings button
                        pausePendingAction = () -> {
                            // Save game state before switching to settings
                            saveGameState();
                            // Set flags for returning from settings
                            PlayScreen.setReturningFromSettings(true);
                            PlayScreen.setWasPausedBeforeSettings(true);
                            SettingsScreen.setReturnScreen("pause");
                            GameApp.switchScreen("settings");
                        };
                    } else if (i == 2) {
                        // Quit button
                        pausePendingAction = () -> {
                            // Stop ingame music and go to main menu
                            if (soundManager != null) {
                                soundManager.stopIngameMusic();
                            }
                            GameApp.switchScreen("menu");
                        };
                    }
                    
                    pausePressTimer = 0f;
                    break;
                }
            }
        }
        
        // Update button pressed states
        if (pausePendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : pauseButtons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        } else if (pausePressedButton != null) {
            pausePressedButton.setPressed(true);
        }
    }
    
    /**
     * Render pause menu overlay (dark screen + title + buttons).
     */
    private void renderPauseOverlay() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Calculate fade alpha (0 to 1)
        float fadeAlpha = Math.min(pauseFadeTimer / PAUSE_FADE_DURATION, 1.0f);
        
        // Draw dark overlay (semi-transparent black to darken the gameplay)
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, (int)(fadeAlpha * 150)); // ~60% opacity for darker effect
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // Render buttons (they manage their own sprite batch)
        if (pauseButtons != null) {
            for (Button button : pauseButtons) {
                button.render();
            }
        }
        
        // Render title and text in sprite batch
        GameApp.startSpriteRendering();
        
        // Draw pause title image
        renderPauseTitle(centerX, centerY);
        
        // Draw button text labels
        renderPauseButtonText(centerX, centerY);
        
        GameApp.endSpriteRendering();
    }
    
    /**
     * Draw pause title image.
     * Same approach as renderGameOverTitle() - scale to 60% screen width, position at centerY - 60
     */
    private void renderPauseTitle(float centerX, float centerY) {
        if (!GameApp.hasTexture("pause_title")) {
            GameApp.addTexture("pause_title", "assets/ui/pause.png");
            if (!GameApp.hasTexture("pause_title")) {
                return;
            }
        }
        
        float screenWidth = GameApp.getWorldWidth();
        float titleWidth = 600f;
        float titleHeight = 200f;
        
        try {
            int texWidth = GameApp.getTextureWidth("pause_title");
            int texHeight = GameApp.getTextureHeight("pause_title");
            if (texWidth > 0 && texHeight > 0) {
                float targetWidth = screenWidth * 0.75f; // Slightly larger than game over title
                float aspectRatio = (float)texHeight / texWidth;
                titleWidth = targetWidth;
                titleHeight = targetWidth * aspectRatio;
            }
        } catch (Exception e) {
            // Use default
        }
        
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = centerY - 105f; // Lowered by 20f
        
        GameApp.drawTexture("pause_title", titleX, titleY, titleWidth, titleHeight);
    }
    
    /**
     * Draw pause menu button text labels.
     * Using unified colors for better contrast on colored buttons.
     */
    private void renderPauseButtonText(float centerX, float centerY) {
        if (pauseButtons == null || pauseButtons.size() < 3) return;
        
        String fontName = "buttonFontSmall"; // Small font for 640x360 world
        
        // Resume button text (green button)
        Button resumeButton = pauseButtons.get(0);
        float resumeCenterX = resumeButton.getX() + resumeButton.getWidth() / 2;
        float resumeCenterY = resumeButton.getY() + resumeButton.getHeight() / 2;
        float resumeTextHeight = GameApp.getTextHeight(fontName, "RESUME");
        float resumeAdjustedY = resumeCenterY + resumeTextHeight * 0.15f;
        GameApp.drawTextCentered(fontName, "RESUME", resumeCenterX, resumeAdjustedY, "button_green_text");
        
        // Settings button text (orange button)
        Button settingsButton = pauseButtons.get(1);
        float settingsCenterX = settingsButton.getX() + settingsButton.getWidth() / 2;
        float settingsCenterY = settingsButton.getY() + settingsButton.getHeight() / 2;
        float settingsTextHeight = GameApp.getTextHeight(fontName, "SETTINGS");
        float settingsAdjustedY = settingsCenterY + settingsTextHeight * 0.15f;
        GameApp.drawTextCentered(fontName, "SETTINGS", settingsCenterX, settingsAdjustedY, "button_orange_text");
        
        // Quit button text (red button)
        Button quitButton = pauseButtons.get(2);
        float quitCenterX = quitButton.getX() + quitButton.getWidth() / 2;
        float quitCenterY = quitButton.getY() + quitButton.getHeight() / 2;
        float quitTextHeight = GameApp.getTextHeight(fontName, "QUIT GAME");
        float quitAdjustedY = quitCenterY + quitTextHeight * 0.15f;
        GameApp.drawTextCentered(fontName, "QUIT GAME", quitCenterX, quitAdjustedY, "button_red_text");
    }
    
    /**
     * Resume game from pause (called by SettingsScreen when returning).
     */
    public void resumeFromSettings() {
        // Game was paused when going to settings, keep it paused but reset button state
        pausePendingAction = null;
        pausePressedButton = null;
        pausePressTimer = 0f;
        
        // Re-initialize buttons in case they were disposed
        pauseButtonsInitialized = false;
        initializePauseButtons();
    }
    
    /**
     * Check if game is currently paused.
     */
    public boolean isPaused() {
        return isPaused;
    }

    private void spawnBossNearPlayer(float playerX, float playerY, float moveDirX, float moveDirY) {
        float distance = 400f + (float) (Math.random() * 100f);

        float bx;
        float by;

        float length = (float) Math.sqrt(moveDirX * moveDirX + moveDirY * moveDirY);
        if (length > 0.001f) {
            float nx = moveDirX / length;
            float ny = moveDirY / length;

            // behind player
            bx = playerX - nx * distance;
            by = playerY - ny * distance;
        } else {
            double angle = Math.random() * Math.PI * 2.0;
            bx = playerX + (float) Math.cos(angle) * distance;
            by = playerY + (float) Math.sin(angle) * distance;
        }

        int hp = 200 + (int) (Math.random() * 101); // 200-300
        Boss boss = new Boss(bx, by, hp);

        if (bosses != null) {
            bosses.add(boss);
        }

        bossSpawned = true;

        GameApp.log("Boss spawned at (" + bx + ", " + by + ") with HP " + hp);
    }

}
