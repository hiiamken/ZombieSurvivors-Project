package nl.saxion.game.screens;

import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.core.PlayerData;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for entering player information before starting the game.
 * Asks for: Username, Class (dropdown), Group Number (dropdown)
 */
public class PlayerInputScreen extends ScalableGameScreen {

    // Input fields
    private String username = "";
    private String studentClass = "";
    private String groupNumber = "";
    
    // Dropdown options
    private static final String[] CLASS_OPTIONS = {"SP", "SO", "SQ", "Other"};
    private static final String[] GROUP_OPTIONS = {"54", "58", "64", "72", "81", "85", "Other"};
    
    // Selected dropdown indices (-1 means custom/Other)
    private int selectedClassIndex = -1;
    private int selectedGroupIndex = -1;
    
    // Dropdown open state
    private boolean classDropdownOpen = false;
    private boolean groupDropdownOpen = false;
    
    // Which field is currently active (0=username, 1=class, 2=group)
    private int activeField = 0;
    
    // Is "Other" mode active for class/group?
    private boolean classOtherMode = false;
    private boolean groupOtherMode = false;
    
    // Max characters per field
    private static final int MAX_USERNAME_LENGTH = 15;
    private static final int MAX_CLASS_LENGTH = 10;
    private static final int MAX_GROUP_LENGTH = 3;
    
    // Field bounds for click detection
    private float[] fieldXPositions = new float[3];
    private float[] fieldYPositions = new float[3];
    private float fieldWidth = 400f;
    private float fieldHeight = 45f;
    
    // Dropdown dimensions
    private float dropdownItemHeight = 38f;
    
    // UI Buttons
    private List<Button> buttons;
    private boolean resourcesLoaded = false;
    private SoundManager soundManager;
    
    // Button dimensions
    private float buttonWidth;
    private float buttonHeight;
    
    // Delay for button press animation
    private float pressDelay = 0.3f;
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;
    
    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringButton = false;
    private boolean isHoveringField = false;
    
    // Cursor blink for text input
    private float cursorBlinkTimer = 0f;
    private boolean cursorVisible = true;
    
    // Error message display
    private String errorMessage = "";
    private float errorTimer = 0f;
    
    // Success animation
    private float successTimer = 0f;
    private boolean showSuccess = false;
    
    public PlayerInputScreen() {
        super(1280, 720);
    }
    
    @Override
    public void show() {
        DebugLogger.log("PlayerInputScreen.show() called");
        
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
        
        // Reset input fields
        username = "";
        studentClass = "";
        groupNumber = "";
        activeField = 0;
        errorMessage = "";
        showSuccess = false;
        selectedClassIndex = -1;
        selectedGroupIndex = -1;
        classDropdownOpen = false;
        groupDropdownOpen = false;
        classOtherMode = false;
        groupOtherMode = false;
        
        DebugLogger.log("PlayerInputScreen initialized");
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
        
        if (!GameApp.hasFont("inputTitle")) {
            GameApp.addStyledFont("inputTitle", "fonts/upheavtt.ttf", 48,
                    "white", 0f, "black", 3, 3, "gray-700", true);
        }
        if (!GameApp.hasFont("inputLabel")) {
            GameApp.addStyledFont("inputLabel", "fonts/PressStart2P-Regular.ttf", 14,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("inputField")) {
            GameApp.addStyledFont("inputField", "fonts/PressStart2P-Regular.ttf", 16,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("inputPlaceholder")) {
            GameApp.addStyledFont("inputPlaceholder", "fonts/PressStart2P-Regular.ttf", 14,
                    "gray-600", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("inputError")) {
            GameApp.addStyledFont("inputError", "fonts/PressStart2P-Regular.ttf", 12,
                    "red-400", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("inputCounter")) {
            GameApp.addStyledFont("inputCounter", "fonts/PressStart2P-Regular.ttf", 10,
                    "gray-500", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("inputHint")) {
            GameApp.addStyledFont("inputHint", "fonts/PressStart2P-Regular.ttf", 11,
                    "gray-500", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("dropdownItem")) {
            GameApp.addStyledFont("dropdownItem", "fonts/PressStart2P-Regular.ttf", 13,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        if (!GameApp.hasFont("buttonFont")) {
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 40,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }
        
        if (!GameApp.hasColor("button_green_text")) {
            GameApp.addColor("button_green_text", 25, 50, 25);
        }
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        if (!GameApp.hasColor("dropdown_bg")) {
            GameApp.addColor("dropdown_bg", 35, 35, 55);
        }
        if (!GameApp.hasColor("dropdown_hover")) {
            GameApp.addColor("dropdown_hover", 60, 60, 100);
        }
        
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
        if (!GameApp.hasTexture("mainmenu_bg")) {
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        }
        
        resourcesLoaded = true;
    }
    
    private void createButtons() {
        buttons = new ArrayList<>();
        
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        float scale = 0.7f;
        
        buttonWidth = texW * scale;
        buttonHeight = texH * scale;
        
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        float buttonY = 80f;
        float buttonSpacing = 30f;
        
        // Back button (red)
        float backX = centerX - buttonWidth - buttonSpacing / 2;
        Button backButton = new Button(backX, buttonY, buttonWidth, buttonHeight, "");
        backButton.setOnClick(() -> {});
        if (GameApp.hasTexture("red_long")) {
            backButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        buttons.add(backButton);
        
        // Start button (green)
        float startX = centerX + buttonSpacing / 2;
        Button startButton = new Button(startX, buttonY, buttonWidth, buttonHeight, "");
        startButton.setOnClick(() -> {});
        if (GameApp.hasTexture("green_long")) {
            startButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        buttons.add(startButton);
    }
    
    @Override
    public void hide() {
        if (soundManager != null) {
            soundManager.stopMusic();
        }
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
        
        if (GameApp.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        
        GameApp.clearScreen("black");
        
        // Update cursor blink
        cursorBlinkTimer += delta;
        if (cursorBlinkTimer >= 0.5f) {
            cursorBlinkTimer = 0f;
            cursorVisible = !cursorVisible;
        }
        
        // Update error timer
        if (errorTimer > 0) {
            errorTimer -= delta;
            if (errorTimer <= 0) {
                errorMessage = "";
            }
        }
        
        // Update success animation
        if (showSuccess) {
            successTimer += delta;
            if (successTimer >= 0.3f) {
                showSuccess = false;
                successTimer = 0f;
                GameApp.switchScreen("play");
                return;
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
        handleTextInput();
        handleFieldClicks();
        
        if (pendingAction == null && !showSuccess) {
            handleButtonInput();
        } else if (pressedButton != null) {
            pressedButton.setPressed(true);
        }
        
        drawBackground();
        drawInputForm();
        
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
    
    private void drawInputForm() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        
        float panelWidth = 650f;
        float panelHeight = 480f;
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2 + 30f;
        
        // Panel background
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(15, 15, 25, 235);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Panel border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(3f);
        GameApp.setColor(80, 80, 140, 255);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        GameApp.startSpriteRendering();
        
        // Title
        float titleY = panelY + panelHeight - 55f;
        GameApp.drawTextCentered("inputTitle", "ENTER YOUR INFO", centerX, titleY, "white");
        
        // Subtitle - brighter for visibility
        float subtitleY = titleY - 30f;
        GameApp.drawTextCentered("inputHint", "Fill in your details to join the leaderboard", centerX, subtitleY, "gray-300");
        
        // Field layout
        float fieldStartY = subtitleY - 60f;
        float fieldSpacing = 100f;
        fieldWidth = 450f;
        fieldHeight = 50f;
        float labelOffsetY = 35f;
        
        String[] labels = {"USERNAME", "CLASS", "GROUP"};
        
        for (int i = 0; i < 3; i++) {
            float fieldY = fieldStartY - i * fieldSpacing;
            float fieldX = centerX - fieldWidth / 2;
            
            fieldXPositions[i] = fieldX;
            fieldYPositions[i] = fieldY - fieldHeight;
            
            // Label
            GameApp.drawTextCentered("inputLabel", labels[i], centerX, fieldY + labelOffsetY, "white");
            
            GameApp.endSpriteRendering();
            
            // Field background
            GameApp.startShapeRenderingFilled();
            if (i == activeField && !classDropdownOpen && !groupDropdownOpen) {
                GameApp.setColor(50, 50, 75, 255);
            } else if ((i == 1 && !studentClass.isEmpty()) || (i == 2 && !groupNumber.isEmpty()) || (i == 0 && !username.isEmpty())) {
                GameApp.setColor(35, 45, 40, 255);
            } else {
                GameApp.setColor(30, 30, 45, 255);
            }
            GameApp.drawRect(fieldX, fieldY - fieldHeight, fieldWidth, fieldHeight);
            GameApp.endShapeRendering();
            
            // Field border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            if (i == activeField && !classDropdownOpen && !groupDropdownOpen) {
                float pulse = (float) (0.7f + 0.3f * Math.sin(cursorBlinkTimer * 6));
                int brightness = (int) (200 * pulse);
                GameApp.setColor(brightness, brightness, 80, 255);
            } else if ((i == 0 && !username.isEmpty()) || (i == 1 && !studentClass.isEmpty()) || (i == 2 && !groupNumber.isEmpty())) {
                GameApp.setColor(60, 120, 80, 255);
            } else {
                GameApp.setColor(60, 60, 90, 255);
            }
            GameApp.drawRect(fieldX, fieldY - fieldHeight, fieldWidth, fieldHeight);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            
            float textY = fieldY - fieldHeight / 2 - 3f;
            
            if (i == 0) {
                // Username field - text input
                if (username.isEmpty() && activeField != 0) {
                    GameApp.drawTextCentered("inputPlaceholder", "Enter your name...", centerX, textY, "gray-400");
                } else {
                    String displayValue = username;
                    if (activeField == 0 && cursorVisible && !classDropdownOpen && !groupDropdownOpen) {
                        displayValue = displayValue + "|";
                    } else if (activeField == 0) {
                        displayValue = displayValue + " ";
                    }
                    GameApp.drawTextCentered("inputField", displayValue, centerX, textY, "white");
                }
                
                // Character counter - brighter color
                String counterText = username.length() + "/" + MAX_USERNAME_LENGTH;
                String counterColor = username.length() >= MAX_USERNAME_LENGTH ? "red-400" : "gray-300";
                GameApp.drawText("inputCounter", counterText, fieldX + fieldWidth - 60f, fieldY - fieldHeight - 15f, counterColor);
                
            } else if (i == 1) {
                // Class field - dropdown or text input (Other mode)
                String displayText;
                if (classOtherMode) {
                    displayText = studentClass.isEmpty() ? "" : studentClass;
                    if (activeField == 1 && cursorVisible && !classDropdownOpen && !groupDropdownOpen) {
                        displayText = displayText + "|";
                    }
                } else if (selectedClassIndex >= 0 && selectedClassIndex < CLASS_OPTIONS.length - 1) {
                    displayText = CLASS_OPTIONS[selectedClassIndex];
                } else {
                    displayText = "";
                }
                
                if (displayText.isEmpty() && activeField != 1) {
                    GameApp.drawTextCentered("inputPlaceholder", "Click to select...", centerX, textY, "gray-400");
                } else {
                    GameApp.drawTextCentered("inputField", displayText, centerX, textY, "white");
                }
                
                // Dropdown arrow indicator - brighter
                GameApp.drawText("inputField", classDropdownOpen ? "^" : "v", fieldX + fieldWidth - 35f, textY, "gray-200");
                
                if (classOtherMode) {
                    String counterText = studentClass.length() + "/" + MAX_CLASS_LENGTH;
                    String counterColor = studentClass.length() >= MAX_CLASS_LENGTH ? "red-400" : "gray-300";
                    GameApp.drawText("inputCounter", counterText, fieldX + fieldWidth - 60f, fieldY - fieldHeight - 15f, counterColor);
                }
                
            } else if (i == 2) {
                // Group field - dropdown or text input (Other mode)
                String displayText;
                if (groupOtherMode) {
                    displayText = groupNumber.isEmpty() ? "" : groupNumber;
                    if (activeField == 2 && cursorVisible && !classDropdownOpen && !groupDropdownOpen) {
                        displayText = displayText + "|";
                    }
                } else if (selectedGroupIndex >= 0 && selectedGroupIndex < GROUP_OPTIONS.length - 1) {
                    displayText = GROUP_OPTIONS[selectedGroupIndex];
                } else {
                    displayText = "";
                }
                
                if (displayText.isEmpty() && activeField != 2) {
                    GameApp.drawTextCentered("inputPlaceholder", "Click to select...", centerX, textY, "gray-400");
                } else {
                    GameApp.drawTextCentered("inputField", displayText, centerX, textY, "white");
                }
                
                // Dropdown arrow indicator - brighter
                GameApp.drawText("inputField", groupDropdownOpen ? "^" : "v", fieldX + fieldWidth - 35f, textY, "gray-200");
                
                if (groupOtherMode) {
                    String counterText = groupNumber.length() + "/" + MAX_GROUP_LENGTH;
                    String counterColor = groupNumber.length() >= MAX_GROUP_LENGTH ? "red-400" : "gray-300";
                    GameApp.drawText("inputCounter", counterText, fieldX + fieldWidth - 60f, fieldY - fieldHeight - 15f, counterColor);
                }
            }
            
            // Checkmark for filled fields
            if (i == 0 && !username.isEmpty()) {
                GameApp.drawText("inputCounter", "OK", fieldX + fieldWidth + 10f, textY, "green-400");
            } else if (i == 1 && !studentClass.isEmpty()) {
                GameApp.drawText("inputCounter", "OK", fieldX + fieldWidth + 10f, textY, "green-400");
            } else if (i == 2 && !groupNumber.isEmpty()) {
                GameApp.drawText("inputCounter", "OK", fieldX + fieldWidth + 10f, textY, "green-400");
            }
        }
        
        GameApp.endSpriteRendering();
        
        // Draw dropdowns on top
        drawDropdowns(centerX, fieldStartY, fieldSpacing);
        
        GameApp.startSpriteRendering();
        
        // Hint text - brighter for visibility
        float hintY = fieldStartY - 3 * fieldSpacing - 10f;
        GameApp.drawTextCentered("inputHint", "TAB to switch | ENTER to start", centerX, hintY, "gray-300");
        
        // Error message
        if (!errorMessage.isEmpty()) {
            float errorY = hintY - 25f;
            GameApp.drawTextCentered("inputError", errorMessage, centerX, errorY, "red-400");
        }
        
        // Progress indicator - brighter for visibility
        int filledCount = 0;
        if (!username.isEmpty()) filledCount++;
        if (!studentClass.isEmpty()) filledCount++;
        if (!groupNumber.isEmpty()) filledCount++;
        
        float progressY = panelY + 15f;
        String progressText = filledCount + "/3 fields completed";
        String progressColor = filledCount == 3 ? "green-400" : "gray-300";
        GameApp.drawTextCentered("inputHint", progressText, centerX, progressY, progressColor);
        
        GameApp.endSpriteRendering();
    }
    
    private void drawDropdowns(float centerX, float fieldStartY, float fieldSpacing) {
        float fieldX = centerX - fieldWidth / 2;
        
        // Class dropdown
        if (classDropdownOpen) {
            float dropdownY = fieldStartY - 1 * fieldSpacing - fieldHeight;
            float dropdownHeight = CLASS_OPTIONS.length * dropdownItemHeight;
            
            // Background
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(25, 25, 40, 250);
            GameApp.drawRect(fieldX, dropdownY - dropdownHeight, fieldWidth, dropdownHeight);
            GameApp.endShapeRendering();
            
            // Border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 80, 140, 255);
            GameApp.drawRect(fieldX, dropdownY - dropdownHeight, fieldWidth, dropdownHeight);
            GameApp.endShapeRendering();
            
            // Items
            com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
            float worldMouseX = mouseWorld.x;
            float worldMouseY = mouseWorld.y;
            
            for (int i = 0; i < CLASS_OPTIONS.length; i++) {
                float itemY = dropdownY - (i + 1) * dropdownItemHeight;
                
                // Hover highlight
                if (worldMouseX >= fieldX && worldMouseX <= fieldX + fieldWidth &&
                    worldMouseY >= itemY && worldMouseY <= itemY + dropdownItemHeight) {
                    GameApp.startShapeRenderingFilled();
                    GameApp.setColor(60, 60, 100, 200);
                    GameApp.drawRect(fieldX + 2f, itemY, fieldWidth - 4f, dropdownItemHeight);
                    GameApp.endShapeRendering();
                }
                
                GameApp.startSpriteRendering();
                String optionText = CLASS_OPTIONS[i];
                if (i == CLASS_OPTIONS.length - 1) {
                    optionText = "Other (custom)";
                }
                GameApp.drawTextCentered("dropdownItem", optionText, centerX, itemY + dropdownItemHeight / 2 - 3f, "white");
                GameApp.endSpriteRendering();
            }
        }
        
        // Group dropdown
        if (groupDropdownOpen) {
            float dropdownY = fieldStartY - 2 * fieldSpacing - fieldHeight;
            float dropdownHeight = GROUP_OPTIONS.length * dropdownItemHeight;
            
            // Adjust if dropdown goes below screen
            float adjustedDropdownY = dropdownY;
            if (dropdownY - dropdownHeight < 100f) {
                // Draw above the field instead
                adjustedDropdownY = dropdownY + fieldHeight + dropdownHeight;
            }
            
            // Background
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(25, 25, 40, 250);
            GameApp.drawRect(fieldX, adjustedDropdownY - dropdownHeight, fieldWidth, dropdownHeight);
            GameApp.endShapeRendering();
            
            // Border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 80, 140, 255);
            GameApp.drawRect(fieldX, adjustedDropdownY - dropdownHeight, fieldWidth, dropdownHeight);
            GameApp.endShapeRendering();
            
            // Items
            com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
            float worldMouseX = mouseWorld.x;
            float worldMouseY = mouseWorld.y;
            
            for (int i = 0; i < GROUP_OPTIONS.length; i++) {
                float itemY = adjustedDropdownY - (i + 1) * dropdownItemHeight;
                
                // Hover highlight
                if (worldMouseX >= fieldX && worldMouseX <= fieldX + fieldWidth &&
                    worldMouseY >= itemY && worldMouseY <= itemY + dropdownItemHeight) {
                    GameApp.startShapeRenderingFilled();
                    GameApp.setColor(60, 60, 100, 200);
                    GameApp.drawRect(fieldX + 2f, itemY, fieldWidth - 4f, dropdownItemHeight);
                    GameApp.endShapeRendering();
                }
                
                GameApp.startSpriteRendering();
                String optionText = GROUP_OPTIONS[i];
                if (i == GROUP_OPTIONS.length - 1) {
                    optionText = "Other (0-100)";
                }
                GameApp.drawTextCentered("dropdownItem", optionText, centerX, itemY + dropdownItemHeight / 2 - 3f, "white");
                GameApp.endSpriteRendering();
            }
        }
    }
    
    private void drawButtonText() {
        GameApp.startSpriteRendering();
        
        String[] buttonTexts = {"BACK", "START GAME"};
        String[] buttonColors = {"button_red_text", "button_green_text"};
        
        for (int i = 0; i < buttons.size() && i < buttonTexts.length; i++) {
            Button button = buttons.get(i);
            String text = buttonTexts[i];
            String colorName = buttonColors[i];
            
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;
            
            float textHeight = GameApp.getTextHeight("buttonFont", text);
            float adjustedY = buttonCenterY + textHeight * 0.15f;
            
            GameApp.drawTextCentered("buttonFont", text, buttonCenterX, adjustedY, colorName);
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void handleFieldClicks() {
        if (!GameApp.isButtonJustPressed(0)) {
            updateCursor();
            return;
        }
        
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        float panelHeight = 480f;
        float panelY = centerY - panelHeight / 2 + 30f;
        float subtitleY = panelY + panelHeight - 55f - 30f;
        float fieldStartY = subtitleY - 60f;
        float fieldSpacing = 100f;
        float fieldX = centerX - fieldWidth / 2;
        
        // Handle class dropdown clicks
        if (classDropdownOpen) {
            float dropdownY = fieldStartY - 1 * fieldSpacing - fieldHeight;
            
            for (int i = 0; i < CLASS_OPTIONS.length; i++) {
                float itemY = dropdownY - (i + 1) * dropdownItemHeight;
                
                if (worldMouseX >= fieldX && worldMouseX <= fieldX + fieldWidth &&
                    worldMouseY >= itemY && worldMouseY <= itemY + dropdownItemHeight) {
                    
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 0.8f);
                    }
                    
                    if (i == CLASS_OPTIONS.length - 1) {
                        // "Other" selected
                        classOtherMode = true;
                        selectedClassIndex = -1;
                        studentClass = "";
                        activeField = 1;
                    } else {
                        classOtherMode = false;
                        selectedClassIndex = i;
                        studentClass = CLASS_OPTIONS[i];
                    }
                    
                    classDropdownOpen = false;
                    return;
                }
            }
            
            // Clicked outside dropdown - close it
            classDropdownOpen = false;
            return;
        }
        
        // Handle group dropdown clicks
        if (groupDropdownOpen) {
            float dropdownY = fieldStartY - 2 * fieldSpacing - fieldHeight;
            float groupDropdownHeight = GROUP_OPTIONS.length * dropdownItemHeight;
            
            float adjustedDropdownY = dropdownY;
            if (dropdownY - groupDropdownHeight < 100f) {
                adjustedDropdownY = dropdownY + fieldHeight + groupDropdownHeight;
            }
            
            for (int i = 0; i < GROUP_OPTIONS.length; i++) {
                float itemY = adjustedDropdownY - (i + 1) * dropdownItemHeight;
                
                if (worldMouseX >= fieldX && worldMouseX <= fieldX + fieldWidth &&
                    worldMouseY >= itemY && worldMouseY <= itemY + dropdownItemHeight) {
                    
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 0.8f);
                    }
                    
                    if (i == GROUP_OPTIONS.length - 1) {
                        // "Other" selected
                        groupOtherMode = true;
                        selectedGroupIndex = -1;
                        groupNumber = "";
                        activeField = 2;
                    } else {
                        groupOtherMode = false;
                        selectedGroupIndex = i;
                        groupNumber = GROUP_OPTIONS[i];
                    }
                    
                    groupDropdownOpen = false;
                    return;
                }
            }
            
            // Clicked outside dropdown - close it
            groupDropdownOpen = false;
            return;
        }
        
        // Handle field clicks
        for (int i = 0; i < 3; i++) {
            if (worldMouseX >= fieldXPositions[i] && worldMouseX <= fieldXPositions[i] + fieldWidth &&
                worldMouseY >= fieldYPositions[i] && worldMouseY <= fieldYPositions[i] + fieldHeight) {
                
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.5f);
                }
                
                if (i == 1 && !classOtherMode) {
                    // Class field - toggle dropdown
                    classDropdownOpen = !classDropdownOpen;
                    groupDropdownOpen = false;
                    activeField = 1;
                } else if (i == 2 && !groupOtherMode) {
                    // Group field - toggle dropdown
                    groupDropdownOpen = !groupDropdownOpen;
                    classDropdownOpen = false;
                    activeField = 2;
                } else {
                    // Text input field
                    activeField = i;
                    classDropdownOpen = false;
                    groupDropdownOpen = false;
                }
                
                cursorBlinkTimer = 0f;
                cursorVisible = true;
                return;
            }
        }
    }
    
    private void updateCursor() {
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        boolean hoveringField = false;
        for (int i = 0; i < 3; i++) {
            if (worldMouseX >= fieldXPositions[i] && worldMouseX <= fieldXPositions[i] + fieldWidth &&
                worldMouseY >= fieldYPositions[i] && worldMouseY <= fieldYPositions[i] + fieldHeight) {
                hoveringField = true;
                break;
            }
        }
        
        if (hoveringField != isHoveringField) {
            isHoveringField = hoveringField;
            if (hoveringField && cursorHover != null) {
                Gdx.graphics.setCursor(cursorHover);
            } else if (!isHoveringButton && cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            }
        }
    }
    
    private void handleTextInput() {
        // Close dropdowns on Escape
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (classDropdownOpen || groupDropdownOpen) {
                classDropdownOpen = false;
                groupDropdownOpen = false;
                return;
            }
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 2.5f);
            }
            GameApp.switchScreen("menu");
            return;
        }
        
        // Don't process text input when dropdown is open
        if (classDropdownOpen || groupDropdownOpen) {
            return;
        }
        
        // Tab to switch fields
        if (GameApp.isKeyJustPressed(Input.Keys.TAB)) {
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || 
                           Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            if (shift) {
                activeField = (activeField + 2) % 3;
            } else {
                activeField = (activeField + 1) % 3;
            }
            cursorBlinkTimer = 0f;
            cursorVisible = true;
            return;
        }
        
        // Arrow navigation
        if (GameApp.isKeyJustPressed(Input.Keys.UP)) {
            activeField = (activeField + 2) % 3;
            cursorBlinkTimer = 0f;
            cursorVisible = true;
            return;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.DOWN)) {
            activeField = (activeField + 1) % 3;
            cursorBlinkTimer = 0f;
            cursorVisible = true;
            return;
        }
        
        // Enter to start
        if (GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
            tryStartGame();
            return;
        }
        
        // Space to open dropdown (for class/group fields if not in Other mode)
        if (GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            if (activeField == 1 && !classOtherMode) {
                classDropdownOpen = !classDropdownOpen;
                return;
            } else if (activeField == 2 && !groupOtherMode) {
                groupDropdownOpen = !groupDropdownOpen;
                return;
            }
        }
        
        // Only process text input for username, or class/group in Other mode
        boolean canType = (activeField == 0) || 
                         (activeField == 1 && classOtherMode) || 
                         (activeField == 2 && groupOtherMode);
        
        if (!canType) return;
        
        // Backspace
        if (GameApp.isKeyJustPressed(Input.Keys.BACKSPACE) || GameApp.isKeyJustPressed(Input.Keys.DEL)) {
            switch (activeField) {
                case 0:
                    if (username.length() > 0) {
                        username = username.substring(0, username.length() - 1);
                    }
                    break;
                case 1:
                    if (studentClass.length() > 0) {
                        studentClass = studentClass.substring(0, studentClass.length() - 1);
                    }
                    break;
                case 2:
                    if (groupNumber.length() > 0) {
                        groupNumber = groupNumber.substring(0, groupNumber.length() - 1);
                    }
                    break;
            }
            return;
        }
        
        // Character input
        String typed = getTypedCharacter();
        if (typed != null && !typed.isEmpty()) {
            switch (activeField) {
                case 0:
                    if (username.length() < MAX_USERNAME_LENGTH) {
                        username += typed;
                    }
                    break;
                case 1:
                    if (studentClass.length() < MAX_CLASS_LENGTH) {
                        studentClass += typed;
                    }
                    break;
                case 2:
                    // Group number in Other mode - only accept digits
                    if (groupNumber.length() < MAX_GROUP_LENGTH && typed.matches("\\d")) {
                        groupNumber += typed;
                    }
                    break;
            }
        }
    }
    
    private String getTypedCharacter() {
        // Letters A-Z
        for (int i = Input.Keys.A; i <= Input.Keys.Z; i++) {
            if (GameApp.isKeyJustPressed(i)) {
                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || 
                               Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                char c = (char) ('a' + (i - Input.Keys.A));
                if (shift) {
                    c = Character.toUpperCase(c);
                }
                return String.valueOf(c);
            }
        }
        
        // Numbers 0-9
        for (int i = Input.Keys.NUM_0; i <= Input.Keys.NUM_9; i++) {
            if (GameApp.isKeyJustPressed(i)) {
                return String.valueOf((char) ('0' + (i - Input.Keys.NUM_0)));
            }
        }
        
        // Numpad 0-9
        for (int i = Input.Keys.NUMPAD_0; i <= Input.Keys.NUMPAD_9; i++) {
            if (GameApp.isKeyJustPressed(i)) {
                return String.valueOf((char) ('0' + (i - Input.Keys.NUMPAD_0)));
            }
        }
        
        // Space (only for username)
        if (activeField == 0 && GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            return " ";
        }
        
        // Punctuation for username
        if (activeField == 0) {
            if (GameApp.isKeyJustPressed(Input.Keys.MINUS)) return "-";
            if (GameApp.isKeyJustPressed(Input.Keys.PERIOD)) return ".";
        }
        
        return null;
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
            } else if (!isHoveringField) {
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
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    pressedButton = button;
                    button.setPressed(true);
                    
                    if (i == 0) {
                        pendingAction = () -> GameApp.switchScreen("menu");
                    } else if (i == 1) {
                        pendingAction = () -> tryStartGame();
                    }
                    
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
    
    private void tryStartGame() {
        // Validate
        if (username.trim().isEmpty()) {
            showError("Please enter your username!");
            activeField = 0;
            return;
        }
        if (studentClass.trim().isEmpty()) {
            showError("Please select or enter your class!");
            activeField = 1;
            return;
        }
        if (groupNumber.trim().isEmpty()) {
            showError("Please select or enter your group!");
            activeField = 2;
            return;
        }
        
        // Validate group number if in Other mode
        if (groupOtherMode) {
            try {
                int groupNum = Integer.parseInt(groupNumber.trim());
                if (groupNum < 0 || groupNum > 100) {
                    showError("Group number must be 0-100!");
                    activeField = 2;
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Invalid group number!");
                activeField = 2;
                return;
            }
        }
        
        // Create player data
        PlayerData player = new PlayerData(
            username.trim(),
            studentClass.trim().toUpperCase(),
            groupNumber.trim()
        );
        PlayerData.setCurrentPlayer(player);
        
        DebugLogger.log("Player registered: " + player.toString());
        
        if (soundManager != null) {
            soundManager.playSound("levelup", 0.8f);
        }
        
        showSuccess = true;
        successTimer = 0f;
    }
    
    private void showError(String message) {
        errorMessage = message;
        errorTimer = 3.0f;
    }
    
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
