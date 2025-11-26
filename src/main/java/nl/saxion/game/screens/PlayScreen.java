package nl.saxion.game.screens;

import nl.saxion.game.systems.InputController;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {
        System.out.println("PlayScreen loaded");

        // Load your player texture
        GameApp.addTexture("player", "assets/player/player.png");

        // Input controller
        input = new InputController();
    }

    @Override
    public void hide() {
        System.out.println("PlayScreen hidden");
        GameApp.disposeTexture("player");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        float moveX = input.getMoveX();
        float moveY = input.getMoveY();

        if (moveX != 0 || moveY != 0) {
            System.out.println("MOVE: " + moveX + ", " + moveY);
        }

        if (input.isShoot()) {
            System.out.println("SHOOT PRESSED");
        }

        GameApp.startSpriteRendering();
        GameApp.drawTexture("player", 300, 250, 32, 32);
        GameApp.endSpriteRendering();
    }
}
