package nl.saxion.game.screens;

import com.badlogic.gdx.Input;
import nl.saxion.game.ui.Button;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;

// Main menu screen with background image and interactive buttons
public class MainMenuScreen extends ScalableGameScreen {

    // Background image dimensions (original image size: 1200x800)
    private static final float BG_WIDTH = 1200f;
    private static final float BG_HEIGHT = 800f;

    // Button dimensions matching the image buttons
    private static final float BUTTON_WIDTH = 180f;
    private static final float BUTTON_HEIGHT = 42f;

    private List<Button> buttons;
    private int selectedButtonIndex = 0;

    // Track if resources are loaded
    private boolean resourcesLoaded = false;

    public MainMenuScreen() {
        // Match the screen size to the background image
        super(1200, 800);
    }

    @Override
    public void show() {
        loadResources();
        createButtons();
        updateButtonSelection();
    }

    private void loadResources() {
        if (!resourcesLoaded) {
            // Load menu background
            if (!GameApp.hasTexture("menu_background")) {
                GameApp.addTexture("menu_background", "assets/ui/menu_background.png");
            }

            // Add custom font for menu if needed
            if (!GameApp.hasFont("menu_font")) {
                GameApp.addFont("menu_font", "fonts/basic.ttf", 24);
            }

            // Add custom colors for menu
            if (!GameApp.hasColor("menu_yellow")) {
                GameApp.addColor("menu_yellow", 255, 255, 0);
            }
            if (!GameApp.hasColor("menu_green")) {
                GameApp.addColor("menu_green", 50, 205, 50);
            }

            resourcesLoaded = true;
        }
    }

    private void createButtons() {
        buttons = new ArrayList<>();

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Scale factors to adjust button positions based on actual screen size
        float scaleX = screenWidth / BG_WIDTH;
        float scaleY = screenHeight / BG_HEIGHT;

        // Button positions in the original image (measured from top-left)
        // The buttons are roughly centered horizontally and positioned in the middle-right area
        // START GAME: center around x=700, y=380
        // SETTINGS: center around x=700, y=455
        // QUIT: center around x=700, y=530

        float buttonCenterX = 700 * scaleX;
        float buttonX = buttonCenterX - (BUTTON_WIDTH * scaleX) / 2;

        float startY = 365 * scaleY;
        float spacing = 75 * scaleY;
        float btnWidth = BUTTON_WIDTH * scaleX;
        float btnHeight = BUTTON_HEIGHT * scaleY;

        // START GAME button - invisible hitbox over the image button
        Button startButton = new Button(buttonX, startY, btnWidth, btnHeight, "");
        startButton.setOnClick(() -> {
            GameApp.log("Starting game...");
            GameApp.switchScreen("play");
        });
        startButton.setVisible(false); // Don't draw, just use for click detection
        buttons.add(startButton);

        // SETTINGS button
        Button settingsButton = new Button(buttonX, startY + spacing, btnWidth, btnHeight, "");
        settingsButton.setOnClick(() -> {
            GameApp.log("Opening settings...");
            GameApp.switchScreen("settings");
        });
        settingsButton.setVisible(false);
        buttons.add(settingsButton);

        // QUIT button
        Button quitButton = new Button(buttonX, startY + spacing * 2, btnWidth, btnHeight, "");
        quitButton.setOnClick(() -> {
            GameApp.log("Quitting game...");
            GameApp.quit();
        });
        quitButton.setVisible(false);
        buttons.add(quitButton);
    }

    @Override
    public void hide() {
        // Keep resources loaded for quick return to menu
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // Clear screen
        GameApp.clearScreen("black");

        // Get mouse position
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();

        // Handle input
        handleInput(mouseX, mouseY);

        // Update buttons
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).update(mouseX, mouseY);

            // Update selection based on hover
            if (buttons.get(i).isHovered()) {
                selectedButtonIndex = i;
                updateButtonSelection();
            }
        }

        // Draw background
        GameApp.startSpriteRendering();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        GameApp.drawTexture("menu_background", 0, 0, screenWidth, screenHeight);
        GameApp.endSpriteRendering();

        // Draw hover highlights on buttons
        drawButtonHighlights();

        // Draw selection indicator
        drawSelectionIndicator();
    }

    private void handleInput(float mouseX, float mouseY) {
        // Keyboard navigation
        if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
            selectedButtonIndex--;
            if (selectedButtonIndex < 0) {
                selectedButtonIndex = buttons.size() - 1;
            }
            updateButtonSelection();
        }

        if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
            selectedButtonIndex++;
            if (selectedButtonIndex >= buttons.size()) {
                selectedButtonIndex = 0;
            }
            updateButtonSelection();
        }

        // Enter to activate selected button
        if (GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)) {
            buttons.get(selectedButtonIndex).click();
        }

        // Mouse click
        if (GameApp.isButtonJustPressed(0)) {
            for (Button button : buttons) {
                if (button.containsPoint(mouseX, mouseY)) {
                    button.click();
                    break;
                }
            }
        }
    }

    private void updateButtonSelection() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setSelected(i == selectedButtonIndex);
        }
    }

    private void drawButtonHighlights() {
        // Draw semi-transparent highlight over hovered/selected buttons
        for (Button button : buttons) {
            button.renderHoverEffect(60);
        }
    }

    private void drawSelectionIndicator() {
        // Draw arrow indicator next to selected button
        Button selected = buttons.get(selectedButtonIndex);

        float screenWidth = GameApp.getWorldWidth();
        float scaleX = screenWidth / BG_WIDTH;

        GameApp.startSpriteRendering();
        float arrowX = selected.getX() - 25 * scaleX;
        float arrowY = selected.getY() + selected.getHeight() / 2;

        // Draw arrow indicator with custom yellow color
        GameApp.drawTextCentered("default", ">>", arrowX, arrowY, "menu_yellow");
        GameApp.endSpriteRendering();
    }
}

