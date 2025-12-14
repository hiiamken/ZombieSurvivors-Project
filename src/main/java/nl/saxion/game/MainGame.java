package nl.saxion.game;

import nl.saxion.game.screens.MainMenuScreen;
import nl.saxion.game.screens.PlayScreen;
import nl.saxion.game.screens.SettingsScreen;
import nl.saxion.gameapp.GameApp;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.utils.DebugLogger;

public class MainGame {

    public static GameConfig config;

    public static void main(String[] args) {

        // Register all game screens
        GameApp.addScreen("menu", new MainMenuScreen());
        GameApp.addScreen("play", new PlayScreen());
        GameApp.addScreen("settings", new SettingsScreen());


        //heyy!!

        // Start game with main menu

        config = ConfigManager.loadConfig();
        GameApp.log("Loaded config. masterVolume = " + config.masterVolume);

        // Set debug logger enabled state from config
        DebugLogger.setEnabled(config.debugEnabled);
        if (config.debugEnabled) {
            GameApp.log("Debug logging enabled");
        }


        GameApp.start(
                "Zombie Survivors",
                1200, 800,
                60,
                false,
                "menu"
        );

    }
    public static GameConfig getConfig() {
        return config;
    }

}
