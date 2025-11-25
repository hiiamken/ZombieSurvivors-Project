package nl.saxion.game.yourgamename;

import com.badlogic.gdx.Input;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

public class YourGameScreen extends ScalableGameScreen {
    String randomBoxColor = "violet-500";
    String randomBackgroundColor = "black";
    Box centeredBox = new Box();

    public YourGameScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        randomBoxColor = getRandomColor();
        centeredBox.width = 400;
        centeredBox.height = 400;
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // Calculate where the box would be (we draw it in the center of the world)
        float boxX = getWorldWidth() / 2 - centeredBox.width / 2;
        float boxY = getWorldHeight() / 2 - centeredBox.height / 2;

        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mouseX = getMouseX();
            float mouseY = getMouseY();

            if (mouseX > boxX && mouseX < boxX + centeredBox.width && mouseY > boxY && mouseY < boxY + centeredBox.height) {
                // If we pressed the box, then change the color of the box
                randomBoxColor = getRandomColor();
            } else {
                // Otherwise change the color of the background
                randomBackgroundColor = getRandomColor();
            }
        }

        // Draw elements
        GameApp.clearScreen(randomBackgroundColor);
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(boxX, boxY, centeredBox.width, centeredBox.height, randomBoxColor);
        GameApp.endShapeRendering();

    }

    private String getRandomColor() {
        int randomIndex = (int)GameApp.random(0, GameApp.getAllColors().length-1);
        return GameApp.getAllColors()[randomIndex];
    }

    @Override
    public void hide() {

    }
}
