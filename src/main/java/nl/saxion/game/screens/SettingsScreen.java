package nl.saxion.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;


// Settings screen with volume controls
public class SettingsScreen extends ScalableGameScreen {

    // Static field to track which screen to return to
    // "menu" = return to main menu (default)
    // "pause" = return to pause menu in PlayScreen
    private static String returnScreen = "menu";
    
    /**
     * Set the screen to return to when closing settings.
     * @param screen "menu" or "pause"
     */
    public static void setReturnScreen(String screen) {
        returnScreen = screen;
    }
    
    /**
     * Get the current return screen.
     */
    public static String getReturnScreen() {
        return returnScreen;
    }

    // Volume settings (0-100)
    private int masterVolume = 100;
    private int sfxVolume = 100;
    private int musicVolume = 70;

    // Fullscreen setting
    private boolean isFullscreen = true;
    
    // Sound manager for background music
    private SoundManager soundManager;

    // Cursor management
    private Cursor cursorPointer; // Left side (pointer/default)
    private Cursor cursorHover;   // Right side (hover)


    // Colors - using custom registered colors
    private static final String BG_COLOR = "black";
    
    // Coordinate grid system for easier positioning
    // Panel occupies 75% width, 85% height, centered
    // Use percentage values (0.0 to 1.0) relative to panel dimensions
    private static final float PANEL_WIDTH_RATIO = 0.75f;  // 75% of screen width
    private static final float PANEL_HEIGHT_RATIO = 0.85f; // 85% of screen height
    
    // Tab configuration
    // Tabs are horizontal, 5 cells wide (columns 2-6), 1 cell tall
    // Each tab starts at column 2 (10% from left)
    private static final float TAB_X_OFFSET_RATIO = 0.10f;  // 10% from left (column 2)
    private static final int NUM_TABS = 4;
    
    // Cell positions for each tab (starting cell number)
    // Tab 1: cell 32 (row 4, col 2) - FULL SCREEN
    // Tab 2: cell 42 (row 5, col 2) - MASTER VOLUME
    // Tab 3: cell 52 (row 6, col 2) - MUSIC
    // Tab 4: cell 62 (row 7, col 2) - SOUND EFFECT
    private static final int[] TAB_START_CELLS = {32, 42, 52, 62};
    private static final String[] TAB_LABELS = {"FULL SCREEN", "MASTER VOLUME", "MUSIC", "SOUND EFFECT"};
    
    // Font name for settings screen
    private static final String SETTINGS_FONT = "settings_font";
    private static final String SETTINGS_FONT_SMALL = "settings_font_small";
    
    // Toggle button position (between cells 37 and 38)
    // Row 4 (same as FULL SCREEN tab), columns 7-8
    private static final float TOGGLE_X_RATIO = 0.65f; // 65% from left (center of columns 7 and 8)
    
    // Cached panel dimensions for click handling
    private float cachedPanelX, cachedPanelY, cachedPanelWidth, cachedPanelHeight;
    
    // Volume slider configuration
    private static final int VOLUME_LEVELS = 9; // 9 segments
    private static final float SLIDER_X_RATIO = 0.60f; // Start of slider (60% from left)
    
    // Button press animation - Master Volume
    private boolean masterMinusPressed = false;
    private boolean masterPlusPressed = false;
    private float masterMinusPressTimer = 0f;
    private float masterPlusPressTimer = 0f;
    
    // Button press animation - Music
    private boolean musicMinusPressed = false;
    private boolean musicPlusPressed = false;
    private float musicMinusPressTimer = 0f;
    private float musicPlusPressTimer = 0f;
    
    // Button press animation - Sound Effect
    private boolean sfxMinusPressed = false;
    private boolean sfxPlusPressed = false;
    private float sfxMinusPressTimer = 0f;
    private float sfxPlusPressTimer = 0f;
    
    private static final float BUTTON_PRESS_DURATION = 0.1f; // 100ms animation

    public SettingsScreen() {
        super(640, 360); // 16:9 aspect ratio - smaller world size for zoom effect
    }

    @Override
    public void show() {
        // Load cursors
        loadPusheenCursors();
        
        // Initialize sound manager for background music
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        // Only start background music if returning to menu (not from pause)
        // When coming from pause, the ingame music is already playing (at reduced volume)
        if (soundManager != null && !"pause".equals(returnScreen)) {
            soundManager.playMusic(true);
        }

        loadResources();
        loadSettingsFromConfig();
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

    // Load settings from config file
    private void loadSettingsFromConfig() {
        GameConfig config = ConfigManager.loadConfig();
        masterVolume = (int) (config.masterVolume * 100);
        sfxVolume = (int) (config.sfxVolume * 100);
        musicVolume = (int) (config.musicVolume * 100);
        isFullscreen = config.fullscreen;
        
        // Apply fullscreen setting immediately when loading
        applyFullscreenSetting();
        
        // Apply volume to sound manager
        if (soundManager != null) {
            soundManager.setMasterVolume(masterVolume / 100f);
            soundManager.setMusicVolume(musicVolume / 100f);
            soundManager.setSFXVolume(sfxVolume / 100f);
        }
    }

    // Save settings to config file
    private void saveSettingsToConfig() {
        GameConfig config = ConfigManager.loadConfig();
        config.masterVolume = masterVolume / 100f;
        config.sfxVolume = sfxVolume / 100f;
        config.musicVolume = musicVolume / 100f;
        config.fullscreen = isFullscreen;
        ConfigManager.saveConfig(config);
    }

    private void loadResources() {
        // Load popup panel texture
        if (!GameApp.hasTexture("popup_panel")) {
            GameApp.addTexture("popup_panel", "assets/ui/Popup_Panel.png");
        }
        
        // Load tab texture
        if (!GameApp.hasTexture("tab_1")) {
            GameApp.addTexture("tab_1", "assets/ui/Tab_1.png");
        }
        
        // Load custom font for settings screen title (large, like Credits)
        if (!GameApp.hasFont(SETTINGS_FONT)) {
            GameApp.addFont(SETTINGS_FONT, "fonts/upheavtt.ttf", 20);
        }
        
        // Load smaller font for tab labels
        if (!GameApp.hasFont(SETTINGS_FONT_SMALL)) {
            GameApp.addFont(SETTINGS_FONT_SMALL, "fonts/PixelOperatorMono-Bold.ttf", 8);
        }
        
        // Load toggle button textures
        if (!GameApp.hasTexture("green_toggle")) {
            GameApp.addTexture("green_toggle", "assets/ui/green_toggle.png");
        }
        if (!GameApp.hasTexture("red_toggle")) {
            GameApp.addTexture("red_toggle", "assets/ui/red_toggle.png");
        }
        
        // Load volume slider textures
        if (!GameApp.hasTexture("blue_segment")) {
            GameApp.addTexture("blue_segment", "assets/ui/blue_segment_slider.png");
        }
        if (!GameApp.hasTexture("black_segment")) {
            GameApp.addTexture("black_segment", "assets/ui/black_segment_slider.png");
        }
        if (!GameApp.hasTexture("black_left_slider")) {
            GameApp.addTexture("black_left_slider", "assets/ui/black_left_slider.png");
        }
        if (!GameApp.hasTexture("black_mid_slider")) {
            GameApp.addTexture("black_mid_slider", "assets/ui/black_mid_slider.png");
        }
        if (!GameApp.hasTexture("black_right_slider")) {
            GameApp.addTexture("black_right_slider", "assets/ui/black_right_slider.png");
        }
        
        // Load +/- button textures
        if (!GameApp.hasTexture("tiny_black_minus")) {
            GameApp.addTexture("tiny_black_minus", "assets/ui/tiny_black_minus.png");
        }
        if (!GameApp.hasTexture("tiny_black_plus")) {
            GameApp.addTexture("tiny_black_plus", "assets/ui/tiny_black_plus.png");
        }
        if (!GameApp.hasTexture("tiny_blue_minus")) {
            GameApp.addTexture("tiny_blue_minus", "assets/ui/tiny_blue_minus.png");
        }
        if (!GameApp.hasTexture("tiny_blue_plus")) {
            GameApp.addTexture("tiny_blue_plus", "assets/ui/tiny_blue_plus.png");
        }
        
        // Load individual icon textures
        if (!GameApp.hasTexture("icon_fullscreen")) {
            GameApp.addTexture("icon_fullscreen", "assets/ui/fullscreen.png");
        }
        if (!GameApp.hasTexture("icon_master_volume")) {
            GameApp.addTexture("icon_master_volume", "assets/ui/mastervolumne.png");
        }
        if (!GameApp.hasTexture("icon_music")) {
            GameApp.addTexture("icon_music", "assets/ui/music.png");
        }
        if (!GameApp.hasTexture("icon_sfx")) {
            GameApp.addTexture("icon_sfx", "assets/ui/soundeffect.png");
        }
        if (!GameApp.hasTexture("close_button")) {
            GameApp.addTexture("close_button", "assets/ui/close.png");
        }
        
        // Load background texture
        if (!GameApp.hasTexture("mainmenu_bg")) {
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        }
    }


    @Override
    public void hide() {
        // Save settings when leaving screen
        saveSettingsToConfig();
        
        // Don't stop music when leaving settings - keep it playing for menu
        // Music will continue in MainMenuScreen

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

        // Clear screen with dark background
        GameApp.clearScreen(BG_COLOR);
        
        // Draw background texture
        drawBackground();

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Calculate panel dimensions using grid system
        float panelWidth = screenWidth * PANEL_WIDTH_RATIO;
        float panelHeight = screenHeight * PANEL_HEIGHT_RATIO;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;
        
        // Draw popup panel first
        GameApp.startSpriteRendering();
        
        if (GameApp.hasTexture("popup_panel")) {
            // Draw popup panel background
            GameApp.drawTexture("popup_panel", panelX, panelY, panelWidth, panelHeight);
        } else {
            // Fallback: draw a dark rectangle if texture is missing
        GameApp.endSpriteRendering();
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(30, 30, 60, 255); // Dark blue-gray
            GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
            GameApp.endShapeRendering();
            GameApp.startSpriteRendering();
        }
        
        // Draw tabs using grid coordinates
        drawTabs(panelX, panelY, panelWidth, panelHeight);
        
        // Draw fullscreen toggle button
        drawFullscreenToggle(panelX, panelY, panelWidth, panelHeight);
        
        // Draw Master Volume slider
        drawMasterVolumeSlider(panelX, panelY, panelWidth, panelHeight);
        
        // Draw Music slider
        drawMusicSlider(panelX, panelY, panelWidth, panelHeight);
        
        // Draw Sound Effect slider
        drawSfxSlider(panelX, panelY, panelWidth, panelHeight);
        
        // Draw SETTINGS title at cells 15-16 (row 2, columns 5-6)
        drawTitle(panelX, panelY, panelWidth, panelHeight);
        
        GameApp.endSpriteRendering();
        
        // Cache panel dimensions for click handling
        cachedPanelX = panelX;
        cachedPanelY = panelY;
        cachedPanelWidth = panelWidth;
        cachedPanelHeight = panelHeight;
        
        // Update button press animation timers
        updateButtonAnimations(delta);
        
        // Handle F11 key to toggle fullscreen (works from any screen)
        if (GameApp.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F11)) {
            isFullscreen = !isFullscreen;
            applyFullscreenSetting();
            saveSettingsToConfig();
        }
        
        // Handle input (click on toggle and volume buttons)
        handleInput();
        
        // Grid disabled - uncomment to debug positioning
        // drawCoordinateGrid(panelX, panelY, panelWidth, panelHeight);
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
    
    // Handle mouse input for toggle button and volume controls
    private void handleInput() {
        if (GameApp.isButtonJustPressed(0)) { // Left mouse button
            // Get mouse position in world coordinates
            float mouseX = GameApp.getMousePositionInWindowX();
            float mouseY = GameApp.getMousePositionInWindowY();
        float windowWidth = GameApp.getWindowWidth();
        float windowHeight = GameApp.getWindowHeight();
            float screenWidth = GameApp.getWorldWidth();
            float screenHeight = GameApp.getWorldHeight();
        
        // Scale mouse coordinates from window to world
        float scaleX = screenWidth / windowWidth;
        float scaleY = screenHeight / windowHeight;
        float worldMouseX = mouseX * scaleX;
            float worldMouseY = (windowHeight - mouseY) * scaleY; // Flip Y
            
            // Check if click is on fullscreen toggle
            float cellWidth = cachedPanelWidth / 10f;
            float cellHeight = cachedPanelHeight / 10f;
            float tabHeight = cellHeight * 0.8f;
            
            // Toggle position: center of columns 7-8, row 4
            float toggleWidth = cellWidth;
            float toggleHeight = tabHeight;
            float toggleX = cachedPanelX + (cachedPanelWidth * TOGGLE_X_RATIO) - (toggleWidth / 2f) + 5f;
            float toggleY = cachedPanelY + (6 * cellHeight); // Row 4
            
            if (worldMouseX >= toggleX && worldMouseX <= toggleX + toggleWidth &&
                worldMouseY >= toggleY && worldMouseY <= toggleY + toggleHeight) {
                // Toggle fullscreen
                isFullscreen = !isFullscreen;
                applyFullscreenSetting();
                saveSettingsToConfig();
            }
            
            // Check if click is on Master Volume +/- buttons in cell 49
            float sliderHeight = tabHeight;
            float masterVolumeY = cachedPanelY + (5 * cellHeight); // Row 5
            
            // Buttons in cell 49 (column 9, row 5)
            float cell49X = cachedPanelX + (cellWidth * 8); // Column 9 starts at index 8
            float buttonSize = tabHeight * 0.5f; // Smaller buttons
            float buttonSpacing = 2f;
            
            // Calculate button positions (same as in draw)
            float totalButtonWidth = (buttonSize * 2) + buttonSpacing;
            float buttonsStartX = cell49X + (cellWidth - totalButtonWidth) / 2f;
            float buttonY = masterVolumeY + (sliderHeight - buttonSize) / 2f;
            
            // Minus button position
            float minusX = buttonsStartX;
            
            // Plus button position
            float plusX = minusX + buttonSize + buttonSpacing;
            
            // Check minus button click
            if (worldMouseX >= minusX && worldMouseX <= minusX + buttonSize &&
                worldMouseY >= buttonY && worldMouseY <= buttonY + buttonSize) {
                if (masterVolume > 0) {
                    // Decrease by 11 (100/9 ≈ 11, gives 10 levels: 0,11,22,...,99,100)
                    masterVolume = Math.max(0, masterVolume - 11);
                    masterMinusPressed = true;
                    masterMinusPressTimer = BUTTON_PRESS_DURATION;
                    applyMasterVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check plus button click
            if (worldMouseX >= plusX && worldMouseX <= plusX + buttonSize &&
                worldMouseY >= buttonY && worldMouseY <= buttonY + buttonSize) {
                if (masterVolume < 100) {
                    // Increase by 11
                    masterVolume = Math.min(100, masterVolume + 11);
                    masterPlusPressed = true;
                    masterPlusPressTimer = BUTTON_PRESS_DURATION;
                    applyMasterVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check if click is on Music +/- buttons (row 6, cell 59)
            float musicY = cachedPanelY + (4 * cellHeight); // Row 6
            float cell59X = cachedPanelX + (cellWidth * 8);
            float musicButtonsStartX = cell59X + (cellWidth - totalButtonWidth) / 2f;
            float musicButtonY = musicY + (sliderHeight - buttonSize) / 2f;
            
            float musicMinusX = musicButtonsStartX;
            float musicPlusX = musicMinusX + buttonSize + buttonSpacing;
            
            // Check music minus button
            if (worldMouseX >= musicMinusX && worldMouseX <= musicMinusX + buttonSize &&
                worldMouseY >= musicButtonY && worldMouseY <= musicButtonY + buttonSize) {
                if (musicVolume > 0) {
                    musicVolume = Math.max(0, musicVolume - 11);
                    musicMinusPressed = true;
                    musicMinusPressTimer = BUTTON_PRESS_DURATION;
                    applyMusicVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check music plus button
            if (worldMouseX >= musicPlusX && worldMouseX <= musicPlusX + buttonSize &&
                worldMouseY >= musicButtonY && worldMouseY <= musicButtonY + buttonSize) {
                if (musicVolume < 100) {
                    musicVolume = Math.min(100, musicVolume + 11);
                    musicPlusPressed = true;
                    musicPlusPressTimer = BUTTON_PRESS_DURATION;
                    applyMusicVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check if click is on Sound Effect +/- buttons (row 7, cell 69)
            float sfxY = cachedPanelY + (3 * cellHeight); // Row 7
            float cell69X = cachedPanelX + (cellWidth * 8);
            float sfxButtonsStartX = cell69X + (cellWidth - totalButtonWidth) / 2f;
            float sfxButtonY = sfxY + (sliderHeight - buttonSize) / 2f;
            
            float sfxMinusX = sfxButtonsStartX;
            float sfxPlusX = sfxMinusX + buttonSize + buttonSpacing;
            
            // Check sfx minus button
            if (worldMouseX >= sfxMinusX && worldMouseX <= sfxMinusX + buttonSize &&
                worldMouseY >= sfxButtonY && worldMouseY <= sfxButtonY + buttonSize) {
                if (sfxVolume > 0) {
                    sfxVolume = Math.max(0, sfxVolume - 11);
                    sfxMinusPressed = true;
                    sfxMinusPressTimer = BUTTON_PRESS_DURATION;
                    applySfxVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check sfx plus button
            if (worldMouseX >= sfxPlusX && worldMouseX <= sfxPlusX + buttonSize &&
                worldMouseY >= sfxButtonY && worldMouseY <= sfxButtonY + buttonSize) {
                if (sfxVolume < 100) {
                    sfxVolume = Math.min(100, sfxVolume + 11);
                    sfxPlusPressed = true;
                    sfxPlusPressTimer = BUTTON_PRESS_DURATION;
                    applySfxVolume();
                    saveSettingsToConfig();
                }
            }
            
            // Check if click is on close button (row 2, column 9, shifted left)
            float closeButtonSize = cellHeight * 0.8f;
            float closeButtonX = cachedPanelX + (9 * cellWidth) + (cellWidth - closeButtonSize) / 2f - 40f;
            float closeButtonY = cachedPanelY + (8.3f * cellHeight) + (cellHeight - closeButtonSize) / 2f;
            
            if (worldMouseX >= closeButtonX && worldMouseX <= closeButtonX + closeButtonSize &&
                worldMouseY >= closeButtonY && worldMouseY <= closeButtonY + closeButtonSize) {
                // Check which screen to return to
                if ("pause".equals(returnScreen)) {
                    // Return to pause menu in PlayScreen
                    // Set flags to preserve pause state
                    PlayScreen.setReturningFromSettings(true);
                    PlayScreen.setWasPausedBeforeSettings(true);
                    // Reset returnScreen for next time
                    returnScreen = "menu";
                    GameApp.switchScreen("play");
                } else {
                    // Switch back to main menu (default)
                    GameApp.switchScreen("menu");
                }
            }
        }
    }
    
    // Update button press animations
    private void updateButtonAnimations(float delta) {
        // Master volume buttons
        if (masterMinusPressed) {
            masterMinusPressTimer -= delta;
            if (masterMinusPressTimer <= 0) {
                masterMinusPressed = false;
            }
        }
        if (masterPlusPressed) {
            masterPlusPressTimer -= delta;
            if (masterPlusPressTimer <= 0) {
                masterPlusPressed = false;
            }
        }
        
        // Music buttons
        if (musicMinusPressed) {
            musicMinusPressTimer -= delta;
            if (musicMinusPressTimer <= 0) {
                musicMinusPressed = false;
            }
        }
        if (musicPlusPressed) {
            musicPlusPressTimer -= delta;
            if (musicPlusPressTimer <= 0) {
                musicPlusPressed = false;
            }
        }
        
        // Sound effect buttons
        if (sfxMinusPressed) {
            sfxMinusPressTimer -= delta;
            if (sfxMinusPressTimer <= 0) {
                sfxMinusPressed = false;
            }
        }
        if (sfxPlusPressed) {
            sfxPlusPressTimer -= delta;
            if (sfxPlusPressTimer <= 0) {
                sfxPlusPressed = false;
            }
        }
    }
    
    // Draw fullscreen toggle button at cells 37-38
    private void drawFullscreenToggle(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;
        float tabHeight = cellHeight * 0.8f;
        
        // Toggle position: center of columns 7-8, row 4
        // Column 7 starts at 60%, column 8 at 70%, center = 65%
        float toggleWidth = cellWidth;
        float toggleHeight = tabHeight;
        float toggleX = panelX + (panelWidth * TOGGLE_X_RATIO) - (toggleWidth / 2f) + 5f; // Move right by 15px
        float toggleY = panelY + (6 * cellHeight); // Row 4 (same as FULL SCREEN tab)
        
        // Draw appropriate toggle texture
        String textureName = isFullscreen ? "green_toggle" : "red_toggle";
        if (GameApp.hasTexture(textureName)) {
            GameApp.drawTexture(textureName, toggleX, toggleY, toggleWidth, toggleHeight);
        }
    }
    
    // Apply fullscreen setting to the game window
    private void applyFullscreenSetting() {
        if (isFullscreen) {
            // Set to fullscreen
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            // Set to windowed mode (1280x720)
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
    
    // Apply master volume to sound manager immediately
    private void applyMasterVolume() {
            if (soundManager != null) {
            soundManager.setMasterVolume(masterVolume / 100f);
        }
    }
    
    // Apply music volume to sound manager immediately
    private void applyMusicVolume() {
                if (soundManager != null) {
            soundManager.setMusicVolume(musicVolume / 100f);
        }
    }
    
    // Apply sfx volume to sound manager immediately
    private void applySfxVolume() {
                if (soundManager != null) {
            soundManager.setSFXVolume(sfxVolume / 100f);
        }
    }
    
    // Draw Master Volume slider (row 5, same as MASTER VOLUME tab)
    private void drawMasterVolumeSlider(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;
        float tabHeight = cellHeight * 0.8f;
        
        // Slider position: row 5 (same as MASTER VOLUME tab), start at column 7 + 5px offset
        float sliderStartX = panelX + (panelWidth * SLIDER_X_RATIO) + 5f;
        float sliderY = panelY + (5 * cellHeight); // Row 5
        
        // Slider is 2 cells wide
        float sliderWidth = cellWidth * 2f;
        float sliderHeight = tabHeight;
        
        // Each segment width (9 segments in slider)
        float segmentWidth = sliderWidth / VOLUME_LEVELS;
        
        // Calculate filled segments from masterVolume (0-100)
        // Volume levels: 100, 89, 78, 67, 56, 45, 34, 23, 12, 1, 0
        // Segments:      9,   8,  7,  6,  5,  4,  3,  2,  1,  1, 0
        int filledSegments;
        if (masterVolume <= 0) {
            filledSegments = 0;
        } else if (masterVolume >= 100) {
            filledSegments = VOLUME_LEVELS; // 9 segments at 100%
        } else {
            // Simple division: 89/11=8, 78/11=7, ..., 12/11=1
            filledSegments = masterVolume / 11;
            // Ensure at least 1 segment when volume > 0
            filledSegments = Math.max(1, filledSegments);
        }
        
        // Draw slider based on fill level
        if (filledSegments == 0) {
            // All empty - draw full black_segment slider (shift +1 pixel right to align)
            if (GameApp.hasTexture("black_segment")) {
                GameApp.drawTexture("black_segment", sliderStartX, sliderY, sliderWidth, sliderHeight);
            }
        } else {
            // Draw full blue slider as background
            if (GameApp.hasTexture("blue_segment")) {
                GameApp.drawTexture("blue_segment", sliderStartX, sliderY, sliderWidth, sliderHeight);
            }
            
            // Draw black segments over unfilled parts
            for (int i = filledSegments; i < VOLUME_LEVELS; i++) {
                // Calculate segment position, shift left 1px and add width to fill gaps
                float segmentX = sliderStartX + (i * segmentWidth) - 1f;
                float renderWidth = segmentWidth + 1f; // Add 1 pixel to width to fill gaps
                
                // Choose correct black texture based on position
                String blackTexture;
                if (i == VOLUME_LEVELS - 1) {
                    blackTexture = "black_right_slider";
                } else {
                    blackTexture = "black_mid_slider";
                }
                
                if (GameApp.hasTexture(blackTexture)) {
                    GameApp.drawTexture(blackTexture, segmentX, sliderY, renderWidth, sliderHeight);
                }
            }
        }
        
        // Draw +/- buttons in cell 49 (column 9, row 5)
        float cell49X = panelX + (cellWidth * 8);
        float buttonSize = tabHeight * 0.5f;
        float buttonSpacing = 2f;
        
        // Center both buttons in cell 49
        float totalButtonWidth = (buttonSize * 2) + buttonSpacing;
        float buttonsStartX = cell49X + (cellWidth - totalButtonWidth) / 2f;
        float buttonY = sliderY + (sliderHeight - buttonSize) / 2f;
        
        // Minus button
        float minusX = buttonsStartX;
        String minusTexture = masterMinusPressed ? "tiny_blue_minus" : "tiny_black_minus";
        if (GameApp.hasTexture(minusTexture)) {
            GameApp.drawTexture(minusTexture, minusX, buttonY, buttonSize, buttonSize);
        }
        
        // Plus button
        float plusX = minusX + buttonSize + buttonSpacing;
        String plusTexture = masterPlusPressed ? "tiny_blue_plus" : "tiny_black_plus";
        if (GameApp.hasTexture(plusTexture)) {
            GameApp.drawTexture(plusTexture, plusX, buttonY, buttonSize, buttonSize);
        }
    }
    
    // Draw Music slider (row 6, same as MUSIC tab)
    private void drawMusicSlider(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;
        float tabHeight = cellHeight * 0.8f;
        
        // Slider position: row 6 (same as MUSIC tab)
        float sliderStartX = panelX + (panelWidth * SLIDER_X_RATIO) + 5f;
        float sliderY = panelY + (4 * cellHeight); // Row 6
        
        float sliderWidth = cellWidth * 2f;
        float sliderHeight = tabHeight;
        float segmentWidth = sliderWidth / VOLUME_LEVELS;
        
        // Calculate filled segments from musicVolume (0-100)
        int filledSegments;
        if (musicVolume <= 0) {
            filledSegments = 0;
        } else if (musicVolume >= 100) {
            filledSegments = VOLUME_LEVELS;
        } else {
            filledSegments = musicVolume / 11;
            filledSegments = Math.max(1, filledSegments);
        }
        
        // Draw slider
        if (filledSegments == 0) {
            if (GameApp.hasTexture("black_segment")) {
                GameApp.drawTexture("black_segment", sliderStartX + 1f, sliderY, sliderWidth, sliderHeight);
            }
        } else {
            if (GameApp.hasTexture("blue_segment")) {
                GameApp.drawTexture("blue_segment", sliderStartX, sliderY, sliderWidth, sliderHeight);
            }
            
            for (int i = filledSegments; i < VOLUME_LEVELS; i++) {
                float segmentX = sliderStartX + (i * segmentWidth) - 1f;
                float renderWidth = segmentWidth + 1f;
                
                String blackTexture;
                if (i == VOLUME_LEVELS - 1) {
                    blackTexture = "black_right_slider";
                } else {
                    blackTexture = "black_mid_slider";
                }
                
                if (GameApp.hasTexture(blackTexture)) {
                    GameApp.drawTexture(blackTexture, segmentX, sliderY, renderWidth, sliderHeight);
                }
            }
        }
        
        // Draw +/- buttons
        float cell59X = panelX + (cellWidth * 8);
        float buttonSize = tabHeight * 0.5f;
        float buttonSpacing = 2f;
        float totalButtonWidth = (buttonSize * 2) + buttonSpacing;
        float buttonsStartX = cell59X + (cellWidth - totalButtonWidth) / 2f;
        float buttonY = sliderY + (sliderHeight - buttonSize) / 2f;
        
        float minusX = buttonsStartX;
        String minusTexture = musicMinusPressed ? "tiny_blue_minus" : "tiny_black_minus";
        if (GameApp.hasTexture(minusTexture)) {
            GameApp.drawTexture(minusTexture, minusX, buttonY, buttonSize, buttonSize);
        }
        
        float plusX = minusX + buttonSize + buttonSpacing;
        String plusTexture = musicPlusPressed ? "tiny_blue_plus" : "tiny_black_plus";
        if (GameApp.hasTexture(plusTexture)) {
            GameApp.drawTexture(plusTexture, plusX, buttonY, buttonSize, buttonSize);
        }
    }
    
    // Draw Sound Effect slider (row 7, same as SOUND EFFECT tab)
    private void drawSfxSlider(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;
        float tabHeight = cellHeight * 0.8f;
        
        // Slider position: row 7 (same as SOUND EFFECT tab)
        float sliderStartX = panelX + (panelWidth * SLIDER_X_RATIO) + 5f;
        float sliderY = panelY + (3 * cellHeight); // Row 7
        
        float sliderWidth = cellWidth * 2f;
        float sliderHeight = tabHeight;
        float segmentWidth = sliderWidth / VOLUME_LEVELS;
        
        // Calculate filled segments from sfxVolume (0-100)
        int filledSegments;
        if (sfxVolume <= 0) {
            filledSegments = 0;
        } else if (sfxVolume >= 100) {
            filledSegments = VOLUME_LEVELS;
        } else {
            filledSegments = sfxVolume / 11;
            filledSegments = Math.max(1, filledSegments);
        }
        
        // Draw slider
        if (filledSegments == 0) {
            if (GameApp.hasTexture("black_segment")) {
                GameApp.drawTexture("black_segment", sliderStartX + 1f, sliderY, sliderWidth, sliderHeight);
            }
        } else {
            if (GameApp.hasTexture("blue_segment")) {
                GameApp.drawTexture("blue_segment", sliderStartX, sliderY, sliderWidth, sliderHeight);
            }
            
            for (int i = filledSegments; i < VOLUME_LEVELS; i++) {
                float segmentX = sliderStartX + (i * segmentWidth) - 1f;
                float renderWidth = segmentWidth + 1f;
                
                String blackTexture;
                if (i == VOLUME_LEVELS - 1) {
                    blackTexture = "black_right_slider";
                } else {
                    blackTexture = "black_mid_slider";
                }
                
                if (GameApp.hasTexture(blackTexture)) {
                    GameApp.drawTexture(blackTexture, segmentX, sliderY, renderWidth, sliderHeight);
                }
            }
        }
        
        // Draw +/- buttons
        float cell69X = panelX + (cellWidth * 8);
        float buttonSize = tabHeight * 0.5f;
        float buttonSpacing = 2f;
        float totalButtonWidth = (buttonSize * 2) + buttonSpacing;
        float buttonsStartX = cell69X + (cellWidth - totalButtonWidth) / 2f;
        float buttonY = sliderY + (sliderHeight - buttonSize) / 2f;
        
        float minusX = buttonsStartX;
        String minusTexture = sfxMinusPressed ? "tiny_blue_minus" : "tiny_black_minus";
        if (GameApp.hasTexture(minusTexture)) {
            GameApp.drawTexture(minusTexture, minusX, buttonY, buttonSize, buttonSize);
        }
        
        float plusX = minusX + buttonSize + buttonSpacing;
        String plusTexture = sfxPlusPressed ? "tiny_blue_plus" : "tiny_black_plus";
        if (GameApp.hasTexture(plusTexture)) {
            GameApp.drawTexture(plusTexture, plusX, buttonY, buttonSize, buttonSize);
        }
    }

    // Draw tabs using grid coordinate system
    // GRID COORDINATE REFERENCE (10x10 grid, cells numbered 1-100):
    // Row 1 (TOP):    cells 1-10    -> Y = panelY + 9*cellHeight (top of panel)
    // Row 2:          cells 11-20   -> Y = panelY + 8*cellHeight
    // Row 3:          cells 21-30   -> Y = panelY + 7*cellHeight
    // Row 4:          cells 31-40   -> Y = panelY + 6*cellHeight (Tab 1: 32-36)
    // Row 5:          cells 41-50   -> Y = panelY + 5*cellHeight (Tab 2: 42-46)
    // Row 6:          cells 51-60   -> Y = panelY + 4*cellHeight (Tab 3: 52-56)
    // Row 7:          cells 61-70   -> Y = panelY + 3*cellHeight (Tab 4: 62-66)
    // Row 8:          cells 71-80   -> Y = panelY + 2*cellHeight (Tab 5: 72-76)
    // Row 9:          cells 81-90   -> Y = panelY + 1*cellHeight (Tab 6: 82-86)
    // Row 10 (BOTTOM): cells 91-100 -> Y = panelY + 0*cellHeight (bottom of panel)
    private void drawTabs(float panelX, float panelY, float panelWidth, float panelHeight) {
        if (!GameApp.hasTexture("tab_1")) {
            return;
        }
        
        // Calculate grid cell dimensions
        float cellWidth = panelWidth / 10f;  // 1 cell width
        float cellHeight = panelHeight / 10f; // 1 cell height
        
        // Tab dimensions: horizontal, 5 cells wide, slightly less than 1 cell tall (for spacing)
        float tabWidth = cellWidth * 5f;  // 5 cells wide
        float tabHeight = cellHeight * 0.8f; // 80% of cell height to create gap between tabs
        
        // Calculate X position (all tabs start at column 2 = 10% from left)
        float tabX = panelX + (panelWidth * TAB_X_OFFSET_RATIO);
        
        // Draw all tabs with labels
        for (int i = 0; i < NUM_TABS; i++) {
            int startCell = TAB_START_CELLS[i];
            
            // Calculate row from cell number (1-indexed)
            // Cell 32 -> row 4, Cell 42 -> row 5, etc.
            int row = ((startCell - 1) / 10) + 1;
            
            // Calculate Y position from row
            // LibGDX: Y=0 at bottom, Y increases upward
            // Row 4 (cells 31-40) -> Y = panelY + (10 - 4) * cellHeight = panelY + 6 * cellHeight
            float tabY = panelY + ((10 - row) * cellHeight);
            
            // Draw tab texture
            GameApp.drawTexture("tab_1", tabX, tabY, tabWidth, tabHeight);
            
            // Draw tab label with black outline (left-aligned, smaller font)
            String label = TAB_LABELS[i];
            float textX = tabX + 45f; // Left margin
            // Adjust Y to center text vertically (drawText uses baseline, so offset down by ~3px)
            float textY = tabY + (tabHeight / 2f) - 8f;
            
            // Draw icons for each tab
            float iconSize = tabHeight * 0.8f;
            // Fullscreen icon needs different X offset
            float iconX = (i == 0) ? textX - 27f : textX - 30f;
            float iconY = tabY + (tabHeight - iconSize) / 2f; // Center icon vertically
            
            String iconTexture = null;
            if (i == 0) {
                iconTexture = "icon_fullscreen";
            } else if (i == 1) {
                iconTexture = "icon_master_volume";
            } else if (i == 2) {
                iconTexture = "icon_music";
            } else if (i == 3) {
                iconTexture = "icon_sfx";
            }
            
            if (iconTexture != null && GameApp.hasTexture(iconTexture)) {
                GameApp.drawTexture(iconTexture, iconX, iconY, iconSize, iconSize);
            }
            
            drawTextWithOutlineLeft(label, textX, textY);
        }
    }
    
    // Draw SETTINGS title at cells 15-16 (row 2, columns 5-6)
    private void drawTitle(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;
        
        // Cells 15-16 are in row 2, columns 5-6
        // Row 2: Y = panelY + 8 * cellHeight
        // Columns 5-6: X = panelX + 4.5 * cellWidth (center between columns 5 and 6)
        // Move up a bit: add 0.3 * cellHeight
        float titleX = panelX + (4.5f * cellWidth) + (cellWidth / 2f);
        float titleY = panelY + (8.3f * cellHeight) + (cellHeight / 2f);
        
        drawTextWithOutline("SETTINGS", titleX, titleY);
        
        // Draw close button on the same row, right side (column 9, shifted left)
        float closeButtonSize = cellHeight * 0.8f;
        float closeButtonX = panelX + (9 * cellWidth) + (cellWidth - closeButtonSize) / 2f - 40f;
        float closeButtonY = panelY + (8.3f * cellHeight) + (cellHeight - closeButtonSize) / 2f;
        
        if (GameApp.hasTexture("close_button")) {
            GameApp.drawTexture("close_button", closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);
        }
    }
    
    // Draw text with black outline (white text, black border) - centered
    private void drawTextWithOutline(String text, float x, float y) {
        // Draw black outline (8 directions)
        float offset = 1f;
        GameApp.drawTextCentered(SETTINGS_FONT, text, x - offset, y, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x + offset, y, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x, y - offset, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x, y + offset, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x - offset, y - offset, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x + offset, y - offset, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x - offset, y + offset, "black");
        GameApp.drawTextCentered(SETTINGS_FONT, text, x + offset, y + offset, "black");
        
        // Draw white text on top
        GameApp.drawTextCentered(SETTINGS_FONT, text, x, y, "white");
    }
    
    // Draw text with black outline (white text, black border) - left-aligned, smaller font
    private void drawTextWithOutlineLeft(String text, float x, float y) {
        // Draw black outline (8 directions)
        float offset = 1f;
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x - offset, y, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x + offset, y, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x, y - offset, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x, y + offset, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x - offset, y - offset, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x + offset, y - offset, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x - offset, y + offset, "black");
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x + offset, y + offset, "black");
        
        // Draw white text on top
        GameApp.drawText(SETTINGS_FONT_SMALL, text, x, y, "white");
    }
    
    // Draw coordinate grid for debugging (optional - can be enabled/disabled)
    private void drawCoordinateGrid(float panelX, float panelY, float panelWidth, float panelHeight) {
        // Draw all grid lines using shape rendering (thicker lines for visibility)
        GameApp.startShapeRenderingFilled();
        
        int numCols = 10; // Number of columns
        int numRows = 10; // Number of rows
        
        // Draw horizontal grid lines (every 10% of panel height) - 50% opacity
        for (int i = 0; i <= numRows; i++) {
            float y = panelY + (panelHeight * (i / (float)numRows));
            GameApp.setColor(255, 0, 0, 100); // Red, 50% opacity
            GameApp.drawRect(panelX, y - 1, panelWidth, 2);
        }
        
        // Draw vertical grid lines (every 10% of panel width) - 50% opacity
        for (int i = 0; i <= numCols; i++) {
            float x = panelX + (panelWidth * (i / (float)numCols));
            GameApp.setColor(0, 255, 0, 100); // Green, 50% opacity
            GameApp.drawRect(x - 1, panelY, 2, panelHeight);
        }
        
        GameApp.endShapeRendering();
        
        // Draw cell numbers (1, 2, 3...) in each grid cell
        GameApp.startSpriteRendering();
        GameApp.setColor(255, 255, 255, 255);
        
        float cellWidth = panelWidth / numCols;
        float cellHeight = panelHeight / numRows;
        int cellNumber = 1; // Start numbering from 1
        
        // Number cells from top-left to bottom-right (row by row)
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                // Calculate center of each cell
                float cellX = panelX + (col * cellWidth) + (cellWidth / 2f);
                float cellY = panelY + ((numRows - row - 1) * cellHeight) + (cellHeight / 2f);
                
                // Draw cell number centered in the cell
                String label = String.valueOf(cellNumber);
                GameApp.drawTextCentered("default", label, cellX, cellY, "white");
                
                cellNumber++;
            }
        }
        
        GameApp.endSpriteRendering();
    }

    // Getters for volume values (can be used by other systems)
    public int getMasterVolume() {
        return masterVolume;
    }

    public int getSfxVolume() {
        return sfxVolume;
    }

    public int getMusicVolume() {
        return musicVolume;
    }

    public float getMasterVolumeFloat() {
        return masterVolume / 100f;
    }

    public float getSfxVolumeFloat() {
        return sfxVolume / 100f;
    }

    public float getMusicVolumeFloat() {
        return musicVolume / 100f;
    }
}

