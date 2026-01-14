package nl.saxion.game.screens;

import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

/**
 * How to Play screen with pagination showing:
 * Page 1: Gameplay instructions with enemy renders
 * Page 2: All 7 passive items with descriptions
 * Page 3: Upgrade stats for piston and revolution mechanics
 */
public class HowToPlayScreen extends ScalableGameScreen {
    
    private static final int TOTAL_PAGES = 3;
    private int currentPage = 1;
    
    // UI Components
    private List<Button> buttons;
    private SoundManager soundManager;
    private boolean resourcesLoaded = false;
    
    // Button dimensions and positions
    private float buttonWidth;
    private float buttonHeight;
    
    // Animation and interaction
    private float pressDelay = 0.3f;
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;
    
    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    
    // Page transition animation
    private float pageTransitionTimer = 0f;
    private boolean isTransitioning = false;
    private int targetPage = 1;
    
    // WASD key animation
    private float wasdAnimTimer = 0f;
    private int currentKeyIndex = 0; // 0=W, 1=A, 2=S, 3=D
    private float keyPressDuration = 0.8f; // Duration each key stays pressed
    private String[] keyNames = {"W", "A", "S", "D"};
    
    // Navigation button bounds (for click detection)
    private float prevButtonX, prevButtonY, prevButtonW, prevButtonH;
    private float nextButtonX, nextButtonY, nextButtonW, nextButtonH;
    
    // Passive items data for page 2 - using actual game data from PassiveItemType
    private static final String[] PASSIVE_ITEMS = {
        "Power Herb", "Iron Shield", "Swift Boots", 
        "Lucky Coin", "Magnet Stone", "Life Essence", "Vitality Core"
    };
    
    private static final String[] PASSIVE_DESCRIPTIONS = {
        "+10% damage per level",
        "-5% damage taken per level", 
        "+10% movement speed per level",
        "+5% critical chance per level",
        "+20% pickup range per level",
        "+0.2 HP/sec per level",
        "+20% max HP per level"
    };
    
    // Enemy data for page 1
    private static final String[] ENEMY_TYPES = {
        "Basic Zombie", "Fast Runner", "Tank Zombie", "Boss Enemy"
    };
    
    private static final String[] ENEMY_DESCRIPTIONS = {
        "Slow movement, low health, appears in groups",
        "Fast movement, low health, dangerous in numbers",
        "Slow movement, very high health, heavy damage",
        "Unique abilities, massive health, rare spawns"
    };
    
    public HowToPlayScreen() {
        super(1280, 720);
    }
    
    @Override
    public void show() {
        loadCursors();
        
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        GameConfig config = ConfigManager.loadConfig();
        soundManager.setMasterVolume(config.masterVolume);
        soundManager.setMusicVolume(config.musicVolume);
        soundManager.setSFXVolume(config.sfxVolume);
        
        if (soundManager != null) {
            soundManager.playMusic(true);
        }
        
        loadResources();
        createButtons();
        
        // Force reload WASD sprite sheets since they get disposed in hide()
        GameApp.addSpriteSheet("key_W", "assets/ui/W.png", 19, 21);
        GameApp.addSpriteSheet("key_A", "assets/ui/A.png", 19, 21);
        GameApp.addSpriteSheet("key_S", "assets/ui/S.png", 19, 21);
        GameApp.addSpriteSheet("key_D", "assets/ui/D.png", 19, 21);
        
        currentPage = 1;
        isTransitioning = false;
        pageTransitionTimer = 0f;
        
        // Reset WASD animation state to prevent rendering bugs when returning to screen
        wasdAnimTimer = 0f;
        currentKeyIndex = 0;
    }
    
    private void loadCursors() {
        try {
            String pointerPath = "assets/ui/pointer.png";
            Pixmap pointerSource = new Pixmap(Gdx.files.internal(pointerPath));
            int targetSize = 32;
            Pixmap pointerPixmap = new Pixmap(targetSize, targetSize, pointerSource.getFormat());
            pointerPixmap.drawPixmap(pointerSource,
                    0, 0, pointerSource.getWidth(), pointerSource.getHeight(),
                    0, 0, targetSize, targetSize);
            cursorPointer = Gdx.graphics.newCursor(pointerPixmap, 0, 0);
            pointerPixmap.dispose();
            pointerSource.dispose();
            
            String cursorPath = "assets/ui/cursor.png";
            Pixmap cursorSource = new Pixmap(Gdx.files.internal(cursorPath));
            Pixmap cursorPixmap = new Pixmap(targetSize, targetSize, cursorSource.getFormat());
            cursorPixmap.drawPixmap(cursorSource,
                    0, 0, cursorSource.getWidth(), cursorSource.getHeight(),
                    0, 0, targetSize, targetSize);
            cursorHover = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
            cursorPixmap.dispose();
            cursorSource.dispose();
            
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            } else {
                GameApp.showCursor();
            }
        } catch (Exception e) {
            GameApp.log("Could not load cursors: " + e.getMessage());
            GameApp.showCursor();
        }
    }
    
    private void loadResources() {
        if (resourcesLoaded) return;
        
        // Fonts
        if (!GameApp.hasFont("howToPlayTitle")) {
            GameApp.addStyledFont("howToPlayTitle", "fonts/upheavtt.ttf", 52,
                    "white", 0f, "black", 3, 3, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlaySubtitle")) {
            GameApp.addStyledFont("howToPlaySubtitle", "fonts/PressStart2P-Regular.ttf", 18,
                    "yellow-300", 0f, "black", 2, 2, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlayText")) {
            GameApp.addStyledFont("howToPlayText", "fonts/PressStart2P-Regular.ttf", 14,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlayItemTitle")) {
            GameApp.addStyledFont("howToPlayItemTitle", "fonts/PixelOperatorMono-Bold.ttf", 16,
                    "orange-400", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlayItemDesc")) {
            GameApp.addStyledFont("howToPlayItemDesc", "fonts/PressStart2P-Regular.ttf", 11,
                    "gray-300", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlayPageNum")) {
            GameApp.addStyledFont("howToPlayPageNum", "fonts/PressStart2P-Regular.ttf", 16,
                    "gray-400", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("buttonFont")) {
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 36,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }
        
        // Page navigation fonts
        if (!GameApp.hasFont("pageButtonFont")) {
            GameApp.addStyledFont("pageButtonFont", "fonts/PressStart2P-Regular.ttf", 12,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("howToPlayPageInfo")) {
            GameApp.addStyledFont("howToPlayPageInfo", "fonts/PressStart2P-Regular.ttf", 14,
                    "gray-300", 0f, "black", 1, 1, "gray-700", true);
        }
        
        // Colors
        if (!GameApp.hasColor("button_green_text")) {
            GameApp.addColor("button_green_text", 25, 50, 25);
        }
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        if (!GameApp.hasColor("button_blue_text")) {
            GameApp.addColor("button_blue_text", 25, 35, 60);
        }
        
        // Textures
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
        if (!GameApp.hasTexture("blue_long")) {
            GameApp.addTexture("blue_long", "assets/ui/blue_long.png");
        }
        if (!GameApp.hasTexture("blue_pressed_long")) {
            GameApp.addTexture("blue_pressed_long", "assets/ui/blue_pressed_long.png");
        }
        if (!GameApp.hasTexture("mainmenu_bg")) {
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        }
        
        // Load WASD key sprite sheets (3 frames of 19x21 each, total sheet 57x21)
        GameApp.addSpriteSheet("key_W", "assets/ui/W.png", 19, 21);
        GameApp.addSpriteSheet("key_A", "assets/ui/A.png", 19, 21);
        GameApp.addSpriteSheet("key_S", "assets/ui/S.png", 19, 21);
        GameApp.addSpriteSheet("key_D", "assets/ui/D.png", 19, 21);
        
        // Load passive item textures for Page 2 - using actual PNG file names from ResourceLoader
        try { 
            if (!GameApp.hasTexture("passive_powerherb")) {
                GameApp.addTexture("passive_powerherb", "assets/ui/spinach.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load spinach.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_ironshield")) {
                GameApp.addTexture("passive_ironshield", "assets/ui/armor.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load armor.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_swiftboots")) {
                GameApp.addTexture("passive_swiftboots", "assets/ui/wings.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load wings.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_luckycoin")) {
                GameApp.addTexture("passive_luckycoin", "assets/ui/clover.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load clover.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_magnetstone")) {
                GameApp.addTexture("passive_magnetstone", "assets/ui/Attractorb.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load Attractorb.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_lifeessence")) {
                GameApp.addTexture("passive_lifeessence", "assets/ui/pummarola.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load pummarola.png"); }
        
        try { 
            if (!GameApp.hasTexture("passive_vitalitycore")) {
                GameApp.addTexture("passive_vitalitycore", "assets/ui/hollowhear.png"); 
            }
        } catch (Exception e) { GameApp.log("Warning: Could not load hollowhear.png"); }
        
        resourcesLoaded = true;
    }
    
    private void createButtons() {
        buttons = new ArrayList<>();
        
        int texW = GameApp.getTextureWidth("red_long");
        int texH = GameApp.getTextureHeight("red_long");
        float scale = 0.6f;
        
        buttonWidth = texW * scale;
        buttonHeight = texH * scale;
        
        float buttonY = 20f;
        
        // Back button (red) - bottom left
        float backX = 50f;
        Button backButton = new Button(backX, buttonY, buttonWidth, buttonHeight, "");
        backButton.setOnClick(() -> {});
        if (GameApp.hasTexture("red_long")) {
            backButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        buttons.add(backButton);
    }
    
    @Override
    public void hide() {
        // Don't stop music when leaving - keep it playing for other menu screens
        // Music will only stop when entering PlayScreen or quitting game
        if (cursorPointer != null) {
            cursorPointer.dispose();
            cursorPointer = null;
        }
        if (cursorHover != null) {
            cursorHover.dispose();
            cursorHover = null;
        }
        
        // Dispose WASD sprite sheets to free memory
        GameApp.disposeSpritesheet("key_W");
        GameApp.disposeSpritesheet("key_A");
        GameApp.disposeSpritesheet("key_S");
        GameApp.disposeSpritesheet("key_D");
    }
    
    @Override
    public void render(float delta) {
        super.render(delta);
        
        if (GameApp.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        
        // Handle keyboard navigation
        if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) {
            previousPage();
        }
        if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) {
            nextPage();
        }
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goBack();
        }
        
        GameApp.clearScreen("black");
        
        // Update page transition
        if (isTransitioning) {
            pageTransitionTimer += delta * 4f;
            if (pageTransitionTimer >= 1f) {
                pageTransitionTimer = 1f;
                isTransitioning = false;
                currentPage = targetPage;
            }
        }
        
        // Update WASD animation (only on page 1)
        if (currentPage == 1) {
            wasdAnimTimer += delta;
            if (wasdAnimTimer >= keyPressDuration * 4) {
                wasdAnimTimer = 0f; // Reset after full cycle
            }
        }
        
        // Update press delay timer
        if (pendingAction != null && pressedButton != null) {
            pressTimer += delta;
            if (pressTimer >= pressDelay) {
                Runnable action = pendingAction;
                pendingAction = null;
                pressedButton = null;
                pressTimer = 0f;
                action.run();
            }
        }
        
        // Handle input
        if (pendingAction == null) {
            handleButtonInput();
        } else if (pressedButton != null) {
            pressedButton.setPressed(true);
        }
        
        drawBackground();
        drawCurrentPage(delta);
        
        // Render buttons
        for (Button button : buttons) {
            button.render();
        }
        
        drawButtonText();
    }
    
    private void drawBackground() {
        if (!GameApp.hasTexture("mainmenu_bg")) return;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        int texWidth = GameApp.getTextureWidth("mainmenu_bg");
        int texHeight = GameApp.getTextureHeight("mainmenu_bg");
        
        float bgWidth = screenWidth;
        float bgHeight = screenHeight;
        
        if (texWidth > 0 && texHeight > 0) {
            float screenAspect = screenWidth / screenHeight;
            float texAspect = (float) texWidth / texHeight;
            
            if (screenAspect > texAspect) {
                bgWidth = screenWidth;
                bgHeight = bgWidth / texAspect;
            } else {
                bgHeight = screenHeight;
                bgWidth = bgHeight * texAspect;
            }
        }
        
        float bgX = (screenWidth - bgWidth) / 2f;
        float bgY = (screenHeight - bgHeight) / 2f;
        
        GameApp.startSpriteRendering();
        GameApp.drawTexture("mainmenu_bg", bgX, bgY, bgWidth, bgHeight);
        GameApp.endSpriteRendering();
    }
    
    private void drawCurrentPage(float delta) {
        drawHowToPlayPanel();
    }
    
    private void drawHowToPlayPanel() {
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        // Layout constants - matching RanksScreen style
        float panelWidth = 1000f;
        float panelHeight = 600f;
        float panelX = centerX - panelWidth / 2;
        float panelY = 85f; // Above BACK button
        
        // Panel background
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(20, 20, 35, 230);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Panel border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(2f);
        GameApp.setColor(70, 70, 120, 255);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Title area
        GameApp.startSpriteRendering();
        float titleY = panelY + panelHeight - 45f;
        GameApp.drawTextCentered("howToPlayTitle", "HOW TO PLAY", centerX, titleY, "yellow-400");
        
        // Subtitle with page info
        String[] pageTitles = {"Game Basics", "Passive Items", "Weapon Upgrades"};
        String subtitle = pageTitles[currentPage - 1];
        GameApp.drawTextCentered("howToPlaySubtitle", subtitle, centerX, titleY - 40f, "gray-400");
        GameApp.endSpriteRendering();
        
        // Content area
        float contentStartX = panelX + 40f;
        float contentWidth = panelWidth - 80f;
        float contentStartY = titleY - 80f;
        
        // Draw page content
        switch (currentPage) {
            case 1:
                drawPage1GameplayInstructions(contentStartX, contentStartY, contentWidth, centerX);
                break;
            case 2:
                drawPage2PassiveItems(contentStartX, contentStartY, contentWidth, centerX);
                break;
            case 3:
                drawPage3UpgradeStats(contentStartX, contentStartY, contentWidth, centerX);
                break;
        }
        
        // Draw pagination controls
        drawPaginationControls(panelX, panelY, panelWidth, contentStartX, contentWidth, centerX);
    }
    
    private void drawPage1GameplayInstructions(float contentStartX, float contentStartY, float contentWidth, float centerX) {
        GameApp.startSpriteRendering();
        
        // Section 1: Player Movement - use larger font size like "Game Basics"
        float sectionY = contentStartY;
        GameApp.drawText("howToPlaySubtitle", "PLAYER MOVEMENT", contentStartX, sectionY - 20, "orange-400");
        
        // WASD Keys Layout - Shift left to make room for descriptions on right
        float keyWidth = 19f;  // Sprite sheet width
        float keyHeight = 21f; // Sprite sheet height
        float keyScale = 2.5f; // Increased scale for better visibility
        float scaledKeyWidth = keyWidth * keyScale;
        float scaledKeyHeight = keyHeight * keyScale;
        float keySpacing = 12f; // Slightly increased spacing
        
        // Position keys on left side to make room for descriptions on right
        float keysStartX = contentStartX + 280f;
        float keysStartY = sectionY - 40f;
        
        // Calculate which key should be pressed based on time
        int activeKey = (int)((wasdAnimTimer / keyPressDuration) % 4);
        
        // Draw WASD keys in real keyboard layout
        // Top row: W key (centered above S)
        float wX = keysStartX + scaledKeyWidth + keySpacing; // Centered above S
        float wY = keysStartY;
        drawWASDKey("W", wX, wY, scaledKeyWidth, scaledKeyHeight, activeKey == 0);
        
        // Bottom row: A, S, D keys in horizontal line
        float bottomRowY = keysStartY - scaledKeyHeight - keySpacing;
        
        // A key (left)
        float aX = keysStartX;
        drawWASDKey("A", aX, bottomRowY, scaledKeyWidth, scaledKeyHeight, activeKey == 1);
        
        // S key (center)
        float sX = keysStartX + scaledKeyWidth + keySpacing;
        drawWASDKey("S", sX, bottomRowY, scaledKeyWidth, scaledKeyHeight, activeKey == 2);
        
        // D key (right)
        float dX = keysStartX + (scaledKeyWidth + keySpacing) * 2;
        drawWASDKey("D", dX, bottomRowY, scaledKeyWidth, scaledKeyHeight, activeKey == 3);
        
        // Movement descriptions - positioned to the right of keys
        float descX = keysStartX + (scaledKeyWidth * 3) + (keySpacing * 2) + 40f;
        GameApp.drawText("howToPlayText", "W - Move Up", descX, bottomRowY + 40f, "white");
        GameApp.drawText("howToPlayText", "A - Move Left", descX, bottomRowY + 20f, "white");
        GameApp.drawText("howToPlayText", "S - Move Down", descX, bottomRowY, "white");
        GameApp.drawText("howToPlayText", "D - Move Right", descX, bottomRowY - 20f, "white");
        GameApp.drawText("howToPlayItemDesc", "Use Arrow Keys as alternative", descX, bottomRowY - 40f, "gray-400");
        
        // Section 2: Core Mechanics - use larger font size and add more items
        float mechanicsY = sectionY - 200f;
        GameApp.drawText("howToPlaySubtitle", "CORE MECHANICS", contentStartX, mechanicsY, "green-400");
        
        String[] mechanics = {
            "- Your weapon fires automatically at nearby enemies",
            "- Collect XP orbs from defeated enemies to level up", 
            "- Choose powerful upgrades when you level up",
            "- Avoid enemy contact - they deal damage on touch",
            "- You can use W+A, S+D,... to move",
            "- Survive as long as possible for high scores"
        };
        
        float lineHeight = 30f; // Reduced line height to fit better
        for (int i = 0; i < mechanics.length; i++) {
            float textY = mechanicsY - 35f - i * lineHeight;
            GameApp.drawText("howToPlayText", mechanics[i], contentStartX + 30f, textY, "white");
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void drawWASDKey(String keyLetter, float x, float y, float width, float height, boolean isPressed) {
        String spritesheetName = "key_" + keyLetter;
        
        // Determine which frame to show: 0=normal, 1=pressed
        int frameColumn = isPressed ? 1 : 0; // Frame 0 = normal, Frame 1 = pressed
        int frameRow = 0; // All frames are in row 0 (horizontal sprite sheet)
        
        try {
            // Draw the sprite sheet frame
            GameApp.drawSpritesheetFrame(spritesheetName, frameRow, frameColumn, x, y - height, width, height);
        } catch (Exception e) {
            // Fallback: draw colored rectangle with text if sprite sheet fails
            GameApp.endSpriteRendering();
            
            // Draw key background
            GameApp.startShapeRenderingFilled();
            if (isPressed) {
                GameApp.setColor(100, 150, 255, 220); // Blue when pressed
            } else {
                GameApp.setColor(60, 60, 80, 180); // Dark gray when not pressed
            }
            GameApp.drawRect(x, y - height, width, height);
            GameApp.endShapeRendering();
            
            // Draw key border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            if (isPressed) {
                GameApp.setColor(150, 200, 255, 255); // Bright blue border when pressed
            } else {
                GameApp.setColor(120, 120, 140, 255); // Gray border when not pressed
            }
            GameApp.drawRect(x, y - height, width, height);
            GameApp.endShapeRendering();
            
            // Draw letter text
            GameApp.startSpriteRendering();
            String textColor = isPressed ? "blue-300" : "white";
            GameApp.drawTextCentered("howToPlayItemTitle", keyLetter, x + width / 2, y - height / 2 + 5f, textColor);
        }
    }
    
    private void drawPage2PassiveItems(float contentStartX, float contentStartY, float contentWidth, float centerX) {
        GameApp.startSpriteRendering();
        
        // Header - use larger font size like Page 1
        GameApp.drawText("howToPlaySubtitle", "PASSIVE ITEMS", contentStartX, contentStartY - 20, "orange-400");
        
        // Items in two columns with larger spacing
        float leftColumnX = contentStartX + 40f;
        float rightColumnX = contentStartX + contentWidth / 2 + 40f;
        float itemStartY = contentStartY - 80f;
        float itemSpacing = 80f; // Increased spacing since no Strategy Tips
        float iconSize = 45f; // Larger icons
        
        // Correct texture names from PassiveItemType enum
        String[] textureNames = {
            "passive_powerherb", "passive_ironshield", "passive_swiftboots",
            "passive_luckycoin", "passive_magnetstone", "passive_lifeessence", "passive_vitalitycore"
        };
        
        for (int i = 0; i < PASSIVE_ITEMS.length; i++) {
            float itemX = (i % 2 == 0) ? leftColumnX : rightColumnX;
            float itemY = itemStartY - (i / 2) * itemSpacing;
            
            // Try to render actual passive item texture using correct texture name
            String itemTextureName = textureNames[i];
            boolean textureRendered = false;
            
            try {
                if (GameApp.hasTexture(itemTextureName)) {
                    GameApp.drawTexture(itemTextureName, itemX, itemY - iconSize, iconSize, iconSize);
                    textureRendered = true;
                }
            } catch (Exception e) {
                // Texture not found or failed to render, use fallback
            }
            
            // Fallback: colored rectangle if texture not available
            if (!textureRendered) {
                GameApp.endSpriteRendering();
                GameApp.startShapeRenderingFilled();
                
                // Different colors for different item types
                switch (i % 6) {
                    case 0: GameApp.setColor(120, 60, 60, 200); break;  // Health - Red
                    case 1: GameApp.setColor(60, 120, 60, 200); break;  // Speed - Green  
                    case 2: GameApp.setColor(120, 80, 40, 200); break;  // Damage - Orange
                    case 3: GameApp.setColor(60, 60, 120, 200); break;  // Shield - Blue
                    case 4: GameApp.setColor(100, 60, 120, 200); break; // Fire Rate - Purple
                    default: GameApp.setColor(120, 120, 60, 200); break; // Life Steal - Yellow
                }
                
                GameApp.drawRect(itemX, itemY - iconSize, iconSize, iconSize);
                
                // Border for the icon
                GameApp.endShapeRendering();
                GameApp.startShapeRenderingOutlined();
                GameApp.setLineWidth(2f);
                GameApp.setColor(200, 200, 200, 255);
                GameApp.drawRect(itemX, itemY - iconSize, iconSize, iconSize);
                GameApp.endShapeRendering();
                GameApp.startSpriteRendering();
            }
            
            // Item name - larger font
            GameApp.drawText("howToPlayText", PASSIVE_ITEMS[i], itemX + iconSize + 15f, itemY - 5f, "orange-300");
            
            // Item description - larger and more readable
            GameApp.drawText("howToPlayItemDesc", PASSIVE_DESCRIPTIONS[i], itemX + iconSize + 15f, itemY - 25f, "gray-300");
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void drawPage3UpgradeStats(float contentStartX, float contentStartY, float contentWidth, float centerX) {
        GameApp.startSpriteRendering();
        
        // Header - use larger font size like other pages
        GameApp.drawText("howToPlaySubtitle", "WEAPON UPGRADES", contentStartX, contentStartY - 20, "orange-400");
        
        // Left column: Piston Upgrades (actual data from WeaponUpgrade.java) - larger size
        float leftColumnX = contentStartX + 20f;
        GameApp.drawText("howToPlayText", "PISTON UPGRADES (10 Levels)", leftColumnX, contentStartY - 60f, "blue-400");
        GameApp.drawText("howToPlayItemDesc", "Level up your weapon for increased power", leftColumnX, contentStartY - 85f, "gray-400");
        
        // Real Piston upgrade levels from WeaponUpgrade.java - 10 levels with multi-shot
        String[] pistonLevels = {
            "Lv1: Base weapon (2 bullets)",
            "Lv2: +1 bullet, +10% damage",
            "Lv3: MULTI-SHOT +3 front bullets",
            "Lv4: +1 bullet, +15% damage",
            "Lv5: +20% fire rate",
            "Lv6: Pierce 1 enemy, +10% fire rate",
            "Lv7: +1 bullet, +20% damage",
            "Lv8: MULTI-SHOT +3 back bullets",
            "Lv9: Pierce +1, +15% fire rate",
            "Lv10: +2 bullets, +30% dmg (MAX)"
        };
        
        float lineHeight = 20f; // Smaller to fit 10 levels
        for (int i = 0; i < pistonLevels.length; i++) {
            float textY = contentStartY - 115f - i * lineHeight;
            // Highlight multi-shot levels (now at index 2 and 7)
            String color = (i == 2 || i == 7) ? "green-400" : "white";
            GameApp.drawText("howToPlayText", pistonLevels[i], leftColumnX + 10f, textY, color);
        }
        
        // Right column: Evolution System (actual data from game) - larger size and improved content
        float rightColumnX = contentStartX + contentWidth / 2 + 40f;
        GameApp.drawText("howToPlayText", "EVOLUTION SYSTEM", rightColumnX, contentStartY - 60f, "purple-400");
        GameApp.drawText("howToPlayItemDesc", "Transform your weapon into its ultimate form", rightColumnX, contentStartY - 85f, "gray-400");
        
        // Improved evolution info - updated with lifesteal nerf
        String[] evolutionInfo = {
            "Requirements:",
            "  - Weapon at Level 10 (MAX)",
            "  - All passive items at MAX",
            "",
            "Death Spiral Features:",
            "  - 8 bullets rotating pattern",
            "  - Infinite pierce enemies",
            "  - +200% damage multiplier",
            "  - 10% LIFESTEAL on hit",
            "  - Slower fire rate (nerfed)"
        };
        
        for (int i = 0; i < evolutionInfo.length; i++) {
            float textY = contentStartY - 115f - i * lineHeight;
            if (evolutionInfo[i].isEmpty()) {
                continue; // Skip empty lines for spacing
            }
            String color = evolutionInfo[i].endsWith(":") ? "yellow-400" : "white";
            // Highlight lifesteal
            if (evolutionInfo[i].contains("LIFESTEAL")) {
                color = "green-400";
            }
            GameApp.drawText("howToPlayText", evolutionInfo[i], rightColumnX + 10f, textY, color);
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void drawPaginationControls(float panelX, float panelY, float panelWidth, float contentStartX, float contentWidth, float centerX) {
        // Pagination area - dedicated space at bottom of panel
        float paginationY = panelY + 10f;
        float paginationHeight = 45f;
        
        // Separator line above pagination
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(60, 60, 100, 150);
        GameApp.drawRect(contentStartX, paginationY + paginationHeight, contentWidth, 2f);
        GameApp.endShapeRendering();
        
        float navButtonW = 110f;
        float navButtonH = 36f;
        float navButtonY = paginationY + (paginationHeight - navButtonH) / 2;
        
        // Previous button (left side)
        prevButtonX = contentStartX + 10f;
        prevButtonY = navButtonY;
        prevButtonW = navButtonW;
        prevButtonH = navButtonH;
        
        // Next button (right side)
        nextButtonX = contentStartX + contentWidth - navButtonW - 10f;
        nextButtonY = navButtonY;
        nextButtonW = navButtonW;
        nextButtonH = navButtonH;
        
        // Draw Previous button
        if (currentPage > 1) {
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(50, 70, 120, 220);
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 100, 160, 255);
            
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "< PREV", prevButtonX + prevButtonW / 2, prevButtonY + prevButtonH / 2 + 3f, "white");
            GameApp.endSpriteRendering();
        } else {
            // Disabled state
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(30, 30, 50, 100);
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "< PREV", prevButtonX + prevButtonW / 2, prevButtonY + prevButtonH / 2 + 3f, "gray-600");
            GameApp.endSpriteRendering();
        }
        
        // Draw Next button
        if (currentPage < TOTAL_PAGES) {
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(50, 70, 120, 220);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 100, 160, 255);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "NEXT >", nextButtonX + nextButtonW / 2, nextButtonY + nextButtonH / 2 + 3f, "white");
            GameApp.endSpriteRendering();
        } else {
            // Disabled state
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(30, 30, 50, 100);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "NEXT >", nextButtonX + nextButtonW / 2, nextButtonY + nextButtonH / 2 + 3f, "gray-600");
            GameApp.endSpriteRendering();
        }
        
        // Page indicator in center
        GameApp.startSpriteRendering();
        String pageInfo = "Page " + currentPage + " of " + TOTAL_PAGES;
        GameApp.drawTextCentered("howToPlayPageInfo", pageInfo, centerX, navButtonY + navButtonH / 2 + 3f, "gray-300");
        GameApp.endSpriteRendering();
    }
    
    private void drawButtonText() {
        GameApp.startSpriteRendering();
        
        // Only BACK button now
        if (buttons.size() > 0) {
            Button button = buttons.get(0);
            String text = "BACK";
            String colorName = "button_red_text";
            
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;
            
            float textHeight = GameApp.getTextHeight("buttonFont", text);
            float adjustedY = buttonCenterY + textHeight * 0.15f;
            
            GameApp.drawTextCentered("buttonFont", text, buttonCenterX, adjustedY, colorName);
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void handleButtonInput() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        
        float windowWidth = Gdx.graphics.getWidth();
        float windowHeight = Gdx.graphics.getHeight();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        float scaleX = screenWidth / windowWidth;
        float scaleY = screenHeight / windowHeight;
        
        float worldMouseX = mouseX * scaleX;
        float worldMouseY = (windowHeight - mouseY) * scaleY;
        
        boolean isHovering = false;
        
        // Handle BACK button (only button in the buttons list now)
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            boolean hovering = button.containsPoint(worldMouseX, worldMouseY);
            
            if (hovering) {
                isHovering = true;
                button.setSelected(true);
                
                if (GameApp.isButtonJustPressed(0)) {
                    button.setPressed(true);
                    pressedButton = button;
                    
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 0.8f);
                    }
                    
                    // Only BACK button now
                    pendingAction = this::goBack;
                    pressTimer = 0f;
                }
            } else {
                button.setSelected(false);
                button.setPressed(false);
            }
        }
        
        // Handle pagination buttons (drawn manually)
        if (GameApp.isButtonJustPressed(0)) {
            // Check Previous button
            if (currentPage > 1 && worldMouseX >= prevButtonX && worldMouseX <= prevButtonX + prevButtonW &&
                worldMouseY >= prevButtonY && worldMouseY <= prevButtonY + prevButtonH) {
                
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.8f);
                }
                previousPage();
                return;
            }
            
            // Check Next button
            if (currentPage < TOTAL_PAGES && worldMouseX >= nextButtonX && worldMouseX <= nextButtonX + nextButtonW &&
                worldMouseY >= nextButtonY && worldMouseY <= nextButtonY + nextButtonH) {
                
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.8f);
                }
                nextPage();
                return;
            }
        }
        
        // Check hover for pagination buttons
        if ((currentPage > 1 && worldMouseX >= prevButtonX && worldMouseX <= prevButtonX + prevButtonW &&
             worldMouseY >= prevButtonY && worldMouseY <= prevButtonY + prevButtonH) ||
            (currentPage < TOTAL_PAGES && worldMouseX >= nextButtonX && worldMouseX <= nextButtonX + nextButtonW &&
             worldMouseY >= nextButtonY && worldMouseY <= nextButtonY + nextButtonH)) {
            isHovering = true;
        }
        
        // Update cursor
        if (isHovering && cursorHover != null) {
            Gdx.graphics.setCursor(cursorHover);
        } else if (cursorPointer != null) {
            Gdx.graphics.setCursor(cursorPointer);
        }
    }
    
    private void previousPage() {
        if (currentPage > 1 && !isTransitioning) {
            wasdAnimTimer = 0f;
            targetPage = currentPage - 1;
            isTransitioning = true;
            pageTransitionTimer = 0f;
        }
    }
    
    private void nextPage() {
        if (currentPage < TOTAL_PAGES && !isTransitioning) {
            wasdAnimTimer = 0f;
            targetPage = currentPage + 1;
            isTransitioning = true;
            pageTransitionTimer = 0f;
        }
    }
    
    private void goBack() {
        GameApp.switchScreen("playerinput");
    }
    
    
    protected void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
        } else {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }
}
