package nl.saxion.game.systems;

import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.utils.TMXMapData;
import nl.saxion.game.utils.TMXParser;
import nl.saxion.gameapp.GameApp;

import java.util.HashMap;
import java.util.Map;

// Handles loading and disposing of game resources
public class ResourceLoader {
    private SoundManager soundManager;
    
    public void loadGameResources() {
        // Load audio resources
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        // Load volume settings from config and apply to sound manager
        GameConfig config = ConfigManager.loadConfig();
        soundManager.setMasterVolume(config.masterVolume);
        soundManager.setMusicVolume(config.musicVolume);
        soundManager.setSFXVolume(config.sfxVolume);
        
        GameApp.log("PlayScreen loaded");

        GameApp.addTexture("bullet", "assets/Bullet/Bullet.png");

        // Load player sprite sheets (32x32 frames)
        GameApp.addSpriteSheet("player_idle_sheet", "assets/player/Rambo_Idle.png", 32, 32);
        GameApp.addSpriteSheet("player_run_left_sheet", "assets/player/Rambo_Run(Left).png", 32, 32);
        GameApp.addSpriteSheet("player_run_right_sheet", "assets/player/Rambo_Run(Right).png", 32, 32);
        GameApp.addSpriteSheet("player_death_sheet", "assets/player/Rambo_Death.png", 32, 32);
        GameApp.addSpriteSheet("player_hit_sheet", "assets/player/Player_Hit.png", 32, 32);

        // Create player animations
        GameApp.addAnimationFromSpritesheet("player_idle", "player_idle_sheet", 0.15f, true);
        GameApp.addAnimationFromSpritesheet("player_run_left", "player_run_left_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("player_run_right", "player_run_right_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("player_death", "player_death_sheet", 0.2f, false);
        GameApp.addAnimationFromSpritesheet("player_hit", "player_hit_sheet", 0.1f, false);

        // Load zombie sprite sheets - Type 1 (original)
        GameApp.addSpriteSheet("zombie_idle_sheet", "assets/enemy/Zombie_Idle.png", 32, 32);
        GameApp.addSpriteSheet("zombie_run_sheet", "assets/enemy/Zombie_Run.png", 32,32);
        GameApp.addSpriteSheet("zombie_hit_sheet", "assets/enemy/Zombie_Hit.png", 32,32);
        GameApp.addSpriteSheet("zombie_death1_sheet", "assets/enemy/Zombie_Death_1.png", 32,32);
        GameApp.addSpriteSheet("zombie_death2_sheet", "assets/enemy/Zombie_Death_2.png", 32,32);

        // Load zombie sprite sheets - Type 3
        GameApp.addSpriteSheet("zombie3_idle_sheet", "assets/enemy/Zombie 3_idle .png", 32, 32);
        GameApp.addSpriteSheet("zombie3_run_sheet", "assets/enemy/Zombie 3_run .png", 32, 32);
        GameApp.addSpriteSheet("zombie3_hit_sheet", "assets/enemy/Zombie 3_Hit .png", 32, 32);
        GameApp.addSpriteSheet("zombie3_death_sheet", "assets/enemy/Zombie 3_death.png", 32, 32);

        // Load zombie sprite sheets - Type 4
        GameApp.addSpriteSheet("zombie4_idle_sheet", "assets/enemy/Zombie 4_idle.png", 32, 32);
        GameApp.addSpriteSheet("zombie4_run_sheet", "assets/enemy/Zombie 4_run.png", 32, 32);
        GameApp.addSpriteSheet("zombie4_hit_sheet", "assets/enemy/Zombie 4_hit.png", 32, 32);
        GameApp.addSpriteSheet("zombie4_death_sheet", "assets/enemy/Zombie 4_death 4.png", 32, 32);

        // Create zombie animations - Type 1 (original)
        GameApp.addAnimationFromSpritesheet("zombie_idle", "zombie_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("zombie_run", "zombie_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("zombie_death", "zombie_death1_sheet", 0.2f, false);

        // Create zombie animations - Type 3
        GameApp.addAnimationFromSpritesheet("zombie3_idle", "zombie3_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("zombie3_run", "zombie3_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("zombie3_hit", "zombie3_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("zombie3_death", "zombie3_death_sheet", 0.2f, false);

        // Create zombie animations - Type 4
        GameApp.addAnimationFromSpritesheet("zombie4_idle", "zombie4_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("zombie4_run", "zombie4_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("zombie4_hit", "zombie4_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("zombie4_death", "zombie4_death_sheet", 0.2f, false);

        GameApp.addTexture("enemy", "assets/Bullet/Bullet.png");

        // Load XP orb sprite sheet
        GameApp.addSpriteSheet("orb_sheet", "assets/enemy/orb.png", 16, 16);

        // Create XP orb animation from row 9, columns 19-22 (4 frames)
        GameApp.addEmptyAnimation("orb_animation", 0.15f, true);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 19);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 20);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 21);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 22);

        // Load 16 individual map textures
        // Note: Game uses room_00.png to room_15.png, NOT map1.png
        // If you change map images, update room_XX.png files, not map1.png
        int loadedCount = 0;
        for (int i = 0; i < 16; i++) {
            String roomKey = getRoomTextureKey(i);
            String roomPath = "assets/maps/room_" + String.format("%02d", i) + ".png";
            try {
                // Dispose old texture if exists to force reload (allows seeing file changes)
                if (GameApp.hasTexture(roomKey)) {
                    GameApp.disposeTexture(roomKey);
                }
                GameApp.addTexture(roomKey, roomPath);
                loadedCount++;
            } catch (Exception e) {
                GameApp.log("Warning: Could not load " + roomPath + " - " + e.getMessage());
            }
        }

        GameApp.log("Loaded " + loadedCount + " map textures (room_00.png to room_15.png)");
    }

    public Map<Integer, TMXMapData> loadTMXMaps() {
        // Load 16 TMX maps for collision detection
        Map<Integer, TMXMapData> tmxMapDataByRoomIndex = new HashMap<>();
        int loadedMaps = 0;
        for (int i = 0; i < 16; i++) {
            int mapNumber = i + 1; // map1, map2, ..., map16
            String tmxPath = "assets/maps/map" + mapNumber + ".tmx";
            TMXMapData mapData = TMXParser.loadFromTMX(tmxPath);
            if (mapData != null) {
                tmxMapDataByRoomIndex.put(i, mapData); // room index i corresponds to map(i+1)
                loadedMaps++;
            } else {
                GameApp.log("❌ Warning: Could not load " + tmxPath);
            }
        }
        GameApp.log("✅ Successfully loaded " + loadedMaps + "/16 TMX maps for collision");
        return tmxMapDataByRoomIndex;
    }

    public void disposeGameResources() {
        GameApp.log("PlayScreen hidden");
        
        // Dispose audio resources
        if (soundManager != null) {
            soundManager.dispose();
            soundManager = null;
        }
        
        GameApp.disposeTexture("bullet");
        GameApp.disposeTexture("enemy");

        // Dispose player animations
        GameApp.disposeAnimation("player_idle");
        GameApp.disposeAnimation("player_run_left");
        GameApp.disposeAnimation("player_run_right");
        GameApp.disposeAnimation("player_death");
        GameApp.disposeAnimation("player_hit");

        GameApp.disposeSpritesheet("player_idle_sheet");
        GameApp.disposeSpritesheet("player_run_left_sheet");
        GameApp.disposeSpritesheet("player_run_right_sheet");
        GameApp.disposeSpritesheet("player_death_sheet");
        GameApp.disposeSpritesheet("player_hit_sheet");

        // Dispose zombie animations - Type 1
        GameApp.disposeAnimation("zombie_idle");
        GameApp.disposeAnimation("zombie_run");
        GameApp.disposeAnimation("zombie_hit");
        GameApp.disposeAnimation("zombie_death");

        GameApp.disposeSpritesheet("zombie_idle_sheet");
        GameApp.disposeSpritesheet("zombie_run_sheet");
        GameApp.disposeSpritesheet("zombie_hit_sheet");
        GameApp.disposeSpritesheet("zombie_death1_sheet");
        GameApp.disposeSpritesheet("zombie_death2_sheet");

        // Dispose zombie animations - Type 3
        GameApp.disposeAnimation("zombie3_idle");
        GameApp.disposeAnimation("zombie3_run");
        GameApp.disposeAnimation("zombie3_hit");
        GameApp.disposeAnimation("zombie3_death");

        GameApp.disposeSpritesheet("zombie3_idle_sheet");
        GameApp.disposeSpritesheet("zombie3_run_sheet");
        GameApp.disposeSpritesheet("zombie3_hit_sheet");
        GameApp.disposeSpritesheet("zombie3_death_sheet");

        // Dispose zombie animations - Type 4
        GameApp.disposeAnimation("zombie4_idle");
        GameApp.disposeAnimation("zombie4_run");
        GameApp.disposeAnimation("zombie4_hit");
        GameApp.disposeAnimation("zombie4_death");

        GameApp.disposeSpritesheet("zombie4_idle_sheet");
        GameApp.disposeSpritesheet("zombie4_run_sheet");
        GameApp.disposeSpritesheet("zombie4_hit_sheet");
        GameApp.disposeSpritesheet("zombie4_death_sheet");

        // Dispose XP orb animation and sprite sheet
        GameApp.disposeAnimation("orb_animation");
        GameApp.disposeSpritesheet("orb_sheet");

        // Dispose all map textures
        for (int i = 0; i < 16; i++) {
            String roomKey = getRoomTextureKey(i);
            GameApp.disposeTexture(roomKey);
        }
    }

    private String getRoomTextureKey(int mapIndex) {
        return "room_" + String.format("%02d", mapIndex);
    }
    
    /**
     * Get the SoundManager instance.
     * @return SoundManager instance
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }
}

