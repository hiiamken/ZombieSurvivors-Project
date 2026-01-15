package nl.saxion.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Professional Credits Screen with scrolling animation.
 * Displays game credits like a real game production.
 */
public class CreditsScreen extends ScalableGameScreen {

    // Sound manager
    private SoundManager soundManager;
    
    // Track where we came from (to return to correct screen)
    private static String previousScreen = "menu";
    
    /**
     * Set the screen to return to when back is pressed
     */
    public static void setPreviousScreen(String screen) {
        previousScreen = screen;
    }

    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringButton = false;

    // Button
    private List<Button> buttons;
    private float buttonWidth;
    private float buttonHeight;
    
    // Button press animation
    private float pressDelay = 0.3f;
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;

    // Scrolling animation
    private float scrollY = 0f;
    private float scrollSpeed = 45f; // pixels per second
    private boolean autoScroll = true;
    private float totalCreditsHeight = 0f;
    
    // Animation timer
    private float animTimer = 0f;

    // Settings
    private int masterVolume = 100;
    private int sfxVolume = 100;
    private int musicVolume = 70;
    private boolean isFullscreen = true;

    // Credits content structure
    private static class CreditSection {
        String title;
        String[] names;
        
        CreditSection(String title, String[] names) {
            this.title = title;
            this.names = names;
        }
    }
    
    private List<CreditSection> creditSections;

    public CreditsScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        loadCursors();

        soundManager = new SoundManager();
        soundManager.loadAllSounds();

        loadResources();
        loadSettingsFromConfig();
        createButtons();
        initCreditsContent();
        
        // Reset scroll position - start at top for film reel effect
        scrollY = 0f; // Start from top
        autoScroll = true;
        animTimer = 0f; // Reset animation timer

        if (soundManager != null && musicVolume > 0) {
            soundManager.playMusic(true);
        }
    }

    private void loadSettingsFromConfig() {
        GameConfig config = ConfigManager.loadConfig();
        masterVolume = (int) (config.masterVolume * 100);
        sfxVolume = (int) (config.sfxVolume * 100);
        musicVolume = (int) (config.musicVolume * 100);
        isFullscreen = config.fullscreen;

        if (isFullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        if (soundManager != null) {
            soundManager.setMasterVolume(masterVolume / 100f);
            soundManager.setMusicVolume(musicVolume / 100f);
            soundManager.setSFXVolume(sfxVolume / 100f);
        }
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
            GameApp.log("Failed to load cursors: " + e.getMessage());
            GameApp.showCursor();
        }
    }

    private void loadResources() {
        // Background
        if (!GameApp.hasTexture("mainmenu_bg")) {
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        }
        
        // Buttons
        if (!GameApp.hasTexture("red_long")) {
            GameApp.addTexture("red_long", "assets/ui/red_long.png");
        }
        if (!GameApp.hasTexture("red_pressed_long")) {
            GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        }
        
        // Saxion logo
        if (!GameApp.hasTexture("saxion_logo")) {
            GameApp.addTexture("saxion_logo", "assets/ui/saxion.png");
        }

        // Fonts - Professional hierarchy
        if (!GameApp.hasFont("creditsGameTitle")) {
            GameApp.addStyledFont("creditsGameTitle", "fonts/upheavtt.ttf", 56,
                    "yellow-400", 0f, "black", 4, 4, "gray-700", true);
        }
        if (!GameApp.hasFont("creditsSectionTitle")) {
            GameApp.addStyledFont("creditsSectionTitle", "fonts/upheavtt.ttf", 28,
                    "yellow-300", 0f, "black", 2, 2, "gray-700", true);
        }
        if (!GameApp.hasFont("creditsRole")) {
            GameApp.addStyledFont("creditsRole", "fonts/PixelOperatorMono-Bold.ttf", 16,
                    "gray-400", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("creditsName")) {
            GameApp.addStyledFont("creditsName", "fonts/PressStart2P-Regular.ttf", 18,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("creditsSmall")) {
            GameApp.addStyledFont("creditsSmall", "fonts/PixelOperatorMono-Bold.ttf", 18,
                    "gray-400", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("creditsSpecial")) {
            GameApp.addStyledFont("creditsSpecial", "fonts/upheavtt.ttf", 22,
                    "cyan-400", 0f, "black", 2, 2, "gray-700", true);
        }
        if (!GameApp.hasFont("buttonFont")) {
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 40,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }

        // Colors
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        if (!GameApp.hasColor("credits_gold")) {
            GameApp.addColor("credits_gold", 255, 215, 0);
        }
    }

    private void createButtons() {
        buttons = new ArrayList<>();

        int texW = GameApp.getTextureWidth("red_long");
        int texH = GameApp.getTextureHeight("red_long");
        float scale = 0.7f;

        buttonWidth = texW * scale;
        buttonHeight = texH * scale;

        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;

        float buttonY = 20f;
        Button backButton = new Button(centerX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight, "");
        backButton.setOnClick(() -> {});
        if (GameApp.hasTexture("red_long")) {
            backButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        buttons.add(backButton);
    }

    private void initCreditsContent() {
        creditSections = new ArrayList<>();
        
        // Developed by
        creditSections.add(new CreditSection("DEVELOPED BY", new String[]{
            "Team 72 - SP Class"
        }));
        
        // Team members - all equal
        creditSections.add(new CreditSection("TEAM MEMBERS", new String[]{
            "Thuong Tran",
            "Daniel Lehter",
            "Arnold Ayiku",
            "Mehmet Yildirim"
        }));
        
        // Teacher Guide
        creditSections.add(new CreditSection("TEACHER GUIDE", new String[]{
            "Craig Bradley"
        }));
        
        // Saxion University - Title shown ABOVE the logo
        creditSections.add(new CreditSection("", new String[]{
            "SAXION_LOGO" // Special marker for logo (title drawn separately above)
        }));
        
        // Game Inspiration
        creditSections.add(new CreditSection("INSPIRED BY", new String[]{
            "Vampire Survivors"
        }));
        
        // Special Thanks - Contributors
        creditSections.add(new CreditSection("SPECIAL THANKS", new String[]{
            "Noah Gooningson"
        }));
        
        // Legal Notice
        creditSections.add(new CreditSection("LEGAL NOTICE", new String[]{
            "This is a non-commercial educational project",
            "Created for learning purposes only",
            "No commercial intent or profit"
        }));
        
        // Calculate total credits height
        calculateTotalHeight();
    }

    private void calculateTotalHeight() {
        float height = 50f; // Reduced initial padding
        
        for (CreditSection section : creditSections) {
            if (!section.title.isEmpty()) {
                height += 50f; // Reduced section title spacing
            }
            for (String name : section.names) {
                if (name.equals("SAXION_LOGO")) {
                    height += 140f; // More space for larger logo with proper spacing
                } else {
                    height += 35f; // Reduced name spacing
                }
            }
            height += 25f; // Reduced section spacing
        }
        
        height += 50f; // Reduced end padding
        totalCreditsHeight = height;
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
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        
        animTimer += delta;

        // Handle F11 for fullscreen
        if (GameApp.isKeyJustPressed(Input.Keys.F11)) {
            isFullscreen = !isFullscreen;
            if (isFullscreen) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                Gdx.graphics.setWindowedMode(1280, 720);
            }
            GameConfig config = ConfigManager.loadConfig();
            config.fullscreen = isFullscreen;
            ConfigManager.saveConfig(config);
        }

        // Handle escape to go back
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 2.5f);
            }
            GameApp.switchScreen(previousScreen);
            return;
        }

        // Handle scroll input
        handleScrollInput(delta);
        
        // Auto-resume scrolling when no manual input
        if (!GameApp.isKeyPressed(Input.Keys.UP) && !GameApp.isKeyPressed(Input.Keys.DOWN) &&
            !GameApp.isKeyPressed(Input.Keys.W) && !GameApp.isKeyPressed(Input.Keys.S)) {
            // Resume auto scroll after manual control
            if (!autoScroll) {
                autoScroll = true;
            }
        }

        // Auto scroll - credits move UP like real film credits (scrollY increases)
        if (autoScroll) {
            scrollY += scrollSpeed * delta;
            
            // When credits fully leave top, restart from bottom
            if (scrollY > totalCreditsHeight + 200f) {
                scrollY = 0f; // Restart from bottom
            }
        }

        // Handle button press animation
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

        if (pendingAction == null) {
            handleButtonInput();
        } else if (pressedButton != null) {
            pressedButton.setPressed(true);
        }

        // Draw
        GameApp.clearScreen("black");
        drawBackground();
        drawCreditsPanel();
        drawScrollingCredits();
        
        // Draw buttons
        for (Button button : buttons) {
            button.render();
        }
        drawButtonText();
    }

    private void handleScrollInput(float delta) {
        // Manual scroll with UP/DOWN
        // DOWN = scroll faster (content moves down faster)
        // UP = scroll backward (content moves up/back)
        if (GameApp.isKeyPressed(Input.Keys.DOWN) || GameApp.isKeyPressed(Input.Keys.S)) {
            scrollY += scrollSpeed * 3 * delta; // Speed up scrolling
            autoScroll = false;
        }
        if (GameApp.isKeyPressed(Input.Keys.UP) || GameApp.isKeyPressed(Input.Keys.W)) {
            scrollY -= scrollSpeed * 3 * delta; // Scroll back
            autoScroll = false;
        }
        
        // Space to toggle auto scroll
        if (GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            autoScroll = !autoScroll;
        }
        
        // Optional clamp - only limit max, don't block negative values
        scrollY = Math.min(scrollY, totalCreditsHeight + 200f);
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

    private void drawCreditsPanel() {
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        float panelWidth = 800f;
        float panelHeight = 550f;
        float panelX = centerX - panelWidth / 2;
        float panelY = 90f;
        
        // Dark semi-transparent panel
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(15, 15, 25, 220);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Panel border with gradient effect
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(3f);
        GameApp.setColor(80, 80, 120, 255);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Inner border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(1f);
        GameApp.setColor(50, 50, 80, 150);
        GameApp.drawRect(panelX + 5f, panelY + 5f, panelWidth - 10f, panelHeight - 10f);
        GameApp.endShapeRendering();
        
        // Top decorative line
        GameApp.startShapeRenderingFilled();
        float pulse = (float) (0.7f + 0.3f * Math.sin(animTimer * 2));
        GameApp.setColor((int)(255 * pulse), (int)(200 * pulse), 50, 200);
        GameApp.drawRect(panelX + 50f, panelY + panelHeight - 8f, panelWidth - 100f, 3f);
        GameApp.endShapeRendering();
        
        // Title at top
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("creditsGameTitle", "CREDITS", centerX, panelY + panelHeight - 45f, "yellow-400");
        GameApp.endSpriteRendering();
    }

    private void drawScrollingCredits() {
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        // Panel bounds
        float panelY = 90f;
        float panelHeight = 550f;
        
        // STRICT visible area - text only shows in this zone
        float visibleTop = panelY + panelHeight - 90f;   // Below CREDITS title
        float visibleBottom = panelY + 70f;               // Above panel bottom (and BACK button)
        
        // START just below visible area - credits move UP like real film credits
        float startY = visibleBottom - 40f;
        float currentY = startY + scrollY; // Credits move UP as scrollY increases
        
        GameApp.startSpriteRendering();
        
        // Render sections in normal order (will appear from bottom to top)
        for (CreditSection section : creditSections) {
            // Section title
            if (!section.title.isEmpty()) {
                float titleY = currentY;
                
                // STRICT bounds check - only draw within visible area
                if (titleY <= visibleTop && titleY >= visibleBottom) {
                    // Special styling for different sections
                    if (section.title.equals("INSPIRED BY")) {
                        GameApp.drawTextCentered("creditsSpecial", section.title, centerX, titleY, "cyan-400");
                    } else {
                        GameApp.drawTextCentered("creditsSectionTitle", section.title, centerX, titleY, "yellow-300");
                    }
                }
                currentY -= 50f; // Move up for next item
            }
            
            // Names
            for (String name : section.names) {
                float itemY = currentY;
                
                // STRICT bounds check - only draw within visible area
                if (itemY <= visibleTop && itemY >= visibleBottom) {
                    if (name.equals("SAXION_LOGO")) {
                        // Draw "UNIVERSITY" title ABOVE the logo first
                        GameApp.drawTextCentered("creditsSectionTitle", "UNIVERSITY", centerX, itemY + 70f, "yellow-300");
                        
                        // Draw Saxion logo BELOW the title
                        if (GameApp.hasTexture("saxion_logo")) {
                            float logoHeight = 100f;  // Slightly smaller to fit with title
                            float logoWidth = 180f;  // 16:9 aspect ratio
                            GameApp.drawTexture("saxion_logo", centerX - logoWidth/2, itemY - logoHeight/2, logoWidth, logoHeight);
                        }
                        currentY -= 160f; // More space for title + logo
                    } else {
                        // Special styling for legal notice
                        if (section.title.equals("LEGAL NOTICE")) {
                            GameApp.drawTextCentered("creditsSmall", name, centerX, itemY, "gray-300");
                        } else {
                            GameApp.drawTextCentered("creditsName", name, centerX, itemY, "white");
                        }
                        currentY -= 35f; // Move up for next item
                    }
                } else {
                    // Still need to advance position even if not drawing
                    if (name.equals("SAXION_LOGO")) {
                        currentY -= 160f; // Match the drawing spacing
                    } else {
                        currentY -= 35f;
                    }
                }
            }
            
            currentY -= 25f; // Section spacing
        }
        
        GameApp.endSpriteRendering();
    }

    private void drawButtonText() {
        GameApp.startSpriteRendering();

        for (Button button : buttons) {
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;

            float textHeight = GameApp.getTextHeight("buttonFont", "BACK");
            float adjustedY = buttonCenterY + textHeight * 0.15f;

            GameApp.drawTextCentered("buttonFont", "BACK", buttonCenterX, adjustedY, "button_red_text");
        }

        GameApp.endSpriteRendering();
    }

    private void handleButtonInput() {
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;

        boolean hoveringAnyButton = false;
        for (Button button : buttons) {
            if (button.containsPoint(worldMouseX, worldMouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }

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

        for (Button button : buttons) {
            button.update(worldMouseX, worldMouseY);
        }

        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed) {
            for (Button button : buttons) {
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }

                    pressedButton = button;
                    button.setPressed(true);

                    pendingAction = () -> GameApp.switchScreen(previousScreen);

                    pressTimer = 0f;
                    break;
                }
            }
        }

        if (pendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : buttons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        }
    }
}
