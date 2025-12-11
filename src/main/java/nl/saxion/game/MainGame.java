package nl.saxion.game;

import nl.saxion.game.screens.MainMenuScreen;
import nl.saxion.game.screens.PlayScreen;
import nl.saxion.game.screens.SettingsScreen;
import nl.saxion.gameapp.GameApp;

public class MainGame {
    public static void main(String[] args) {

        // Register all game screens
        GameApp.addScreen("menu", new MainMenuScreen());
        GameApp.addScreen("play", new PlayScreen());
        GameApp.addScreen("settings", new SettingsScreen());


        //heyy!!

        // Start game with main menu

        GameApp.start(
                "Zombie Survivors",
                1200, 800,
                60,
                false,
                "menu"
        );
    }
}
