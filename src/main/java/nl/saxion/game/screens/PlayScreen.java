package nl.saxion.game.screens;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.systems.InputController;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    public PlayScreen() {
        super(800, 600);
    }

    private float playerX = 300;
    private float playerY = 250;
    private final float playerWidth = 32;
    private final float playerHeight = 32;
    private float playerSpeed = 200f;

    // (Task 5) – bullet list
    private List<Bullet> bullets;


    @Override
    public void show() {
        System.out.println("PlayScreen loaded");

        // Load your player texture
        GameApp.addTexture("player", "assets/player/player.png");

        // Input controller
        input = new InputController();

        // ARNOLD – bullet texture + list
        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");
        bullets = new ArrayList<>();

    }

    @Override
    public void hide() {
        System.out.println("PlayScreen hidden");
        GameApp.disposeTexture("player");

        // ARNOLD
        GameApp.disposeTexture("bullet");
    }
     //player





    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        float moveX = input.getMoveX();
        float moveY = input.getMoveY();

        if (moveX != 0 || moveY != 0) {
            System.out.println("MOVE: " + moveX + ", " + moveY);
        }

        // ARNOLD – TEMP player movement
        playerX += moveX * playerSpeed * delta;
        playerY += moveY * playerSpeed * delta;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        if (playerX < 0) playerX = 0;
        if (playerY < 0) playerY = 0;
        if (playerX > worldW - playerWidth) playerX = worldW - playerWidth;
        if (playerY > worldH - playerHeight) playerY = worldH - playerHeight;

        if (input.isShoot()) {
            System.out.println("SHOOT PRESSED");
            float dirX = 0;
            float dirY = 1; // up

            float bulletStartX = playerX + playerWidth / 2f - 4; // 8x8 bullet
            float bulletStartY = playerY + playerHeight / 2f;

            bullets.add(new Bullet(bulletStartX, bulletStartY, dirX, dirY));
        }

        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(delta);
            if (b.isOffScreen()) {
                it.remove();

                GameApp.startSpriteRendering();
                GameApp.drawTexture("player", 300, 250, 32, 32);
                GameApp.endSpriteRendering();
            }


        }
    }}