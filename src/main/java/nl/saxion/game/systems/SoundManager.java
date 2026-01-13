package nl.saxion.game.systems;

import com.badlogic.gdx.audio.Music;
import nl.saxion.gameapp.GameApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all game audio (sound effects and background music).
 * Uses GameApp audio methods for loading and playback.
 */
public class SoundManager {
    // Sound effect keys mapping
    private Map<String, String> soundKeys;
    
    // Music keys
    private static final String MENU_MUSIC_KEY = "bgm";  // Background music for menus
    private static final String INGAME_MUSIC_KEY = "ingame_music";  // Background music for gameplay
    
    // Volume settings
    private float masterVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private float musicVolume = 0.7f;
    
    // Track if music is currently playing
    private boolean isMenuMusicPlaying = false;
    private boolean isIngameMusicPlaying = false;
    
    public SoundManager() {
        soundKeys = new HashMap<>();
    }
    
    /**
     * Load all sound effects and background music.
     * Should be called during game initialization.
     */
    public void loadAllSounds() {
        // Load sound effects
        loadSound("clickbutton", "audio/clickbutton.mp3");
        loadSound("pickupitem", "audio/pickupitem.mp3");
        loadSound("shooting", "audio/shooting.mp3");
        loadSound("levelup", "audio/levelup.mp3");
        loadSound("damaged", "audio/damaged.mp3");
        loadSound("gameover", "audio/gameover.mp3");
        loadSound("jackpot", "audio/jackpot.mp3"); // Gacha jackpot sound
        
        // Load background music for menus (if available)
        try {
            GameApp.addMusic(MENU_MUSIC_KEY, "audio/background.mp3");
            if (GameApp.hasMusic(MENU_MUSIC_KEY)) {
                GameApp.log("Menu background music loaded successfully");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load menu background music: " + e.getMessage());
        }
        
        // Load ingame background music (if available)
        try {
            GameApp.addMusic(INGAME_MUSIC_KEY, "audio/ingame.mp3");
            if (GameApp.hasMusic(INGAME_MUSIC_KEY)) {
                GameApp.log("Ingame background music loaded successfully");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load ingame background music: " + e.getMessage());
        }
        
        GameApp.log("SoundManager: Loaded " + soundKeys.size() + " sound effects");
    }
    
    /**
     * Load a single sound effect.
     * @param name Internal name for the sound
     * @param path Path to the sound file
     */
    private void loadSound(String name, String path) {
        try {
            GameApp.addSound(name, path);
            if (GameApp.hasSound(name)) {
                soundKeys.put(name, name);
                GameApp.log("✅ Loaded sound: " + name + " from " + path);
            } else {
                GameApp.log("❌ Warning: Sound file not found: " + path);
            }
        } catch (Exception e) {
            GameApp.log("❌ Warning: Could not load sound " + name + " from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Play a sound effect with current SFX volume.
     * @param name Sound name (e.g., "shoot", "enemy_death")
     * @param volume Volume multiplier (0.0 - 1.0), will be multiplied by sfxVolume
     */
    public void playSound(String name, float volume) {
        if (!soundKeys.containsKey(name)) {
            // Sound not loaded, skip silently
            GameApp.log("Warning: Sound '" + name + "' not found in soundKeys map. Available sounds: " + soundKeys.keySet());
            return;
        }
        
        if (!GameApp.hasSound(name)) {
            GameApp.log("Warning: Sound '" + name + "' not found in GameApp");
            return;
        }
        
        // Calculate final volume: master * sfx * provided volume
        // Allow volume > 1.0f for louder sounds (will be clamped to 1.0f at the end)
        float finalVolume = masterVolume * sfxVolume * volume;
        finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
        
        try {
            GameApp.playSound(name, finalVolume);
        } catch (Exception e) {
            GameApp.log("Error playing sound " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Play a sound effect with default SFX volume.
     * @param name Sound name
     */
    public void playSound(String name) {
        playSound(name, 1.0f);
    }
    
    /**
     * Play menu background music with looping.
     * @param loop Whether to loop the music
     */
    public void playMusic(boolean loop) {
        playMenuMusic(loop);
    }
    
    /**
     * Play menu background music with looping.
     * @param loop Whether to loop the music
     */
    public void playMenuMusic(boolean loop) {
        if (!GameApp.hasMusic(MENU_MUSIC_KEY)) {
            return;
        }
        
        // Stop ingame music if playing
        stopIngameMusic();
        
        // Check if music is already playing (even from another SoundManager instance)
        try {
            Music music = GameApp.getMusic(MENU_MUSIC_KEY);
            if (music != null && music.isPlaying()) {
                // Music is already playing, just update volume if needed
                // Menu music volume is 10% of normal music volume
                float finalVolume = masterVolume * musicVolume * 0.1f;
                finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                music.setVolume(finalVolume);
                isMenuMusicPlaying = true;
                return;
            }
        } catch (Exception e) {
            // Ignore, will try to play
        }
        
        // Don't restart if this instance thinks it's playing
        if (isMenuMusicPlaying) {
            return;
        }
        
        try {
            // Calculate final volume: master * music * 0.1 (10% volume for menu)
            float finalVolume = masterVolume * musicVolume * 0.1f;
            finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
            
            GameApp.playMusic(MENU_MUSIC_KEY, loop, finalVolume);
            isMenuMusicPlaying = true;
            GameApp.log("Menu background music started");
        } catch (Exception e) {
            GameApp.log("Error playing menu music: " + e.getMessage());
        }
    }
    
    /**
     * Play ingame background music with looping.
     * @param loop Whether to loop the music
     */
    public void playIngameMusic(boolean loop) {
        if (!GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            return;
        }
        
        // Stop menu music if playing
        stopMusic();
        
        // Check if music is already playing
        try {
            Music music = GameApp.getMusic(INGAME_MUSIC_KEY);
            if (music != null && music.isPlaying()) {
                // Music is already playing, just update volume if needed
                // Ingame music volume is 10% of normal music volume
                float finalVolume = masterVolume * musicVolume * 0.1f;
                finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                music.setVolume(finalVolume);
                isIngameMusicPlaying = true;
                return;
            }
        } catch (Exception e) {
            // Ignore, will try to play
        }
        
        // Don't restart if this instance thinks it's playing
        if (isIngameMusicPlaying) {
            return;
        }
        
        try {
            // Calculate final volume: master * music * 0.1 (10% volume for ingame)
            float finalVolume = masterVolume * musicVolume * 0.1f;
            finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
            
            GameApp.playMusic(INGAME_MUSIC_KEY, loop, finalVolume);
            isIngameMusicPlaying = true;
            GameApp.log("Ingame background music started");
        } catch (Exception e) {
            GameApp.log("Error playing ingame music: " + e.getMessage());
        }
    }
    
    /**
     * Stop menu background music.
     */
    public void stopMusic() {
        stopMenuMusic();
    }
    
    /**
     * Stop menu background music.
     */
    public void stopMenuMusic() {
        if (!GameApp.hasMusic(MENU_MUSIC_KEY)) {
            return;
        }
        
        try {
            GameApp.stopMusic(MENU_MUSIC_KEY);
            isMenuMusicPlaying = false;
        } catch (Exception e) {
            GameApp.log("Error stopping menu music: " + e.getMessage());
        }
    }
    
    /**
     * Stop ingame background music.
     */
    public void stopIngameMusic() {
        if (!GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            return;
        }
        
        try {
            GameApp.stopMusic(INGAME_MUSIC_KEY);
            isIngameMusicPlaying = false;
        } catch (Exception e) {
            GameApp.log("Error stopping ingame music: " + e.getMessage());
        }
    }
    
    /**
     * Set ingame music volume temporarily (e.g., when level up menu is open).
     * @param volumeMultiplier Volume multiplier (0.0 - 1.0), will be multiplied by normal volume
     */
    public void setIngameMusicVolumeTemporary(float volumeMultiplier) {
        if (!GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            return;
        }
        
        if (!isIngameMusicPlaying) {
            return;
        }
        
        try {
            Music music = GameApp.getMusic(INGAME_MUSIC_KEY);
            if (music != null && music.isPlaying()) {
                // Calculate volume: master * music * 0.1 (normal) * volumeMultiplier (temporary reduction)
                float finalVolume = masterVolume * musicVolume * 0.1f * GameApp.clamp(volumeMultiplier, 0f, 1f);
                finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                music.setVolume(finalVolume);
            }
        } catch (Exception e) {
            GameApp.log("Error setting temporary ingame music volume: " + e.getMessage());
        }
    }
    
    /**
     * Restore ingame music volume to normal (after level up menu closes).
     */
    public void restoreIngameMusicVolume() {
        if (!GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            return;
        }
        
        if (!isIngameMusicPlaying) {
            return;
        }
        
        try {
            Music music = GameApp.getMusic(INGAME_MUSIC_KEY);
            if (music != null && music.isPlaying()) {
                // Restore to normal volume: master * music * 0.1 (10% volume for ingame)
                float finalVolume = masterVolume * musicVolume * 0.1f;
                finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                music.setVolume(finalVolume);
            }
        } catch (Exception e) {
            GameApp.log("Error restoring ingame music volume: " + e.getMessage());
        }
    }
    
    /**
     * Set SFX volume (0.0 - 1.0).
     * @param volume Volume level
     */
    public void setSFXVolume(float volume) {
        sfxVolume = GameApp.clamp(volume, 0f, 1f);
    }
    
    /**
     * Set music volume (0.0 - 1.0).
     * Also updates currently playing music if available.
     * @param volume Volume level
     */
    public void setMusicVolume(float volume) {
        musicVolume = GameApp.clamp(volume, 0f, 1f);
        
        // Update menu music volume if playing (10% volume)
        if (isMenuMusicPlaying && GameApp.hasMusic(MENU_MUSIC_KEY)) {
            try {
                Music music = GameApp.getMusic(MENU_MUSIC_KEY);
                if (music != null) {
                    // Menu music volume is 10% of normal music volume
                    float finalVolume = masterVolume * musicVolume * 0.1f;
                    finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                    music.setVolume(finalVolume);
                }
            } catch (Exception e) {
                GameApp.log("Error setting menu music volume: " + e.getMessage());
            }
        }
        
        // Update ingame music volume if playing (10% volume)
        if (isIngameMusicPlaying && GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            try {
                Music music = GameApp.getMusic(INGAME_MUSIC_KEY);
                if (music != null) {
                    // Ingame music volume is 10% of normal music volume
                    float finalVolume = masterVolume * musicVolume * 0.1f;
                    finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                    music.setVolume(finalVolume);
                }
            } catch (Exception e) {
                GameApp.log("Error setting ingame music volume: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set master volume (affects both SFX and music).
     * @param volume Volume level (0.0 - 1.0)
     */
    public void setMasterVolume(float volume) {
        masterVolume = GameApp.clamp(volume, 0f, 1f);
        
        // Update menu music volume if playing (10% volume)
        if (isMenuMusicPlaying && GameApp.hasMusic(MENU_MUSIC_KEY)) {
            try {
                Music music = GameApp.getMusic(MENU_MUSIC_KEY);
                if (music != null) {
                    // Menu music volume is 10% of normal music volume
                    float finalVolume = masterVolume * musicVolume * 0.1f;
                    finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                    music.setVolume(finalVolume);
                }
            } catch (Exception e) {
                GameApp.log("Error updating menu music master volume: " + e.getMessage());
            }
        }
        
        // Update ingame music volume if playing (10% volume)
        if (isIngameMusicPlaying && GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            try {
                Music music = GameApp.getMusic(INGAME_MUSIC_KEY);
                if (music != null) {
                    // Ingame music volume is 10% of normal music volume
                    float finalVolume = masterVolume * musicVolume * 0.1f;
                    finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                    music.setVolume(finalVolume);
                }
            } catch (Exception e) {
                GameApp.log("Error updating ingame music master volume: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get current SFX volume.
     * @return SFX volume (0.0 - 1.0)
     */
    public float getSFXVolume() {
        return sfxVolume;
    }
    
    /**
     * Get current music volume.
     * @return Music volume (0.0 - 1.0)
     */
    public float getMusicVolume() {
        return musicVolume;
    }
    
    /**
     * Get master volume.
     * @return Master volume (0.0 - 1.0)
     */
    public float getMasterVolume() {
        return masterVolume;
    }
    
    /**
     * Check if menu music is currently playing.
     * @return true if menu music is playing
     */
    public boolean isMusicPlaying() {
        return isMenuMusicPlaying;
    }
    
    /**
     * Check if ingame music is currently playing.
     * @return true if ingame music is playing
     */
    public boolean isIngameMusicPlaying() {
        return isIngameMusicPlaying;
    }
    
    /**
     * Dispose all audio resources.
     * Should be called when screen is hidden or game is closing.
     */
    public void dispose() {
        // Stop all music
        stopMenuMusic();
        stopIngameMusic();
        
        // Dispose menu music
        if (GameApp.hasMusic(MENU_MUSIC_KEY)) {
            try {
                GameApp.disposeMusic(MENU_MUSIC_KEY);
            } catch (Exception e) {
                GameApp.log("Error disposing menu music: " + e.getMessage());
            }
        }
        
        // Dispose ingame music
        if (GameApp.hasMusic(INGAME_MUSIC_KEY)) {
            try {
                GameApp.disposeMusic(INGAME_MUSIC_KEY);
            } catch (Exception e) {
                GameApp.log("Error disposing ingame music: " + e.getMessage());
            }
        }
        
        // Dispose all sounds
        for (String soundKey : soundKeys.values()) {
            if (GameApp.hasSound(soundKey)) {
                try {
                    GameApp.disposeSound(soundKey);
                } catch (Exception e) {
                    GameApp.log("Error disposing sound " + soundKey + ": " + e.getMessage());
                }
            }
        }
        
        soundKeys.clear();
        GameApp.log("SoundManager disposed");
    }
}
