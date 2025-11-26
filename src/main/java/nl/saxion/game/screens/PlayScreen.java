package nl.saxion.game.screens;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.systems.InputController;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    // 🔵 DANIEL – future Player object (Task 3 & 4)
    // private Player player;

    // 🟢 ARNOLD – TEMP player data so bullets can be tested
    private float playerX = 300;
    private float playerY = 250;
    private final float playerWidth = 32;
    private final float playerHeight = 32;
    private float playerSpeed = 200f;

    // 🟢 ARNOLD – bullets
    private List<Bullet> bullets;

    // 🟢 ARNOLD – Task 6: Weapon for player
    private Weapon weapon;


    // 🔵 THUONG – base constructor
    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {

        // 🔵 THUONG – original setup
        System.out.println("PlayScreen loaded");
        GameApp.addTexture("player", "assets/player/player.png");
        input = new InputController();

        // 🟢 ARNOLD – bullet texture + list
        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");
        bullets = new ArrayList<>();

        // 🟢 ARNOLD – Task 6: base weapon (pistol)
        // 3 shots per second, 10 damage
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 10.0f, 10);
    }

    @Override
    public void hide() {

        // 🔵 THUONG
        System.out.println("PlayScreen hidden");
        GameApp.disposeTexture("player");

        // 🟢 ARNOLD
        GameApp.disposeTexture("bullet");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // 🔵 THUONG – read input
        float moveX = input.getMoveX();
        float moveY = input.getMoveY();

        if (moveX != 0 || moveY != 0) {
            System.out.println("MOVE: " + moveX + ", " + moveY);
        }

        // 🟢 ARNOLD – TEMP player movement (will be replaced by Daniel’s Player)
        playerX += moveX * playerSpeed * delta;
        playerY += moveY * playerSpeed * delta;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        if (playerX < 0) playerX = 0;
        if (playerY < 0) playerY = 0;
        if (playerX > worldW - playerWidth)  playerX = worldW - playerWidth;
        if (playerY > worldH - playerHeight) playerY = worldH - playerHeight;

        // 🟢 ARNOLD – Task 6: update weapon cooldown
        weapon.update(delta);

        // 🟢 ARNOLD – weapon-controlled shooting
        // Hold SPACE -> isShootHeld()
        if (input.isShootHeld() && weapon.canFire()) {
            System.out.println("FIRE!");

            float dirX = 0;
            float dirY = 1; // up

            float bulletStartX = playerX + playerWidth / 2f - 4;
            float bulletStartY = playerY + playerHeight / 2f;

            // damage comes from Weapon
            bullets.add(new Bullet(bulletStartX, bulletStartY, dirX, dirY, weapon.getDamage()));

            // start cooldown
            weapon.onFire();
        }

        // 🟢 ARNOLD – update bullets & remove off-screen
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(delta);

            if (b.isOffScreen()) {
                it.remove();
            }
        }

        // 🔵 THUONG – base rendering, extended by Arnold
        GameApp.startSpriteRendering();

        // draw temp player
        GameApp.drawTexture("player", playerX, playerY, playerWidth, playerHeight);

        // draw bullets
        for (Bullet b : bullets) {
            GameApp.drawTexture("bullet", b.getX(), b.getY(), 8, 8);
        }

        GameApp.endSpriteRendering();
    }

    // 🔵 DANIEL – later will add getPlayerStatus() etc. here
}
