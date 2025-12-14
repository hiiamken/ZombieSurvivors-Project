package nl.saxion.game.screens;

import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.List;

// Simple main menu with gradient background and 3 buttons (PLAY, OPTIONS, QUIT)
public class MainMenuScreen extends ScalableGameScreen {

    private List<Button> buttons;
    private boolean resourcesLoaded = false;

    // Button dimensions (will be set based on sprite size)
    private float buttonWidth = 280f; // Increased width for better appearance
    private float buttonHeight = 50f;
    private float buttonSpacing = 20f;

    // Delay for button press animation
    private float pressDelay = 0.15f; // 150ms delay
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;

    public MainMenuScreen() {
        super(1200, 800);
    }

    @Override
    public void show() {
        // Initialize debug logger
        DebugLogger.log("MainMenuScreen.show() called");

        // Show cursor for mouse interaction
        GameApp.showCursor();
        DebugLogger.log("Cursor shown");

        loadResources();
        createButtons();

        DebugLogger.log("MainMenuScreen initialized with %d buttons", buttons.size());
    }

    private void loadResources() {
        if (!resourcesLoaded) {
            DebugLogger.log("Loading fonts...");

            // Load PressStart2P font for button text (pixel-perfect for UI)
            // Use darker base color and adjust styling for better visibility
            GameApp.addStyledFont("buttonFont", "fonts/PressStart2P-Regular.ttf", 20,
                    "gray-200", 2f, "black", 2, 2, "gray-600", true);
            DebugLogger.log("Loaded buttonFont: PressStart2P-Regular.ttf");

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

            resourcesLoaded = true;
            DebugLogger.log("Resources loaded");
        }
    }

    private void createButtons() {
        buttons = new ArrayList<>();
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
        // Dispose font
        GameApp.disposeFont("buttonFont");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

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

        // Draw gradient background (purplish-blue like in image)
        drawGradientBackground();

        // Get mouse position
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();

        // Convert mouse Y from window coordinate (bottom-left origin) to world coordinate (top-left origin)
        float screenHeight = GameApp.getWorldHeight();
        float worldMouseY = screenHeight - mouseY;

        // Handle input with converted coordinates (only if not waiting for delay)
        if (pendingAction == null) {
            handleInput(mouseX, worldMouseY);
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
                button.update(mouseX, worldMouseY);
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
            if (i == 0) {
                text = "PLAY";
            } else if (i == 1) {
                text = "SETTINGS";
            } else {
                text = "QUIT";
            }

            // Calculate center of button
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;

            // Adjust vertical position slightly down for better visual balance
            // Account for font baseline and border
            float textHeight = GameApp.getTextHeight("buttonFont", text);
            float adjustedY = buttonCenterY - textHeight * 0.1f; // Slight offset down

            // Draw text centered on button with darker color
            GameApp.drawTextCentered("buttonFont", text, buttonCenterX, adjustedY, "gray-200");
        }

        GameApp.endSpriteRendering();
    }

    // Draw gradient background (purplish-blue #333146 with subtle gradient)
    private void drawGradientBackground() {
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

    private void handleInput(float mouseX, float mouseY) {
        // Only mouse interaction - no keyboard navigation
        boolean isMouseDown = GameApp.isButtonPressed(0);
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);

        // Debug mouse position every 60 frames (1 second at 60fps)
        if (System.currentTimeMillis() % 1000 < 16) {
            DebugLogger.log("Mouse position (world): (%.1f, %.1f), isMouseDown: %s, isMouseJustPressed: %s",
                    mouseX, mouseY, isMouseDown, isMouseJustPressed);
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
}
