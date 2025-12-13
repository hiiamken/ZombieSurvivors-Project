package nl.saxion.game.config;

import com.badlogic.gdx.Input;
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

    public GameConfig() {}

    public static GameConfig createDefault() {
        GameConfig cfg = new GameConfig();

        cfg.masterVolume = 1.0f;
        cfg.musicVolume = 0.8f;
        cfg.sfxVolume = 0.8f;

        cfg.keyMoveUp = Input.Keys.W;
        cfg.keyMoveDown = Input.Keys.S;
        cfg.keyMoveLeft = Input.Keys.A;
        cfg.keyMoveRight = Input.Keys.D;
        cfg.keyShoot = Input.Keys.SPACE;

        return cfg;
    }

    public void validate() {
        // Clamp volumes
        masterVolume = GameApp.clamp(masterVolume, 0f, 1f);
        musicVolume = GameApp.clamp(musicVolume, 0f, 1f);
        sfxVolume = GameApp.clamp(sfxVolume, 0f, 1f);

        // Validate keys (fallback to defaults if invalid)
        if (keyMoveUp == Input.Keys.UNKNOWN) keyMoveUp = Input.Keys.W;
        if (keyMoveDown == Input.Keys.UNKNOWN) keyMoveDown = Input.Keys.S;
        if (keyMoveLeft == Input.Keys.UNKNOWN) keyMoveLeft = Input.Keys.A;
        if (keyMoveRight == Input.Keys.UNKNOWN) keyMoveRight = Input.Keys.D;
        if (keyShoot == Input.Keys.UNKNOWN) keyShoot = Input.Keys.SPACE;
    }
}
