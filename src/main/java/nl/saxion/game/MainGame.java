 package nl.saxion.game;

import nl.saxion.game.screens.PlayScreen;
import nl.saxion.gameapp.GameApp;

public class MainGame {
    public static void main(String[] args) {

        GameApp.addScreen("play", new PlayScreen());

        //heyy!!
        GameApp.start(
                "Zombie Survivors",
                1980, 920,
                60,
                false,
                "play"
        );
    }
}
