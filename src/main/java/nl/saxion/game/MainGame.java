 package nl.saxion.game;

import nl.saxion.game.screens.PlayScreen;
import nl.saxion.gameapp.GameApp;

public class MainGame {
    public static void main(String[] args) {

        GameApp.addScreen("play", new PlayScreen());

        GameApp.start(
                "Zombie Survivors",
                800, 600,
                60,
                false,
                "play"
        );
    }
}
