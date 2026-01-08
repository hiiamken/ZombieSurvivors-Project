package nl.saxion.game;

import nl.saxion.game.screens.MainMenuScreen;
import nl.saxion.game.screens.PlayScreen;
import nl.saxion.game.screens.SettingsScreen;
import nl.saxion.game.screens.CreditsScreen;
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
        GameApp.addScreen("credits", new CreditsScreen());
        // GameOverScreen is no longer needed - game over is now an overlay on PlayScreen


        //heyy!!

        // Start game with main menu

        config = ConfigManager.loadConfig();
        GameApp.log("Loaded config. masterVolume = " + config.masterVolume);

        // Set debug logger enabled state from config
        DebugLogger.setEnabled(config.debugEnabled);
        if (config.debugEnabled) {
            GameApp.log("Debug logging enabled");
        }

        // Window size: 16:9 aspect ratio (HD)
        // Enable resizable window (true) - allows user to resize window with mouse
        GameApp.start(
                "Zombie Survivors",
                1280, 720,
                60,
                true,  // resizable = true - allows window resizing
                "menu"
        );
        
        // Apply fullscreen setting from config after game starts
        if (config.fullscreen) {
            com.badlogic.gdx.Gdx.graphics.setFullscreenMode(com.badlogic.gdx.Gdx.graphics.getDisplayMode());
        } else {
            com.badlogic.gdx.Gdx.graphics.setWindowedMode(1280, 720);
        }

    }
    public static GameConfig getConfig() {
        return config;
    }

}
