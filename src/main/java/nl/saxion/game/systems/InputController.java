package nl.saxion.game.systems;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import nl.saxion.game.config.GameConfig;
import nl.saxion.gameapp.GameApp;

public class InputController {

    private GameConfig config;

    public InputController(GameConfig config) {
        this.config = config;
    }

    // THUONG – movement input (Task 2)
    public boolean isMoveUp() {
        return GameApp.isKeyPressed(config.keyMoveUp);
    }

    public boolean isMoveDown() {
        return GameApp.isKeyPressed(config.keyMoveDown);
    }

    public boolean isMoveLeft() {
        return GameApp.isKeyPressed(config.keyMoveLeft);
    }

    public boolean isMoveRight() {
        return GameApp.isKeyPressed(config.keyMoveRight);
    }

    // THUONG – original shoot (just pressed once)
    public boolean isShoot() {
        return GameApp.isKeyJustPressed(config.keyShoot);
    }

    // ARNOLD (Task 6) – shoot HELD (for fire-rate weapon system)
    public boolean isShootHeld() {
        return GameApp.isKeyPressed(Input.Keys.SPACE);
    }

    // THUONG – movement as axes
    public float getMoveX() {
        float x = 0;
        if (isMoveRight()) x += 1;
        if (isMoveLeft()) x -= 1;
        return x;
    }

    public float getMoveY() {
        float y = 0;
        if (isMoveUp()) y += 1;
        if (isMoveDown()) y -= 1;
        return y;
    }
}
