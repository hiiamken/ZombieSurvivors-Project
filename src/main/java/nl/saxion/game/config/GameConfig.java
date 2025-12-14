package nl.saxion.game.config;

import nl.saxion.gameapp.GameApp;

public class GameConfig {

    public float masterVolume;
    public float musicVolume;
    public float sfxVolume;

    public int keyMoveUp;
    public int keyMoveDown;
    public int keyMoveLeft;
    public int keyMoveRight;
    public int keyShoot;

    public boolean debugEnabled;

    public GameConfig() {}

    public static GameConfig createDefault() {
        GameConfig cfg = new GameConfig();

        cfg.masterVolume = 1.0f;
        cfg.musicVolume = 0.8f;
        cfg.sfxVolume = 0.8f;

        cfg.keyMoveUp = com.badlogic.gdx.Input.Keys.W;
        cfg.keyMoveDown = com.badlogic.gdx.Input.Keys.S;
        cfg.keyMoveLeft = com.badlogic.gdx.Input.Keys.A;
        cfg.keyMoveRight = com.badlogic.gdx.Input.Keys.D;
        cfg.keyShoot = com.badlogic.gdx.Input.Keys.SPACE;

        cfg.debugEnabled = false; // Debug disabled by default

        return cfg;
    }

    public void validate() {
        masterVolume = GameApp.clamp(masterVolume, 0f, 1f);
        musicVolume = GameApp.clamp(musicVolume, 0f, 1f);
        sfxVolume = GameApp.clamp(sfxVolume, 0f, 1f);
    }
}
