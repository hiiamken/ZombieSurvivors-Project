package nl.saxion.game.screens;


import nl.saxion.game.MainGame;
import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.core.GameState;
import nl.saxion.game.core.PlayerData;
import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.BreakableObject;
import nl.saxion.game.entities.Cat;
import nl.saxion.game.entities.ZombieHand;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.HealingItem;
import nl.saxion.game.entities.LevelUpOption;
import nl.saxion.game.entities.PassiveItem;
import nl.saxion.game.entities.PassiveItemType;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.StatUpgradeType;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.entities.WeaponUpgrade;
import nl.saxion.game.entities.XPOrb;
import nl.saxion.game.entities.OrbType;
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
import nl.saxion.game.systems.GachaSystem;
import nl.saxion.game.ui.Button;
import nl.saxion.game.ui.HUD;
import nl.saxion.game.ui.LevelUpMenuRenderer;
import nl.saxion.game.entities.TreasureChest;
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
    private static List<BreakableObject> savedBreakableObjects = null;
    private static List<HealingItem> savedHealingItems = null;

    private static float savedGameTime = 0f;
    private static int savedScore = 0;
    private static float savedPlayerWorldX = 0f;
    private static float savedPlayerWorldY = 0f;
    private static boolean savedIngameMusicStarted = false;
    private static int savedCurrentRound = 0;

    // MiniBoss system - spawns at end of each round (every 60 seconds)
    private List<Boss> bosses;
    private int currentRound = 0;
    private static final float ROUND_DURATION = 60f; // 1 round = 60 seconds
    private static final int TOTAL_ROUNDS = 10; // 10 rounds in 10 minutes
    
    // Treasure Chest system - spawns when MiniBoss is killed
    private List<TreasureChest> treasureChests;
    private static List<TreasureChest> savedTreasureChests = null;
    // Track bosses that already spawned chests to prevent duplicates
    private java.util.Set<Boss> bossesThatSpawnedChest = new java.util.HashSet<>();
    
    // Track late wave bosses (minute 7+) - these only spawn red orbs, no chests
    private java.util.Set<Boss> lateWaveBosses = new java.util.HashSet<>();
    
    // Victory transition state (smooth transition to winner screen)
    private boolean isVictoryTransition = false;
    private float victoryTransitionTimer = 0f;
    private static final float VICTORY_TRANSITION_DURATION = 2.0f; // 2 seconds for transition
    private float victoryFadeAlpha = 0f; // For fade effect
    
    // Late wave miniboss spawn timer
    private float lateWaveBossSpawnTimer = 0f;
    private static final float LATE_WAVE_BOSS_SPAWN_INTERVAL = 1.2f; // Spawn boss faster (was 2.0s)
    
    // Gacha system - triggers when chest is opened
    private GachaSystem gachaSystem;
    private boolean isGachaActive = false;

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
        savedBreakableObjects = breakableObjects;
        savedHealingItems = healingItems;
        savedGameTime = gameTime;
        savedScore = score;
        savedPlayerWorldX = playerWorldX;
        savedPlayerWorldY = playerWorldY;
        savedIngameMusicStarted = ingameMusicStarted;
        savedBosses = bosses;
        savedTreasureChests = treasureChests;
        savedCurrentRound = currentRound;
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
            treasureChests = savedTreasureChests;
            currentRound = savedCurrentRound;
            xpOrbs = savedXpOrbs;
            breakableObjects = savedBreakableObjects;
            healingItems = savedHealingItems;
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
            
            // Re-setup gacha system references
            if (gachaSystem != null) {
                gachaSystem.setPlayer(player);
                gachaSystem.setWeapon(weapon);
                gachaSystem.setSoundManager(soundManager);
            }
        }

        // Clear saved state
        savedPlayer = null;
        savedWeapon = null;
        savedBullets = null;
        savedEnemies = null;
        savedXpOrbs = null;
        savedBosses = null;
        savedTreasureChests = null;
        savedBreakableObjects = null;
        savedHealingItems = null;
    }

    private InputController input;
    private HUD hud;
    private LevelUpMenuRenderer levelUpMenuRenderer;

    // Cursor management
    private Cursor cursorPointer; // For click/default state
    private Cursor cursorHover;   // For hover state
    private Cursor cursorInvisible; // Invisible cursor for gameplay
    private boolean isCursorHidden = false; // Track cursor visibility

    private Player player;
    private Weapon weapon;
    private List<Bullet> bullets;
    private List<Enemy> enemies;
    private List<XPOrb> xpOrbs;
    private List<BreakableObject> breakableObjects;
    private List<HealingItem> healingItems;
    private List<Cat> cats; // Background cats for decoration
    private List<ZombieHand> zombieHands; // Background zombie hands for decoration

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
    private static final float GAME_DURATION = 600f; // 10 minutes countdown (600 seconds)
    private float gameTime = GAME_DURATION;
    private int score = 0;
    private int killCount = 0; // Track number of monsters killed
    private float playerWorldX;
    private float playerWorldY;

    // Level up menu
    private boolean isLevelUpActive = false;
    private List<LevelUpOption> levelUpOptions = new ArrayList<>();
    
    // Post-evolution bonus selection (null = not yet selected, after selection auto-applies on level up)
    private LevelUpOption.Type selectedPostEvolutionBonus = null;

    private static final boolean USE_LEVELUP_MENU_V2 = true;

    // Keep delta so the menu animates smoothly even though game is paused during level-up
    private float lastDelta = 1f / 60f;

    // Menu entrance animation
    private float levelUpMenuAnimTimer = 0f; // 0..1
    private boolean levelUpMenuOpening = false;

    // Selection and highlight animation
    private int levelUpSelectedIndex = 0;
    private int lastLevelUpSelectedIndex = -1;
    private float levelUpSelectAnim = 0f; // 0..1 smoothing
    
    // Rainbow XP bar animation timer
    private float levelUpRainbowTimer = 0f;
    
    // Level up menu arrow animation
    private float levelUpArrowAnimTimer = 0f;
    
    // Falling orbs effect for level up menu with rotation and 3 orb types
    private static class FallingOrb {
        float x, y, speed, size, alpha;
        float rotation;       // Current rotation angle in degrees
        float rotationSpeed;  // Rotation speed (degrees per second), 0 = no rotation
        OrbType orbType;      // Type of orb (BLUE, GREEN, RED)
        
        FallingOrb(float x, float y, float speed, float size, OrbType orbType) {
            this.x = x; this.y = y; this.speed = speed; this.size = size; this.alpha = 1f;
            this.orbType = orbType;
            // Random rotation: 50% chance to rotate, 50% chance to stay still
            if (GameApp.random(0f, 1f) < 0.5f) {
                this.rotationSpeed = GameApp.random(-180f, 180f); // Random rotation direction and speed
            } else {
                this.rotationSpeed = 0f; // No rotation
            }
            this.rotation = GameApp.random(0f, 360f); // Random starting angle
        }
    }
    private List<FallingOrb> levelUpFallingOrbs = new ArrayList<>();
    private boolean levelUpOrbsInitialized = false;

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
            mapRenderer.setResourceLoader(resourceLoader); // Enable sharp rendering
            enemySpawner = new EnemySpawner();
            collisionHandler = new CollisionHandler();
            gameRenderer = new GameRenderer();
            gameStateManager = new GameStateManager();
            damageTextSystem = new DamageTextSystem();

            collisionHandler.setDamageTextSystem(damageTextSystem);
            if (soundManager != null) {
                collisionHandler.setSoundManager(soundManager);
            }
            collisionHandler.setPlayer(player);

            input = new InputController(MainGame.getConfig());
            hud = new HUD();
            levelUpMenuRenderer = new LevelUpMenuRenderer();

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
        mapRenderer.setResourceLoader(resourceLoader); // Enable sharp rendering
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
        
        // Link player to collision handler for lifesteal
        collisionHandler.setPlayer(player);

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
        levelUpMenuRenderer = new LevelUpMenuRenderer();

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

        // === HUD & GAMEPLAY FONTS - Mixed hierarchy ===
        // Button font: upheavtt for bold buttons - size 24
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
        if (!GameApp.hasFont("buttonFontSmall")) {
            GameApp.addStyledFont("buttonFontSmall", "fonts/upheavtt.ttf", 20,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }

        // Load level up menu fonts - improved readability and scaling
        // IMPORTANT: Only load if not already loaded to prevent font size changes on screen transitions
        if (!GameApp.hasFont("levelUpTitleFont")) {
            GameApp.addStyledFont("levelUpTitleFont", "fonts/PressStart2P-Regular.ttf", 28,
                "yellow-400", 2.5f, "black", 3, 3, "orange-800", true);
        }
        if (!GameApp.hasFont("levelUpItemFont")) {
            GameApp.addStyledFont("levelUpItemFont", "fonts/PressStart2P-Regular.ttf", 16,
                "white", 2.0f, "black", 2, 2, "gray-800", true);
        }
        if (!GameApp.hasFont("levelUpNewFont")) {
            GameApp.addStyledFont("levelUpNewFont", "fonts/PressStart2P-Regular.ttf", 12,
                "orange-400", 1.5f, "black", 2, 2, "orange-900", true);
        }
        if (!GameApp.hasFont("levelUpLevelFont")) {
            GameApp.addStyledFont("levelUpLevelFont", "fonts/PressStart2P-Regular.ttf", 18,
                "cyan-300", 2.0f, "black", 2, 2, "blue-900", true);
        }
        if (!GameApp.hasFont("levelUpDescFont")) {
            GameApp.addStyledFont("levelUpDescFont", "fonts/PressStart2P-Regular.ttf", 6,
                "gray-300", 0.8f, "black", 1, 1, "gray-800", false);
        }

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

        // === HUD & GAMEPLAY FONTS - Mixed hierarchy ===
        // Button font: upheavtt for bold buttons - size 24
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
        
        // Load arrow texture for level up menu
        if (!GameApp.hasTexture("arrow")) {
            GameApp.addTexture("arrow", "assets/ui/arrow.png");
        }
        
        // Load post-evolution bonus textures
        if (!GameApp.hasTexture("star")) {
            GameApp.addTexture("star", "assets/ui/star.png");
        }
        if (!GameApp.hasTexture("skull_icon")) {
            GameApp.addTexture("skull_icon", "assets/ui/skull.png");
        }
        if (!GameApp.hasTexture("chicken")) {
            GameApp.addTexture("chicken", "assets/ui/chicken.png");
        }
        
        // Level up menu fonts already loaded in constructor - no need to reload here
        // This prevents font size changes when transitioning between screens

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

            // Create invisible cursor (1x1 transparent pixel)
            Pixmap invisiblePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            invisiblePixmap.setColor(0, 0, 0, 0); // Fully transparent
            invisiblePixmap.fill();
            cursorInvisible = Gdx.graphics.newCursor(invisiblePixmap, 0, 0);
            invisiblePixmap.dispose();

            // Set default to pointer
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            }
        } catch (Exception e) {
            GameApp.log("Could not load cursors: " + e.getMessage());
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
        if (cursorInvisible != null) {
            cursorInvisible.dispose();
            cursorInvisible = null;
        }

        // Stop ingame music when leaving gameplay
        if (soundManager != null) {
            soundManager.stopIngameMusic();
        }
        
        // Dispose map renderer
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        
        if (resourceLoader != null) {
            resourceLoader.disposeGameResources();
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        lastDelta = delta;
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

        // Update cursor visibility (hide during gameplay, show on interactive screens)
        updateCursorVisibility();
        
        // Handle ingame music delay (start music after delay to allow click sound)
        if (!ingameMusicStarted && soundManager != null && !isPaused) {
            ingameMusicDelayTimer += delta;
            if (ingameMusicDelayTimer >= INGAME_MUSIC_DELAY) {
                soundManager.playIngameMusic(true);
                ingameMusicStarted = true;
            }
        }
        
        // Update ingame music system (handles 3-track cycling: ingame → ingame2 → ingame3 → loop)
        if (soundManager != null && ingameMusicStarted) {
            soundManager.updateIngameMusic(delta);
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

            // Render frozen game background using unified method
            renderFrozenGameBackground();
            
            // Add consistent dark overlay for pause menu
            float screenWidth = GameApp.getWorldWidth();
            float screenHeight = GameApp.getWorldHeight();
            GameApp.enableTransparency();
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(0, 0, 0, 120); // Semi-transparent overlay
            GameApp.drawRect(0, 0, screenWidth, screenHeight);
            GameApp.endShapeRendering();

            // Render HUD (keeps EXP bar and stats visible like level-up screen)
            renderHUD();
            
            // Render pause overlay on top
            renderPauseOverlay();
            return; // Skip game updates
        }

        // Handle gacha system (pause game when active)
        if (isGachaActive && gachaSystem != null) {
            // Check if gacha system is still active
            if (!gachaSystem.isActive()) {
                // Gacha finished unexpectedly, reset flag
                isGachaActive = false;
                GameApp.log("Gacha system inactive, resetting isGachaActive flag");
            } else {
                // NOTE: Don't clear other chests - they should remain for player to collect later
                
                // IMPORTANT: Update gacha animation FIRST!
                gachaSystem.update(delta);
                
                handleGachaInput();
                
                // Render frozen game background using unified method
                renderFrozenGameBackground();
                
                // Add consistent dark overlay for gacha menu
                float screenWidth = GameApp.getWorldWidth();
                float screenHeight = GameApp.getWorldHeight();
                GameApp.enableTransparency();
                GameApp.startShapeRenderingFilled();
                GameApp.setColor(0, 0, 0, 120); // Semi-transparent overlay
                GameApp.drawRect(0, 0, screenWidth, screenHeight);
                GameApp.endShapeRendering();
                
                // Render HUD (keeps EXP bar and stats visible like level-up screen)
                renderHUD();
                
                // Render gacha overlay on top
                gachaSystem.render();
                return; // Skip game updates
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
            // Render frozen game background using unified method
            renderFrozenGameBackground();
            
            // Add consistent dark overlay for level up menu
            GameApp.enableTransparency();
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(0, 0, 0, 120); // Semi-transparent overlay
            GameApp.drawRect(0, 0, screenWidth, screenHeight);
            GameApp.endShapeRendering();
            
            // Update rainbow timer for XP bar
            levelUpRainbowTimer += delta * 2f;
            if (levelUpRainbowTimer > 1f) levelUpRainbowTimer -= 1f;
            renderHUD();
            renderLevelUpMenu();
            return; // Skip game updates
        }

        // Update countdown timer (only if not game over)
        if (!isGameOver && gameTime > 0) {
            gameTime -= delta;

            // Check if time ran out - PLAYER WINS!
            if (gameTime <= 0) {
                gameTime = 0;
                // Trigger victory transition when player survives 10 minutes!
                if (!player.isDying() && !isVictoryTransition) {
                    GameApp.log("Player survived 10 minutes - Starting victory transition!");
                    isVictoryTransition = true;
                    victoryTransitionTimer = 0f;
                    victoryFadeAlpha = 0f;
                    
                    // Stop ingame music immediately
                    if (soundManager != null) {
                        soundManager.stopIngameMusic();
                    }
                }
            }
        }
        
        // Handle victory transition (smooth transition with zombie disappear effect)
        if (isVictoryTransition) {
            victoryTransitionTimer += delta;
            
            // Fade effect progress (0 to 1 over transition duration)
            victoryFadeAlpha = Math.min(1f, victoryTransitionTimer / VICTORY_TRANSITION_DURATION);
            
            // Make all enemies and bosses fade out and disappear
            // (they will be rendered with decreasing alpha)
            
            // Transition complete - switch to winner screen
            if (victoryTransitionTimer >= VICTORY_TRANSITION_DURATION) {
                // Save score to leaderboard before switching
                saveScoreToLeaderboard();
                
                // Set winner screen data and switch to it
                WinnerScreen.setScore(score);
                String playerName = PlayerData.hasCurrentPlayer() ? PlayerData.getCurrentPlayer().getUsername() : "SURVIVOR";
                WinnerScreen.setPlayerName(playerName);
                WinnerScreen.setSurvivalTime(600f); // Full 10 minutes survived
                WinnerScreen.setComingFromGame(true); // Mark as coming from game for white flash transition
                GameApp.switchScreen("winner");
                return;
            }
            
            // During transition, skip normal game updates but still render
            return;
        }

        // ============================================
        // FREEZE GAME WHEN TIME IS UP (isGameOver)
        // Skip all game logic updates, only render and handle game over UI
        // ============================================
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
            
            // Only update animations (for visual continuity) but skip game logic
            updateAnimationsOnly();
            
            // Skip to render section - don't update any game logic
            renderGameFrozen();
            return;
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
            boss.update(delta, player.getX(), player.getY(), bosses); // Pass bosses list for separation collision
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
        GameApp.updateAnimation("boss_hit");
        GameApp.updateAnimation("boss_death");


        // Update XP orb animation
        GameApp.updateAnimation("orb_animation");

        // Update breakable object animations for ALL object types
        String[] objectTypes = {"barrel", "box", "rock", "sign", "mushroom", "chest"};
        for (String type : objectTypes) {
            GameApp.updateAnimation(type + "_idle");
            GameApp.updateAnimation(type + "_break");
        }
        
        // Update breakable objects
        for (BreakableObject obj : breakableObjects) {
            obj.update(delta);
        }
        
        // Update background cats animation and check for easter egg healing
        if (cats != null) {
            for (Cat cat : cats) {
                cat.update(delta);
                
                // Easter egg: Check if player touches cat -> instant heal
                if (player != null && !player.isDying()) {
                    // Use playerWorldX/Y (world coordinates) - same as cat coordinates
                    int healAmount = cat.checkPlayerProximityHeal(playerWorldX, playerWorldY, delta);
                    if (healAmount > 0) {
                        player.heal(healAmount);
                        // Play meow sound for cat healing
                        if (soundManager != null) {
                            soundManager.playSound("meoww", 0.8f);
                        }
                    }
                }
            }
        }
        
        // Update background zombie hands animation and check for trap damage
        if (zombieHands != null) {
            for (ZombieHand hand : zombieHands) {
                hand.update(delta);
                
                // Trap feature: Check if player takes damage from zombie hand
                if (player != null && !player.isDying()) {
                    // Use playerWorldX/Y (world coordinates) - same as hand coordinates
                    int damage = hand.checkPlayerDamage(playerWorldX, playerWorldY);
                    if (damage > 0) {
                        player.takeDamage(damage);
                        // Play damage sound
                        if (soundManager != null) {
                            soundManager.playSound("damaged", 0.5f);
                        }
                    }
                }
            }
        }

        // Enemy spawning (spawn behind player like Vampire Survivors)
        float playerMoveDirX = player.getLastMoveDirectionX();
        float playerMoveDirY = player.getLastMoveDirectionY();
        // Pass elapsed time (not countdown) for difficulty scaling
        float elapsedTime = GAME_DURATION - gameTime;

        // MiniBoss spawn at end of each round (every 60 seconds)
        int expectedRound = (int)(elapsedTime / ROUND_DURATION);
        if (expectedRound > currentRound && expectedRound <= TOTAL_ROUNDS) {
            // New round! Spawn MiniBoss
            currentRound = expectedRound;
            spawnMiniBossNearPlayer(playerWorldX, playerWorldY, playerMoveDirX, playerMoveDirY, currentRound);
            GameApp.log("Round " + currentRound + " completed! MiniBoss spawned!");
        }
        
        enemySpawner.update(delta, elapsedTime, playerWorldX, playerWorldY, playerMoveDirX, playerMoveDirY, enemies);
        
        // Late wave miniboss spawning (minute 7+)
        updateLateWaveSpawning(delta, playerWorldX, playerWorldY);

        // Collision detection
        collisionHandler.update(delta);
        // Set elapsed time for exponential damage scaling
        collisionHandler.setGameElapsedTime(elapsedTime);
        // Pass wall collision checker to prevent bullets hitting enemies through walls
        CollisionChecker wallChecker = mapRenderer::checkWallCollision;
        collisionHandler.handleBulletEnemyCollisions(bullets, enemies,
                (score) -> { addScore(score); addKill(); },
                (enemy) -> spawnXPOrbsAtEnemy(enemy),
                wallChecker);
        collisionHandler.handleEnemyPlayerCollisions(player, enemies);
        
        // Handle stampede zombie collisions
        handleStampedeZombieCollisions(bullets, player);

        collisionHandler.handleBulletBossCollisions(
                bullets,
                bosses,
                (Integer s) -> { addScore(s); addKill(); },
                (Boss boss) -> spawnTreasureChestAtBoss(boss), // Spawn chest instead of XP orbs
                wallChecker
        );

        collisionHandler.handleBossPlayerCollisions(player, bosses);

        // Handle bullet vs breakable object collisions
        collisionHandler.handleBulletBreakableObjectCollisions(
                bullets,
                breakableObjects,
                (obj) -> spawnItemAtBreakableObject(obj),
                wallChecker
        );

        // Update damage texts
        damageTextSystem.update(delta);

        // Update XP orbs
        updateXPOrbs(delta);
        
        // Update healing items (chicken)
        updateHealingItems(delta);
        
        // Update treasure chests
        updateTreasureChests(delta);
        
        // Note: Gacha system is updated in the gacha active block above (with return)
        // No need to update here as it would never reach this point when gacha is active

        // Check for level up (only if gacha not active)
        if (!isLevelUpActive && !isGachaActive && player.checkLevelUp()) {
            showLevelUpMenu();
        }

        // Cleanup: remove dead enemies and enemies too far (soft despawn cleanup)
        collisionHandler.removeDeadOrFarEnemies(enemies, playerWorldX, playerWorldY);
        collisionHandler.removeDestroyedBullets(bullets);
        // Cleanup dead bosses and remove them from tracking set
        bosses.removeIf(boss -> {
            boolean shouldRemove = !boss.isAlive() && boss.isDeathAnimationFinished();
            if (shouldRemove) {
                bossesThatSpawnedChest.remove(boss); // Cleanup tracking
            }
            return shouldRemove;
        });
        
        // Cleanup: remove broken breakable objects
        collisionHandler.removeBrokenObjects(breakableObjects);
        
        // Cleanup: remove collected treasure chests
        if (treasureChests != null) {
            treasureChests.removeIf(TreasureChest::isCollected);
        }


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

        // ----- RENDER -----
        // Render map background first (always render, even when game over)
        mapRenderer.render(playerWorldX, playerWorldY);

        GameApp.startSpriteRendering();

        // Render background cats first (render before everything else)
        renderCats();
        
        // Render background zombie hands (render before objects)
        renderZombieHands();
        
        // Render breakable objects (render before enemies so they appear behind)
        renderBreakableObjects();

        // Render entities
        gameRenderer.renderPlayer();
        gameRenderer.renderEnemies(enemies);
        gameRenderer.renderStampedeZombies(enemySpawner.getStampedeZombies());
        gameRenderer.renderBosses(bosses);
        gameRenderer.renderBullets(bullets);
        
        // Render treasure chests
        renderTreasureChests();

        GameApp.endSpriteRendering();

        // Render damage texts (after sprites, uses its own sprite batch)
        damageTextSystem.render(playerWorldX, playerWorldY);

        // Render health bar below player (uses shape rendering)
        renderPlayerHealthBar();
        
        // Render player blood particles (shape rendering)
        if (player != null) {
            player.renderBloodParticles(playerWorldX, playerWorldY);
        }

        // Render breakable object hit particles (shape rendering)
        renderBreakableObjectParticles();

        // Render XP orbs (uses shape rendering)
        renderXPOrbs();

        // Render healing items (chicken) with glow
        renderHealingItems();

        // Render HUD after sprite rendering (HUD uses shapes and text)
        renderHUD();

        // Render game over overlay if game over
        if (isGameOver) {
            renderGameOverOverlay();
        }
        
        // Render victory transition overlay (white fade with zombie disappear effect)
        if (isVictoryTransition) {
            renderVictoryTransitionOverlay();
        }
    }
    
    /**
     * Render victory transition overlay - white fade effect with "VICTORY!" text
     */
    private void renderVictoryTransitionOverlay() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        // White fade overlay (increasing alpha)
        int alpha = (int)(victoryFadeAlpha * 255);
        
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(255, 255, 255, alpha);
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // Show "VICTORY!" text when fade is past 50%
        if (victoryFadeAlpha > 0.3f) {
            GameApp.startSpriteRendering();
            float textAlpha = Math.min(1f, (victoryFadeAlpha - 0.3f) * 2f);
            // Use styled font for victory text
            String fontKey = GameApp.hasFont("winnerTitle") ? "winnerTitle" : "default";
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;
            GameApp.drawTextCentered(fontKey, "VICTORY!", centerX, centerY, "yellow-400");
            GameApp.endSpriteRendering();
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
        return new PlayerStatus(health, maxHealth, score, killCount, level, currentXP, xpToNext);
    }

    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    /**
     * Increment kill count when an enemy is killed.
     */
    public void addKill() {
        killCount++;
    }

    /**
     * Update animations only (for frozen game state visual continuity).
     * Called when game is over but we still want animations to look alive.
     */
    private void updateAnimationsOnly() {
        // Update player animations (chỉ update nếu animation tồn tại)
        if (GameApp.hasAnimation("player_idle")) {
            GameApp.updateAnimation("player_idle");
            GameApp.updateAnimation("player_run_left");
            GameApp.updateAnimation("player_run_right");
            GameApp.updateAnimation("player_hit");
            GameApp.updateAnimation("player_death");
        }

        // Update zombie animations (chỉ update nếu animation tồn tại)
        if (GameApp.hasAnimation("zombie_idle")) {
            GameApp.updateAnimation("zombie_idle");
            GameApp.updateAnimation("zombie_run");
            GameApp.updateAnimation("zombie_hit");
            GameApp.updateAnimation("zombie_death");
        }
        if (GameApp.hasAnimation("zombie3_idle")) {
            GameApp.updateAnimation("zombie3_idle");
            GameApp.updateAnimation("zombie3_run");
            GameApp.updateAnimation("zombie3_hit");
            GameApp.updateAnimation("zombie3_death");
        }
        if (GameApp.hasAnimation("zombie4_idle")) {
            GameApp.updateAnimation("zombie4_idle");
            GameApp.updateAnimation("zombie4_run");
            GameApp.updateAnimation("zombie4_hit");
            GameApp.updateAnimation("zombie4_death");
        }

        // Update boss animations (chỉ update nếu animation tồn tại)
        if (GameApp.hasAnimation("boss_idle")) {
            GameApp.updateAnimation("boss_idle");
            GameApp.updateAnimation("boss_run");
            GameApp.updateAnimation("boss_hit");
            GameApp.updateAnimation("boss_death");
        }

        // Update object animations (chỉ update nếu animation tồn tại)
        String[] objectTypes = {"barrel", "box", "rock", "sign", "mushroom", "chest"};
        for (String type : objectTypes) {
            if (GameApp.hasAnimation(type + "_idle")) {
                GameApp.updateAnimation(type + "_idle");
                GameApp.updateAnimation(type + "_break");
            }
        }
    }

    /**
     * Render game in frozen state (when game over).
     * Shows all entities but no gameplay updates.
     */
    private void renderGameFrozen() {
        // Render frozen game background using unified method
        renderFrozenGameBackground();
        
        // Add consistent dark overlay for game over screen
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, 120); // Semi-transparent overlay
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();

        // Render HUD (shows 00:00 timer)
        renderHUD();

        // Render game over overlay
        renderGameOverOverlay();
    }

    private void renderHUD() {
        PlayerStatus status = getPlayerStatus();
        
        // Update HUD with weapon and passive items info
        hud.setWeapon(weapon);
        hud.setPassiveItems(player.getOwnedPassiveItems());
        
        // Render rainbow XP bar if level up active, otherwise normal HUD
        if (isLevelUpActive) {
            // Render rainbow XP bar
            renderRainbowXPBar(status, levelUpRainbowTimer);
            
            // Render item icons first (manages its own rendering state)
            hud.renderItemIconsOnly();
            
            // Render HUD text separately (score, level, timer, etc)
            GameApp.startSpriteRendering();
            hud.renderScoreOnly(status);
            hud.renderXPTextOnly(status);
            hud.renderSurvivalTimeOnly(gameTime);
            GameApp.endSpriteRendering();
        } else {
            hud.render(status, gameTime);
        }
    }
    
    /**
     * Render rainbow XP bar (7 colors cycling) when level up menu is active
     */
    private void renderRainbowXPBar(PlayerStatus status, float rainbowTimer) {
        float percent = status.currentXP / (float) status.xpToNext;
        percent = GameApp.clamp(percent, 0f, 1f);
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f;
        float barY = screenHeight - barHeight;
        
        GameApp.startShapeRenderingFilled();
        
        // Background
        GameApp.setColor(40, 40, 40, 255);
        GameApp.drawRect(0, barY, screenWidth, barHeight);
        
        // Rainbow fill (7 colors cycling)
        if (percent > 0) {
            float barWidth = screenWidth * percent;
            
            // 7 rainbow colors: red, orange, yellow, green, cyan, blue, purple
            int[][] rainbowColors = {
                {255, 0, 0},       // Red
                {255, 165, 0},     // Orange
                {255, 255, 0},     // Yellow
                {0, 255, 0},       // Green
                {0, 255, 255},     // Cyan
                {0, 0, 255},       // Blue
                {148, 0, 211}      // Purple
            };
            
            int colorIndex = (int)(rainbowTimer * 7) % 7;
            int[] color = rainbowColors[colorIndex];
            GameApp.setColor(color[0], color[1], color[2], 255);
            GameApp.drawRect(0, barY, barWidth, barHeight);
        }
        
        GameApp.endShapeRendering();
        
        // Border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(3f);
        GameApp.setColor(100, 100, 100, 255);
        GameApp.drawRect(0, barY, screenWidth, barHeight);
        GameApp.endShapeRendering();
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
    // New system: 75% blue orb, 10% green orb, 15% no orb
    // Each enemy drops only ONE orb (or none)
    private void spawnXPOrbsAtEnemy(Enemy enemy) {
        float roll = GameApp.random(0f, 100f);
        
        if (roll < 75f) {
            // 75% chance: spawn BLUE orb
            float offsetX = GameApp.random(-5f, 5f);
            float offsetY = GameApp.random(-5f, 5f);
            XPOrb orb = new XPOrb(enemy.getX() + offsetX, enemy.getY() + offsetY, OrbType.BLUE);
            xpOrbs.add(orb);
        } else if (roll < 85f) {
            // 10% chance: spawn GREEN orb (only from enemies, not objects)
            float offsetX = GameApp.random(-5f, 5f);
            float offsetY = GameApp.random(-5f, 5f);
            XPOrb orb = new XPOrb(enemy.getX() + offsetX, enemy.getY() + offsetY, OrbType.GREEN);
            xpOrbs.add(orb);
        }
        // 15% chance: no orb drops
    }
    
    // Spawn RED orbs when MiniBoss is killed
    private void spawnXPOrbsAtBoss(Boss boss) {
        // MiniBoss drops 5-8 RED orbs (high value)
        int orbCount = GameApp.randomInt(5, 9);
        for (int i = 0; i < orbCount; i++) {
            float offsetX = GameApp.random(-30f, 30f);
            float offsetY = GameApp.random(-30f, 30f);
            XPOrb orb = new XPOrb(boss.getX() + offsetX, boss.getY() + offsetY, OrbType.RED);
            xpOrbs.add(orb);
        }
    }
    
    /**
     * Handle collisions for stampede zombies.
     * - Bullets can damage them
     * - They can damage player on contact
     * - They don't chase player, just run straight
     */
    private void handleStampedeZombieCollisions(java.util.List<Bullet> bullets, Player player) {
        java.util.List<EnemySpawner.StampedeZombie> stampedeZombies = enemySpawner.getStampedeZombies();
        if (stampedeZombies == null || stampedeZombies.isEmpty()) return;
        
        // Player hitbox
        float playerCenterX = player.getX() + Player.SPRITE_SIZE / 2f;
        float playerCenterY = player.getY() + Player.SPRITE_SIZE / 2f;
        float playerRadius = Player.DAMAGE_HITBOX_WIDTH / 2f;
        
        for (EnemySpawner.StampedeZombie sz : stampedeZombies) {
            if (sz.isDead || sz.isDying) continue;
            
            // Check bullet collisions
            for (Bullet b : bullets) {
                if (b.isDestroyed()) continue;
                
                float bulletCenterX = b.getX() + b.getWidth() / 2f;
                float bulletCenterY = b.getY() + b.getHeight() / 2f;
                
                float dx = bulletCenterX - sz.x;
                float dy = bulletCenterY - sz.y;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                
                if (dist < 20f) { // Hit radius
                    int damage = b.getDamage();
                    sz.takeDamage(damage);
                    
                    // Use pierce system - destroy if no pierce left
                    if (!b.canPierce()) {
                        b.destroy();
                    }
                    
                    // Spawn damage text
                    if (damageTextSystem != null) {
                        boolean isCrit = damage > 15;
                        damageTextSystem.spawnDamageText(sz.x, sz.y - 10, damage, isCrit);
                    }
                    
                    // Spawn XP orb if killed
                    if (sz.isDead) {
                        int randomScore = (int) GameApp.random(3, 8); // Random score 3-8 for stampede zombie
                        addScore(randomScore);
                        addKill(); // Increment kill count for stampede zombie
                        // Small chance to drop blue orb
                        if (Math.random() < 0.5) {
                            XPOrb orb = new XPOrb(sz.x, sz.y, OrbType.BLUE);
                            xpOrbs.add(orb);
                        }
                    }
                    break;
                }
            }
            
            // Check player collision (player has built-in invincibility after taking damage)
            if (!sz.isDead && !sz.isDying && player.getHealth() > 0) {
                float szCenterX = sz.x + Enemy.SPRITE_SIZE / 2f;
                float szCenterY = sz.y + Enemy.SPRITE_SIZE / 2f;
                
                float dx = szCenterX - playerCenterX;
                float dy = szCenterY - playerCenterY;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                
                if (dist < playerRadius + 15f) { // Contact radius
                    // Stampede zombie deals LESS damage than normal zombies
                    int damage = 1; // Much less damage - just obstacle, not deadly
                    player.takeDamage(damage);
                    
                    if (damageTextSystem != null) {
                        boolean isCrit = false;
                        damageTextSystem.spawnDamageText(playerCenterX, playerCenterY - 20, damage, isCrit);
                    }
                    
                    // Play hit sound
                    if (soundManager != null) {
                        soundManager.playSound("player_hit", 0.3f);
                    }
                }
            }
            
            // STAMPEDE PUSH: Push normal zombies aside (stampede maintains formation)
            if (!sz.isDead && !sz.isDying) {
                float szCenterX = sz.x + Enemy.SPRITE_SIZE / 2f;
                float szCenterY = sz.y + Enemy.SPRITE_SIZE / 2f;
                float pushRadius = 40f; // Radius to push normal zombies
                
                for (Enemy enemy : enemies) {
                    if (enemy.isDead()) continue;
                    
                    float enemyCenterX = enemy.getX() + Enemy.SPRITE_SIZE / 2f;
                    float enemyCenterY = enemy.getY() + Enemy.SPRITE_SIZE / 2f;
                    
                    float dx = enemyCenterX - szCenterX;
                    float dy = enemyCenterY - szCenterY;
                    float dist = (float)Math.sqrt(dx*dx + dy*dy);
                    
                    if (dist < pushRadius && dist > 0) {
                        // Push normal zombie perpendicular to stampede direction + forward
                        // Calculate perpendicular direction
                        float perpX = -sz.dirY;
                        float perpY = sz.dirX;
                        
                        // Determine which side to push (based on enemy position relative to stampede)
                        float side = dx * perpX + dy * perpY;
                        if (side < 0) {
                            perpX = -perpX;
                            perpY = -perpY;
                        }
                        
                        // Push sideways and slightly forward
                        float pushX = perpX * 0.8f + sz.dirX * 0.3f;
                        float pushY = perpY * 0.8f + sz.dirY * 0.3f;
                        
                        // Apply push (stronger when closer)
                        float pushFactor = (pushRadius - dist) / pushRadius;
                        enemy.applyKnockback(pushX * pushFactor * 2f, pushY * pushFactor * 2f);
                    }
                }
            }
        }
    }


    // Update XP orbs (magnet, collection) - orbs no longer expire
    private void updateXPOrbs(float delta) {
        // Get magnet bonus from MAGNET_STONE passive item
        float magnetBonus = 0f;
        if (player != null) {
            int magnetLevel = player.getPassiveItemLevel(PassiveItemType.MAGNET_STONE);
            magnetBonus = magnetLevel * 20f; // +20 range per level
        }
        
        java.util.Iterator<XPOrb> it = xpOrbs.iterator();
        while (it.hasNext()) {
            XPOrb orb = it.next();

            // Update orb position and magnet with bonus range
            orb.update(delta, player.getX(), player.getY(), magnetBonus);

            // Check if collected
            if (orb.isCollected()) {
                player.addXP(orb.getXPValue());
                // Play pickup item sound at 10% volume
                if (soundManager != null) {
                    soundManager.playSound("pickupitem", 0.1f);
                }
                it.remove();
            }
            // Orbs no longer expire - they persist until game ends
        }
    }

    // =========================
    // BREAKABLE OBJECTS SYSTEM
    // =========================

    /**
     * Renders all breakable objects in the world.
     */
    private void renderBreakableObjects() {
        for (BreakableObject obj : breakableObjects) {
            obj.render(playerWorldX, playerWorldY);
        }
    }
    
    // =========================
    // BACKGROUND CATS SYSTEM
    // =========================
    
    /**
     * Renders all background cats in the world.
     */
    private void renderCats() {
        if (cats == null) return;
        
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();
        
        for (Cat cat : cats) {
            // Calculate screen position (world → screen)
            float offsetX = cat.getX() - playerWorldX;
            float offsetY = cat.getY() - playerWorldY;
            float screenX = worldW / 2f + offsetX;
            float screenY = worldH / 2f + offsetY;
            
            // Only render if in viewport (sử dụng RENDER_SIZE cho viewport check)
            if (screenX + Cat.RENDER_SIZE > 0 && screenX < worldW &&
                screenY + Cat.RENDER_SIZE > 0 && screenY < worldH) {
                cat.render(screenX, screenY);
            }
        }
    }
    
    // =========================
    // BACKGROUND ZOMBIE HANDS SYSTEM
    // =========================
    
    /**
     * Renders all background zombie hands in the world.
     */
    private void renderZombieHands() {
        if (zombieHands == null) return;
        
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();
        
        for (ZombieHand hand : zombieHands) {
            // Calculate screen position (world → screen)
            float offsetX = hand.getX() - playerWorldX;
            float offsetY = hand.getY() - playerWorldY;
            float screenX = worldW / 2f + offsetX;
            float screenY = worldH / 2f + offsetY;
            
            // Only render if in viewport
            if (screenX + ZombieHand.RENDER_SIZE > 0 && screenX < worldW &&
                screenY + ZombieHand.RENDER_SIZE > 0 && screenY < worldH) {
                hand.render(screenX, screenY);
            }
        }
    }
    
    /**
     * Spawns background cats randomly in all 16 rooms.
     * Cats spawn near walls (edges of rooms) using TMX collision data.
     * Each room gets 1-2 cats, spaced far apart.
     * Avoids spawning near player starting position.
     */
    private void spawnCatsInAllRooms() {
        int roomWidth = MapRenderer.getMapTileWidth();   // 960
        int roomHeight = MapRenderer.getMapTileHeight(); // 640
        
        // Minimum distance between cats (giảm một chút để dễ spawn hơn)
        float minDistanceBetweenCats = 250f;
        float minDistanceFromObjects = 100f;
        
        // Tính toán room của player để tránh spawn mèo trong room đó
        int playerRoomRow = (int)Math.floor(playerWorldY / roomHeight);
        int playerRoomCol = (int)Math.floor(playerWorldX / roomWidth);
        // Wrap to 0-3 range
        playerRoomRow = (playerRoomRow % 4 + 4) % 4;
        playerRoomCol = (playerRoomCol % 4 + 4) % 4;
        float minDistanceFromPlayerSpawn = 400f; // Giảm xuống để dễ spawn hơn
        
        // Edge zones - cats spawn in these zones near walls
        float edgeZoneWidth = 100f; // Tăng lên một chút để có nhiều vị trí hơn
        float minDistanceFromWall = 16f; // Giảm xuống để spawn sát tường hơn
        
        int totalCatsSpawned = 0;
        int totalAttemptsFailed = 0;
        
        // Spawn cats in all 16 rooms (4x4 grid)
        for (int roomRow = 0; roomRow < 4; roomRow++) {
            for (int roomCol = 0; roomCol < 4; roomCol++) {
                // Calculate room bounds
                float roomStartX = roomCol * roomWidth;
                float roomStartY = roomRow * roomHeight;
                
                // Track cats spawned in this room for distance checking
                List<float[]> roomCats = new ArrayList<>();
                
                // Spawn 1-2 cats in this room
                int catCount = GameApp.randomInt(1, 3); // 1 to 2 inclusive
                
                for (int i = 0; i < catCount; i++) {
                    // Try to find a valid spawn position near walls
                    int attempts = 0;
                    int maxAttempts = 150; // Tăng số lần thử
                    boolean spawned = false;
                    
                    while (attempts < maxAttempts) {
                        float catX, catY;
                        
                        // Randomly choose which edge to spawn near (top, bottom, left, right)
                        int edge = GameApp.randomInt(0, 4);
                        
                        switch (edge) {
                            case 0: // Top edge (near top wall) - spawn sát tường trên
                                catX = roomStartX + GameApp.random(minDistanceFromWall + 50f, roomWidth - minDistanceFromWall - 50f);
                                catY = roomStartY + GameApp.random(minDistanceFromWall, edgeZoneWidth);
                                break;
                            case 1: // Bottom edge (near bottom wall) - spawn sát tường dưới
                                catX = roomStartX + GameApp.random(minDistanceFromWall + 50f, roomWidth - minDistanceFromWall - 50f);
                                catY = roomStartY + roomHeight - GameApp.random(minDistanceFromWall, edgeZoneWidth);
                                break;
                            case 2: // Left edge (near left wall) - spawn sát tường trái
                                catX = roomStartX + GameApp.random(minDistanceFromWall, edgeZoneWidth);
                                catY = roomStartY + GameApp.random(minDistanceFromWall + 50f, roomHeight - minDistanceFromWall - 50f);
                                break;
                            default: // Right edge (near right wall) - spawn sát tường phải
                                catX = roomStartX + roomWidth - GameApp.random(minDistanceFromWall, edgeZoneWidth);
                                catY = roomStartY + GameApp.random(minDistanceFromWall + 50f, roomHeight - minDistanceFromWall - 50f);
                                break;
                        }
                        
                        // Check if position is valid
                        boolean validPosition = true;
                        
                        if (mapRenderer != null) {
                            // Check ALL 4 corners + center to ensure cat doesn't overlap with wall
                            float spriteSize = Cat.SPRITE_SIZE;
                            float padding = 8f; // Extra padding from walls
                            
                            // Check center
                            float centerX = catX + spriteSize / 2;
                            float centerY = catY + spriteSize / 2;
                            
                            // Check all corners with padding
                            boolean topLeft = mapRenderer.checkWallCollision(catX + padding, catY + padding, 4, 4);
                            boolean topRight = mapRenderer.checkWallCollision(catX + spriteSize - padding - 4, catY + padding, 4, 4);
                            boolean bottomLeft = mapRenderer.checkWallCollision(catX + padding, catY + spriteSize - padding - 4, 4, 4);
                            boolean bottomRight = mapRenderer.checkWallCollision(catX + spriteSize - padding - 4, catY + spriteSize - padding - 4, 4, 4);
                            boolean center = mapRenderer.checkWallCollision(centerX - 2, centerY - 2, 4, 4);
                            
                            if (topLeft || topRight || bottomLeft || bottomRight || center) {
                                validPosition = false;
                            }
                        }
                        
                        // Check minimum distance from other cats in this room
                        if (validPosition) {
                            for (float[] otherCat : roomCats) {
                                float dist = GameApp.distance(catX, catY, otherCat[0], otherCat[1]);
                                if (dist < minDistanceBetweenCats) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from breakable objects
                        if (validPosition) {
                            for (BreakableObject obj : breakableObjects) {
                                float dist = GameApp.distance(catX, catY, obj.getX(), obj.getY());
                                if (dist < minDistanceFromObjects) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from player spawn position (chỉ kiểm tra nếu trong cùng room)
                        if (validPosition && roomRow == playerRoomRow && roomCol == playerRoomCol) {
                            float distFromPlayerSpawn = GameApp.distance(catX, catY, playerWorldX, playerWorldY);
                            if (distFromPlayerSpawn < minDistanceFromPlayerSpawn) {
                                validPosition = false;
                            }
                        }
                        
                        if (validPosition) {
                            // Create random cat type (0-12, 13 different cats)
                            int catType = GameApp.randomInt(0, 13);
                            Cat cat = new Cat(catX, catY, catType);
                            cats.add(cat);
                            roomCats.add(new float[]{catX, catY});
                            totalCatsSpawned++;
                            spawned = true;
                            break;
                        }
                        
                        attempts++;
                    }
                    
                    if (!spawned) {
                        totalAttemptsFailed++;
                    }
                }
            }
        }
        
        GameApp.log("Spawned " + cats.size() + " background cats near walls across 16 rooms (failed attempts: " + totalAttemptsFailed + ")");
    }
    
    /**
     * Spawns background zombie hands randomly in all 16 rooms.
     * Zombie hands spawn randomly across the map, avoiding walls, objects, and cats.
     * Each room gets 1-2 hands, spaced far apart.
     * Avoids spawning near player starting position.
     */
    private void spawnZombieHandsInAllRooms() {
        int roomWidth = MapRenderer.getMapTileWidth();   // 960
        int roomHeight = MapRenderer.getMapTileHeight(); // 640
        
        // Minimum distance between zombie hands
        float minDistanceBetweenHands = 200f;
        float minDistanceFromObjects = 100f;
        float minDistanceFromCats = 100f;
        
        // Tính toán room của player để tránh spawn trong room đó
        int playerRoomRow = (int)Math.floor(playerWorldY / roomHeight);
        int playerRoomCol = (int)Math.floor(playerWorldX / roomWidth);
        // Wrap to 0-3 range
        playerRoomRow = (playerRoomRow % 4 + 4) % 4;
        playerRoomCol = (playerRoomCol % 4 + 4) % 4;
        float minDistanceFromPlayerSpawn = 400f;
        
        // Spawn zone - không nhất thiết phải gần tường, có thể spawn ở giữa map
        float margin = 80f; // Margin từ biên room
        
        int totalHandsSpawned = 0;
        int totalAttemptsFailed = 0;
        
        // Spawn zombie hands in all 16 rooms (4x4 grid)
        for (int roomRow = 0; roomRow < 4; roomRow++) {
            for (int roomCol = 0; roomCol < 4; roomCol++) {
                // Calculate room bounds
                float roomStartX = roomCol * roomWidth;
                float roomStartY = roomRow * roomHeight;
                
                // Track hands spawned in this room for distance checking
                List<float[]> roomHands = new ArrayList<>();
                
                // Spawn 1-2 zombie hands in this room
                int handCount = GameApp.randomInt(1, 3); // 1 to 2 inclusive
                
                for (int i = 0; i < handCount; i++) {
                    // Try to find a valid spawn position
                    int attempts = 0;
                    int maxAttempts = 100;
                    boolean spawned = false;
                    
                    while (attempts < maxAttempts) {
                        // Random position within room (có thể ở giữa, không nhất thiết gần tường)
                        float handX = roomStartX + margin + GameApp.random(0f, roomWidth - 2 * margin);
                        float handY = roomStartY + margin + GameApp.random(0f, roomHeight - 2 * margin);
                        
                        // Check if position is valid
                        boolean validPosition = true;
                        
                        if (mapRenderer != null) {
                            // Check ALL 4 corners + center to ensure hand doesn't overlap with wall
                            float spriteSize = ZombieHand.SPRITE_SIZE;
                            float padding = 8f; // Extra padding from walls
                            
                            // Check center
                            float centerX = handX + spriteSize / 2;
                            float centerY = handY + spriteSize / 2;
                            
                            // Check all corners with padding
                            boolean topLeft = mapRenderer.checkWallCollision(handX + padding, handY + padding, 4, 4);
                            boolean topRight = mapRenderer.checkWallCollision(handX + spriteSize - padding - 4, handY + padding, 4, 4);
                            boolean bottomLeft = mapRenderer.checkWallCollision(handX + padding, handY + spriteSize - padding - 4, 4, 4);
                            boolean bottomRight = mapRenderer.checkWallCollision(handX + spriteSize - padding - 4, handY + spriteSize - padding - 4, 4, 4);
                            boolean center = mapRenderer.checkWallCollision(centerX - 2, centerY - 2, 4, 4);
                            
                            if (topLeft || topRight || bottomLeft || bottomRight || center) {
                                validPosition = false;
                            }
                        }
                        
                        // Check minimum distance from other hands in this room
                        if (validPosition) {
                            for (float[] otherHand : roomHands) {
                                float dist = GameApp.distance(handX, handY, otherHand[0], otherHand[1]);
                                if (dist < minDistanceBetweenHands) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from breakable objects
                        if (validPosition) {
                            for (BreakableObject obj : breakableObjects) {
                                float dist = GameApp.distance(handX, handY, obj.getX(), obj.getY());
                                if (dist < minDistanceFromObjects) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from cats (không spawn trùng với mèo)
                        if (validPosition && cats != null) {
                            for (Cat cat : cats) {
                                float dist = GameApp.distance(handX, handY, cat.getX(), cat.getY());
                                if (dist < minDistanceFromCats) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from player spawn position (chỉ kiểm tra nếu trong cùng room)
                        if (validPosition && roomRow == playerRoomRow && roomCol == playerRoomCol) {
                            float distFromPlayerSpawn = GameApp.distance(handX, handY, playerWorldX, playerWorldY);
                            if (distFromPlayerSpawn < minDistanceFromPlayerSpawn) {
                                validPosition = false;
                            }
                        }
                        
                        if (validPosition) {
                            ZombieHand hand = new ZombieHand(handX, handY);
                            zombieHands.add(hand);
                            roomHands.add(new float[]{handX, handY});
                            totalHandsSpawned++;
                            spawned = true;
                            break;
                        }
                        
                        attempts++;
                    }
                    
                    if (!spawned) {
                        totalAttemptsFailed++;
                    }
                }
            }
        }
        
        GameApp.log("Spawned " + zombieHands.size() + " background zombie hands across 16 rooms (failed attempts: " + totalAttemptsFailed + ")");
    }

    /**
     * Renders hit particles for all breakable objects.
     * Should be called during shape rendering phase.
     */
    private void renderBreakableObjectParticles() {
        GameApp.startShapeRenderingFilled();
        for (BreakableObject obj : breakableObjects) {
            if (obj.hasParticles()) {
                obj.renderParticles(playerWorldX, playerWorldY);
            }
        }
        GameApp.endShapeRendering();
    }

    /**
     * Spawns breakable objects randomly in all 16 rooms.
     * Each room gets 1-5 objects at random positions, with minimum distance between them.
     * Uses TMX wall collision data to avoid spawning near walls.
     * Avoids spawning near player starting position to encourage exploration.
     */
    private void spawnBreakableObjectsInAllRooms() {
        int roomWidth = MapRenderer.getMapTileWidth();   // 960
        int roomHeight = MapRenderer.getMapTileHeight(); // 640
        
        // Minimum distance between objects to avoid clustering
        float minDistanceBetweenObjects = 200f; // Increased from 150 for more spread
        
        // Wall padding - how far from walls objects should spawn (increased for better spacing)
        float wallPadding = 100f;
        
        // Minimum distance from player spawn to encourage exploration
        float minDistanceFromPlayerSpawn = 350f; // Objects won't spawn within this range of player start
        
        // Spawn objects in all 16 rooms (4x4 grid)
        for (int roomRow = 0; roomRow < 4; roomRow++) {
            for (int roomCol = 0; roomCol < 4; roomCol++) {
                // Calculate room bounds
                float roomStartX = roomCol * roomWidth;
                float roomStartY = roomRow * roomHeight;
                
                // Track objects spawned in this room for distance checking
                List<float[]> roomObjects = new ArrayList<>();
                
                // Spawn 1-5 objects in this room
                int objectCount = GameApp.randomInt(1, 6); // 1 to 5 inclusive
                
                for (int i = 0; i < objectCount; i++) {
                    // Try to find a valid spawn position (not in wall, not too close to walls/others)
                    int attempts = 0;
                    int maxAttempts = 80; // More attempts for better placement
                    
                    while (attempts < maxAttempts) {
                        // Random position within room (with larger margin from edges)
                        float margin = 160f; // Larger margin to avoid walls near room edges
                        float objX = roomStartX + margin + GameApp.random(0f, roomWidth - 2 * margin);
                        float objY = roomStartY + margin + GameApp.random(0f, roomHeight - 2 * margin);
                        
                        // Check if position is valid (not in wall) - check the object itself
                        boolean validPosition = true;
                        
                        if (mapRenderer != null) {
                            // Check center point of object
                            float centerX = objX + BreakableObject.RENDER_SIZE / 2;
                            float centerY = objY + BreakableObject.RENDER_SIZE / 2;
                            
                            if (mapRenderer.checkWallCollision(centerX - 16, centerY - 16, 32, 32)) {
                                validPosition = false;
                            }
                            
                            // Check large area around object for walls (with big padding)
                            if (validPosition) {
                                float checkRadius = wallPadding;
                                
                                // Check 16 points in a circle around the object center
                                for (int angle = 0; angle < 360; angle += 22) {
                                    float rad = (float) Math.toRadians(angle);
                                    float checkX = centerX + (float) Math.cos(rad) * checkRadius;
                                    float checkY = centerY + (float) Math.sin(rad) * checkRadius;
                                    
                                    if (mapRenderer.checkWallCollision(checkX - 8, checkY - 8, 16, 16)) {
                                        validPosition = false;
                                        break;
                                    }
                                }
                            }
                            
                            // Check at multiple distances (50, 75, 100 pixels)
                            if (validPosition) {
                                float[] checkDistances = {50f, 75f, 100f};
                                for (float dist : checkDistances) {
                                    // Check 8 cardinal/diagonal directions at this distance
                                    float[] dirs = {0, 45, 90, 135, 180, 225, 270, 315};
                                    for (float dir : dirs) {
                                        float rad = (float) Math.toRadians(dir);
                                        float checkX = centerX + (float) Math.cos(rad) * dist;
                                        float checkY = centerY + (float) Math.sin(rad) * dist;
                                        
                                        if (mapRenderer.checkWallCollision(checkX - 4, checkY - 4, 8, 8)) {
                                            validPosition = false;
                                            break;
                                        }
                                    }
                                    if (!validPosition) break;
                                }
                            }
                        }
                        
                        // Check minimum distance from other objects in this room
                        if (validPosition) {
                            for (float[] otherObj : roomObjects) {
                                float dist = GameApp.distance(objX, objY, otherObj[0], otherObj[1]);
                                if (dist < minDistanceBetweenObjects) {
                                    validPosition = false;
                                    break;
                                }
                            }
                        }
                        
                        // Check minimum distance from player spawn position (avoid cluttering starting area)
                        if (validPosition) {
                            float distFromPlayerSpawn = GameApp.distance(objX, objY, playerWorldX, playerWorldY);
                            if (distFromPlayerSpawn < minDistanceFromPlayerSpawn) {
                                validPosition = false;
                            }
                        }
                        
                        if (validPosition) {
                            // Create random object type
                            BreakableObject obj = new BreakableObject(objX, objY);
                            breakableObjects.add(obj);
                            roomObjects.add(new float[]{objX, objY});
                            break;
                        }
                        
                        attempts++;
                    }
                }
            }
        }
        
        GameApp.log("Spawned " + breakableObjects.size() + " breakable objects across 16 rooms");
    }

    /**
     * Spawns items at the breakable object position when it's destroyed.
     * Has a chance to spawn either XP orbs OR a healing item (chicken).
     * @param obj The breakable object that was destroyed
     */
    private void spawnItemAtBreakableObject(BreakableObject obj) {
        float centerX = obj.getCenterX();
        float centerY = obj.getCenterY();
        
        // 15% chance to spawn healing item (chicken) instead of orbs
        boolean spawnHealing = GameApp.random(0f, 100f) < 15f;
        
        if (spawnHealing) {
            // Spawn a healing item (chicken) - heals 25 HP
            int healAmount = 25;
            HealingItem chicken = new HealingItem(centerX, centerY, healAmount);
            healingItems.add(chicken);
        } else {
            // Spawn 2-4 BLUE XP orbs at the object's position (only BLUE, not GREEN)
            int orbCount = GameApp.randomInt(2, 5);
            for (int i = 0; i < orbCount; i++) {
                float offsetX = GameApp.random(-15f, 15f);
                float offsetY = GameApp.random(-15f, 15f);
                XPOrb orb = new XPOrb(centerX + offsetX, centerY + offsetY, OrbType.BLUE);
                xpOrbs.add(orb);
            }
        }
    }

    // =========================
    // HEALING ITEMS SYSTEM
    // =========================

    /**
     * Updates all healing items (magnet, collection, expiration).
     * Healing items are attracted to player like XP orbs, affected by MAGNET_STONE passive.
     */
    private void updateHealingItems(float delta) {
        if (healingItems == null) return;
        
        // Get magnet bonus from MAGNET_STONE passive item (same as orbs)
        float magnetBonus = 0f;
        if (player != null) {
            int attractorbLevel = player.getPassiveItemLevel(PassiveItemType.MAGNET_STONE);
            magnetBonus = attractorbLevel * 20f; // +20 range per level
        }
        
        java.util.Iterator<HealingItem> it = healingItems.iterator();
        while (it.hasNext()) {
            HealingItem item = it.next();
            
            // Update item position with magnet effect
            item.update(delta, player.getX(), player.getY(), magnetBonus);
            
            // Check if collected - heal player
            if (item.isCollected()) {
                int healAmount = item.getHealAmount();
                player.heal(healAmount);
                
                // Play pickup sound
                if (soundManager != null) {
                    soundManager.playSound("pickupitem", 0.15f);
                }
                
                GameApp.log("Player collected chicken! Healed " + healAmount + " HP");
                it.remove();
                continue;
            }
            
            // Remove expired items
            if (item.isExpired()) {
                it.remove();
            }
        }
    }

    /**
     * Renders all healing items in the world.
     */
    private void renderHealingItems() {
        if (healingItems == null || healingItems.isEmpty()) return;
        
        // First render glow effects (shape rendering)
        GameApp.startShapeRenderingFilled();
        for (HealingItem item : healingItems) {
            item.renderGlow(playerWorldX, playerWorldY);
        }
        GameApp.endShapeRendering();
        
        // Then render chicken sprites
        GameApp.startSpriteRendering();
        for (HealingItem item : healingItems) {
            item.render(playerWorldX, playerWorldY);
        }
        GameApp.endSpriteRendering();
    }

    // Render XP orbs using texture-based rendering (static, no animation)
    private void renderXPOrbs() {
        // Check if new orb textures are available
        boolean hasOrbTextures = GameApp.hasTexture("orb_blue") && 
                                  GameApp.hasTexture("orb_green") && 
                                  GameApp.hasTexture("orb_red");
        
        if (hasOrbTextures) {
            // Use texture-based rendering (static)
            GameApp.startSpriteRendering();
            for (XPOrb orb : xpOrbs) {
                orb.renderWithTexture(playerWorldX, playerWorldY);
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
    // Now supports: Weapon upgrades, Passive items, Stat upgrades, Evolution, and Post-Evolution bonuses
    private void showLevelUpMenu() {
        levelUpOptions.clear();

        // ============================================
        // POST-EVOLUTION: AUTO-APPLY BONUS (no menu)
        // ============================================
        // If player already selected a post-evolution bonus, auto-apply it
        if (weapon.isEvolved() && selectedPostEvolutionBonus != null) {
            if (selectedPostEvolutionBonus == LevelUpOption.Type.BONUS_POINTS) {
                int bonusPoints = 50 + GameApp.randomInt(0, 151); // 50-200 points
                score += bonusPoints;
                GameApp.log("Auto-applied BONUS_POINTS: +" + bonusPoints + " points");
            } else if (selectedPostEvolutionBonus == LevelUpOption.Type.BONUS_HEALTH) {
                player.heal(25);
                GameApp.log("Auto-applied BONUS_HEALTH: +25 HP");
            }
            player.levelUp();
            // No menu shown - bonus auto-applied
            return;
        }
        
        // ============================================
        // POST-EVOLUTION: SHOW BONUS SELECTION (first time only)
        // ============================================
        // If weapon is evolved but no bonus selected yet, show the 2 options
        if (weapon.isEvolved()) {
            levelUpOptions.add(LevelUpOption.createBonusPointsOption());
            levelUpOptions.add(LevelUpOption.createBonusHealthOption());
            activateLevelUpMenuWithSound();
            return;
        }

        // ============================================
        // CHECK FOR EVOLUTION FIRST
        // ============================================
        // If weapon is at max level AND all passive items are maxed, show evolution option
        if (weapon.canEvolve(player.areAllPassiveItemsMaxed())) {
            levelUpOptions.add(LevelUpOption.createEvolutionOption());
            // Evolution available - activate menu with sound
            activateLevelUpMenuWithSound();
            return;
        }

        // ============================================
        // BUILD POOL OF AVAILABLE UPGRADES
        // ============================================
        List<LevelUpOption> availableOptions = new ArrayList<>();

        // 1. Weapon upgrade (if not maxed)
        if (!weapon.isMaxLevel()) {
            availableOptions.add(new LevelUpOption(weapon.getLevel()));
        }

        // 2. Passive items (7 types: Spinach, Armor, Wings, Clover, Attractorb, Pummarola, Hollow Heart)
        // These are the ONLY upgrades besides weapon - no more stat upgrades!
        for (PassiveItemType passiveType : PassiveItemType.values()) {
            int currentLevel = player.getPassiveItemLevel(passiveType);
            if (currentLevel < passiveType.maxLevel) {
                availableOptions.add(new LevelUpOption(passiveType, currentLevel));
            }
        }
        
        // NOTE: StatUpgradeType removed - only 7 passive items + weapon upgrades now

        // If no upgrades available (all maxed), skip menu silently - NO SOUND
        if (availableOptions.isEmpty()) {
            player.levelUp();
            // Don't play sound, don't show menu - all upgrades are maxed
            return;
        }

        // ============================================
        // WEIGHTED RANDOM SELECTION
        // ============================================
        // Only 2 types now: Weapon upgrades and Passive items
        // Weapon has slightly higher weight
        
        // Pick 3 random options (or as many as available)
        int optionsToPick = Math.min(3, availableOptions.size());
        
        for (int i = 0; i < optionsToPick && !availableOptions.isEmpty(); i++) {
            // Calculate weights
            List<Float> weights = new ArrayList<>();
            float totalWeight = 0f;
            
            for (LevelUpOption opt : availableOptions) {
                float weight = 1.0f;
                if (opt.isWeaponUpgrade()) {
                    weight = 1.5f; // Weapon upgrades slightly more common
                } else if (opt.isPassiveUpgrade()) {
                    weight = 1.0f; // Passive items normal weight
                }
                weights.add(weight);
                totalWeight += weight;
            }
            
            // Weighted random selection
            float rand = GameApp.random(0f, totalWeight);
            float cumulative = 0f;
            int selectedIndex = 0;
            
            for (int j = 0; j < weights.size(); j++) {
                cumulative += weights.get(j);
                if (rand <= cumulative) {
                    selectedIndex = j;
                    break;
                }
            }
            
            // Add selected option and remove from pool
            levelUpOptions.add(availableOptions.get(selectedIndex));
            availableOptions.remove(selectedIndex);
        }
        
        // Upgrades available - activate menu with sound
        activateLevelUpMenuWithSound();
    }
    
    /**
     * Helper method to activate level up menu with sound and animations.
     * Only called when there are actual upgrades available.
     */
    private void activateLevelUpMenuWithSound() {
        isLevelUpActive = true;
        levelUpMenuOpening = true;
        levelUpMenuAnimTimer = 0f;
        levelUpSelectedIndex = 0;
        lastLevelUpSelectedIndex = -1;
        levelUpSelectAnim = 0f;

        // Play level up sound - ONLY when menu actually opens
        if (soundManager != null) {
            soundManager.playSound("levelup", 1.0f);
        }

        // Reduce ingame music volume to 30% when level up menu is open
        if (soundManager != null) {
            soundManager.setIngameMusicVolumeTemporary(0.3f);
        }
    }

    // Render level up menu - always use new professional design
    private void renderLevelUpMenu() {
        // Use the new professional upgrade menu design
        renderLevelUpMenuV2();
    }

    // Handle level up menu input
    private void handleLevelUpInput() {

        if (USE_LEVELUP_MENU_V2 && levelUpOptions != null && !levelUpOptions.isEmpty()) {
            // Support both Arrow keys and W/S for navigation
            if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
                levelUpSelectedIndex--;
                if (levelUpSelectedIndex < 0) levelUpSelectedIndex = levelUpOptions.size() - 1;
            } else if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
                levelUpSelectedIndex++;
                if (levelUpSelectedIndex >= levelUpOptions.size()) levelUpSelectedIndex = 0;
            } else if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
                applyLevelUpOption(levelUpSelectedIndex);
                return;
            }
            
            // Mouse click support for selecting upgrades
            if (GameApp.isButtonJustPressed(0)) { // 0 = left mouse button
                int clickedIndex = getLevelUpCardIndexAtMouse();
                if (clickedIndex >= 0 && clickedIndex < levelUpOptions.size()) {
                    applyLevelUpOption(clickedIndex);
                    return;
                }
            }
            
            // Mouse hover to highlight card
            int hoverIndex = getLevelUpCardIndexAtMouse();
            if (hoverIndex >= 0 && hoverIndex < levelUpOptions.size()) {
                levelUpSelectedIndex = hoverIndex;
            }
        }

        // Check for number key presses (1, 2, 3)
        if (GameApp.isKeyJustPressed(Input.Keys.NUM_1) && levelUpOptions.size() > 0) {
            applyLevelUpOption(0);
        } else if (GameApp.isKeyJustPressed(Input.Keys.NUM_2) && levelUpOptions.size() > 1) {
            applyLevelUpOption(1);
        } else if (GameApp.isKeyJustPressed(Input.Keys.NUM_3) && levelUpOptions.size() > 2) {
            applyLevelUpOption(2);
        }
    }
    
    /**
     * Get the index of the level-up card at the current mouse position.
     * Returns -1 if mouse is not over any card.
     */
    private int getLevelUpCardIndexAtMouse() {
        if (levelUpOptions == null || levelUpOptions.isEmpty()) return -1;
        
        // Get mouse position in world coordinates
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
        
        // Menu dimensions (same as in renderLevelUpMenuV2)
        int count = levelUpOptions.size();
        float menuW = screenWidth * 0.58f;
        float cardH = 70f;
        float gap = 6f;
        float headerH = 70f;
        float footerH = 10f;
        float borderPadding = 8f;
        
        float cardsH = (count * cardH) + ((count - 1) * gap);
        float totalH = headerH + cardsH + footerH + borderPadding * 2;
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float menuX = centerX - menuW / 2f;
        float topY = centerY + totalH / 2f;
        
        float cardsTopY = topY - headerH;
        float cardInnerX = menuX + borderPadding;
        float cardInnerW = menuW - borderPadding * 2;
        
        // Check each card
        for (int i = 0; i < count; i++) {
            float cardTop = cardsTopY - i * (cardH + gap);
            float cardY = cardTop - cardH;
            
            // Check if mouse is within this card's bounds
            if (worldMouseX >= cardInnerX && worldMouseX <= cardInnerX + cardInnerW &&
                worldMouseY >= cardY && worldMouseY <= cardTop) {
                return i;
            }
        }
        
        return -1;
    }

    // Apply selected level up option
    // Now handles: Weapon upgrades, Passive items, Stat upgrades, and Evolution
    private void applyLevelUpOption(int index) {
        if (index < 0 || index >= levelUpOptions.size()) return;

        LevelUpOption option = levelUpOptions.get(index);
        
        // Apply upgrade based on type
        switch (option.type) {
            case STAT:
                // Legacy stat upgrade
                player.applyStatUpgrade(option.stat);
                GameApp.log("Applied stat upgrade: " + option.title);
                break;
                
            case WEAPON:
                // Weapon level up
                weapon.levelUp();
                GameApp.log("Weapon upgraded to level " + weapon.getLevel() + "!");
                break;
                
            case PASSIVE:
                // Add or level up passive item
                player.addOrLevelUpPassiveItem(option.passiveItem);
                GameApp.log("Passive item upgraded: " + option.passiveItem.displayName);
                break;
                
            case EVOLUTION:
                // EVOLVE THE WEAPON!
                weapon.evolve();
                GameApp.log("⚡⚡⚡ WEAPON EVOLVED INTO DEATH SPIRAL! ⚡⚡⚡");
                // Play special sound for evolution
                if (soundManager != null) {
                    soundManager.playSound("levelup", 1.0f); // Play twice for dramatic effect
                    soundManager.playSound("levelup", 0.8f);
                }
                break;
                
            case BONUS_POINTS:
                // Post-evolution: Player selected random points per level
                selectedPostEvolutionBonus = LevelUpOption.Type.BONUS_POINTS;
                int bonusPoints = 50 + GameApp.randomInt(0, 151); // 50-200 points
                score += bonusPoints;
                GameApp.log("Selected BONUS_POINTS! +" + bonusPoints + " points this level up");
                break;
                
            case BONUS_HEALTH:
                // Post-evolution: Player selected +25 HP per level
                selectedPostEvolutionBonus = LevelUpOption.Type.BONUS_HEALTH;
                player.heal(25);
                GameApp.log("Selected BONUS_HEALTH! +25 HP this level up");
                break;
        }
        
        // Level up player
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
    
    /**
     * Update cursor visibility based on game state.
     * Hide cursor during active gameplay, show during interactive screens.
     */
    private void updateCursorVisibility() {
        // Determine if we should show cursor (interactive screens)
        boolean shouldShowCursor = isPaused || isGameOver || isLevelUpActive || isGachaActive || isVictoryTransition;
        
        if (shouldShowCursor && isCursorHidden) {
            // Show cursor for interactive screens
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            } else {
                Gdx.input.setCursorCatched(false);
            }
            isCursorHidden = false;
        } else if (!shouldShowCursor && !isCursorHidden) {
            // Hide cursor during gameplay using invisible cursor
            if (cursorInvisible != null) {
                Gdx.graphics.setCursor(cursorInvisible);
            } else {
                Gdx.input.setCursorCatched(true);
            }
            isCursorHidden = true;
        }
    }

    // =========================
    // ADD-ONLY (LevelUp Menu V2)
    // =========================

    private void renderLevelUpMenuV2() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        float dt = (lastDelta > 0f ? lastDelta : 1f / 60f);

        // Entrance animation (fade + slide)
        if (levelUpMenuOpening) {
            levelUpMenuAnimTimer += dt * 6.0f;
            if (levelUpMenuAnimTimer >= 1f) {
                levelUpMenuAnimTimer = 1f;
                levelUpMenuOpening = false;
            }
            // Reset falling orbs when menu opens
            levelUpOrbsInitialized = false;
        } else {
            levelUpMenuAnimTimer = 1f;
        }

        // Update arrow animation timer
        levelUpArrowAnimTimer += dt * 4f;
        if (levelUpArrowAnimTimer > Math.PI * 2) {
            levelUpArrowAnimTimer -= (float)(Math.PI * 2);
        }

        // Smooth selection highlight animation
        if (levelUpSelectedIndex != lastLevelUpSelectedIndex) {
            lastLevelUpSelectedIndex = levelUpSelectedIndex;
            levelUpSelectAnim = 0f;
        }
        levelUpSelectAnim = Math.min(1f, levelUpSelectAnim + dt * 10f);

        float t = easeOutCubic(levelUpMenuAnimTimer);

        // Semi-transparent overlay - gameplay visible behind
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        int overlayAlpha = (int)(100 * t);
        GameApp.setColor(0, 0, 0, overlayAlpha);
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();

        // Initialize and update falling orbs effect
        initAndUpdateFallingOrbs(screenWidth, screenHeight, dt);
        
        // Render falling orbs BEFORE menu (behind)
        renderFallingOrbs();

        if (levelUpOptions == null || levelUpOptions.isEmpty()) return;

        int count = levelUpOptions.size();

        // Menu dimensions - narrower width
        float menuW = screenWidth * 0.58f;
        float cardH = 70f;
        float gap = 6f;
        float headerH = 70f;
        float footerH = 10f;
        float borderWidth = 3f;
        float borderPadding = 8f;

        float cardsH = (count * cardH) + ((count - 1) * gap);
        float totalH = headerH + cardsH + footerH + borderPadding * 2;

        float menuX = centerX - menuW / 2f;
        float targetTopY = centerY + totalH / 2f;
        float startTopY = targetTopY + 60f;
        float topY = lerp(startTopY, targetTopY, t);
        float bottomY = topY - totalH;

        // Draw outer golden border frame
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(218, 165, 32, (int)(255 * t)); // Gold
        // Outer frame
        GameApp.drawRect(menuX - borderWidth, bottomY - borderWidth, menuW + borderWidth * 2, totalH + borderWidth * 2);
        GameApp.endShapeRendering();

        // Dark background inside border
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(35, 35, 45, (int)(245 * t));
        GameApp.drawRect(menuX, bottomY, menuW, totalH);
        GameApp.endShapeRendering();

        // Header - LEVEL UP! with PixelOperator font
        GameApp.startSpriteRendering();
        boolean hasEvolution = levelUpOptions.stream().anyMatch(LevelUpOption::isEvolution);
        float headerY = topY - 38f;
        String headerFont = "levelUpTitleFont"; // PixelOperator font
        if (hasEvolution) {
            GameApp.drawTextCentered(headerFont, "EVOLUTION!", centerX, headerY, "purple-500");
        } else {
            GameApp.drawTextCentered(headerFont, "LEVEL UP!", centerX, headerY, "yellow-500");
        }
        GameApp.endSpriteRendering();

        float cardsTopY = topY - headerH;
        float cardInnerX = menuX + borderPadding;
        float cardInnerW = menuW - borderPadding * 2;

        // Arrow dimensions
        float arrowW = 36f;
        float arrowH = 36f;
        float arrowOffset = (float)(Math.sin(levelUpArrowAnimTimer) * 4f); // Bounce effect

        for (int i = 0; i < count; i++) {
            LevelUpOption option = levelUpOptions.get(i);
            boolean selected = (i == levelUpSelectedIndex);

            float cardTop = cardsTopY - i * (cardH + gap);
            float cardY = cardTop - cardH;

            String icon = option.icon;

            // Draw card golden border
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(218, 165, 32, (int)(255 * t)); // Gold border
            GameApp.drawRect(cardInnerX - 2f, cardY - 2f, cardInnerW + 4f, cardH + 4f);
            GameApp.endShapeRendering();

            // Card background - gray with highlight for selected
            GameApp.startShapeRenderingFilled();
            if (selected) {
                float p = 0.6f + 0.4f * levelUpSelectAnim;
                GameApp.setColor(60, 90, 120, (int)(255 * p * t)); // Blue-gray highlight
            } else {
                GameApp.setColor(55, 55, 65, (int)(240 * t)); // Gray background
            }
            GameApp.drawRect(cardInnerX, cardY, cardInnerW, cardH);
            GameApp.endShapeRendering();

            // Draw selection arrows using arrow.png
            if (selected) {
                GameApp.startSpriteRendering();
                float arrowY = cardY + cardH / 2f - arrowH / 2f;
                
                // Left arrow - pointing right toward option (use negative width to flip)
                float leftArrowX = cardInnerX - arrowW - 8f - arrowOffset;
                if (GameApp.hasTexture("arrow_icon")) {
                    // Flip horizontally by using negative width and offset
                    GameApp.drawTexture("arrow_icon", leftArrowX + arrowW, arrowY, -arrowW, arrowH);
                } else {
                    GameApp.drawTextCentered("default", ">>", leftArrowX + arrowW/2, arrowY + arrowH/2 + 6f, "yellow-400");
                }
                
                // Right arrow (original) - pointing left toward option
                float rightArrowX = cardInnerX + cardInnerW + 8f + arrowOffset;
                if (GameApp.hasTexture("arrow_icon")) {
                    GameApp.drawTexture("arrow_icon", rightArrowX, arrowY, arrowW, arrowH);
                } else {
                    GameApp.drawTextCentered("default", "<<", rightArrowX + arrowW/2, arrowY + arrowH/2 + 6f, "yellow-400");
                }
                GameApp.endSpriteRendering();
            }

            // Icon with black background frame
            GameApp.startShapeRenderingFilled();
            float iconFrameSize = 50f;
            float iconFrameX = cardInnerX + 10f;
            float iconFrameY = cardY + cardH / 2f - iconFrameSize / 2f;
            // Gold border for icon
            GameApp.setColor(218, 165, 32, (int)(255 * t));
            GameApp.drawRect(iconFrameX - 2f, iconFrameY - 2f, iconFrameSize + 4f, iconFrameSize + 4f);
            // Black background
            GameApp.setColor(0, 0, 0, (int)(255 * t));
            GameApp.drawRect(iconFrameX, iconFrameY, iconFrameSize, iconFrameSize);
            GameApp.endShapeRendering();

            // Draw icon texture
            GameApp.startSpriteRendering();
            float iconSize = 40f;
            float iconX = iconFrameX + (iconFrameSize - iconSize) / 2f;
            float iconY = iconFrameY + (iconFrameSize - iconSize) / 2f;
            if (icon != null && GameApp.hasTexture(icon)) {
                GameApp.drawTexture(icon, iconX, iconY, iconSize, iconSize);
            }

            // Text positioning - centered vertically within card
            float textX = iconFrameX + iconFrameSize + 12f;
            float cardCenterY = cardY + cardH / 2f;
            float titleY = cardCenterY + 9f; // Title slightly above center
            float descY = cardCenterY - 20f;  // Description slightly below center

            // Title with "New!" label - use diverse PixelOperator fonts
            String titleFont = "levelUpItemFont"; // PixelOperator-Bold
            String newFont = "levelUpNewFont";    // PixelOperatorHB8 for accent
            String levelFont = "levelUpLevelFont"; // PixelOperatorSC-Bold for level
            boolean isNew = (option.isPassiveUpgrade() && option.passiveCurrentLevel == 0);
            
            // Calculate level text width first to position everything properly
            String levelText = getLevelDisplayText(option);
            float levelTextWidth = GameApp.getTextWidth(levelFont, levelText);
            float levelX = cardInnerX + cardInnerW - levelTextWidth - 15f; // More margin from right edge
            float levelY = cardCenterY; // Center the level text vertically
            
            if (isNew) {
                String itemName = option.passiveItem.displayName;
                float itemNameWidth = GameApp.getTextWidth(titleFont, itemName);
                
                // Draw item name
                GameApp.drawText(titleFont, itemName, textX, titleY, "white");
                
                // Position NEW! label to the right of item name with some spacing, but before level text
                float newLabelX = textX + itemNameWidth + 10f;
                float maxNewX = levelX - GameApp.getTextWidth(newFont, "NEW!") - 10f; // Leave space before level
                newLabelX = Math.min(newLabelX, maxNewX); // Don't overlap with level text
                
                GameApp.drawText(newFont, "NEW!", newLabelX, titleY, "orange-400");
            } else {
                String title = option.isPassiveUpgrade() ? option.passiveItem.displayName : option.title;
                GameApp.drawText(titleFont, title, textX, titleY, "white");
            }

            // Level display on right - properly positioned within frame
            String levelColor = levelText.equals("MAX") ? "yellow-400" : "cyan-300";
            GameApp.drawText(levelFont, levelText, levelX, levelY, levelColor);

            // Description - larger and more readable
            String descText = getFormattedDescription(option);
            GameApp.drawText("levelUpDescFont", descText, textX, descY, "gray-200");

            GameApp.endSpriteRendering();
        }
    }

    /**
     * Initialize and update falling orbs effect for level up menu.
     * Uses 3 orb types with equal distribution (33% each) for visual effect.
     * Orbs can rotate randomly while falling.
     */
    private void initAndUpdateFallingOrbs(float screenWidth, float screenHeight, float dt) {
        float topY = screenHeight + 30f; // Start from above screen
        float fadeStartY = screenHeight * 0.25f; // Start fading at 3/4 screen (25% from bottom)
        float bottomY = screenHeight * 0.05f; // Completely disappear at 5% from bottom
        
        // Initialize orbs if not done - EXTREMELY DENSE for beautiful effect like image
        if (!levelUpOrbsInitialized) {
            levelUpFallingOrbs.clear();
            // Create MANY MORE orbs for very dense rain effect (500 orbs - doubled from 250)
            int orbCount = 500;
            for (int i = 0; i < orbCount; i++) {
                float x = GameApp.random(10f, screenWidth - 10f);
                // Spread orbs from top to fade position for immediate rain effect
                float y = GameApp.random(fadeStartY, topY);
                float speed = GameApp.random(50f, 120f); // Faster falling
                float size = GameApp.random(10f, 18f);
                // Random orb type: equal distribution (33% each)
                OrbType orbType = getRandomOrbTypeForMenu();
                levelUpFallingOrbs.add(new FallingOrb(x, y, speed, size, orbType));
            }
            levelUpOrbsInitialized = true;
        }
        
        // Update orbs - fall down, fade, shrink, rotate, and respawn at top
        for (FallingOrb orb : levelUpFallingOrbs) {
            orb.y -= orb.speed * dt;
            
            // Update rotation
            orb.rotation += orb.rotationSpeed * dt;
            // Keep rotation in 0-360 range
            if (orb.rotation > 360f) orb.rotation -= 360f;
            if (orb.rotation < 0f) orb.rotation += 360f;
            
            // Calculate alpha AND size based on position (fade out and shrink between fadeStartY and bottomY)
            if (orb.y < fadeStartY) {
                // Calculate fade: 1.0 at fadeStartY, 0.0 at bottomY
                float fadeRange = fadeStartY - bottomY;
                float fadeProgress = (fadeStartY - orb.y) / fadeRange;
                fadeProgress = Math.max(0f, Math.min(1f, fadeProgress)); // Clamp 0-1
                
                // Fade out alpha
                orb.alpha = Math.max(0f, 1f - fadeProgress);
                
                // Shrink size as it fades (from original size to 0)
                // Store original size in a temp variable if needed, or recalculate
                // For simplicity, we'll shrink proportionally to alpha
                float originalSize = orb.size / Math.max(0.01f, 1f - fadeProgress + 0.01f); // Reverse calculate
                orb.size = originalSize * orb.alpha;
            } else {
                orb.alpha = 1f;
                // Size stays at original when above fade line
            }
            
            // Respawn at top when completely faded or below bottom
            if (orb.y < bottomY || orb.alpha <= 0f) {
                orb.y = topY + GameApp.random(0f, 50f);
                orb.x = GameApp.random(10f, screenWidth - 10f);
                orb.speed = GameApp.random(50f, 120f);
                orb.size = GameApp.random(10f, 18f); // Reset size
                orb.alpha = 1f;
                // Randomize rotation again on respawn
                if (GameApp.random(0f, 1f) < 0.5f) {
                    orb.rotationSpeed = GameApp.random(-180f, 180f);
                } else {
                    orb.rotationSpeed = 0f;
                }
                orb.orbType = getRandomOrbTypeForMenu();
            }
        }
    }
    
    /**
     * Get random orb type for menu falling effect: equal distribution (33% each)
     * This is just for visual effect, not gameplay balance
     */
    private OrbType getRandomOrbTypeForMenu() {
        float roll = GameApp.random(0f, 100f);
        if (roll < 33.33f) return OrbType.BLUE;
        if (roll < 66.66f) return OrbType.GREEN;
        return OrbType.RED;
    }

    /**
     * Render falling orbs effect with fade out, rotation, and 3 orb types.
     */
    private void renderFallingOrbs() {
        if (levelUpFallingOrbs.isEmpty()) return;
        
        GameApp.enableTransparency();
        GameApp.startSpriteRendering();
        
        // Check if new orb textures are available
        boolean hasOrbTextures = GameApp.hasTexture("orb_blue") && 
                                  GameApp.hasTexture("orb_green") && 
                                  GameApp.hasTexture("orb_red");
        
        for (FallingOrb orb : levelUpFallingOrbs) {
            if (orb.alpha <= 0.01f) continue; // Skip invisible orbs
            
            // Set alpha for fade effect
            int alpha = (int)(orb.alpha * 255);
            GameApp.setColor(255, 255, 255, alpha);
            
            if (hasOrbTextures) {
                // Use new orb textures with rotation
                String textureName = orb.orbType.getTextureName();
                GameApp.drawTexture(textureName, 
                    orb.x - orb.size/2, orb.y - orb.size/2, 
                    orb.size, orb.size, 
                    orb.rotation, false, false);
            } else if (GameApp.hasAnimation("orb_animation")) {
                // Fallback to legacy animation (no rotation support)
                GameApp.drawAnimation("orb_animation", orb.x - orb.size/2, orb.y - orb.size/2, orb.size, orb.size);
            }
        }
        // Reset color to full opacity
        GameApp.setColor(255, 255, 255, 255);
        GameApp.endSpriteRendering();
    }

    /**
     * Format description text professionally for level up options.
     */
    private String getFormattedDescription(LevelUpOption option) {
        if (option.isPassiveUpgrade()) {
            PassiveItemType item = option.passiveItem;
            int nextLevel = option.passiveNextLevel;
            
            return switch (item) {
                case SWIFT_BOOTS -> "+" + (nextLevel * 10) + "% movement speed per level";
                case LIFE_ESSENCE -> "+" + String.format("%.1f", nextLevel * 0.2f) + " HP/sec per level";
                case MAGNET_STONE -> "+" + (nextLevel * 20) + "% pickup range per level";
                case POWER_HERB -> "+" + (nextLevel * 10) + "% damage per level";
                case IRON_SHIELD -> "-" + (nextLevel * 5) + "% damage taken per level";
                case LUCKY_COIN -> "+" + (nextLevel * 5) + "% critical chance per level";
                case VITALITY_CORE -> "+" + (nextLevel * 20) + "% max HP per level";
            };
        } else if (option.isWeaponUpgrade()) {
            return option.description;
        } else if (option.isEvolution()) {
            return "Transform your weapon into its ultimate form!";
        }
        return option.description;
    }
    
    // Helper method to get level display text for different option types
    // Format: "Lv.X" or "MAX" when at max level
    private String getLevelDisplayText(LevelUpOption option) {
        return switch (option.type) {
            case STAT -> {
                int currentLevel = player.getUpgradeLevel(option.stat);
                int maxLevel = player.getMaxUpgradeLevel(option.stat);
                int nextLevel = Math.min(currentLevel + 1, maxLevel);
                yield nextLevel >= maxLevel ? "MAX" : "Lv." + nextLevel;
            }
            case WEAPON -> {
                int nextLevel = option.weaponNextLevel;
                int maxLevel = weapon.getMaxLevel();
                yield nextLevel >= maxLevel ? "MAX" : "Lv." + nextLevel;
            }
            case PASSIVE -> {
                int nextLevel = option.passiveNextLevel;
                int maxLevel = option.passiveItem.maxLevel;
                yield nextLevel >= maxLevel ? "MAX" : "Lv." + nextLevel;
            }
            case EVOLUTION -> "MAX";
            case BONUS_POINTS, BONUS_HEALTH -> ""; // No level display for bonus options
        };
    }

    private String buildPreviewText(StatUpgradeType type, int currentLevel) {
        int maxLevel = 5;
        int nextLevel = Math.min(currentLevel + 1, maxLevel);

        String n = (type == null ? "" : type.name().toUpperCase());

        // Generic fallback preview (works even if enum names vary)
        if (n.contains("HEALTH") || n.contains("HP")) {
            int cur = 10 + currentLevel * 2;
            int nw = 10 + nextLevel * 2;
            return "Current: " + cur + " \u2192 New: " + nw;
        }
        if (n.contains("SPEED") || n.contains("MOVE")) {
            int cur = 100 + currentLevel * 6;
            int nw = 100 + nextLevel * 6;
            return "Current: +" + cur + "% \u2192 New: +" + nw + "%";
        }
        if (n.contains("FIRE") || n.contains("RATE") || n.contains("ATTACK")) {
            int cur = 100 + currentLevel * 8;
            int nw = 100 + nextLevel * 8;
            return "Current: +" + cur + "% \u2192 New: +" + nw + "%";
        }
        if (n.contains("DAMAGE") || n.contains("DMG")) {
            int cur = 100 + currentLevel * 10;
            int nw = 100 + nextLevel * 10;
            return "Current: +" + cur + "% \u2192 New: +" + nw + "%";
        }
        if (n.contains("XP") || n.contains("EXP")) {
            int cur = 100 + currentLevel * 10;
            int nw = 100 + nextLevel * 10;
            return "Current: +" + cur + "% \u2192 New: +" + nw + "%";
        }

        return "Current: Level " + currentLevel + " \u2192 New: Level " + nextLevel;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float easeOutCubic(float x) {
        float a = 1f - x;
        return 1f - (a * a * a);
    }

    private String iconFor(StatUpgradeType type) {
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return "⚔";
        if (n.contains("FIRE") || n.contains("RATE") || n.contains("ATTACK")) return "⚡";
        if (n.contains("HEALTH") || n.contains("HP")) return "❤";
        if (n.contains("SPEED") || n.contains("MOVE")) return "➤";
        if (n.contains("XP") || n.contains("EXP")) return "✦";
        return "★";
    }

    private String themeTextColor(StatUpgradeType type) {
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return "red-500";
        if (n.contains("FIRE") || n.contains("RATE") || n.contains("ATTACK")) return "yellow-500";
        if (n.contains("HEALTH") || n.contains("HP")) return "green-500";
        if (n.contains("SPEED") || n.contains("MOVE")) return "blue-500";
        if (n.contains("XP") || n.contains("EXP")) return "purple-500";
        return "white";
    }

    private int[] themeRGB(StatUpgradeType type) {
        String n = (type == null ? "" : type.name().toUpperCase());
        if (n.contains("DAMAGE") || n.contains("DMG")) return new int[]{231, 76, 60};
        if (n.contains("FIRE") || n.contains("RATE") || n.contains("ATTACK")) return new int[]{241, 196, 15};
        if (n.contains("HEALTH") || n.contains("HP")) return new int[]{46, 204, 113};
        if (n.contains("SPEED") || n.contains("MOVE")) return new int[]{52, 152, 219};
        if (n.contains("XP") || n.contains("EXP")) return new int[]{155, 89, 182};
        return new int[]{255, 255, 255};
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

    }

    // =========================
    // GAME FLOW / RESET
    // =========================

    private void resetGame() {
        // Reset game state to PLAYING (important for Play Again button)
        gameStateManager.setCurrentState(GameState.PLAYING);

        // CRITICAL: Reset WinnerScreen state to prevent showing old win data
        WinnerScreen.resetState();

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
        float speed = 95f; // Increased for easier gameplay
        int maxHealth = 15; // Increased base health for easier gameplay

        player = new Player(startX, startY, speed, maxHealth, null);

        // Set health text callback for regen display
        player.setHealthTextCallback((amount, x, y) -> {
            damageTextSystem.spawnHealthText(x, y, amount);
        });

        bullets = new ArrayList<>();
        // Weapon với random damage: 5-15 (enemy health 15, chết trong 1-3 hit)
        // Increased fire rate from 1.5 to 2.5 shots per second for faster shooting
        // BALANCED WEAPON STATS (Vampire Survivors feel):
        // Fire rate 3.0 = moderate base (upgrades increase this significantly)
        // Damage 8-15 = good base damage, scales well with upgrades
        // Bullet speed 480 = fast bullets for responsive gameplay
        // Bullet size 14x14 for good visibility
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 3.0f, 8, 15, 480f, 14f, 14f);

        enemies = new ArrayList<>();
        bosses = new ArrayList<>();
        xpOrbs = new ArrayList<>();
        breakableObjects = new ArrayList<>();
        healingItems = new ArrayList<>();
        treasureChests = new ArrayList<>();
        cats = new ArrayList<>(); // Background cats for decoration
        zombieHands = new ArrayList<>(); // Background zombie hands for decoration
        bossesThatSpawnedChest.clear(); // Reset tracking set

        isLevelUpActive = false;
        isGachaActive = false;
        gachaCooldown = 0f; // Reset gacha cooldown
        currentRound = 0;
        levelUpOptions.clear();
        selectedPostEvolutionBonus = null; // Reset post-evolution bonus selection
        
        // Initialize gacha system
        gachaSystem = new GachaSystem();
        gachaSystem.setPlayer(player);
        gachaSystem.setWeapon(weapon);
        gachaSystem.setSoundManager(soundManager);

        // ============================================
        // SHOWCASE MODE - For demo/presentation
        // ============================================
        GameConfig config = ConfigManager.loadConfig();
        if (config.showcaseMode) {
            GameApp.log("=== SHOWCASE MODE ENABLED ===");
            
            // 1. Add all passive items at max level (level 5)
            for (PassiveItemType passiveType : PassiveItemType.values()) {
                // Add passive item and level it up to max
                for (int lvl = 0; lvl < passiveType.maxLevel; lvl++) {
                    player.addOrLevelUpPassiveItem(passiveType);
                }
                GameApp.log("Added " + passiveType.displayName + " at max level " + passiveType.maxLevel);
            }
            
            // 2. Level up weapon to max (level 8)
            while (!weapon.isMaxLevel()) {
                weapon.levelUp();
            }
            GameApp.log("Weapon leveled to max: " + weapon.getLevel());
            
            // 3. Evolve weapon (since all conditions are met)
            weapon.evolve();
            GameApp.log("Weapon evolved to DEATH SPIRAL!");
            
            // 4. Set time to 1 minute remaining (14 minutes elapsed)
            // GAME_DURATION = 600s (10 min), we want 1 min left = 60s
            // But user said "đã qua 14 phút" so maybe GAME_DURATION is 15 min (900s)?
            // Let's set to 60f directly for 1 minute countdown
            gameTime = 60f;
            GameApp.log("Time set to 1 minute remaining (showcase mode)");
            
            // 5. Update HUD to show all items
            hud.setWeapon(weapon);
            hud.setPassiveItems(player.getOwnedPassiveItems());
            
            GameApp.log("=== SHOWCASE MODE SETUP COMPLETE ===");
        }

        // Reset game state
        gameTime = config.showcaseMode ? 60f : GAME_DURATION;
        score = 0;
        killCount = 0; // Reset kill count for new game
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

        // Spawn breakable objects randomly across all 16 rooms
        breakableObjects.clear();
        healingItems.clear();
        spawnBreakableObjectsInAllRooms();
        GameApp.log("Spawned breakable objects in all rooms");
        
        // Spawn background cats randomly across all 16 rooms
        cats.clear();
        spawnCatsInAllRooms();
        GameApp.log("Spawned background cats in all rooms");
        
        // Spawn background zombie hands randomly across all 16 rooms
        zombieHands.clear();
        spawnZombieHandsInAllRooms();
        GameApp.log("Spawned background zombie hands in all rooms");

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

        // Calculate button size from texture dimensions with improved scaling
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");

        float buttonWidth = texW / 1.4f; // Bigger buttons for better visibility
        float buttonHeight = texH / 1.4f;
        float buttonSpacing = 12f; // Tighter spacing to reduce empty space

        // Position buttons closer together in center
        float totalButtonHeight = (buttonHeight * 3) + (buttonSpacing * 2);
        float startY = centerY - 20f + totalButtonHeight / 2f - buttonHeight; // Center the button group

        // Resume button (green) - top with improved positioning
        float resumeY = startY;
        Button resumeButton = new Button(centerX - buttonWidth / 2, resumeY, buttonWidth, buttonHeight, "");
        resumeButton.setOnClick(() -> {
            // Handled by handlePauseInput with delay
        });
        if (GameApp.hasTexture("green_long")) {
            resumeButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        pauseButtons.add(resumeButton);

        // Settings button (orange) - middle with better spacing
        float settingsY = resumeY - buttonHeight - buttonSpacing;
        Button settingsButton = new Button(centerX - buttonWidth / 2, settingsY, buttonWidth, buttonHeight, "");
        settingsButton.setOnClick(() -> {
            // Handled by handlePauseInput with delay
        });
        if (GameApp.hasTexture("orange_long")) {
            settingsButton.setSprites("orange_long", "orange_long", "orange_long", "orange_pressed_long");
        }
        pauseButtons.add(settingsButton);

        // Quit button (red) - bottom with consistent spacing
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
                            // Set PlayScreen reference for frozen background
                            SettingsScreen.setPlayScreenReference(this);
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

        // Draw enhanced dark overlay with gradient effect for more professional look
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        
        // Main dark overlay with improved opacity
        GameApp.setColor(0, 0, 0, (int)(fadeAlpha * 180)); // Darker overlay for better contrast
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        
        // Add subtle vignette effect around the menu area
        float menuAreaWidth = screenWidth * 0.6f;
        float menuAreaHeight = screenHeight * 0.8f;
        float menuX = (screenWidth - menuAreaWidth) / 2f;
        float menuY = (screenHeight - menuAreaHeight) / 2f;
        
        // Subtle highlight around menu area
        GameApp.setColor(255, 255, 255, (int)(fadeAlpha * 8)); // Very subtle white glow
        GameApp.drawRect(menuX - 10, menuY - 10, menuAreaWidth + 20, menuAreaHeight + 20);
        
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
        float titleY = centerY - 120f; // Position above buttons but visible

        // Add subtle drop shadow effect for the title
        GameApp.setColor(0, 0, 0, 100); // Semi-transparent black shadow
        GameApp.drawTexture("pause_title", titleX + 2, titleY - 2, titleWidth, titleHeight);
        
        // Draw main title
        GameApp.setColor(255, 255, 255, 255); // Full opacity white
        GameApp.drawTexture("pause_title", titleX, titleY, titleWidth, titleHeight);
    }

    /**
     * Draw pause menu button text labels.
     * Using unified colors for better contrast on colored buttons.
     */
    private void renderPauseButtonText(float centerX, float centerY) {
        if (pauseButtons == null || pauseButtons.size() < 3) return;

        String fontName = "buttonFontSmall"; // Small font for 640x360 world
        
        // Enhanced button text rendering with improved visual hierarchy
        
        // Resume button text (green button) - Primary action
        Button resumeButton = pauseButtons.get(0);
        float resumeCenterX = resumeButton.getX() + resumeButton.getWidth() / 2;
        float resumeCenterY = resumeButton.getY() + resumeButton.getHeight() / 2;
        float resumeTextHeight = GameApp.getTextHeight(fontName, "RESUME");
        float resumeAdjustedY = resumeCenterY + resumeTextHeight * 0.15f;
        
        // Add text shadow for better readability
        GameApp.drawTextCentered(fontName, "RESUME", resumeCenterX + 1, resumeAdjustedY - 1, "black");
        GameApp.drawTextCentered(fontName, "RESUME", resumeCenterX, resumeAdjustedY, "button_green_text");

        // Settings button text (orange button) - Secondary action
        Button settingsButton = pauseButtons.get(1);
        float settingsCenterX = settingsButton.getX() + settingsButton.getWidth() / 2;
        float settingsCenterY = settingsButton.getY() + settingsButton.getHeight() / 2;
        float settingsTextHeight = GameApp.getTextHeight(fontName, "SETTINGS");
        float settingsAdjustedY = settingsCenterY + settingsTextHeight * 0.15f;
        
        // Add text shadow
        GameApp.drawTextCentered(fontName, "SETTINGS", settingsCenterX + 1, settingsAdjustedY - 1, "black");
        GameApp.drawTextCentered(fontName, "SETTINGS", settingsCenterX, settingsAdjustedY, "button_orange_text");

        // Quit button text (red button) - Destructive action
        Button quitButton = pauseButtons.get(2);
        float quitCenterX = quitButton.getX() + quitButton.getWidth() / 2;
        float quitCenterY = quitButton.getY() + quitButton.getHeight() / 2;
        float quitTextHeight = GameApp.getTextHeight(fontName, "QUIT GAME");
        float quitAdjustedY = quitCenterY + quitTextHeight * 0.15f;
        
        // Add text shadow
        GameApp.drawTextCentered(fontName, "QUIT GAME", quitCenterX + 1, quitAdjustedY - 1, "black");
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
    
    /**
     * Render frozen game state for overlay screens with consistent scaling.
     * This method ensures all overlay screens show the same frozen game background.
     */
    public void renderFrozenGameBackground() {
        try {
            // Render the game world in its frozen state
            if (mapRenderer != null) {
                mapRenderer.render(playerWorldX, playerWorldY);
            }
            
            GameApp.startSpriteRendering();
            
            // Render game entities
            if (gameRenderer != null) {
                gameRenderer.renderPlayer();
                gameRenderer.renderEnemies(enemies);
                gameRenderer.renderStampedeZombies(enemySpawner.getStampedeZombies());
                gameRenderer.renderBosses(bosses);
                gameRenderer.renderBullets(bullets);
            }
            
            GameApp.endSpriteRendering();
            
            // Render additional game elements without HUD for cleaner background
            renderPlayerHealthBar();
            
            // Render player blood particles
            if (player != null) {
                player.renderBloodParticles(playerWorldX, playerWorldY);
            }
            
            renderBreakableObjectParticles();
            renderXPOrbs();
            renderHealingItems();
            
        } catch (Exception e) {
            GameApp.log("Error rendering frozen game background: " + e.getMessage());
        }
    }
    
    /**
     * Legacy method for backward compatibility - redirects to unified method.
     * @deprecated Use renderFrozenGameBackground() instead
     */
    @Deprecated
    public void renderFrozenGameForSettings() {
        renderFrozenGameBackground();
    }

    /**
     * Spawn MiniBoss at end of round with HP scaling
     */
    private void spawnMiniBossNearPlayer(float playerX, float playerY, float moveDirX, float moveDirY, int round) {
        float distance = 400f + (float) (Math.random() * 100f);

        float bx;
        float by;

        float length = (float) Math.sqrt(moveDirX * moveDirX + moveDirY * moveDirY);
        if (length > 0.001f) {
            float nx = moveDirX / length;
            float ny = moveDirY / length;

            // Spawn behind player
            bx = playerX - nx * distance;
            by = playerY - ny * distance;
        } else {
            double angle = Math.random() * Math.PI * 2.0;
            bx = playerX + (float) Math.cos(angle) * distance;
            by = playerY + (float) Math.sin(angle) * distance;
        }

        // HP scaled based on elapsed time - EASIER early game
        float elapsedMinutes = (GAME_DURATION - gameTime) / 60f;
        int baseHP;
        float hpMultiplier;
        
        if (elapsedMinutes < 1f) {
            // First minute: very easy boss (50% HP)
            baseHP = 800;
            hpMultiplier = 1f;
        } else if (elapsedMinutes < 2f) {
            // Second minute: easy boss (60% HP)
            baseHP = 1000;
            hpMultiplier = 1f;
        } else if (elapsedMinutes < 3f) {
            // Third minute: moderate boss (75% HP)
            baseHP = 1400;
            hpMultiplier = 1f;
        } else {
            // After minute 3: normal scaling
            baseHP = 2000;
            hpMultiplier = (float) Math.pow(1.15f, round - 3); // 15% per round after round 3
            hpMultiplier = Math.min(hpMultiplier, 4f); // Cap at 4x
        }
        int hp = (int)(baseHP * hpMultiplier) + (int) (Math.random() * 301); // +0-300 random

        // Boss now uses random zombie type (handled in Boss constructor)
        Boss boss = new Boss(bx, by, hp);

        if (bosses != null) {
            bosses.add(boss);
        }

        // Note: Removed levelup sound from miniboss spawn to avoid confusion with actual level up

        GameApp.log("MiniBoss (Round " + round + ") spawned at (" + bx + ", " + by + ") with HP " + hp);
    }
    
    /**
     * Spawn late wave miniboss (minute 7+) - these only drop red orbs, no chests
     */
    private void spawnLateWaveMiniBoss(float playerX, float playerY) {
        float distance = 350f + (float) (Math.random() * 150f);
        double angle = Math.random() * Math.PI * 2.0;
        float bx = playerX + (float) Math.cos(angle) * distance;
        float by = playerY + (float) Math.sin(angle) * distance;
        
        // Late wave bosses have less HP but spawn frequently
        float elapsedTime = GAME_DURATION - gameTime;
        int baseHP = 1200; // Lower HP for frequent late wave bosses
        float hpMultiplier = 1f + (elapsedTime / 60f) * 0.15f; // 15% more HP per minute
        int hp = (int)(baseHP * hpMultiplier) + (int) (Math.random() * 300);
        
        Boss boss = new Boss(bx, by, hp);
        if (bosses != null) {
            bosses.add(boss);
            lateWaveBosses.add(boss); // Mark as late wave boss (no chest drop)
        }
    }
    
    /**
     * Update late wave miniboss spawning (minute 7+)
     * 7:00-8:00: 50% normal zombies, 50% miniboss
     * 8:00-9:00: 30% normal zombies, 70% miniboss  
     * 9:00-9:30: 10% normal zombies, 90% miniboss
     * 9:30-10:00: 100% miniboss
     */
    private void updateLateWaveSpawning(float delta, float playerX, float playerY) {
        float elapsedTime = GAME_DURATION - gameTime; // Time played (0 to 600)
        
        // Only active from minute 7 (420s) onwards
        if (elapsedTime < 420f) return;
        
        lateWaveBossSpawnTimer += delta;
        if (lateWaveBossSpawnTimer < LATE_WAVE_BOSS_SPAWN_INTERVAL) return;
        lateWaveBossSpawnTimer = 0f;
        
        // Calculate miniboss spawn chance based on time - INCREASED for more difficulty
        float minibossChance;
        int bossCount;
        if (elapsedTime >= 570f) {
            // 9:30-10:00 (last 30 seconds): 100% miniboss, spawn 4-5 at once
            minibossChance = 1.0f;
            bossCount = 4 + (int)(Math.random() * 2);
        } else if (elapsedTime >= 540f) {
            // 9:00-9:30: 100% miniboss, spawn 3-4 at once
            minibossChance = 1.0f;
            bossCount = 3 + (int)(Math.random() * 2);
        } else if (elapsedTime >= 480f) {
            // 8:00-9:00: 90% miniboss, spawn 2-3 at once
            minibossChance = 0.9f;
            bossCount = 2 + (int)(Math.random() * 2);
        } else {
            // 7:00-8:00: 75% miniboss, spawn 1-2 at once
            minibossChance = 0.75f;
            bossCount = 1 + (int)(Math.random() * 2);
        }
        
        // Roll for miniboss spawn
        if (Math.random() < minibossChance) {
            for (int i = 0; i < bossCount; i++) {
                spawnLateWaveMiniBoss(playerX, playerY);
            }
        }
    }

    /**
     * Spawn treasure chest when MiniBoss is killed
     * CRITICAL: Only spawn once per boss to prevent duplicates
     * Late wave bosses (minute 7+) only spawn red orbs, no chests
     * If player has maxed all upgrades, only spawn red orbs
     */
    private void spawnTreasureChestAtBoss(Boss boss) {
        if (boss == null || treasureChests == null) return;
        
        // CRITICAL: Check if this boss already spawned a chest (prevent duplicates)
        if (bossesThatSpawnedChest.contains(boss)) {
            return;
        }
        
        // Mark this boss as having spawned rewards
        bossesThatSpawnedChest.add(boss);
        
        // Always spawn RED XP orbs (high value) around the boss
        int orbCount = GameApp.randomInt(5, 10);
        for (int i = 0; i < orbCount; i++) {
            float offsetX = GameApp.random(-30f, 30f);
            float offsetY = GameApp.random(-30f, 30f);
            XPOrb orb = new XPOrb(boss.getX() + offsetX, boss.getY() + offsetY, OrbType.RED);
            xpOrbs.add(orb);
        }
        
        // Check if this is a late wave boss (no chest)
        if (lateWaveBosses.contains(boss)) {
            GameApp.log("Late wave boss killed - only red orbs spawned (no chest)");
            lateWaveBosses.remove(boss);
            return;
        }
        
        // Check if player has maxed all upgrades (weapon max + all passives max)
        boolean allUpgradesMaxed = weapon != null && weapon.isMaxLevel() && 
                                   player != null && player.areAllPassiveItemsMaxed();
        if (allUpgradesMaxed) {
            GameApp.log("All upgrades maxed - only red orbs spawned (no chest)");
            return;
        }
        
        // Spawn treasure chest at boss position
        TreasureChest chest = new TreasureChest(boss.getX(), boss.getY());
        treasureChests.add(chest);
        
        GameApp.log("Treasure chest spawned at MiniBoss position (" + boss.getX() + ", " + boss.getY() + ")");
    }

    // Cooldown to prevent rapid gacha triggers
    private float gachaCooldown = 0f;
    private static final float GACHA_COOLDOWN_DURATION = 3.0f; // 3 seconds cooldown after gacha (increased to prevent rapid triggers)
    
    /**
     * Update treasure chests - check for player proximity and trigger gacha
     */
    private void updateTreasureChests(float delta) {
        if (treasureChests == null) return;
        
        // CRITICAL: Don't process chests if game is over
        if (isGameOver) {
            return;
        }
        
        // Update gacha cooldown
        if (gachaCooldown > 0f) {
            gachaCooldown -= delta;
        }
        
        // Always cleanup collected chests first
        treasureChests.removeIf(TreasureChest::isCollected);
        
        // Don't process chests while gacha is active or on cooldown
        // But DON'T clear other chests - they should remain for later
        if (isGachaActive || gachaCooldown > 0f) {
            return;
        }
        
        // Update all chests
        for (TreasureChest chest : treasureChests) {
            chest.update(delta, playerWorldX, playerWorldY);
        }
        
        // Find ONE chest ready for gacha
        TreasureChest chestToTrigger = null;
        for (TreasureChest chest : treasureChests) {
            if (chest.isReadyForGacha()) {
                chestToTrigger = chest;
                break;
            }
        }
        
        // Trigger gacha for the chest (only if game is not over)
        if (chestToTrigger != null && !isGachaActive && gachaCooldown <= 0f && !isGameOver) {
            // Save chest position BEFORE removing
            float chestX = chestToTrigger.getX();
            float chestY = chestToTrigger.getY();
            
            // CRITICAL: Mark and remove chest IMMEDIATELY
            chestToTrigger.triggerGacha();
            chestToTrigger.collect();
            treasureChests.remove(chestToTrigger);
            
            // CRITICAL: Cleanup ANY other chests that might be ready (safety check)
            int beforeExtraCleanup = treasureChests.size();
            treasureChests.removeIf(chest -> chest.isOpened() || chest.isReadyForGacha());
            if (treasureChests.size() != beforeExtraCleanup) {
                GameApp.log("WARNING: Removed " + (beforeExtraCleanup - treasureChests.size()) + " additional ready chests!");
            }
            
            // Then start gacha with chest position
            startGacha(chestX, chestY);
            GameApp.log("Chest triggered gacha at (" + chestX + ", " + chestY + "). Remaining chests: " + treasureChests.size());
        }
    }

    /**
     * Render treasure chests (call within sprite rendering block)
     * NOTE: Does not render chests when gacha is active
     */
    private void renderTreasureChests() {
        if (treasureChests == null) return;
        if (isGachaActive) return; // Don't render chests during gacha
        
        // Cleanup before rendering
        treasureChests.removeIf(TreasureChest::isCollected);
        
        for (TreasureChest chest : treasureChests) {
            // Double check - don't render collected or opened chests
            if (!chest.isCollected() && !chest.isReadyForGacha()) {
                chest.render(playerWorldX, playerWorldY);
            }
        }
    }

    /**
     * Start the gacha sequence
     * GachaSystem handles its own sound/music management
     */
    private void startGacha(float chestX, float chestY) {
        if (gachaSystem == null) return;
        
        isGachaActive = true;
        
        // Set chest position before starting
        gachaSystem.setChestPosition(chestX, chestY);
        gachaSystem.start(); // GachaSystem will stop music and play jackpot sound
        
        GameApp.log("Gacha started at chest position (" + chestX + ", " + chestY + ")!");
    }

    /**
     * Handle input for gacha system
     * GachaSystem handles its own sound/music management
     */
    private void handleGachaInput() {
        if (gachaSystem == null || !gachaSystem.isActive()) return;
        
        // SPACE to skip animation (jump to final result)
        if (!gachaSystem.isCompleted() && GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            gachaSystem.requestSkip();
            return;
        }
        
        // Only accept input when gacha is completed
        if (gachaSystem.isCompleted()) {
            // Check for Done button click OR keyboard input
            boolean doneClicked = gachaSystem.isDoneButtonClicked();
            boolean keyboardPressed = GameApp.isKeyJustPressed(Input.Keys.ENTER) || 
                                     GameApp.isKeyJustPressed(Input.Keys.SPACE);
            
            if (doneClicked || keyboardPressed) {
                // Apply the upgrade (only applies once due to internal flag)
                gachaSystem.applyUpgrade();
                
                // Close gacha (will restore music automatically)
                gachaSystem.close();
                isGachaActive = false;
                
                // CRITICAL: Cleanup ALL chests that might be in OPENED or ready state
                if (treasureChests != null) {
                    int beforeSize = treasureChests.size();
                    // Remove all collected chests
                    treasureChests.removeIf(TreasureChest::isCollected);
                    // Also remove any chests that are opened (shouldn't exist, but safety check)
                    treasureChests.removeIf(chest -> chest.isOpened() || chest.isReadyForGacha());
                    int afterSize = treasureChests.size();
                    if (beforeSize != afterSize) {
                        GameApp.log("Cleaned up " + (beforeSize - afterSize) + " chests after gacha. Remaining: " + afterSize);
                    }
                }
                
                // Set cooldown to prevent immediate re-trigger
                gachaCooldown = GACHA_COOLDOWN_DURATION;
                
                // Play click sound
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.5f);
                }
                
                GameApp.log("Gacha completed and upgrade applied! All chests cleaned up. Cooldown started.");
            }
        }
    }

}
