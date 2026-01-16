package nl.saxion.game.systems;

import com.badlogic.gdx.audio.Music;
import nl.saxion.gameapp.GameApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all game audio (sound effects and background music).
 * Uses GameApp audio methods for loading and playback.
 * Supports 3-track ingame music cycling: ingame.mp3 → ingame2.mp3 → ingame3.mp3 → loop
 */
public class SoundManager {
    // Sound effect keys mapping
    private Map<String, String> soundKeys;
    
    // Music keys
    private static final String WINNER_MUSIC_KEY = "winner_music";  // Victory music for winner screen
    
    // Menu music tracks (random selection, persists across screen changes)
    private static final String[] MENU_MUSIC_KEYS = {"bgm", "bgm2", "bgm3"};
    private static final String[] MENU_MUSIC_FILES = {"audio/background.mp3", "audio/background2.mp3", "audio/background3.mp3"};
    private static int currentMenuTrackIndex = -1;  // -1 means not yet selected, static to persist across SoundManager instances
    private static boolean menuMusicInitialized = false;  // Static flag to track if menu music was ever selected
    
    // Ingame music tracks (random selection per game, looped)
    private static final String[] INGAME_MUSIC_KEYS = {"ingame_music_1", "ingame_music_2", "ingame_music_3"};
    private static final String[] INGAME_MUSIC_FILES = {"audio/ingame.mp3", "audio/ingame2.mp3", "audio/ingame3.wav"};
    private int currentTrackIndex = 0;  // Randomly selected track for this game session
    
    // Volume settings
    private float masterVolume = 1.0f;
    private float sfxVolume = 1.0f;
    private float musicVolume = 0.7f;
    
    // Track if music is currently playing
    private boolean isMenuMusicPlaying = false;
    private boolean isIngameMusicPlaying = false;
    private boolean isWinnerMusicPlaying = false;
    
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
        loadSound("meoww", "audio/meoww.mp3"); // Cat healing easter egg sound
        
        // Load all 3 menu background music tracks
        for (int i = 0; i < MENU_MUSIC_KEYS.length; i++) {
            String key = MENU_MUSIC_KEYS[i];
            String file = MENU_MUSIC_FILES[i];
            if (!GameApp.hasMusic(key)) {
                try {
                    GameApp.addMusic(key, file);
                    if (GameApp.hasMusic(key)) {
                        GameApp.log("Menu music track " + (i + 1) + " loaded: " + file);
                    }
                } catch (Exception e) {
                    GameApp.log("Warning: Could not load menu music " + file + ": " + e.getMessage());
                }
            } else {
                GameApp.log("Menu music track " + (i + 1) + " already loaded");
            }
        }
        
        // Load winner music (victory screen)
        if (!GameApp.hasMusic(WINNER_MUSIC_KEY)) {
            try {
                GameApp.addMusic(WINNER_MUSIC_KEY, "audio/winner.mp3");
                if (GameApp.hasMusic(WINNER_MUSIC_KEY)) {
                    GameApp.log("Winner music loaded successfully");
                }
            } catch (Exception e) {
                GameApp.log("Warning: Could not load winner music: " + e.getMessage());
            }
        }
        
        // Load all 3 ingame background music tracks for cycling
        // Track order: ingame.mp3 → ingame2.mp3 → ingame3.mp3 → loop back
        for (int i = 0; i < INGAME_MUSIC_KEYS.length; i++) {
            String key = INGAME_MUSIC_KEYS[i];
            String file = INGAME_MUSIC_FILES[i];
            if (!GameApp.hasMusic(key)) {
                try {
                    GameApp.addMusic(key, file);
                    if (GameApp.hasMusic(key)) {
                        GameApp.log("Ingame music track " + (i + 1) + " loaded: " + file);
                    }
                } catch (Exception e) {
                    GameApp.log("Warning: Could not load ingame music " + file + ": " + e.getMessage());
                }
            } else {
                GameApp.log("Ingame music track " + (i + 1) + " already loaded");
            }
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
     * Uses random track selection that persists across screen changes.
     * Only changes track when: game starts, or returning from winner screen.
     * @param loop Whether to loop the music
     */
    public void playMenuMusic(boolean loop) {
        // Stop ingame music if playing
        stopIngameMusic();
        
        // Check if ANY menu music track is already playing - don't restart if so
        for (int i = 0; i < MENU_MUSIC_KEYS.length; i++) {
            String key = MENU_MUSIC_KEYS[i];
            if (GameApp.hasMusic(key)) {
                try {
                    Music music = GameApp.getMusic(key);
                    if (music != null && music.isPlaying()) {
                        // Music is already playing, just update volume
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                        music.setVolume(finalVolume);
                        isMenuMusicPlaying = true;
                        GameApp.log("Menu music track " + (i + 1) + " already playing, skipping restart");
                        return;
                    }
                } catch (Exception e) {
                    // Ignore and continue checking
                }
            }
        }
        
        // Don't restart if this instance thinks it's playing
        if (isMenuMusicPlaying) {
            GameApp.log("This SoundManager instance thinks menu music is playing, skipping");
            return;
        }
        
        // If no track selected yet, randomly select one (first time or after reset)
        if (currentMenuTrackIndex < 0 || !menuMusicInitialized) {
            currentMenuTrackIndex = (int)(Math.random() * MENU_MUSIC_KEYS.length);
            menuMusicInitialized = true;
            GameApp.log("=== RANDOMLY SELECTED MENU TRACK: " + (currentMenuTrackIndex + 1) + "/3: " + MENU_MUSIC_FILES[currentMenuTrackIndex] + " ===");
        }
        
        String key = MENU_MUSIC_KEYS[currentMenuTrackIndex];
        
        if (!GameApp.hasMusic(key)) {
            GameApp.log("Menu music track " + (currentMenuTrackIndex + 1) + " not loaded: " + key);
            // Fallback to first track
            currentMenuTrackIndex = 0;
            key = MENU_MUSIC_KEYS[0];
            if (!GameApp.hasMusic(key)) {
                GameApp.log("No menu music available");
                return;
            }
        }
        
        // Stop any currently playing menu track first
        for (String trackKey : MENU_MUSIC_KEYS) {
            if (GameApp.hasMusic(trackKey)) {
                try {
                    Music m = GameApp.getMusic(trackKey);
                    if (m != null && m.isPlaying()) {
                        m.stop();
                    }
                } catch (Exception ignored) {}
            }
        }
        
        try {
            float finalVolume = masterVolume * musicVolume * 0.1f;
            finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
            
            GameApp.log("Menu music NOT playing, starting track " + (currentMenuTrackIndex + 1) + ": " + MENU_MUSIC_FILES[currentMenuTrackIndex]);
            GameApp.playMusic(key, loop, finalVolume);
            isMenuMusicPlaying = true;
            GameApp.log(">>> NOW PLAYING MENU: Track " + (currentMenuTrackIndex + 1) + "/3: " + MENU_MUSIC_FILES[currentMenuTrackIndex]);
        } catch (Exception e) {
            GameApp.log("Error playing menu music: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Force a new random menu music track selection.
     * Call this when returning from winner screen to main menu.
     */
    public static void randomizeMenuMusic() {
        currentMenuTrackIndex = (int)(Math.random() * MENU_MUSIC_KEYS.length);
        GameApp.log("=== RANDOMIZED NEW MENU TRACK: " + (currentMenuTrackIndex + 1) + "/3 ===");
    }
    
    /**
     * Play ingame background music with 3-track cycling system.
     * Tracks cycle: ingame.mp3 → ingame2.mp3 → ingame3.mp3 → loop back
     * @param loop Whether to loop (ignored - we handle looping via track cycling)
     */
    public void playIngameMusic(boolean loop) {
        // Check if at least first track is loaded
        if (!GameApp.hasMusic(INGAME_MUSIC_KEYS[0])) {
            GameApp.log("Ingame music not loaded, cannot play");
            return;
        }
        
        // Stop menu music if playing
        stopMusic();
        
        // Check if any track is already playing
        if (isIngameMusicPlaying) {
            for (String key : INGAME_MUSIC_KEYS) {
                try {
                    Music music = GameApp.getMusic(key);
                    if (music != null && music.isPlaying()) {
                        // Update volume and return
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        music.setVolume(GameApp.clamp(finalVolume, 0f, 1f));
                        return;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        
        // Randomly select one of 3 tracks for this game session
        currentTrackIndex = (int)(Math.random() * INGAME_MUSIC_KEYS.length);
        GameApp.log("=== RANDOMLY SELECTED TRACK: " + (currentTrackIndex + 1) + "/3: " + INGAME_MUSIC_FILES[currentTrackIndex] + " ===");
        playSelectedTrack();
    }
    
    /**
     * Play the randomly selected track with looping.
     */
    private void playSelectedTrack() {
        String key = INGAME_MUSIC_KEYS[currentTrackIndex];
        
        if (!GameApp.hasMusic(key)) {
            GameApp.log("Track " + (currentTrackIndex + 1) + " not loaded: " + key);
            // Try to load it now
            String file = INGAME_MUSIC_FILES[currentTrackIndex];
            try {
                GameApp.addMusic(key, file);
                GameApp.log("Late-loaded track: " + file);
            } catch (Exception e) {
                GameApp.log("Failed to late-load track: " + e.getMessage());
                // Try another track
                currentTrackIndex = (currentTrackIndex + 1) % INGAME_MUSIC_KEYS.length;
                return;
            }
        }
        
        try {
            // Stop any currently playing track first
            for (String trackKey : INGAME_MUSIC_KEYS) {
                if (GameApp.hasMusic(trackKey)) {
                    try {
                        Music m = GameApp.getMusic(trackKey);
                        if (m != null && m.isPlaying()) {
                            m.stop();
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            float finalVolume = masterVolume * musicVolume * 0.1f;
            finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
            
            // Play WITH loop - single random track loops for entire game
            GameApp.playMusic(key, true, finalVolume);
            isIngameMusicPlaying = true;
            GameApp.log(">>> NOW PLAYING (LOOPED): Track " + (currentTrackIndex + 1) + "/3: " + INGAME_MUSIC_FILES[currentTrackIndex]);
        } catch (Exception e) {
            GameApp.log("Error playing track " + key + ": " + e.getMessage());
        }
    }
    
    /**
     * Update music system - no longer needed for cycling, but kept for compatibility.
     * The selected track now loops automatically.
     * @param delta Time since last frame
     */
    public void updateIngameMusic(float delta) {
        // No action needed - track loops automatically
        // This method is kept for compatibility with existing code
    }
    
    /**
     * Stop menu background music.
     */
    public void stopMusic() {
        stopMenuMusic();
    }
    
    /**
     * Stop menu background music (all tracks).
     */
    public void stopMenuMusic() {
        for (String key : MENU_MUSIC_KEYS) {
            if (GameApp.hasMusic(key)) {
                try {
                    GameApp.stopMusic(key);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        isMenuMusicPlaying = false;
    }
    
    /**
     * Stop all ingame background music tracks.
     */
    public void stopIngameMusic() {
        // Stop all 3 tracks
        for (String key : INGAME_MUSIC_KEYS) {
            if (GameApp.hasMusic(key)) {
                try {
                    GameApp.stopMusic(key);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        isIngameMusicPlaying = false;
        currentTrackIndex = 0;
    }
    
    /**
     * Play winner music (victory screen).
     * @param loop Whether to loop the music
     */
    public void playWinnerMusic(boolean loop) {
        if (!GameApp.hasMusic(WINNER_MUSIC_KEY)) {
            GameApp.log("Winner music not loaded, cannot play");
            return;
        }
        
        // Stop any other music first
        stopMenuMusic();
        stopIngameMusic();
        
        try {
            float finalVolume = masterVolume * musicVolume * 0.5f; // 50% volume for winner music
            finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
            
            GameApp.playMusic(WINNER_MUSIC_KEY, loop, finalVolume);
            isWinnerMusicPlaying = true;
            GameApp.log("Winner music started");
        } catch (Exception e) {
            GameApp.log("Error playing winner music: " + e.getMessage());
        }
    }
    
    /**
     * Stop winner music.
     */
    public void stopWinnerMusic() {
        if (!GameApp.hasMusic(WINNER_MUSIC_KEY)) {
            return;
        }
        
        try {
            GameApp.stopMusic(WINNER_MUSIC_KEY);
            isWinnerMusicPlaying = false;
        } catch (Exception e) {
            GameApp.log("Error stopping winner music: " + e.getMessage());
        }
    }
    
    /**
     * Check if winner music is playing.
     */
    public boolean isWinnerMusicPlaying() {
        return isWinnerMusicPlaying;
    }
    
    /**
     * Set ingame music volume temporarily (e.g., when level up menu is open).
     * @param volumeMultiplier Volume multiplier (0.0 - 1.0), will be multiplied by normal volume
     */
    public void setIngameMusicVolumeTemporary(float volumeMultiplier) {
        if (!isIngameMusicPlaying) return;
        
        // Update volume on currently playing track
        String currentKey = INGAME_MUSIC_KEYS[currentTrackIndex];
        if (!GameApp.hasMusic(currentKey)) return;
        
        try {
            Music music = GameApp.getMusic(currentKey);
            if (music != null && music.isPlaying()) {
                float finalVolume = masterVolume * musicVolume * 0.1f * GameApp.clamp(volumeMultiplier, 0f, 1f);
                music.setVolume(GameApp.clamp(finalVolume, 0f, 1f));
            }
        } catch (Exception e) {
            GameApp.log("Error setting temporary ingame music volume: " + e.getMessage());
        }
    }
    
    /**
     * Restore ingame music volume to normal (after level up menu closes).
     */
    public void restoreIngameMusicVolume() {
        if (!isIngameMusicPlaying) return;
        
        // Restore volume on currently playing track
        String currentKey = INGAME_MUSIC_KEYS[currentTrackIndex];
        if (!GameApp.hasMusic(currentKey)) return;
        
        try {
            Music music = GameApp.getMusic(currentKey);
            if (music != null && music.isPlaying()) {
                float finalVolume = masterVolume * musicVolume * 0.1f;
                music.setVolume(GameApp.clamp(finalVolume, 0f, 1f));
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
        if (isMenuMusicPlaying && currentMenuTrackIndex >= 0) {
            String currentKey = MENU_MUSIC_KEYS[currentMenuTrackIndex];
            if (GameApp.hasMusic(currentKey)) {
                try {
                    Music music = GameApp.getMusic(currentKey);
                    if (music != null) {
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                        music.setVolume(finalVolume);
                    }
                } catch (Exception e) {
                    GameApp.log("Error setting menu music volume: " + e.getMessage());
                }
            }
        }
        
        // Update ingame music volume if playing (10% volume)
        if (isIngameMusicPlaying) {
            String currentKey = INGAME_MUSIC_KEYS[currentTrackIndex];
            if (GameApp.hasMusic(currentKey)) {
                try {
                    Music music = GameApp.getMusic(currentKey);
                    if (music != null) {
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        music.setVolume(GameApp.clamp(finalVolume, 0f, 1f));
                    }
                } catch (Exception e) {
                    GameApp.log("Error setting ingame music volume: " + e.getMessage());
                }
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
        if (isMenuMusicPlaying && currentMenuTrackIndex >= 0) {
            String currentKey = MENU_MUSIC_KEYS[currentMenuTrackIndex];
            if (GameApp.hasMusic(currentKey)) {
                try {
                    Music music = GameApp.getMusic(currentKey);
                    if (music != null) {
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        finalVolume = GameApp.clamp(finalVolume, 0f, 1f);
                        music.setVolume(finalVolume);
                    }
                } catch (Exception e) {
                    GameApp.log("Error updating menu music master volume: " + e.getMessage());
                }
            }
        }
        
        // Update ingame music volume if playing (10% volume)
        if (isIngameMusicPlaying) {
            String currentKey = INGAME_MUSIC_KEYS[currentTrackIndex];
            if (GameApp.hasMusic(currentKey)) {
                try {
                    Music music = GameApp.getMusic(currentKey);
                    if (music != null) {
                        float finalVolume = masterVolume * musicVolume * 0.1f;
                        music.setVolume(GameApp.clamp(finalVolume, 0f, 1f));
                    }
                } catch (Exception e) {
                    GameApp.log("Error updating ingame music master volume: " + e.getMessage());
                }
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
        
        // Dispose all menu music tracks
        for (String key : MENU_MUSIC_KEYS) {
            if (GameApp.hasMusic(key)) {
                try {
                    GameApp.disposeMusic(key);
                } catch (Exception e) {
                    GameApp.log("Error disposing menu music " + key + ": " + e.getMessage());
                }
            }
        }
        
        // Dispose all ingame music tracks
        for (String key : INGAME_MUSIC_KEYS) {
            if (GameApp.hasMusic(key)) {
                try {
                    GameApp.disposeMusic(key);
                } catch (Exception e) {
                    GameApp.log("Error disposing ingame music " + key + ": " + e.getMessage());
                }
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
