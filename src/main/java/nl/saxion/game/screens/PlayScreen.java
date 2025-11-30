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

    private Player player;

    private List<Bullet> bullets;

    private Weapon weapon;

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {

        System.out.println("PlayScreen loaded");
        GameApp.addTexture("player", "assets/player/player.png");
        input = new InputController();
        float startX = 300;
        float startY = 250;
        float speed = 200f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");
        bullets = new ArrayList<>();

        // 3 shots per second, 10 damage
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 5.0f, 10);
    }

    @Override
    public void hide() {

        System.out.println("PlayScreen hidden");
        GameApp.disposeTexture("player");

        GameApp.disposeTexture("bullet");
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // --- UPDATE ---
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        player.update(delta, input, (int) worldW, (int) worldH);
        weapon.update(delta);

        // Shooting – Weapon handles direction + spawn position
        if (input.isShootHeld()) {
            Bullet newBullet = weapon.tryFire(player);
            if (newBullet != null) {
                bullets.add(newBullet);
            }
        }

        // Update bullets and remove off-screen ones
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(delta);

            if (b.isOffScreen()) {
                it.remove();
            }
        }
        // --- RENDER ---
        GameApp.startSpriteRendering();

        player.render();

        for (Bullet b : bullets) {
            b.render();
        }

        GameApp.endSpriteRendering();
    }

    // Later: getPlayerStatus(), HUD, etc.
}
