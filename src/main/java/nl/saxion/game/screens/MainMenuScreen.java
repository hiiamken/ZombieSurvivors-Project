package nl.saxion.game.screens;

import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

// Simple main menu with gradient background and 3 buttons (PLAY, OPTIONS, QUIT)
public class MainMenuScreen extends ScalableGameScreen {

    private List<Button> buttons;
    private boolean resourcesLoaded = false;
    private SoundManager soundManager;

    // Button dimensions (will be calculated from texture size)
    private float buttonWidth;
    private float buttonHeight;
    private float buttonSpacing = 20f;

    // Delay for button press animation (increased to allow sound to play fully)
    private float pressDelay = 0.5f; // 300ms delay to allow sound to play
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;

    // Cursor management
    private Cursor cursorPointer; // Left side (pointer/default)
    private Cursor cursorHover;   // Right side (hover)
    private boolean isHoveringButton = false;
    
    // Track if fullscreen has been applied on first show
    private static boolean fullscreenApplied = false;
    
    // Hint popup state
    private boolean showHintPopup = false;
    private float hintIconX, hintIconY, hintIconSize;
    private float hintPopupOpenCooldown = 0f; // Prevent immediate close after opening
    
    // Version info
    private static final String GAME_VERSION = "v1.0.0";

    public MainMenuScreen() {
        super(1280, 720); // 16:9 aspect ratio
    }

    @Override
    public void show() {
        // Initialize debug logger
        DebugLogger.log("MainMenuScreen.show() called");
        
        // Apply fullscreen setting from config on first show
        if (!fullscreenApplied) {
            applyFullscreenFromConfig();
            fullscreenApplied = true;
        }

        // Load cursors
        loadPusheenCursors();
        
        // Initialize sound manager for button clicks and background music
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        // Apply volume settings from config
        GameConfig config = ConfigManager.loadConfig();
        soundManager.setMasterVolume(config.masterVolume);
        soundManager.setMusicVolume(config.musicVolume);
        soundManager.setSFXVolume(config.sfxVolume);
        
        // Start background music for menu
        if (soundManager != null) {
            soundManager.playMusic(true);
        }

        loadResources();
        createButtons();

        DebugLogger.log("MainMenuScreen initialized with %d buttons", buttons.size());
    }

    // Load cursor images (pointer.png and cursor.png)
    private void loadPusheenCursors() {
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

    private void loadResources() {
        if (!resourcesLoaded) {
            DebugLogger.log("Loading fonts...");

            // Register unified button text colors for all menus
            // GREEN button - dark green-gray for contrast on bright green
            if (!GameApp.hasColor("button_play_color")) {
                GameApp.addColor("button_play_color", 25, 50, 25); // Dark green-gray
            }
            if (!GameApp.hasColor("button_green_text")) {
                GameApp.addColor("button_green_text", 25, 50, 25);
            }
            // ORANGE button - dark brown for contrast on orange
            if (!GameApp.hasColor("button_settings_color")) {
                GameApp.addColor("button_settings_color", 70, 30, 10); // Dark brown
            }
            if (!GameApp.hasColor("button_orange_text")) {
                GameApp.addColor("button_orange_text", 70, 30, 10);
            }
            // RED button - dark maroon for contrast on red/pink
            if (!GameApp.hasColor("button_quit_color")) {
                GameApp.addColor("button_quit_color", 60, 15, 30); // Dark maroon
            }
            if (!GameApp.hasColor("button_red_text")) {
                GameApp.addColor("button_red_text", 60, 15, 30);
            }
            // Yellow button text color (dark brown for contrast on yellow)
            if (!GameApp.hasColor("button_yellow_text")) {
                GameApp.addColor("button_yellow_text", 70, 50, 10); // Dark brown/olive
            }
            // Blue button text color (dark navy for contrast on blue)
            if (!GameApp.hasColor("button_blue_text")) {
                GameApp.addColor("button_blue_text", 20, 30, 70); // Dark navy blue
            }
            
            // Button font with subtle shadow (size 40 for smaller buttons)
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 40,
                    "white", 0f, "black", 2, 2, "gray-700", true);
            DebugLogger.log("Loaded buttonFont: upheavtt.ttf (size 32)");

            DebugLogger.log("Loading button sprites...");

            // Load green button sprites (PLAY)
            if (!GameApp.hasTexture("green_long")) {
                GameApp.addTexture("green_long", "assets/ui/green_long.png");
                DebugLogger.log("Loaded green_long: %s", GameApp.hasTexture("green_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("green_long already loaded");
            }
            if (!GameApp.hasTexture("green_pressed_long")) {
                GameApp.addTexture("green_pressed_long", "assets/ui/green_pressed_long.png");
                DebugLogger.log("Loaded green_pressed_long: %s", GameApp.hasTexture("green_pressed_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("green_pressed_long already loaded");
            }

            // Load orange button sprites (OPTIONS)
            if (!GameApp.hasTexture("orange_long")) {
                GameApp.addTexture("orange_long", "assets/ui/orange_long.png");
                DebugLogger.log("Loaded orange_long: %s", GameApp.hasTexture("orange_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("orange_long already loaded");
            }
            if (!GameApp.hasTexture("orange_pressed_long")) {
                GameApp.addTexture("orange_pressed_long", "assets/ui/orange_pressed_long.png");
                DebugLogger.log("Loaded orange_pressed_long: %s", GameApp.hasTexture("orange_pressed_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("orange_pressed_long already loaded");
            }

            // Load yellow button sprites (LEADERBOARD)
            if (!GameApp.hasTexture("yellow_long")) {
                GameApp.addTexture("yellow_long", "assets/ui/yellow_long.png");
                DebugLogger.log("Loaded yellow_long: %s", GameApp.hasTexture("yellow_long") ? "SUCCESS" : "FAILED");
            }
            if (!GameApp.hasTexture("yellow_pressed_long")) {
                GameApp.addTexture("yellow_pressed_long", "assets/ui/yellow_pressed_long.png");
                DebugLogger.log("Loaded yellow_pressed_long: %s", GameApp.hasTexture("yellow_pressed_long") ? "SUCCESS" : "FAILED");
            }

            // Load blue button sprites (CREDITS)
            if (!GameApp.hasTexture("blue_long")) {
                GameApp.addTexture("blue_long", "assets/ui/blue_long.png");
                DebugLogger.log("Loaded blue_long: %s", GameApp.hasTexture("blue_long") ? "SUCCESS" : "FAILED");
            }
            if (!GameApp.hasTexture("blue_pressed_long")) {
                GameApp.addTexture("blue_pressed_long", "assets/ui/blue_pressed_long.png");
                DebugLogger.log("Loaded blue_pressed_long: %s", GameApp.hasTexture("blue_pressed_long") ? "SUCCESS" : "FAILED");
            }

            // Load red button sprites (QUIT)
            if (!GameApp.hasTexture("red_long")) {
                GameApp.addTexture("red_long", "assets/ui/red_long.png");
                DebugLogger.log("Loaded red_long: %s", GameApp.hasTexture("red_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("red_long already loaded");
            }
            if (!GameApp.hasTexture("red_pressed_long")) {
                GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
                DebugLogger.log("Loaded red_pressed_long: %s", GameApp.hasTexture("red_pressed_long") ? "SUCCESS" : "FAILED");
            } else {
                DebugLogger.log("red_pressed_long already loaded");
            }

            // Load title image
            if (!GameApp.hasTexture("zombiesTitle")) {
                GameApp.addTexture("zombiesTitle", "assets/ui/ZombiesTitle.png");
                boolean loaded = GameApp.hasTexture("zombiesTitle");
                DebugLogger.log("Loaded zombiesTitle: %s", loaded ? "SUCCESS" : "FAILED");
                if (!loaded) {
                    GameApp.log("ERROR: Could not load ZombiesTitle.png from assets/ui/");
                }
            } else {
                DebugLogger.log("zombiesTitle already loaded");
            }

            // Load main menu background image
            if (!GameApp.hasTexture("mainmenu_bg")) {
                GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
                boolean loaded = GameApp.hasTexture("mainmenu_bg");
                DebugLogger.log("Loaded mainmenu_bg: %s", loaded ? "SUCCESS" : "FAILED");
                if (!loaded) {
                    GameApp.log("ERROR: Could not load mainmenu.png from assets/ui/");
                }
            } else {
                DebugLogger.log("mainmenu_bg already loaded");
            }
            
            // Load hint icon for upcoming features
            if (!GameApp.hasTexture("hint_icon")) {
                GameApp.addTexture("hint_icon", "assets/ui/hint.png");
                DebugLogger.log("Loaded hint_icon: %s", GameApp.hasTexture("hint_icon") ? "SUCCESS" : "FAILED");
            }
            
            // Load version font (smaller, subtle)
            if (!GameApp.hasFont("versionFont")) {
                GameApp.addStyledFont("versionFont", "fonts/PixelOperator.ttf", 16,
                        "white", 0f, "black", 1, 1, "gray-700", true);
            }

            resourcesLoaded = true;
            DebugLogger.log("Resources loaded");
        }
    }

    private void createButtons() {
        buttons = new ArrayList<>();
        
        // Calculate button size from texture dimensions with scale
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        float scale = 0.8f; // Smaller buttons
        
        buttonWidth = texW * scale;
        buttonHeight = texH * scale;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Calculate total height of all buttons with spacing (5 buttons now)
        float totalHeight = (buttonHeight * 5) + (buttonSpacing * 4);
        float startY = (screenHeight - totalHeight) / 2 + buttonHeight * 2 + buttonSpacing + 40f; // Move up 10f more

        // Center buttons horizontally
        float buttonX = (screenWidth - buttonWidth) / 2;

        // 1. PLAY button (green) - top
        Button playButton = new Button(buttonX, startY, buttonWidth, buttonHeight, "");
        playButton.setOnClick(() -> {
            DebugLogger.log("PLAY button clicked!");
            GameApp.log("Starting game...");
        });
        if (GameApp.hasTexture("green_long")) {
            playButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        buttons.add(playButton);

        // 2. LEADERBOARD button (yellow)
        float leaderboardY = startY - buttonHeight - buttonSpacing;
        Button leaderboardButton = new Button(buttonX, leaderboardY, buttonWidth, buttonHeight, "");
        leaderboardButton.setOnClick(() -> {
            DebugLogger.log("LEADERBOARD button clicked!");
            GameApp.log("Opening leaderboard...");
        });
        if (GameApp.hasTexture("yellow_long")) {
            leaderboardButton.setSprites("yellow_long", "yellow_long", "yellow_long", "yellow_pressed_long");
        }
        buttons.add(leaderboardButton);

        // 3. SETTINGS button (orange)
        float settingsY = leaderboardY - buttonHeight - buttonSpacing;
        Button settingsButton = new Button(buttonX, settingsY, buttonWidth, buttonHeight, "");
        settingsButton.setOnClick(() -> {
            DebugLogger.log("SETTINGS button clicked!");
            GameApp.log("Opening settings...");
        });
        if (GameApp.hasTexture("orange_long")) {
            settingsButton.setSprites("orange_long", "orange_long", "orange_long", "orange_pressed_long");
        }
        buttons.add(settingsButton);

        // 4. CREDITS button (blue)
        float creditsY = settingsY - buttonHeight - buttonSpacing;
        Button creditsButton = new Button(buttonX, creditsY, buttonWidth, buttonHeight, "");
        creditsButton.setOnClick(() -> {
            DebugLogger.log("CREDITS button clicked!");
            GameApp.log("Opening credits...");
        });
        if (GameApp.hasTexture("blue_long")) {
            creditsButton.setSprites("blue_long", "blue_long", "blue_long", "blue_pressed_long");
        }
        buttons.add(creditsButton);

        // 5. QUIT button (red) - bottom
        float quitY = creditsY - buttonHeight - buttonSpacing;
        Button quitButton = new Button(buttonX, quitY, buttonWidth, buttonHeight, "");
        quitButton.setOnClick(() -> {
            DebugLogger.log("QUIT button clicked!");
            GameApp.log("Quitting game...");
        });
        if (GameApp.hasTexture("red_long")) {
            quitButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        buttons.add(quitButton);
        
        DebugLogger.log("Created 5 menu buttons: PLAY, LEADERBOARD, SETTINGS, CREDITS, QUIT");
    }

    @Override
    public void hide() {
        // Don't stop music when leaving menu - keep it playing for other menu screens
        // Music will only stop when entering PlayScreen or quitting game
        // This prevents music from resetting when switching between menu screens
        
        // Dispose font
        GameApp.disposeFont("buttonFont");

        // Dispose cursors
        if (cursorPointer != null) {
            cursorPointer.dispose();
            cursorPointer = null;
        }
        if (cursorHover != null) {
            cursorHover.dispose();
            cursorHover = null;
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        
        // Handle F11 key to toggle fullscreen
        if (GameApp.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F11)) {
            toggleFullscreen();
        }
        
        // Clear screen to remove any previous screen content
        GameApp.clearScreen("black");

        // Update press delay timer
        if (pendingAction != null && pressedButton != null) {
            pressTimer += delta;
            if (pressTimer >= pressDelay) {
                // Delay finished, execute action
                DebugLogger.log("Press delay finished, executing action");
                Runnable action = pendingAction;
                pendingAction = null;
                pressedButton = null;
                pressTimer = 0f;
                action.run();
            }
        }

        // Draw main menu background image
        drawBackground();

        // Draw title image at top
        drawTitle();

        // Get mouse position in world coordinates using ScalableGameScreen helper
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;

        // Handle input with converted coordinates (only if not waiting for delay)
        if (pendingAction == null) {
            handleInput(worldMouseX, worldMouseY);
        } else {
            // Keep button pressed during delay
            if (pressedButton != null) {
                pressedButton.setPressed(true);
            }
        }

        // Update and draw buttons
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (pendingAction == null) {
                button.update(worldMouseX, worldMouseY);
            }

            button.render();
        }

        // Draw button text labels
        drawButtonText();
        
        // Draw version display (bottom right)
        drawVersionDisplay();
        
        // Draw hint icon (bottom left) and handle click
        drawHintIcon(worldMouseX, worldMouseY);
        
        // Draw hint popup if active
        if (showHintPopup) {
            drawHintPopup(delta);
        }
    }

    // Draw text labels on buttons
    private void drawButtonText() {
        GameApp.startSpriteRendering();

        String[] buttonTexts = {"PLAY", "RANKS", "SETTINGS", "CREDITS", "QUIT"};
        String[] buttonColors = {"button_green_text", "button_yellow_text", "button_orange_text", "button_blue_text", "button_red_text"};

        for (int i = 0; i < buttons.size() && i < buttonTexts.length; i++) {
            Button button = buttons.get(i);
            String text = buttonTexts[i];
            String colorName = buttonColors[i];

            // Calculate center of button
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;

            // Adjust vertical position - move all buttons up
            float textHeight = GameApp.getTextHeight("buttonFont", text);
            float adjustedY = buttonCenterY + textHeight * 0.15f;

            // Draw text centered on button with custom color
            GameApp.drawTextCentered("buttonFont", text, buttonCenterX, adjustedY, colorName);
        }

        GameApp.endSpriteRendering();
    }

    // Draw main menu background image
    private void drawBackground() {
        if (!GameApp.hasTexture("mainmenu_bg")) {
            // Try to load again if not found
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
            if (!GameApp.hasTexture("mainmenu_bg")) {
                DebugLogger.log("ERROR: mainmenu_bg texture still not found after reload!");
                // Fallback to gradient if image not found
                drawGradientFallback();
                return;
            }
        }

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Get texture dimensions
        float bgWidth = screenWidth;
        float bgHeight = screenHeight;

        try {
            int texWidth = GameApp.getTextureWidth("mainmenu_bg");
            int texHeight = GameApp.getTextureHeight("mainmenu_bg");
            if (texWidth > 0 && texHeight > 0) {
                // Scale to COVER entire screen (fill, may crop)
                float screenAspect = screenWidth / screenHeight;
                float texAspect = (float) texWidth / texHeight;

                if (screenAspect > texAspect) {
                    // Screen is wider, fit to width (crop top/bottom)
                    bgWidth = screenWidth;
                    bgHeight = bgWidth / texAspect;
                } else {
                    // Screen is taller, fit to height (crop left/right)
                    bgHeight = screenHeight;
                    bgWidth = bgHeight * texAspect;
                }
            }
        } catch (Exception e) {
            DebugLogger.log("Could not get texture dimensions, using screen size");
        }

        // Center the background
        float bgX = (screenWidth - bgWidth) / 2f;
        float bgY = (screenHeight - bgHeight) / 2f;

        // Render background (must be in sprite batch)
        GameApp.startSpriteRendering();
        GameApp.drawTexture("mainmenu_bg", bgX, bgY, bgWidth, bgHeight);
        GameApp.endSpriteRendering();
    }

    // Fallback gradient background if image fails to load
    private void drawGradientFallback() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Base color: #333146 (purplish-blue)
        // Top: slightly lighter, Bottom: slightly darker
        int topR = 60, topG = 55, topB = 80;   // Lighter at top
        int bottomR = 40, bottomG = 35, bottomB = 60;  // Darker at bottom

        // Draw gradient using multiple rectangles
        int steps = 50;
        GameApp.startShapeRenderingFilled();

        for (int i = 0; i < steps; i++) {
            float y = (screenHeight / steps) * i;
            float height = screenHeight / steps;

            // Interpolate color
            float t = (float) i / steps;
            int r = (int) (topR + (bottomR - topR) * t);
            int g = (int) (topG + (bottomG - topG) * t);
            int b = (int) (topB + (bottomB - topB) * t);

            GameApp.setColor(r, g, b, 255);
            GameApp.drawRect(0, y, screenWidth, height);
        }

        GameApp.endShapeRendering();
    }

    // Draw title image at top center of screen
    private void drawTitle() {
        if (!GameApp.hasTexture("zombiesTitle")) {
            // Try to load again if not found
            GameApp.addTexture("zombiesTitle", "assets/ui/ZombiesTitle.png");
            if (!GameApp.hasTexture("zombiesTitle")) {
                DebugLogger.log("ERROR: zombiesTitle texture still not found after reload!");
                return; // Still not found, skip rendering
            }
        }

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Get actual texture dimensions
        float titleWidth = 800f; // Default size
        float titleHeight = 250f; // Default size

        try {
            int texWidth = GameApp.getTextureWidth("zombiesTitle");
            int texHeight = GameApp.getTextureHeight("zombiesTitle");
            if (texWidth > 0 && texHeight > 0) {
                // Scale to fit screen width (90% of screen - doubled from 45%)
                float targetWidth = screenWidth * 0.65f;
                float aspectRatio = (float)texHeight / texWidth;
                titleWidth = targetWidth;
                titleHeight = targetWidth * aspectRatio;
            }
        } catch (Exception e) {
            // Use default size if texture dimensions unavailable
        }

        // Center horizontally, position at top (moved up for better balance)
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = screenHeight - titleHeight + 70f; // Move up 20f for better balance

        // Render title (must be in sprite batch)
        GameApp.startSpriteRendering();
        GameApp.drawTexture("zombiesTitle", titleX, titleY, titleWidth, titleHeight);
        GameApp.endSpriteRendering();
    }

    private void handleInput(float mouseX, float mouseY) {
        // Only mouse interaction - no keyboard navigation
        boolean isMouseDown = GameApp.isButtonPressed(0);
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);

        // Check if hovering over any button for cursor switching
        boolean hoveringAnyButton = false;
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            if (button.containsPoint(mouseX, mouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }

        // Switch cursor based on hover state and click state
        if (cursorPointer != null && cursorHover != null) {
            if (isMouseDown || isMouseJustPressed) {
                // When clicking, use pointer cursor and reset hover state
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringButton = false; // Reset to allow hover cursor after release
            } else if (hoveringAnyButton) {
                // Hovering over button - use hover cursor
                if (!isHoveringButton) {
                    // Just started hovering
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringButton = true;
                }
            } else {
                // Not hovering - use pointer cursor
                if (isHoveringButton) {
                    // Just stopped hovering
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringButton = false;
                }
            }
        }

        // Button names for debugging
        String[] buttonNames = {"PLAY", "LEADERBOARD", "SETTINGS", "CREDITS", "QUIT"};

        // Update button pressed states based on mouse
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            boolean isMouseOver = button.containsPoint(mouseX, mouseY);

            // Show pressed state when mouse is down over button
            button.setPressed(isMouseDown && isMouseOver);

            // Debug button state
            if (isMouseOver && isMouseJustPressed && i < buttonNames.length) {
                DebugLogger.log("%s button: Mouse clicked! Button bounds: (%.1f, %.1f) size (%.1f x %.1f)",
                        buttonNames[i], button.getX(), button.getY(), button.getWidth(), button.getHeight());
            }
        }

        // Mouse click (on release) to activate button
        if (isMouseJustPressed) {
            DebugLogger.log("Mouse button just pressed at (%.1f, %.1f)", mouseX, mouseY);

            boolean clickedAny = false;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                if (button.containsPoint(mouseX, mouseY)) {
                    String buttonName = i < buttonNames.length ? buttonNames[i] : "UNKNOWN";
                    DebugLogger.log("%s button: Click detected! Starting press delay...", buttonName);

                    // Store button and action for delayed execution
                    pressedButton = button;
                    button.setPressed(true);

                    // Play button click sound at higher volume
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    // Create delayed action based on button index
                    final int buttonIndex = i;
                    switch (buttonIndex) {
                        case 0: // PLAY
                            pendingAction = () -> {
                                DebugLogger.log("PLAY button action executing after delay");
                                // Go to player input screen first to collect player info
                                GameApp.switchScreen("playerinput");
                            };
                            break;
                        case 1: // RANKS (LEADERBOARD)
                            pendingAction = () -> {
                                DebugLogger.log("RANKS button action executing after delay");
                                GameApp.switchScreen("ranks");
                            };
                            break;
                        case 2: // SETTINGS
                            pendingAction = () -> {
                                DebugLogger.log("SETTINGS button action executing after delay");
                                GameApp.switchScreen("settings");
                            };
                            break;
                        case 3: // CREDITS
                            pendingAction = () -> {
                                DebugLogger.log("CREDITS button action executing after delay");
                                GameApp.switchScreen("credits");
                            };
                            break;
                        case 4: // QUIT
                            pendingAction = () -> {
                                DebugLogger.log("QUIT button action executing after delay");
                                GameApp.quit();
                            };
                            break;
                    }

                    pressTimer = 0f;
                    clickedAny = true;
                    break;
                }
            }

            if (!clickedAny) {
                DebugLogger.log("Click detected but no button was hit. Mouse at (%.1f, %.1f)", mouseX, mouseY);
                // Log all button positions for debugging
                for (int i = 0; i < buttons.size(); i++) {
                    Button button = buttons.get(i);
                    String buttonName = i == 0 ? "PLAY" : (i == 1 ? "OPTIONS" : "QUIT");
                    DebugLogger.log("  %s button bounds: x=%.1f-%.1f, y=%.1f-%.1f",
                            buttonName, button.getX(), button.getX() + button.getWidth(),
                            button.getY(), button.getY() + button.getHeight());
                }
            }
        }
    }
    
    // Apply fullscreen setting from config file
    private void applyFullscreenFromConfig() {
        GameConfig config = ConfigManager.loadConfig();
        if (config.fullscreen) {
            // Set to fullscreen
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            // Set to windowed mode (1280x720)
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
    
    // Toggle fullscreen and update config
    private void toggleFullscreen() {
        GameConfig config = ConfigManager.loadConfig();
        config.fullscreen = !config.fullscreen;
        ConfigManager.saveConfig(config);
        
        if (config.fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
    
    // Draw version display in bottom right corner
    private void drawVersionDisplay() {
        float screenWidth = GameApp.getWorldWidth();
        float padding = 15f;
        
        GameApp.startSpriteRendering();
        
        // Draw version text with subtle styling
        String versionText = GAME_VERSION;
        float textWidth = GameApp.getTextWidth("versionFont", versionText);
        float textX = screenWidth - textWidth - padding;
        float textY = padding + 10f;
        
        // Draw with semi-transparent white
        GameApp.setColor(200, 200, 200, 180);
        GameApp.drawText("versionFont", versionText, textX, textY, "gray-400");
        
        GameApp.endSpriteRendering();
    }
    
    // Draw hint icon in bottom left corner and handle click
    private void drawHintIcon(float mouseX, float mouseY) {
        float padding = 15f;
        hintIconSize = 48f;
        hintIconX = padding;
        hintIconY = padding;
        
        // Don't process hint icon clicks if popup is showing
        if (showHintPopup) {
            // Just draw the icon without interaction
            if (GameApp.hasTexture("hint_icon")) {
                GameApp.startSpriteRendering();
                GameApp.drawTexture("hint_icon", hintIconX, hintIconY, hintIconSize, hintIconSize);
                GameApp.endSpriteRendering();
            }
            return;
        }
        
        // Check if mouse is over hint icon
        boolean isHoveringHint = mouseX >= hintIconX && mouseX <= hintIconX + hintIconSize &&
                                  mouseY >= hintIconY && mouseY <= hintIconY + hintIconSize;
        
        // Handle click on hint icon to OPEN popup
        if (isHoveringHint && GameApp.isButtonJustPressed(0) && pendingAction == null) {
            showHintPopup = true;
            hintPopupOpenCooldown = 0.3f; // Cooldown to prevent immediate close
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 1.5f);
            }
        }
        
        // Draw hint icon
        if (GameApp.hasTexture("hint_icon")) {
            GameApp.startSpriteRendering();
            
            // Scale up slightly when hovering
            float drawSize = isHoveringHint ? hintIconSize * 1.1f : hintIconSize;
            float drawX = hintIconX - (drawSize - hintIconSize) / 2f;
            float drawY = hintIconY - (drawSize - hintIconSize) / 2f;
            
            GameApp.drawTexture("hint_icon", drawX, drawY, drawSize, drawSize);
            GameApp.endSpriteRendering();
        }
    }
    
    // Draw hint popup with upcoming features
    private void drawHintPopup(float delta) {
        // Update cooldown timer
        if (hintPopupOpenCooldown > 0f) {
            hintPopupOpenCooldown -= delta;
        }
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        // Popup dimensions - LARGER, nearly full screen
        float popupWidth = screenWidth * 0.85f;  // 85% of screen width
        float popupHeight = screenHeight * 0.80f; // 80% of screen height
        float popupX = (screenWidth - popupWidth) / 2f;
        float popupY = (screenHeight - popupHeight) / 2f;
        
        // Draw semi-transparent overlay (lighter to show background through)
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, 120); // Lighter overlay to show mainmenu background
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // Draw popup background
        GameApp.startShapeRenderingFilled();
        // Dark purple-blue background with slight transparency
        GameApp.setColor(25, 20, 45, 230);
        GameApp.drawRect(popupX, popupY, popupWidth, popupHeight);
        // Thick border
        float borderWidth = 5f;
        GameApp.setColor(120, 90, 180, 255);
        GameApp.drawRect(popupX, popupY, popupWidth, borderWidth); // Bottom
        GameApp.drawRect(popupX, popupY + popupHeight - borderWidth, popupWidth, borderWidth); // Top
        GameApp.drawRect(popupX, popupY, borderWidth, popupHeight); // Left
        GameApp.drawRect(popupX + popupWidth - borderWidth, popupY, borderWidth, popupHeight); // Right
        GameApp.endShapeRendering();
        
        // Draw text content - LARGER sizes
        GameApp.startSpriteRendering();
        
        float centerX = popupX + popupWidth / 2f;
        float textY = popupY + popupHeight - 80f;
        float lineHeight = 65f; // Larger line spacing
        
        // Title - BIGGER (use buttonFont which is larger)
        String title = "COMING SOON!";
        float titleWidth = GameApp.getTextWidth("buttonFont", title);
        GameApp.drawText("buttonFont", title, centerX - titleWidth/2, textY, "yellow-400");
        textY -= lineHeight + 30f;
        
        // Upcoming features list - use buttonFont for larger text
        String[] features = {
            "New Weapons (Katana, Molotov...)",
            "New Characters",
            "New Zombie Types",
            "New Maps",
            "Achievement System"
        };
        
        float textX = popupX + 80f;
        for (String feature : features) {
            // Draw bullet point
            GameApp.drawText("buttonFont", "*", textX, textY, "yellow-400");
            // Draw feature text
            GameApp.drawText("buttonFont", feature, textX + 40f, textY, "white");
            textY -= lineHeight;
        }
        
        // Close hint text at bottom
        textY = popupY + 50f;
        String closeText = "Click anywhere to close";
        float closeWidth = GameApp.getTextWidth("buttonFont", closeText);
        GameApp.drawText("buttonFont", closeText, centerX - closeWidth/2, textY, "gray-500");
        
        GameApp.endSpriteRendering();
        
        // Close popup when clicking anywhere AFTER cooldown expires
        if (hintPopupOpenCooldown <= 0f && GameApp.isButtonJustPressed(0)) {
            showHintPopup = false;
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 1.0f);
            }
        }
    }
}
