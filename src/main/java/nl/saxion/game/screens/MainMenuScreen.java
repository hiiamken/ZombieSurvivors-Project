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

            // Register custom button text colors (RGB values from hex)
            if (!GameApp.hasColor("button_play_color")) {
                GameApp.addColor("button_play_color", 47, 87, 83); // #2f5753
            }
            if (!GameApp.hasColor("button_settings_color")) {
                GameApp.addColor("button_settings_color", 171, 81, 48); // #ab5130
            }
            if (!GameApp.hasColor("button_quit_color")) {
                GameApp.addColor("button_quit_color", 79, 29, 76); // #4f1d4c
            }
            
            // Load upheavtt font for button text with larger size
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 50,
                    "gray-200", 2f, "black", 2, 2, "gray-600", true);
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

            resourcesLoaded = true;
            DebugLogger.log("Resources loaded");
        }
    }

    private void createButtons() {
        buttons = new ArrayList<>();
        
        // Calculate button size from texture dimensions with scale
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        int scale = 1; // 2 or 3
        
        buttonWidth = texW * scale;
        buttonHeight = texH * scale;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Calculate total height of all buttons with spacing
        float totalHeight = (buttonHeight * 3) + (buttonSpacing * 2);
        float startY = (screenHeight - totalHeight) / 2 + buttonHeight;

        // Center buttons horizontally
        float buttonX = (screenWidth - buttonWidth) / 2;

        // PLAY button (green) - top
        Button playButton = new Button(buttonX, startY, buttonWidth, buttonHeight, "");
        playButton.setOnClick(() -> {
            DebugLogger.log("PLAY button clicked!");
            GameApp.log("Starting game...");
            // Action will be executed after delay in render()
        });
        if (GameApp.hasTexture("green_long")) {
            playButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
            DebugLogger.log("PLAY button: sprites set (green_long, green_pressed_long)");
        } else {
            DebugLogger.log("PLAY button: green_long texture NOT FOUND!");
        }
        buttons.add(playButton);
        DebugLogger.log("PLAY button created at (%.1f, %.1f) size (%.1f x %.1f)", buttonX, startY, buttonWidth, buttonHeight);

        // OPTIONS button (orange) - middle
        float optionsY = startY - buttonHeight - buttonSpacing;
        Button optionsButton = new Button(buttonX, optionsY, buttonWidth, buttonHeight, "");
        optionsButton.setOnClick(() -> {
            DebugLogger.log("OPTIONS button clicked!");
            GameApp.log("Opening settings...");
            // Action will be executed after delay in render()
        });
        if (GameApp.hasTexture("orange_long")) {
            optionsButton.setSprites("orange_long", "orange_long", "orange_long", "orange_pressed_long");
            DebugLogger.log("OPTIONS button: sprites set (orange_long, orange_pressed_long)");
        } else {
            DebugLogger.log("OPTIONS button: orange_long texture NOT FOUND!");
        }
        buttons.add(optionsButton);
        DebugLogger.log("OPTIONS button created at (%.1f, %.1f) size (%.1f x %.1f)", buttonX, optionsY, buttonWidth, buttonHeight);

        // QUIT button (red) - bottom
        float quitY = startY - (buttonHeight + buttonSpacing) * 2;
        Button quitButton = new Button(buttonX, quitY, buttonWidth, buttonHeight, "");
        quitButton.setOnClick(() -> {
            DebugLogger.log("QUIT button clicked!");
            GameApp.log("Quitting game...");
            // Action will be executed after delay in render()
        });
        if (GameApp.hasTexture("red_long")) {
            quitButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
            DebugLogger.log("QUIT button: sprites set (red_long, red_pressed_long)");
        } else {
            DebugLogger.log("QUIT button: red_long texture NOT FOUND!");
        }
        buttons.add(quitButton);
        DebugLogger.log("QUIT button created at (%.1f, %.1f) size (%.1f x %.1f)", buttonX, quitY, buttonWidth, buttonHeight);
    }

    @Override
    public void hide() {
        // Stop background music when leaving menu
        if (soundManager != null) {
            soundManager.stopMusic();
            // Don't dispose here - keep sounds loaded for other screens
            // Only dispose when game is closing
        }
        
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
    }

    // Draw text labels on buttons
    private void drawButtonText() {
        GameApp.startSpriteRendering();

        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            String text = "";
            String colorName = "";
            if (i == 0) {
                text = "PLAY";
                colorName = "button_play_color";
            } else if (i == 1) {
                text = "SETTINGS";
                colorName = "button_settings_color";
            } else {
                text = "QUIT";
                colorName = "button_quit_color";
            }

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
                // Scale to fill screen while maintaining aspect ratio
                float screenAspect = screenWidth / screenHeight;
                float texAspect = (float) texWidth / texHeight;

                if (screenAspect > texAspect) {
                    // Screen is wider, fit to height
                    bgHeight = screenHeight;
                    bgWidth = bgHeight * texAspect;
                } else {
                    // Screen is taller, fit to width
                    bgWidth = screenWidth;
                    bgHeight = bgWidth / texAspect;
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
                // Scale to fit screen width (60% of screen - reduced from 70% to make it less dominant)
                float targetWidth = screenWidth * 0.45f;
                float aspectRatio = (float)texHeight / texWidth;
                titleWidth = targetWidth;
                titleHeight = targetWidth * aspectRatio;
            }
        } catch (Exception e) {
            // Use default size if texture dimensions unavailable
        }

        // Center horizontally, position at top (moved up 30px to give more space for buttons)
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = screenHeight - titleHeight - 10f; // 70px from top (reduced from 100px)

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

        // Update button pressed states based on mouse
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            boolean isMouseOver = button.containsPoint(mouseX, mouseY);

            // Show pressed state when mouse is down over button
            button.setPressed(isMouseDown && isMouseOver);

            // Debug button state
            if (isMouseOver && isMouseJustPressed) {
                String buttonName = i == 0 ? "PLAY" : (i == 1 ? "OPTIONS" : "QUIT");
                DebugLogger.log("%s button: Mouse clicked! Button bounds: (%.1f, %.1f) size (%.1f x %.1f)",
                        buttonName, button.getX(), button.getY(), button.getWidth(), button.getHeight());
            }
        }

        // Mouse click (on release) to activate button
        if (isMouseJustPressed) {
            DebugLogger.log("Mouse button just pressed at (%.1f, %.1f)", mouseX, mouseY);

            boolean clickedAny = false;
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                if (button.containsPoint(mouseX, mouseY)) {
                    String buttonName = i == 0 ? "PLAY" : (i == 1 ? "OPTIONS" : "QUIT");
                    DebugLogger.log("%s button: Click detected! Starting press delay...", buttonName);

                    // Store button and action for delayed execution
                    pressedButton = button;
                    button.setPressed(true);

                    // Play button click sound at higher volume (0.7f = 70% volume)
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    // Create delayed action based on button
                    if (i == 0) {
                        // PLAY button
                        pendingAction = () -> {
                            DebugLogger.log("PLAY button action executing after delay");
                            GameApp.switchScreen("play");
                        };
                    } else if (i == 1) {
                        // OPTIONS button
                        pendingAction = () -> {
                            DebugLogger.log("OPTIONS button action executing after delay");
                            GameApp.switchScreen("settings");
                        };
                    } else {
                        // QUIT button
                        pendingAction = () -> {
                            DebugLogger.log("QUIT button action executing after delay");
                            GameApp.quit();
                        };
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
}
