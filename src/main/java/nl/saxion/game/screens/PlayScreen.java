package nl.saxion.game.screens;

import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.systems.InputController;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import nl.saxion.game.entities.Player;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    // 🔵 DANIEL – future Player object (Task 3 & 4)
    private Player player;


    // 🟢 ARNOLD – TEMP player data so bullets can be tested

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
        float startX = 300;
        float startY = 250;
        float speed = 200f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);


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

        // Read input
        float moveX = input.getMoveX();
        float moveY = input.getMoveY();

        if (moveX != 0 || moveY != 0) {
            System.out.println("MOVE: " + moveX + ", " + moveY);
        }

        // TEMP player movement (will be replaced by Daniel’s Player)

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        player.update(delta, input, (int)worldW, (int)worldH);


        // Update weapon cooldown
        weapon.update(delta);

        // shooting
        // Hold SPACE -> isShootHeld()
        if (input.isShootHeld() && weapon.canFire()) {
            System.out.println("FIRE!");

            float dirX = 0;
            float dirY = 1; // up

            float playerX = player.getX();
            float playerY = player.getY();
            float playerWidth = Player.SPRITE_SIZE;
            float playerHeight = Player.SPRITE_SIZE;

            float bulletStartX = playerX + playerWidth / 2f - 4;
            float bulletStartY = playerY + playerHeight / 2f;


            // damage comes from Weapon
            bullets.add(new Bullet(bulletStartX, bulletStartY, dirX, dirY, weapon.getDamage()));

            // start cooldown
            weapon.onFire();
        }

        // Update bullets & remove off-screen
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(delta);

            if (b.isOffScreen()) {
                it.remove();
            }
        }

        // Base rendering, extended by Arnold
        GameApp.startSpriteRendering();

        // draw temp player
        player.render();

        // draw bullets
        for (Bullet b : bullets) {
            GameApp.drawTexture("bullet", b.getX(), b.getY(), 8, 8);
        }

        GameApp.endSpriteRendering();
    }

    // Later will add getPlayerStatus() etc. here
}
