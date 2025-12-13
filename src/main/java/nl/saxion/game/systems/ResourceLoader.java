package nl.saxion.game.systems;

import nl.saxion.game.utils.TMXMapData;
import nl.saxion.game.utils.TMXParser;
import nl.saxion.gameapp.GameApp;

import java.util.HashMap;
import java.util.Map;

public class ResourceLoader {

    public void loadGameResources() {
        GameApp.log("PlayScreen loaded");

        // ---------- COLORS (fix: Color 'darkgray' not found) ----------
        // Only add if missing, so it won't spam / re-add every time
        if (!GameApp.hasColor("darkgray")) GameApp.addColor("darkgray", 80, 80, 80);
        if (!GameApp.hasColor("gray"))     GameApp.addColor("gray", 140, 140, 140);
        if (!GameApp.hasColor("green"))    GameApp.addColor("green", 0, 200, 0);
        if (!GameApp.hasColor("white"))    GameApp.addColor("white", 255, 255, 255);
        if (!GameApp.hasColor("black"))    GameApp.addColor("black", 0, 0, 0);

        // ---------- TEXTURES ----------
        GameApp.addTexture("bullet", "assets/Bullet/Bullet.png");

        // XP orb texture (make sure the file exists at: src/main/resources/assets/xp/xp_orb.png)
        GameApp.addTexture("xp_orb", "assets/xp/xp_orb.png");

        // ---------- PLAYER SPRITESHEETS ----------
        GameApp.addSpriteSheet("player_idle_sheet", "assets/player/Rambo_Idle.png", 32, 32);
        GameApp.addSpriteSheet("player_run_left_sheet", "assets/player/Rambo_Run(Left).png", 32, 32);
        GameApp.addSpriteSheet("player_run_right_sheet", "assets/player/Rambo_Run(Right).png", 32, 32);
        GameApp.addSpriteSheet("player_death_sheet", "assets/player/Rambo_Death.png", 32, 32);
        GameApp.addSpriteSheet("player_hit_sheet", "assets/player/Player_Hit.png", 32, 32);

        GameApp.addAnimationFromSpritesheet("player_idle", "player_idle_sheet", 0.15f, true);
        GameApp.addAnimationFromSpritesheet("player_run_left", "player_run_left_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("player_run_right", "player_run_right_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("player_death", "player_death_sheet", 0.2f, false);
        GameApp.addAnimationFromSpritesheet("player_hit", "player_hit_sheet", 0.1f, false);

        // ---------- ZOMBIE SPRITESHEETS ----------
        GameApp.addSpriteSheet("zombie_idle_sheet", "assets/enemy/Zombie_Idle.png", 32, 32);
        GameApp.addSpriteSheet("zombie_run_sheet", "assets/enemy/Zombie_Run.png", 32, 32);
        GameApp.addSpriteSheet("zombie_hit_sheet", "assets/enemy/Zombie_Hit.png", 32, 32);
        GameApp.addSpriteSheet("zombie_death1_sheet", "assets/enemy/Zombie_Death_1.png", 32, 32);
        GameApp.addSpriteSheet("zombie_death2_sheet", "assets/enemy/Zombie_Death_2.png", 32, 32);

        GameApp.addAnimationFromSpritesheet("zombie_idle", "zombie_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("zombie_run", "zombie_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("zombie_death", "zombie_death1_sheet", 0.2f, false);

        // Optional fallback texture (not required if you always use animations)
        GameApp.addTexture("enemy", "assets/enemy/Zombie_Idle.png");

        // ---------- MAP TEXTURES ----------
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
        GameApp.hideCursor();
    }

    public Map<Integer, TMXMapData> loadTMXMaps() {
        Map<Integer, TMXMapData> tmxMapDataByRoomIndex = new HashMap<>();
        int loadedMaps = 0;

        for (int i = 0; i < 16; i++) {
            int mapNumber = i + 1;
            String tmxPath = "assets/maps/map" + mapNumber + ".tmx";
            TMXMapData mapData = TMXParser.loadFromTMX(tmxPath);

            if (mapData != null) {
                tmxMapDataByRoomIndex.put(i, mapData);
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

        GameApp.disposeTexture("bullet");
        GameApp.disposeTexture("enemy");
        GameApp.disposeTexture("xp_orb");

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

        GameApp.disposeAnimation("zombie_idle");
        GameApp.disposeAnimation("zombie_run");
        GameApp.disposeAnimation("zombie_hit");
        GameApp.disposeAnimation("zombie_death");

        GameApp.disposeSpritesheet("zombie_idle_sheet");
        GameApp.disposeSpritesheet("zombie_run_sheet");
        GameApp.disposeSpritesheet("zombie_hit_sheet");
        GameApp.disposeSpritesheet("zombie_death1_sheet");
        GameApp.disposeSpritesheet("zombie_death2_sheet");

        for (int i = 0; i < 16; i++) {
            GameApp.disposeTexture(getRoomTextureKey(i));
        }
    }

    private String getRoomTextureKey(int mapIndex) {
        return "room_" + String.format("%02d", mapIndex);
    }
}
