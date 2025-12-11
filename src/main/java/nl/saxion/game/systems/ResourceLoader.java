package nl.saxion.game.systems;

import nl.saxion.game.utils.TMXMapData;
import nl.saxion.game.utils.TMXParser;
import nl.saxion.gameapp.GameApp;

import java.util.HashMap;
import java.util.Map;

// Handles loading and disposing of game resources
public class ResourceLoader {
    public void loadGameResources() {
        GameApp.log("PlayScreen loaded");

        GameApp.addTexture("player", "assets/player/auraRambo.png");
        GameApp.addTexture("bullet", "assets/Bullet/Bullet.png");

        GameApp.addSpriteSheet("zombie_idle_sheet", "assets/enemy/Zombie_Idle.png", 32, 32);
        GameApp.addSpriteSheet("zombie_run_sheet", "assets/enemy/Zombie_Run.png", 32,32);
        GameApp.addSpriteSheet("zombie_hit_sheet", "assets/enemy/Zombie_Hit.png", 32,32);
        GameApp.addSpriteSheet("zombie_death1_sheet", "assets/enemy/Zombie_Death_1.png", 32,32);
        GameApp.addSpriteSheet("zombie_death2_sheet", "assets/enemy/Zombie_Death_2.png", 32,32);

        GameApp.addAnimationFromSpritesheet("zombie_idle", "zombie_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("zombie_run", "zombie_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("zombie_death", "zombie_death1_sheet", 0.2f, false);

        GameApp.addTexture("enemy", "assets/Bullet/Bullet.png");

        // Load 16 individual map textures
        int loadedCount = 0;
        for (int i = 0; i < 16; i++) {
            String roomKey = getRoomTextureKey(i);
            String roomPath = "assets/maps/room_" + String.format("%02d", i) + ".png";
            try {
                GameApp.addTexture(roomKey, roomPath);
                loadedCount++;
            } catch (Exception e) {
                GameApp.log("Warning: Could not load " + roomPath + " - " + e.getMessage());
            }
        }

        GameApp.log("Loaded " + loadedCount + " map textures (room_00.png to room_15.png)");

        // Hide cursor for better game experience
        GameApp.hideCursor();
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
        GameApp.disposeTexture("player");
        GameApp.disposeTexture("bullet");
        GameApp.disposeTexture("enemy");

        GameApp.disposeAnimation("zombie_idle");
        GameApp.disposeAnimation("zombie_run");
        GameApp.disposeAnimation("zombie_hit");
        GameApp.disposeAnimation("zombie_death");

        GameApp.disposeSpritesheet("zombie_idle_sheet");
        GameApp.disposeSpritesheet("zombie_run_sheet");
        GameApp.disposeSpritesheet("zombie_hit_sheet");
        GameApp.disposeSpritesheet("zombie_death1_sheet");
        GameApp.disposeSpritesheet("zombie_death2_sheet");

        // Dispose all map textures
        for (int i = 0; i < 16; i++) {
            String roomKey = getRoomTextureKey(i);
            GameApp.disposeTexture(roomKey);
        }
    }

    private String getRoomTextureKey(int mapIndex) {
        return "room_" + String.format("%02d", mapIndex);
    }
}

