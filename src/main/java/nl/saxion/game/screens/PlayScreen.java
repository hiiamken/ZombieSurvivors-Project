package nl.saxion.game.screens;

import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class PlayScreen extends ScalableGameScreen {

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {
        System.out.println("PlayScreen loaded");

        // Load your player texture
        GameApp.addTexture("player", "assets/player/player.png");
    }

    @Override
    public void hide() {
        System.out.println("PlayScreen hidden");

        // Dispose to free memory
        GameApp.disposeTexture("player");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");


        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(100, 100, 200, 100, "blue-500");
        GameApp.endShapeRendering();


        GameApp.startSpriteRendering();
        GameApp.drawTexture("player", 300, 250, 64, 64); // scaled to 64x64
        GameApp.endSpriteRendering();
    }
}
