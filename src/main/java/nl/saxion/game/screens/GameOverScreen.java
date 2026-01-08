package nl.saxion.game.screens;

import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the game over screen display, input, and rendering.
 * Extends ScalableGameScreen with world size 1280x720 like MainMenu.
 */
public class GameOverScreen extends ScalableGameScreen {
    private static final float FADE_DURATION = 1.0f; // 1 second fade
    
    // Static score storage to pass from PlayScreen
    private static int storedScore = 0;
    
    public static void setScore(int score) {
        storedScore = score;
    }
    
    private int score = 0;
    private float gameOverFadeTimer = 0f;
    private float colorPulseTimer = 0f;
    
    // Game over buttons
    private List<Button> gameOverButtons;
    private boolean buttonsInitialized = false;
    private boolean resourcesLoaded = false;
    
    // Sound manager for button clicks
    private SoundManager soundManager;
    
    // Delay for button press animation
    private float pressDelay = 0.15f; // 150ms delay
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;
    
    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringButton = false;
    
    public GameOverScreen() {
        super(1280, 720); // 16:9 aspect ratio - same as MainMenu
        gameOverButtons = new ArrayList<>();
    }
    
    @Override
    public void show() {
        // Load score from static storage
        score = storedScore;
        
        // Initialize sound manager
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        // Load resources
        loadResources();
        
        // Log texture dimensions (always, not just on first load)
        logTextureInfo();
        
        // Initialize buttons
        initializeButtons();
        
        // Load cursors
        loadCursors();
        
        // Reset fade animation
        gameOverFadeTimer = 0f;
        
        // Play game over sound
        if (soundManager != null) {
            soundManager.playSound("gameover", 1.0f);
        }
    }
    
    /**
     * Log texture dimensions for debugging (called every time screen shows).
     */
    private void logTextureInfo() {
        try {
            if (GameApp.hasTexture("gameover_bg")) {
                int bgWidth = GameApp.getTextureWidth("gameover_bg");
                int bgHeight = GameApp.getTextureHeight("gameover_bg");
                if (bgWidth > 0 && bgHeight > 0) {
                    float bgAspect = (float) bgWidth / bgHeight;
                    float worldAspect = 1280f / 720f; // 1.78
                    DebugLogger.log("Background texture: %dx%d (aspect: %.2f), World: 1280x720 (aspect: %.2f)", 
                        bgWidth, bgHeight, bgAspect, worldAspect);
                    if (Math.abs(bgAspect - worldAspect) > 0.1f) {
                        DebugLogger.log("WARNING: Background aspect ratio (%.2f) doesn't match world (%.2f). Image will be cropped.", 
                            bgAspect, worldAspect);
                    } else {
                        DebugLogger.log("Background aspect ratio matches world - perfect fit!");
                    }
                }
            }
        } catch (Exception e) {
            DebugLogger.log("Could not get background texture dimensions: %s", e.getMessage());
        }
    }
    
    @Override
    public void hide() {
        // Stop sounds when leaving screen
        if (soundManager != null) {
            soundManager.stopMusic();
        }
        
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
    
    /**
     * Load resources (fonts, textures, etc.)
     */
    private void loadResources() {
        if (resourcesLoaded) return;
        
        // Load game over fonts
        GameApp.addStyledFont("gameOverTitle", "fonts/Emulogic-zrEw.ttf", 72,
                "red-500", 2f, "black", 3, 3, "red-900", true);
        GameApp.addFont("gameOverText", "fonts/PressStart2P-Regular.ttf", 16, true);
        
        // Register unified button text colors
        // GREEN button - dark green-gray for contrast on bright green
        if (!GameApp.hasColor("gameover_play_again_color")) {
            GameApp.addColor("gameover_play_again_color", 25, 50, 25); // Dark green-gray
        }
        if (!GameApp.hasColor("button_green_text")) {
            GameApp.addColor("button_green_text", 25, 50, 25);
        }
        // RED button - dark maroon for contrast on red/pink
        if (!GameApp.hasColor("gameover_back_menu_color")) {
            GameApp.addColor("gameover_back_menu_color", 60, 15, 30); // Dark maroon
        }
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        
        // Button font for GameOverScreen (1280x720 world, half-size buttons)
        // Size 28 fits well with medium buttons
        GameApp.addStyledFont("gameOverButtonFont", "fonts/upheavtt.ttf", 28,
                "white", 0f, "black", 2, 2, "gray-700", true);
        
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
        
        // Load game over background and title
        if (!GameApp.hasTexture("gameover_bg")) {
            GameApp.addTexture("gameover_bg", "assets/ui/gameover.png");
            // Log texture dimensions once when loaded
            try {
                int bgWidth = GameApp.getTextureWidth("gameover_bg");
                int bgHeight = GameApp.getTextureHeight("gameover_bg");
                if (bgWidth > 0 && bgHeight > 0) {
                    float bgAspect = (float) bgWidth / bgHeight;
                    DebugLogger.log("Background texture loaded: %dx%d (aspect: %.2f), World: 1280x720 (aspect: 1.78)", 
                        bgWidth, bgHeight, bgAspect);
                    if (Math.abs(bgAspect - 1.78f) > 0.1f) {
                        DebugLogger.log("WARNING: Background aspect ratio (%.2f) doesn't match world (1.78). Image will be cropped.", bgAspect);
                    }
                }
            } catch (Exception e) {
                DebugLogger.log("Could not get background texture dimensions");
            }
        }
        if (!GameApp.hasTexture("gameover_title")) {
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
            // Log texture dimensions once when loaded
            try {
                int titleWidth = GameApp.getTextureWidth("gameover_title");
                int titleHeight = GameApp.getTextureHeight("gameover_title");
                if (titleWidth > 0 && titleHeight > 0) {
                    DebugLogger.log("Title texture loaded: %dx%d", titleWidth, titleHeight);
                }
            } catch (Exception e) {
                DebugLogger.log("Could not get title texture dimensions");
            }
        }
        
        resourcesLoaded = true;
    }
    
    /**
     * Load cursor images
     */
    private void loadCursors() {
        try {
            // Load pointer cursor
            String pointerPath = "assets/ui/pointer.png";
            Pixmap pointerSource = new Pixmap(Gdx.files.internal(pointerPath));
            int pointerSourceWidth = pointerSource.getWidth();
            int pointerSourceHeight = pointerSource.getHeight();
            
            int targetSize = 32;
            Pixmap pointerPixmap = new Pixmap(targetSize, targetSize, pointerSource.getFormat());
            pointerPixmap.drawPixmap(pointerSource,
                    0, 0, pointerSourceWidth, pointerSourceHeight,
                    0, 0, targetSize, targetSize);
            cursorPointer = Gdx.graphics.newCursor(pointerPixmap, 0, 0);
            pointerPixmap.dispose();
            pointerSource.dispose();
            
            // Load hover cursor
            String cursorPath = "assets/ui/cursor.png";
            Pixmap cursorSource = new Pixmap(Gdx.files.internal(cursorPath));
            int cursorSourceWidth = cursorSource.getWidth();
            int cursorSourceHeight = cursorSource.getHeight();
            
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
            GameApp.showCursor();
        }
    }
    
    /**
     * Update the game over screen (fade animation, button delays, etc.).
     */
    @Override
    public void render(float delta) {
        // Debug log (only once per frame for first few frames)
        boolean isFirstFrame = (gameOverFadeTimer < 0.1f);
        
        if (isFirstFrame) {
            float windowW = GameApp.getWindowWidth();
            float windowH = GameApp.getWindowHeight();
            float worldW = GameApp.getWorldWidth();
            float worldH = GameApp.getWorldHeight();
            
            DebugLogger.log("═══════════════════════════════════════════════════════");
            DebugLogger.log("=== GameOverScreen Render - START ===");
            DebugLogger.log("Window size: %.0fx%.0f (aspect: %.4f)", windowW, windowH, windowW / windowH);
            DebugLogger.log("World size: %.0fx%.0f (aspect: %.4f)", worldW, worldH, worldW / worldH);
            DebugLogger.log("Gdx.graphics.getWidth/Height: %dx%d", 
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            DebugLogger.log("SpriteBatch state check (before background): %s", 
                GameApp.getSpriteBatch().isDrawing() ? "DRAWING" : "NOT DRAWING");
        }
        
        // 1. Draw full-window background FIRST (before viewport is applied)
        // This draws directly to screen coordinates, filling entire window including letterboxing areas
        // MUST be drawn before super.render() to avoid being cleared
        if (isFirstFrame) {
            DebugLogger.log(">>> STEP 1: Drawing background BEFORE super.render() (bypassing viewport)");
        }
        drawBackgroundFullWindow();
        
        if (isFirstFrame) {
            DebugLogger.log(">>> STEP 2: Background drawn, now calling super.render()");
        }
        
        // 2. Let ScalableGameScreen render world + viewport (may clear screen, but background is already drawn)
        super.render(delta);
        
        if (isFirstFrame) {
            DebugLogger.log(">>> STEP 3: After super.render()");
            DebugLogger.log("SpriteBatch state check (after super.render): %s", 
                GameApp.getSpriteBatch().isDrawing() ? "DRAWING" : "NOT DRAWING");
        }
        
        if (isFirstFrame) {
            DebugLogger.log(">>> STEP 4: Background drawn");
            DebugLogger.log("=== GameOverScreen Render - CONTINUE ===");
        }
        
        // Update fade timer
        if (gameOverFadeTimer < FADE_DURATION) {
            gameOverFadeTimer += delta;
        }
        
        // Update color pulse animation
        colorPulseTimer += delta * 0.2f;
        
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
        
        // Handle input (only if not waiting for delay)
        if (pendingAction == null) {
            handleInput();
        } else {
            // Keep button pressed during delay
            if (pressedButton != null) {
                pressedButton.setPressed(true);
            }
        }
        
        // Render the game over screen (world-based UI)
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Calculate fade alpha (0 to 1)
        float fadeAlpha = Math.min(gameOverFadeTimer / FADE_DURATION, 1.0f);
        
        // Draw very subtle vignette effect (reduced strength to match MainMenu style)
        drawVignette(screenWidth, screenHeight, fadeAlpha);
        
        // Render buttons first (they manage their own sprite batch)
        if (gameOverButtons != null) {
            for (Button button : gameOverButtons) {
                button.render();
            }
        }
        
        // Now render text in sprite batch
        GameApp.startSpriteRendering();
        
        // Draw "GAME OVER" title image (replacing text with image like MainMenu)
        drawTitle(centerX, centerY);
        
        // Draw score centered below title with better formatting
        String scoreText = String.format("SCORE: %,d", score); // Format with commas
        float titleY = centerY + 90; // Same position as title
        float titleHeight = 200f; // Default title height for spacing calculation
        try {
            if (GameApp.hasTexture("gameover_title")) {
                titleHeight = GameApp.getTextureHeight("gameover_title");
            }
        } catch (Exception e) {
            // Use default if texture not available
        }
        float scoreTextHeight = GameApp.getTextHeight("gameOverText", scoreText);
        float scoreY = titleY - titleHeight / 2 - scoreTextHeight * 2.2f; // Better spacing
        GameApp.drawTextCentered("gameOverText", scoreText, centerX, scoreY, "white");
        
        // Draw button text labels
        drawButtonText(centerX, centerY);
        
        GameApp.endSpriteRendering();
        
        // Final debug log (only first frame)
        if (isFirstFrame) {
            DebugLogger.log(">>> STEP 4: All rendering complete");
            DebugLogger.log("SpriteBatch state check (final): %s", 
                GameApp.getSpriteBatch().isDrawing() ? "DRAWING" : "NOT DRAWING");
            DebugLogger.log("=== GameOverScreen Render - END ===");
            DebugLogger.log("═══════════════════════════════════════════════════════");
        }
    }
    
    /**
     * Handle input for the game over screen - uses world coordinates from ScalableGameScreen.
     */
    private void handleInput() {
        // Get mouse position in world coordinates using ScalableGameScreen helper
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
                isHoveringButton = false;
            } else if (hoveringAnyButton) {
                if (!isHoveringButton) {
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringButton = true;
                }
            } else {
                if (isHoveringButton) {
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringButton = false;
                }
            }
        }
        
        // Update buttons with world mouse position
        for (Button button : gameOverButtons) {
            button.update(worldMouseX, worldMouseY);
        }
        
        // Handle mouse click (only if not waiting for delay)
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed && pendingAction == null) {
            for (int i = 0; i < gameOverButtons.size(); i++) {
                Button button = gameOverButtons.get(i);
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    String buttonName = i == 0 ? "PLAY AGAIN" : "BACK TO MENU";
                    DebugLogger.log("%s button clicked! Starting press delay...", buttonName);
                    
                    // Play button click sound at 2.5f volume
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    // Store button and action for delayed execution
                    pressedButton = button;
                    button.setPressed(true);
                    
                    // Create delayed action based on button
                    if (i == 0) {
                        // Play Again button
                        pendingAction = () -> {
                            DebugLogger.log("PLAY AGAIN action executing after delay");
                            GameApp.switchScreen("play");
                        };
                    } else if (i == 1) {
                        // Back to Menu button
                        pendingAction = () -> {
                            DebugLogger.log("BACK TO MENU action executing after delay");
                            GameApp.switchScreen("menu");
                        };
                    }
                    
                    pressTimer = 0f;
                    break;
                }
            }
        }
        
        // Update button pressed states (only if not waiting for delay)
        if (pendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : gameOverButtons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        }
    }
    
    /**
     * Get the game over buttons (for cursor switching).
     */
    public List<Button> getButtons() {
        return gameOverButtons;
    }
    
    /**
     * Initialize the game over buttons.
     */
    private void initializeButtons() {
        if (buttonsInitialized) return;
        
        gameOverButtons.clear();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        // Calculate button size from texture dimensions - divide by 0.75
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        
        float buttonWidth = texW / 2f;
        float buttonHeight = texH / 2f;
        float buttonSpacing = 20f; // Spacing between buttons
        
        // Play Again button (green) - positioned lower on screen
        float playAgainY = centerY - 80; // Better vertical positioning
        Button playAgainButton = new Button(centerX - buttonWidth / 2, playAgainY, buttonWidth, buttonHeight, "");
        playAgainButton.setOnClick(() -> {
            // Will be handled by handleInput with delay
        });
        if (GameApp.hasTexture("green_long")) {
            playAgainButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        gameOverButtons.add(playAgainButton);
        
        // Back to Menu button (red) - positioned below Play Again
        float backToMenuY = playAgainY - buttonHeight - buttonSpacing;
        Button backToMenuButton = new Button(centerX - buttonWidth / 2, backToMenuY, buttonWidth, buttonHeight, "");
        backToMenuButton.setOnClick(() -> {
            // Will be handled by handleInput with delay
        });
        if (GameApp.hasTexture("red_long")) {
            backToMenuButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        gameOverButtons.add(backToMenuButton);
        
        // Debug: log button positions (only if debug enabled)
        DebugLogger.log("Game Over buttons initialized:");
        DebugLogger.log("  PLAY AGAIN: (%.1f, %.1f)", playAgainButton.getX(), playAgainButton.getY());
        DebugLogger.log("  BACK TO MENU: (%.1f, %.1f)", backToMenuButton.getX(), backToMenuButton.getY());
        
        buttonsInitialized = true;
    }
    
    /**
     * Draw game over title image at top center of screen.
     */
    private void drawTitle(float centerX, float centerY) {
        if (!GameApp.hasTexture("gameover_title")) {
            // Try to load again if not found
            GameApp.addTexture("gameover_title", "assets/ui/gameovertitle.png");
            if (!GameApp.hasTexture("gameover_title")) {
                DebugLogger.log("ERROR: gameover_title texture still not found after reload!");
                return; // Still not found, skip rendering
            }
        }

        float screenWidth = GameApp.getWorldWidth();

        // Get actual texture dimensions
        float titleWidth = 600f; // Default size
        float titleHeight = 200f; // Default size

        try {
            int texWidth = GameApp.getTextureWidth("gameover_title");
            int texHeight = GameApp.getTextureHeight("gameover_title");
            if (texWidth > 0 && texHeight > 0) {
                // Scale to fit screen width (60% of screen - same as MainMenu title)
                float targetWidth = screenWidth * 0.6f;
                float aspectRatio = (float)texHeight / texWidth;
                titleWidth = targetWidth;
                titleHeight = targetWidth * aspectRatio;
            }
        } catch (Exception e) {
            // Use default size if texture dimensions unavailable
        }

        // Center horizontally, position at top (same as MainMenu style)
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = centerY - 20f; // Same position as before (lowered 30px from original)

        // Render title (must be in sprite batch)
        GameApp.drawTexture("gameover_title", titleX, titleY, titleWidth, titleHeight);
    }

    /**
     * Draw button text labels.
     */
    private void drawButtonText(float centerX, float centerY) {
        if (gameOverButtons == null || gameOverButtons.size() < 2) return;
        
        // Play Again button text
        Button playAgainButton = gameOverButtons.get(0);
        float playAgainCenterX = playAgainButton.getX() + playAgainButton.getWidth() / 2;
        float playAgainCenterY = playAgainButton.getY() + playAgainButton.getHeight() / 2;
        float playAgainTextHeight = GameApp.getTextHeight("gameOverButtonFont", "PLAY AGAIN");
        float playAgainAdjustedY = playAgainCenterY + playAgainTextHeight * 0.15f; // Move up like main menu
        GameApp.drawTextCentered("gameOverButtonFont", "PLAY AGAIN", playAgainCenterX, playAgainAdjustedY, "button_green_text");
        
        // Back to Menu button text
        Button backToMenuButton = gameOverButtons.get(1);
        float backToMenuCenterX = backToMenuButton.getX() + backToMenuButton.getWidth() / 2;
        float backToMenuCenterY = backToMenuButton.getY() + backToMenuButton.getHeight() / 2;
        float backToMenuTextHeight = GameApp.getTextHeight("gameOverButtonFont", "BACK TO MENU");
        float backToMenuAdjustedY = backToMenuCenterY + backToMenuTextHeight * 0.15f; // Move up like main menu
        GameApp.drawTextCentered("gameOverButtonFont", "BACK TO MENU", backToMenuCenterX, backToMenuAdjustedY, "button_red_text");
    }
    
    /**
     * Draw game over background image - fill entire window using LibGDX directly.
     * This bypasses viewport to draw in screen coordinates.
     * MUST be called BEFORE super.render() to avoid being cleared.
     */
    private void drawBackgroundFullWindow() {
        boolean isFirstFrame = (gameOverFadeTimer < 0.1f);
        
        if (isFirstFrame) {
            DebugLogger.log("--- drawBackgroundFullWindow() START ---");
        }
        
        // Check if texture exists
        if (!GameApp.hasTexture("gameover_bg")) {
            if (isFirstFrame) {
                DebugLogger.log("WARNING: gameover_bg texture not found, trying to load...");
            }
            GameApp.addTexture("gameover_bg", "assets/ui/gameover.png");
            if (!GameApp.hasTexture("gameover_bg")) {
                DebugLogger.log("ERROR: gameover_bg texture still not found after reload!");
                return;
            }
        }

        // Get window size (screen coordinates)
        int windowW = Gdx.graphics.getWidth();
        int windowH = Gdx.graphics.getHeight();
        float windowW_f = GameApp.getWindowWidth();
        float windowH_f = GameApp.getWindowHeight();
        
        // Get texture object and dimensions
        Texture texture = GameApp.getTexture("gameover_bg");
        if (texture == null) {
            DebugLogger.log("ERROR: Could not get texture object from GameApp!");
            return;
        }
        
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();
        int texWidth_api = GameApp.getTextureWidth("gameover_bg");
        int texHeight_api = GameApp.getTextureHeight("gameover_bg");
        
        // Get world dimensions for comparison
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();
        
        // Detailed debug log (only first frame)
        if (isFirstFrame) {
            DebugLogger.log("  Texture from LibGDX: %dx%d", texWidth, texHeight);
            DebugLogger.log("  Texture from GameApp API: %dx%d", texWidth_api, texHeight_api);
            DebugLogger.log("  Window size (Gdx.graphics): %dx%d", windowW, windowH);
            DebugLogger.log("  Window size (GameApp API): %.2fx%.2f", windowW_f, windowH_f);
            DebugLogger.log("  World size: %.2fx%.2f", worldW, worldH);
            DebugLogger.log("  Texture aspect: %.4f", (float)texWidth / texHeight);
            DebugLogger.log("  Window aspect (Gdx): %.4f", (float)windowW / windowH);
            DebugLogger.log("  Window aspect (GameApp): %.4f", windowW_f / windowH_f);
            DebugLogger.log("  World aspect: %.4f", worldW / worldH);
            DebugLogger.log("  Drawing at SCREEN coordinates: (0, 0) size: %dx%d", windowW, windowH);
        }
        
        // Draw background directly in screen coordinates (bypass viewport)
        // Disable scissor test to allow drawing outside viewport bounds
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        
        // Use LibGDX SpriteBatch with screen-space projection matrix
        SpriteBatch batch = GameApp.getSpriteBatch();
        
        if (isFirstFrame) {
            DebugLogger.log("  SpriteBatch before draw: isDrawing=%s", batch.isDrawing());
        }
        
        // Ensure batch is in correct state - if already drawing, end it first
        boolean wasDrawing = batch.isDrawing();
        if (wasDrawing) {
            if (isFirstFrame) {
                DebugLogger.log("  WARNING: SpriteBatch was already drawing! Ending it first...");
            }
            batch.end();
        }
        
        // Set viewport to full window (bypass any viewport restrictions)
        Gdx.gl.glViewport(0, 0, windowW, windowH);
        
        // Set up screen-space camera for drawing in screen coordinates
        OrthographicCamera screenCamera = new OrthographicCamera(windowW, windowH);
        screenCamera.setToOrtho(false, windowW, windowH);
        screenCamera.update();
        
        // Set projection matrix to screen space
        batch.setProjectionMatrix(screenCamera.combined);
        
        if (isFirstFrame) {
            DebugLogger.log("  Set GL viewport to: 0, 0, %d, %d", windowW, windowH);
            DebugLogger.log("  Created screen camera: %dx%d", windowW, windowH);
            DebugLogger.log("  Camera position: (%.2f, %.2f)", screenCamera.position.x, screenCamera.position.y);
        }
        
        // Draw background in screen space (no viewport transformation)
        batch.begin();
        batch.draw(texture, 0, 0, windowW, windowH);
        batch.end();
        
        if (isFirstFrame) {
            DebugLogger.log("  Background drawn at screen coords: (0, 0) to (%d, %d)", windowW, windowH);
            DebugLogger.log("  SpriteBatch after draw: isDrawing=%s", batch.isDrawing());
            DebugLogger.log("--- drawBackgroundFullWindow() END ---");
        }
        
        // Note: Viewport will be reset by ScalableGameScreen on next frame
        // Projection matrix will be reset when GameApp.startSpriteRendering() is called next
    }

    // Fallback gradient background if image fails to load
    private void drawGradientFallback(float width, float height, float alpha) {
        // Very subtle pulse effect - gentle and smooth
        float pulse = (float) (0.95f + 0.05f * Math.sin(colorPulseTimer * Math.PI * 2));
        
        // More steps for smoother gradient
        int steps = 100;
        GameApp.startShapeRenderingFilled();
        
        for (int i = 0; i < steps; i++) {
            float y = (height / steps) * i;
            float rectHeight = height / steps;
            
            // Smooth interpolation from top to bottom
            float t = (float) i / steps;
            
            // Softer, darker colors - less eye strain
            // Dark blue-gray at top transitioning to pure black at bottom
            float colorPhase = (float) (colorPulseTimer * 0.15f + t * 0.3f);
            
            // Base dark blue-gray tones (much darker)
            float baseRed = 25f;
            float baseGreen = 20f;
            float baseBlue = 35f;
            
            // Very subtle color variation
            float redIntensity = baseRed + 5f * (float) Math.sin(colorPhase);
            float greenIntensity = baseGreen + 3f * (float) Math.sin(colorPhase + 1.0f);
            float blueIntensity = baseBlue + 5f * (float) Math.sin(colorPhase + 2.0f);
            
            // Smooth fade to black at bottom
            float fadeFactor = (float) Math.pow(1 - t, 1.5f); // Smooth curve
            int r = (int) (redIntensity * fadeFactor * pulse * alpha);
            int g = (int) (greenIntensity * fadeFactor * pulse * alpha);
            int b = (int) (blueIntensity * fadeFactor * pulse * alpha);
            
            // Ensure values stay in valid range
            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));
            
            GameApp.setColor(r, g, b, (int)(255 * alpha));
            GameApp.drawRect(0, y, width, rectHeight);
        }
        
        GameApp.endShapeRendering();
    }
    
    /**
     * Draw very subtle vignette effect - reduced to match MainMenu style.
     * Draws dark gradient overlays on all four edges.
     */
    private void drawVignette(float width, float height, float alpha) {
        // Vignette strength - much lighter to match MainMenu (no heavy overlay)
        float vignetteStrength = 0.15f * alpha; // Very subtle, just for slight edge darkening
        
        int steps = 25; // Number of gradient steps per edge
        float edgeSize = Math.min(width, height) * 0.25f; // 25% of smaller dimension
        
        GameApp.startShapeRenderingFilled();
        
        // Top edge - dark gradient from top to center
        for (int i = 0; i < steps; i++) {
            float y = (edgeSize / steps) * i;
            float rectHeight = edgeSize / steps;
            float t = (float) i / steps;
            // Quadratic fade for smooth transition
            float darknessFactor = (float) (1.0f - t * t);
            int darkness = (int) (255 * vignetteStrength * darknessFactor);
            GameApp.setColor(0, 0, 0, darkness);
            GameApp.drawRect(0, y, width, rectHeight);
        }
        
        // Bottom edge - dark gradient from bottom to center
        for (int i = 0; i < steps; i++) {
            float y = height - (edgeSize / steps) * (i + 1);
            float rectHeight = edgeSize / steps;
            float t = (float) i / steps;
            float darknessFactor = (float) (1.0f - t * t);
            int darkness = (int) (255 * vignetteStrength * darknessFactor);
            GameApp.setColor(0, 0, 0, darkness);
            GameApp.drawRect(0, y, width, rectHeight);
        }
        
        // Left edge - dark gradient from left to center
        for (int i = 0; i < steps; i++) {
            float x = (edgeSize / steps) * i;
            float rectWidth = edgeSize / steps;
            float t = (float) i / steps;
            float darknessFactor = (float) (1.0f - t * t);
            int darkness = (int) (255 * vignetteStrength * darknessFactor);
            GameApp.setColor(0, 0, 0, darkness);
            GameApp.drawRect(x, 0, rectWidth, height);
        }
        
        // Right edge - dark gradient from right to center
        for (int i = 0; i < steps; i++) {
            float x = width - (edgeSize / steps) * (i + 1);
            float rectWidth = edgeSize / steps;
            float t = (float) i / steps;
            float darknessFactor = (float) (1.0f - t * t);
            int darkness = (int) (255 * vignetteStrength * darknessFactor);
            GameApp.setColor(0, 0, 0, darkness);
            GameApp.drawRect(x, 0, rectWidth, height);
        }
        
        GameApp.endShapeRendering();
    }
}
