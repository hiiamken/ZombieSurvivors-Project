package nl.saxion.game.systems;

import com.badlogic.gdx.Input;
import nl.saxion.game.core.GameState;
import nl.saxion.gameapp.GameApp;

// Handles menu and game over screens
public class GameStateManager {
    private GameState currentState = GameState.MENU;
    private int score = 0;

    public GameState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameState state) {
        currentState = state;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void handleMenuInput(Runnable onStartGame) {
        boolean enterPressed = GameApp.isKeyJustPressed(Input.Keys.ENTER);
        if (enterPressed) {
            onStartGame.run();
            currentState = GameState.PLAYING;
        }
    }

    public void handleGameOverInput(Runnable onRestartGame) {
        boolean rPressed = GameApp.isKeyJustPressed(Input.Keys.R);

        if (rPressed) {
            onRestartGame.run();
            currentState = GameState.PLAYING;
        }
    }

    public void renderMenuScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "ZOMBIE SURVIVORS", 260, 200, "white");
        GameApp.drawText("default", "Press ENTER to start", 220, 260, "white");

        GameApp.endSpriteRendering();
    }

    public void renderGameOverScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "GAME OVER", 280, 220, "white");

        String scoreText = "Score: " + score;
        GameApp.drawText("default", scoreText, 300, 240, "white");

        GameApp.drawText("default", "Press R to restart", 240, 300, "white");

        GameApp.endSpriteRendering();
    }
}

