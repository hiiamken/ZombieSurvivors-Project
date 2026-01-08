package nl.saxion.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * Credits screen - displays game credits with a popup panel.
 * Shares the same styling and behavior as SettingsScreen.
 */
public class CreditsScreen extends ScalableGameScreen {

    // Sound manager for background music
    private SoundManager soundManager;

    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringCloseButton = false;

    // Colors
    private static final String BG_COLOR = "black";

    // Panel configuration (SAME as SettingsScreen)
    private static final float PANEL_WIDTH_RATIO = 0.75f;
    private static final float PANEL_HEIGHT_RATIO = 0.85f;

    // Font names
    private static final String CREDITS_FONT = "credits_font";           // Title font (large)
    private static final String CREDITS_FONT_HEADING = "credits_font_heading"; // Section headings (medium-bold)
    private static final String CREDITS_FONT_SMALL = "credits_font_small";     // Names (small)

    // Cached panel dimensions for click handling
    private float cachedPanelX, cachedPanelY, cachedPanelWidth, cachedPanelHeight;

    // Settings (loaded from config)
    private int masterVolume = 100;
    private int sfxVolume = 100;
    private int musicVolume = 70;
    private boolean isFullscreen = true;

    public CreditsScreen() {
        super(640, 360); // 16:9 aspect ratio - SAME as SettingsScreen
    }

    @Override
    public void show() {
        // Load cursors
        loadCursors();

        // Initialize sound manager for background music
        soundManager = new SoundManager();
        soundManager.loadAllSounds();

        // Load resources
        loadResources();
        
        // Load settings from config and apply them
        loadSettingsFromConfig();
        
        // Play background music ONLY if music is enabled in config
        if (soundManager != null && musicVolume > 0) {
            soundManager.playMusic(true);
        }
    }

    /**
     * Load settings from config file and apply them.
     */
    private void loadSettingsFromConfig() {
        GameConfig config = ConfigManager.loadConfig();
        masterVolume = (int) (config.masterVolume * 100);
        sfxVolume = (int) (config.sfxVolume * 100);
        musicVolume = (int) (config.musicVolume * 100);
        isFullscreen = config.fullscreen;

        // Apply fullscreen setting
        applyFullscreenSetting();

        // Apply volume to sound manager
        if (soundManager != null) {
            soundManager.setMasterVolume(masterVolume / 100f);
            soundManager.setMusicVolume(musicVolume / 100f);
            soundManager.setSFXVolume(sfxVolume / 100f);
        }
    }

    /**
     * Apply fullscreen setting to the game window.
     */
    private void applyFullscreenSetting() {
        if (isFullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }

    /**
     * Save settings to config file.
     */
    private void saveSettingsToConfig() {
        GameConfig config = ConfigManager.loadConfig();
        config.masterVolume = masterVolume / 100f;
        config.sfxVolume = sfxVolume / 100f;
        config.musicVolume = musicVolume / 100f;
        config.fullscreen = isFullscreen;
        ConfigManager.saveConfig(config);
    }

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

            // Set default cursor
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            } else {
                GameApp.showCursor();
            }
        } catch (Exception e) {
            GameApp.log("Failed to load cursors: " + e.getMessage());
            GameApp.showCursor();
        }
    }

    private void loadResources() {
        // Load popup panel texture
        if (!GameApp.hasTexture("popup_panel")) {
            GameApp.addTexture("popup_panel", "assets/ui/Popup_Panel.png");
        }

        // Load close button texture
        if (!GameApp.hasTexture("close_button")) {
            GameApp.addTexture("close_button", "assets/ui/close.png");
        }

        // Load fonts - clean and professional sizes
        if (!GameApp.hasFont(CREDITS_FONT)) {
            GameApp.addFont(CREDITS_FONT, "fonts/upheavtt.ttf", 24); // Title only
        }
        if (!GameApp.hasFont(CREDITS_FONT_HEADING)) {
            GameApp.addFont(CREDITS_FONT_HEADING, "fonts/upheavtt.ttf", 11); // Section headings
        }
        if (!GameApp.hasFont(CREDITS_FONT_SMALL)) {
            GameApp.addFont(CREDITS_FONT_SMALL, "fonts/upheavtt.ttf", 9); // Names
        }

        // Professional color scheme - subtle and clean
        if (!GameApp.hasColor("credits_heading")) {
            GameApp.addColor("credits_heading", 180, 180, 180); // Light gray for headings
        }
        if (!GameApp.hasColor("credits_name")) {
            GameApp.addColor("credits_name", 255, 255, 255); // White for names
        }
    }

    @Override
    public void hide() {
        // Stop background music
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

    @Override
    public void render(float delta) {
        // IMPORTANT: Call super.render() for proper scaling (same as SettingsScreen)
        super.render(delta);

        // Handle F11 key to toggle fullscreen (same as SettingsScreen)
        if (GameApp.isKeyJustPressed(Input.Keys.F11)) {
            isFullscreen = !isFullscreen;
            applyFullscreenSetting();
            saveSettingsToConfig();
        }

        // Handle input
        handleInput();

        // Clear screen with black
        GameApp.clearScreen(BG_COLOR);

        // Draw panel
        drawPanel();
    }

    private void handleInput() {
        // Get mouse position and convert to world coordinates
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();
        float windowWidth = GameApp.getWindowWidth();
        float windowHeight = GameApp.getWindowHeight();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        float scaleX = screenWidth / windowWidth;
        float scaleY = screenHeight / windowHeight;
        float worldMouseX = mouseX * scaleX;
        float worldMouseY = (windowHeight - mouseY) * scaleY; // Flip Y

        // Check if hovering over close button
        boolean hoveringClose = isOverCloseButton(worldMouseX, worldMouseY);

        // Update cursor based on hover state
        if (cursorPointer != null && cursorHover != null) {
            if (hoveringClose) {
                if (!isHoveringCloseButton) {
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringCloseButton = true;
                }
            } else {
                if (isHoveringCloseButton) {
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringCloseButton = false;
                }
            }
        }

        // Check for click on close button
        if (GameApp.isButtonJustPressed(0)) {
            if (hoveringClose) {
                // Play click sound
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 2.5f);
                }
                // Return to main menu
                GameApp.switchScreen("menu");
            }
        }
    }

    private boolean isOverCloseButton(float worldMouseX, float worldMouseY) {
        // Calculate close button bounds (same logic as drawTitle)
        float cellHeight = cachedPanelHeight / 10f;
        float cellWidth = cachedPanelWidth / 10f;

        float closeButtonSize = cellHeight * 0.8f;
        float closeButtonX = cachedPanelX + (9 * cellWidth) + (cellWidth - closeButtonSize) / 2f - 40f;
        float closeButtonY = cachedPanelY + (8.3f * cellHeight) + (cellHeight - closeButtonSize) / 2f;

        return worldMouseX >= closeButtonX && worldMouseX <= closeButtonX + closeButtonSize &&
               worldMouseY >= closeButtonY && worldMouseY <= closeButtonY + closeButtonSize;
    }

    private void drawPanel() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Calculate panel dimensions (SAME as SettingsScreen: 75% width, 85% height, centered)
        float panelWidth = screenWidth * PANEL_WIDTH_RATIO;
        float panelHeight = screenHeight * PANEL_HEIGHT_RATIO;
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        // Cache panel dimensions
        cachedPanelX = panelX;
        cachedPanelY = panelY;
        cachedPanelWidth = panelWidth;
        cachedPanelHeight = panelHeight;

        // Draw panel background
        GameApp.startSpriteRendering();
        if (GameApp.hasTexture("popup_panel")) {
            GameApp.drawTexture("popup_panel", panelX, panelY, panelWidth, panelHeight);
        }

        // Draw title and close button
        drawTitle(panelX, panelY, panelWidth, panelHeight);

        // Draw credits content
        drawCreditsContent(panelX, panelY, panelWidth, panelHeight);

        GameApp.endSpriteRendering();
    }

    private void drawTitle(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellWidth = panelWidth / 10f;
        float cellHeight = panelHeight / 10f;

        // Title position (SAME as SettingsScreen)
        float titleX = panelX + (4.5f * cellWidth) + (cellWidth / 2f);
        float titleY = panelY + (8.3f * cellHeight) + (cellHeight / 2f);

        drawTextWithOutline("CREDITS", titleX, titleY);

        // Draw close button (SAME position as SettingsScreen)
        float closeButtonSize = cellHeight * 0.8f;
        float closeButtonX = panelX + (9 * cellWidth) + (cellWidth - closeButtonSize) / 2f - 40f;
        float closeButtonY = panelY + (8.3f * cellHeight) + (cellHeight - closeButtonSize) / 2f;

        if (GameApp.hasTexture("close_button")) {
            GameApp.drawTexture("close_button", closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);
        }
    }

    private void drawCreditsContent(float panelX, float panelY, float panelWidth, float panelHeight) {
        float cellHeight = panelHeight / 10f;

        // Center X for text
        float centerX = panelX + panelWidth / 2f;

        // Calculate content area (between title and bottom of panel)
        float contentTop = panelY + (6.5f * cellHeight);  // Below title, moved down 30f
        float contentBottom = panelY + (1.5f * cellHeight); // Above panel bottom
        float contentHeight = contentTop - contentBottom;

        // Total lines: 8 (DEVELOPED BY, TEAM, 4 members, TEACHER GUIDE, CRAIG)
        float lineSpacing = contentHeight / 9f; // Divide evenly

        float currentY = contentTop;

        // --- DEVELOPED BY ---
        drawTextWithColor("DEVELOPED BY", centerX, currentY, CREDITS_FONT_HEADING, "credits_heading");
        currentY -= lineSpacing;

        // Team name
        drawTextWithColor("TEAM 72 - SP CLASS", centerX, currentY, CREDITS_FONT_SMALL, "credits_name");
        currentY -= lineSpacing * 1.2f;

        // Team members
        String[] members = {"THUONG TRAN", "DANIEL LEHTER", "ARNOLD AYIKU", "MEHMET YILDIRIM"};
        for (String member : members) {
            drawTextWithColor(member, centerX, currentY, CREDITS_FONT_SMALL, "credits_name");
            currentY -= lineSpacing * 0.9f;
        }

        currentY -= lineSpacing * 0.3f;

        // --- TEACHER GUIDE ---
        drawTextWithColor("TEACHER GUIDE", centerX, currentY, CREDITS_FONT_HEADING, "credits_heading");
        currentY -= lineSpacing;

        // Teacher name
        drawTextWithColor("CRAIG BRADLEY", centerX, currentY, CREDITS_FONT_SMALL, "credits_name");
    }

    // Draw text with specific font and color (with black outline)
    private void drawTextWithColor(String text, float x, float y, String fontName, String colorName) {
        float offset = 1f;
        // Draw black outline (8 directions)
        GameApp.drawTextCentered(fontName, text, x - offset, y, "black");
        GameApp.drawTextCentered(fontName, text, x + offset, y, "black");
        GameApp.drawTextCentered(fontName, text, x, y - offset, "black");
        GameApp.drawTextCentered(fontName, text, x, y + offset, "black");
        GameApp.drawTextCentered(fontName, text, x - offset, y - offset, "black");
        GameApp.drawTextCentered(fontName, text, x + offset, y - offset, "black");
        GameApp.drawTextCentered(fontName, text, x - offset, y + offset, "black");
        GameApp.drawTextCentered(fontName, text, x + offset, y + offset, "black");
        // Draw colored text on top
        GameApp.drawTextCentered(fontName, text, x, y, colorName);
    }

    // Draw text with black outline - centered (large font)
    private void drawTextWithOutline(String text, float x, float y) {
        float offset = 1f;
        // Draw black outline
        GameApp.drawTextCentered(CREDITS_FONT, text, x - offset, y, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x + offset, y, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x, y + offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x - offset, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x + offset, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x - offset, y + offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT, text, x + offset, y + offset, "black");
        // Draw white text
        GameApp.drawTextCentered(CREDITS_FONT, text, x, y, "white");
    }

    // Draw text with black outline - centered (small font)
    private void drawTextWithOutlineSmall(String text, float x, float y) {
        float offset = 1f;
        // Draw black outline
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x - offset, y, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x + offset, y, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x, y + offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x - offset, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x + offset, y - offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x - offset, y + offset, "black");
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x + offset, y + offset, "black");
        // Draw white text
        GameApp.drawTextCentered(CREDITS_FONT_SMALL, text, x, y, "white");
    }
}
