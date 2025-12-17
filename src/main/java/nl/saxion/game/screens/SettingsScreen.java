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

// Settings screen with volume controls
public class SettingsScreen extends ScalableGameScreen {

    // Volume settings (0-100)
    private int masterVolume = 100;
    private int sfxVolume = 100;
    private int musicVolume = 70;

    // Setting items
    private List<SettingItem> settingItems;
    private int selectedItemIndex = 0;

    // Back button
    private Button backButton;
    
    // Sound manager for background music
    private SoundManager soundManager;

    // Cursor management
    private Cursor cursorPointer; // Left side (pointer/default)
    private Cursor cursorHover;   // Right side (hover)
    private boolean isHoveringButton = false;


    // Colors - using custom registered colors
    private static final String BG_COLOR = "black";
    private static final String TEXT_COLOR = "white";
    private static final String HIGHLIGHT_COLOR = "settings_yellow";
    private static final String BAR_BG_COLOR = "darkgray";
    private static final String BAR_FILL_COLOR = "settings_green";

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
        
        // Start background music for menu (if not already playing)
        if (soundManager != null) {
            soundManager.playMusic(true);
        }

        loadResources();
        loadSettingsFromConfig();
        createSettingItems();
        createBackButton();
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
    }

    // Save settings to config file
    private void saveSettingsToConfig() {
        GameConfig config = ConfigManager.loadConfig();
        config.masterVolume = masterVolume / 100f;
        config.sfxVolume = sfxVolume / 100f;
        config.musicVolume = musicVolume / 100f;
        ConfigManager.saveConfig(config);
    }

    private void loadResources() {
        // Register custom colors for settings screen
        if (!GameApp.hasColor("settings_yellow")) {
            GameApp.addColor("settings_yellow", 255, 255, 0);
        }
        if (!GameApp.hasColor("settings_green")) {
            GameApp.addColor("settings_green", 50, 205, 50);
        }
    }

    private void createSettingItems() {
        settingItems = new ArrayList<>();

        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        float startY = 150;
        float spacing = 80;

        // Master Volume
        settingItems.add(new SettingItem("Master Volume", centerX, startY, () -> masterVolume, v -> masterVolume = v));

        // SFX Volume
        settingItems.add(new SettingItem("SFX Volume", centerX, startY + spacing, () -> sfxVolume, v -> sfxVolume = v));

        // Music Volume
        settingItems.add(new SettingItem("Music Volume", centerX, startY + spacing * 2, () -> musicVolume, v -> musicVolume = v));
    }

    private void createBackButton() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        float btnWidth = 150;
        float btnHeight = 40;
        float btnX = (screenWidth - btnWidth) / 2;
        float btnY = screenHeight - 100;

        backButton = new Button(btnX, btnY, btnWidth, btnHeight, "< BACK");
        backButton.setOnClick(() -> {
            saveSettingsToConfig();
            GameApp.log("Settings saved. Returning to main menu...");
            GameApp.switchScreen("menu");
        });
        backButton.setColors("darkgray", "gray", "white", "white");
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

        // Get mouse position
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();

        // Handle input
        handleInput(mouseX, mouseY);

        // Update back button
        backButton.update(mouseX, mouseY);

        // Draw title
        drawTitle();

        // Draw settings
        drawSettings();

        // Draw back button
        backButton.render();

        // Draw instructions
        drawInstructions();
    }

    private void handleInput(float mouseX, float mouseY) {
        // Check if hovering over back button or setting items for cursor switching
        boolean hoveringAnyButton = false;
        if (backButton.containsPoint(mouseX, mouseY)) {
            hoveringAnyButton = true;
        }
        for (SettingItem item : settingItems) {
            if (item.containsPoint(mouseX, mouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }

        // Switch cursor based on hover state and click state
        if (cursorPointer != null && cursorHover != null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
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

        // Navigate between settings with up/down
        if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
            selectedItemIndex--;
            if (selectedItemIndex < 0) {
                selectedItemIndex = settingItems.size(); // Include back button
            }
        }

        if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
            selectedItemIndex++;
            if (selectedItemIndex > settingItems.size()) {
                selectedItemIndex = 0;
            }
        }

        // Adjust volume with left/right
        if (selectedItemIndex < settingItems.size()) {
            SettingItem item = settingItems.get(selectedItemIndex);

            if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) {
                item.decrease();
            }

            if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) {
                item.increase();
            }
        }

        // Enter to go back if back button is selected
        if (GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            if (selectedItemIndex == settingItems.size()) {
                // Play button click sound at 2.5f volume
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 2.5f);
                }
                backButton.click();
            }
        }

        // Escape to go back (saves settings)
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            saveSettingsToConfig();
            // Play button click sound at 2.5f volume
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 2.5f);
            }
            backButton.click();
        }

        // Mouse click on back button
        if (GameApp.isButtonJustPressed(0)) {
            if (backButton.containsPoint(mouseX, mouseY)) {
                // Play button click sound at 2.5f volume
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 2.5f);
                }
                backButton.click();
            }

            // Click on volume bars to adjust
            for (int i = 0; i < settingItems.size(); i++) {
                SettingItem item = settingItems.get(i);
                if (item.containsPoint(mouseX, mouseY)) {
                    selectedItemIndex = i;
                    // Calculate volume from click position
                    float barX = item.getBarX();
                    float barWidth = item.getBarWidth();
                    float clickRatio = (mouseX - barX) / barWidth;
                    int newVolume = (int) (clickRatio * 100);
                    newVolume = Math.max(0, Math.min(100, newVolume));
                    item.setValue(newVolume);
                }
            }
        }
    }

    private void drawTitle() {
        float screenWidth = GameApp.getWorldWidth();

        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("default", "SETTINGS", screenWidth / 2, 80, TEXT_COLOR);
        GameApp.endSpriteRendering();
    }

    private void drawSettings() {
        for (int i = 0; i < settingItems.size(); i++) {
            boolean isSelected = (i == selectedItemIndex);
            settingItems.get(i).render(isSelected);
        }

        // Highlight back button if selected
        backButton.setSelected(selectedItemIndex == settingItems.size());
    }

    private void drawInstructions() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("default", "Use UP/DOWN to navigate, LEFT/RIGHT to adjust",
                screenWidth / 2, screenHeight - 40, "gray");
        GameApp.endSpriteRendering();
    }

    // Inner class for setting items
    private class SettingItem {
        private String label;
        private float x, y;
        private float barWidth = 200;
        private float barHeight = 20;
        private ValueGetter getter;
        private ValueSetter setter;

        interface ValueGetter {
            int get();
        }

        interface ValueSetter {
            void set(int value);
        }

        public SettingItem(String label, float x, float y, ValueGetter getter, ValueSetter setter) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.getter = getter;
            this.setter = setter;
        }

        public void render(boolean isSelected) {
            String textColor = isSelected ? HIGHLIGHT_COLOR : TEXT_COLOR;
            int value = getter.get();

            // Draw label
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("default", label + ": " + value + "%", x, y, textColor);
            GameApp.endSpriteRendering();

            // Draw volume bar background
            float barX = x - barWidth / 2;
            float barY = y + 25;

            GameApp.startShapeRenderingFilled();
            GameApp.drawRect(barX, barY, barWidth, barHeight, BAR_BG_COLOR);

            // Draw filled portion
            float fillWidth = (value / 100f) * barWidth;
            if (fillWidth > 0) {
                GameApp.drawRect(barX, barY, fillWidth, barHeight, BAR_FILL_COLOR);
            }
            GameApp.endShapeRendering();

            // Draw border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.drawRect(barX, barY, barWidth, barHeight, isSelected ? HIGHLIGHT_COLOR : TEXT_COLOR);
            GameApp.endShapeRendering();

            // Draw +/- indicators
            if (isSelected) {
                GameApp.startSpriteRendering();
                GameApp.drawTextCentered("default", "<", barX - 15, barY + barHeight / 2, HIGHLIGHT_COLOR);
                GameApp.drawTextCentered("default", ">", barX + barWidth + 15, barY + barHeight / 2, HIGHLIGHT_COLOR);
                GameApp.endSpriteRendering();
            }
        }

        public void increase() {
            int value = getter.get();
            value = Math.min(100, value + 5);
            setter.set(value);
            // Auto-save when volume changes
            saveSettingsToConfig();
        }

        public void decrease() {
            int value = getter.get();
            value = Math.max(0, value - 5);
            setter.set(value);
            // Auto-save when volume changes
            saveSettingsToConfig();
        }

        public void setValue(int value) {
            setter.set(value);
            // Auto-save when volume changes
            saveSettingsToConfig();
        }

        public boolean containsPoint(float px, float py) {
            float barX = x - barWidth / 2;
            float barY = y + 25;
            return px >= barX && px <= barX + barWidth && py >= barY && py <= barY + barHeight;
        }

        public float getBarX() {
            return x - barWidth / 2;
        }

        public float getBarWidth() {
            return barWidth;
        }
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

