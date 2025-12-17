package nl.saxion.game.systems;

import nl.saxion.game.core.GameState;
import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;

import java.util.ArrayList;
import java.util.List;

// Handles menu and game over screens
public class GameStateManager {
    private GameState currentState = GameState.MENU;
    private int score = 0;
    private float gameOverFadeTimer = 0f;
    private static final float FADE_DURATION = 1.0f; // 1 second fade

    // Game over buttons
    private List<Button> gameOverButtons;
    private boolean buttonsInitialized = false;
    private float colorPulseTimer = 0f;

    // Getter for game over buttons (for cursor switching)
    public List<Button> getGameOverButtons() {
        return gameOverButtons;
    }

    // Delay for button press animation
    private float pressDelay = 0.15f; // 150ms delay
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState state) {
        currentState = state;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void resetGameOverFade() {
        gameOverFadeTimer = 0f;
    }

    public void updateGameOverFade(float delta) {
        if (currentState == GameState.GAME_OVER) {
            if (gameOverFadeTimer < FADE_DURATION) {
                gameOverFadeTimer += delta;
            }
            // Update color pulse animation - slower and smoother
            colorPulseTimer += delta * 0.2f; // Very slow, smooth pulse

            // Update press delay timer
            if (pendingAction != null && pressedButton != null) {
                pressTimer += delta;
                if (pressTimer >= pressDelay) {
                    // Delay finished, execute action
                    Runnable action = pendingAction;
                    pendingAction = null;
                    pressedButton = null;
                    pressTimer = 0f;
                    action.run();
                }
            }
        }
    }

    public void initializeGameOverButtons(Runnable onPlayAgain, Runnable onBackToMenu) {
        if (buttonsInitialized) return;

        gameOverButtons = new ArrayList<>();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;

        float buttonWidth = 160f; // Smaller to prevent image stretching
        float buttonHeight = 30f; // Smaller height
        float buttonSpacing = 20f; // Spacing between buttons

        // Play Again button (green) - positioned lower on screen
        float playAgainY = centerY - 80; // Better vertical positioning
        Button playAgainButton = new Button(centerX - buttonWidth / 2, playAgainY, buttonWidth, buttonHeight, "");
        playAgainButton.setOnClick(onPlayAgain);
        if (GameApp.hasTexture("green_long")) {
            playAgainButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        }
        gameOverButtons.add(playAgainButton);

        // Back to Menu button (red) - positioned below Play Again
        float backToMenuY = playAgainY - buttonHeight - buttonSpacing;
        Button backToMenuButton = new Button(centerX - buttonWidth / 2, backToMenuY, buttonWidth, buttonHeight, "");
        backToMenuButton.setOnClick(onBackToMenu);
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

    public void handleGameOverInput(float mouseX, float mouseY, Runnable onPlayAgain, Runnable onBackToMenu) {
        if (gameOverButtons == null || gameOverButtons.isEmpty()) return;

        // Only update buttons if not waiting for delay
        if (pendingAction == null) {
            // Update buttons with mouse position
            for (Button button : gameOverButtons) {
                button.update(mouseX, mouseY);
            }
        } else {
            // Keep button pressed during delay
            if (pressedButton != null) {
                pressedButton.setPressed(true);
            }
        }

        // Handle mouse click (only if not waiting for delay)
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed && pendingAction == null) {
            for (int i = 0; i < gameOverButtons.size(); i++) {
                Button button = gameOverButtons.get(i);
                if (button.containsPoint(mouseX, mouseY)) {
                    String buttonName = i == 0 ? "PLAY AGAIN" : "BACK TO MENU";
                    DebugLogger.log("%s button clicked! Starting press delay...", buttonName);

                    // Store button and action for delayed execution
                    pressedButton = button;
                    button.setPressed(true);

                    // Create delayed action based on button
                    if (i == 0) {
                        // Play Again button
                        pendingAction = () -> {
                            DebugLogger.log("PLAY AGAIN action executing after delay");
                            onPlayAgain.run();
                            currentState = GameState.PLAYING;
                        };
                    } else if (i == 1) {
                        // Back to Menu button
                        pendingAction = () -> {
                            DebugLogger.log("BACK TO MENU action executing after delay");
                            onBackToMenu.run();
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
                button.setPressed(isMouseDown && button.containsPoint(mouseX, mouseY));
            }
        }
    }

    public void renderGameOverScreen() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;

        // Calculate fade alpha (0 to 1)
        float fadeAlpha = Math.min(gameOverFadeTimer / FADE_DURATION, 1.0f);

        // Draw animated gradient background with color pulse
        drawGameOverBackground(screenWidth, screenHeight, fadeAlpha);

        // Render buttons first (they manage their own sprite batch)
        if (gameOverButtons != null) {
            for (Button button : gameOverButtons) {
                button.render();
            }
        }

        // Now render text in sprite batch
        GameApp.startSpriteRendering();

        // Draw "GAME OVER" title - centered as single text
        float titleY = centerY + 120; // Better positioning
        GameApp.drawTextCentered("gameOverTitle", "GAME OVER", centerX, titleY, "red-500");

        // Draw score centered below title with better formatting
        String scoreText = String.format("SCORE: %,d", score); // Format with commas
        float titleHeight = GameApp.getTextHeight("gameOverTitle", "GAME OVER");
        float scoreTextHeight = GameApp.getTextHeight("gameOverText", scoreText);
        float scoreY = titleY - titleHeight / 2 - scoreTextHeight * 2.2f; // Better spacing
        GameApp.drawTextCentered("gameOverText", scoreText, centerX, scoreY, "white");

        // Draw button text labels
        drawGameOverButtonText(centerX, centerY);

        GameApp.endSpriteRendering();
    }

    private void drawGameOverButtonText(float centerX, float centerY) {
        if (gameOverButtons == null || gameOverButtons.size() < 2) return;

        // Play Again button text - perfectly centered
        Button playAgainButton = gameOverButtons.get(0);
        float playAgainCenterX = playAgainButton.getX() + playAgainButton.getWidth() / 2;
        float playAgainCenterY = playAgainButton.getY() + playAgainButton.getHeight() / 2;
        GameApp.drawTextCentered("gameOverButtonFont", "PLAY AGAIN", playAgainCenterX, playAgainCenterY, "white");

        // Back to Menu button text - perfectly centered
        Button backToMenuButton = gameOverButtons.get(1);
        float backToMenuCenterX = backToMenuButton.getX() + backToMenuButton.getWidth() / 2;
        float backToMenuCenterY = backToMenuButton.getY() + backToMenuButton.getHeight() / 2;
        GameApp.drawTextCentered("gameOverButtonFont", "BACK TO MENU", backToMenuCenterX, backToMenuCenterY, "white");
    }

    // Draw smooth, eye-friendly gradient background with subtle animation
    private void drawGameOverBackground(float width, float height, float alpha) {
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
}

