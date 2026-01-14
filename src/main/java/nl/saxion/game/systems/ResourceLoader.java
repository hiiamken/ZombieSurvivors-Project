package nl.saxion.game.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
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
    
    // Map textures loaded with Nearest filter for sharp pixel rendering
    private final Map<String, Texture> mapTexturesWithNearestFilter = new HashMap<>();
    
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

        // Load boss sprite sheets (NEW: dedicated Boss sprites)
        GameApp.addSpriteSheet("boss_idle_sheet", "assets/enemy/Boss_Idle.png", 32, 32);
        GameApp.addSpriteSheet("boss_run_sheet", "assets/enemy/Boss_run.png", 32, 32);
        GameApp.addSpriteSheet("boss_hit_sheet", "assets/enemy/Boss_Hit.png", 32, 32);
        // Note: No Boss_Death.png, fallback to Boss_Hit for death animation
        GameApp.addSpriteSheet("boss_death_sheet", "assets/enemy/Boss_Hit.png", 32, 32);

        // Create boss animations
        GameApp.addAnimationFromSpritesheet("boss_idle", "boss_idle_sheet", 0.2f, true);
        GameApp.addAnimationFromSpritesheet("boss_run", "boss_run_sheet", 0.1f, true);
        GameApp.addAnimationFromSpritesheet("boss_hit", "boss_hit_sheet", 0.15f, false);
        GameApp.addAnimationFromSpritesheet("boss_death", "boss_death_sheet", 0.2f, false);


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

        // Load XP orb sprite sheet (legacy - kept for fallback)
        GameApp.addSpriteSheet("orb_sheet", "assets/enemy/orb.png", 16, 16);

        // Create XP orb animation from row 9, columns 19-22 (4 frames) - legacy fallback
        GameApp.addEmptyAnimation("orb_animation", 0.15f, true);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 19);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 20);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 21);
        GameApp.addAnimationFrameFromSpritesheet("orb_animation", "orb_sheet", 9, 22);
        
        // Load new orb textures (3 types: blue, green, red)
        try { GameApp.addTexture("orb_blue", "assets/ui/orbblue.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load orbblue.png"); }
        
        try { GameApp.addTexture("orb_green", "assets/ui/orbgreen.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load orbgreen.png"); }
        
        try { GameApp.addTexture("orb_red", "assets/ui/orbred.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load orbred.png"); }
        
        GameApp.log("Loaded 3 orb type textures (blue, green, red)");

        // Load breakable object sprite sheet (64x64 frames)
        // Sprite sheet layout: Each object type uses 1 row
        // Cols 0-2: idle animation, Cols 3-6: break animation
        // Row 1: Barrel, Row 3: Box, Row 5: Rock, Row 7: Sign, Row 9: Mushroom, Row 11: Chest
        GameApp.addSpriteSheet("object_sheet", "assets/tiles/object.png", 64, 64);

        // Create animations for all 6 object types
        // Each type uses same row: idle (cols 0-2, looping), break (cols 3-6, not looping)
        createObjectAnimations("barrel", 1);      // Barrel - row 1
        createObjectAnimations("box", 3);         // Box - row 3
        createObjectAnimations("rock", 5);        // Rock - row 5
        createObjectAnimations("sign", 7);        // Sign - row 7
        createObjectAnimations("mushroom", 9);    // Mushroom - row 9
        createObjectAnimations("chest", 11);      // Chest - row 11
        
        GameApp.log("Loaded all 6 breakable object types animations");
        
        // Load chicken texture for healing item
        try {
            GameApp.addTexture("chicken_item", "assets/ui/chicken.png");
            GameApp.log("Loaded chicken healing item texture");
        } catch (Exception e) {
            GameApp.log("Warning: Could not load chicken.png for healing item");
        }

        // Load treasure chest textures - shiny animation frames (1-11) for idle
        for (int i = 1; i <= 11; i++) {
            String key = "chest_shiny_" + i;
            String path = "assets/ui/shiny" + i + ".png";
            try {
                GameApp.addTexture(key, path);
            } catch (Exception e) {
                GameApp.log("Warning: Could not load " + path);
            }
        }
        
        // Load open animation frames (open1, open2) - for opening animation
        try {
            GameApp.addTexture("chest_open_1", "assets/ui/open1.png");
        } catch (Exception e) {
            GameApp.log("Warning: Could not load open1.png");
        }
        try {
            GameApp.addTexture("chest_open_2", "assets/ui/open2.png");
        } catch (Exception e) {
            GameApp.log("Warning: Could not load open2.png");
        }
        
        GameApp.log("Loaded treasure chest animation frames (shiny1-11, open1-2)");

        // Load passive item icons (64x64 images)
        try { GameApp.addTexture("passive_powerherb", "assets/ui/spinach.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load spinach.png"); }
        
        try { GameApp.addTexture("passive_ironshield", "assets/ui/armor.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load armor.png"); }
        
        try { GameApp.addTexture("passive_swiftboots", "assets/ui/wings.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load wings.png"); }
        
        try { GameApp.addTexture("passive_luckycoin", "assets/ui/clover.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load clover.png"); }
        
        try { GameApp.addTexture("passive_magnetstone", "assets/ui/Attractorb.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load Attractorb.png"); }
        
        try { GameApp.addTexture("passive_lifeessence", "assets/ui/pummarola.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load pummarola.png"); }
        
        try { GameApp.addTexture("passive_vitalitycore", "assets/ui/hollowhear.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load hollowhear.png"); }
        
        // Load weapon icon for gacha
        try { GameApp.addTexture("weapon_icon", "assets/Bullet/Bullet.png"); }
        catch (Exception e) { GameApp.log("Warning: Could not load Bullet.png for weapon icon"); }
        
        // Load pistol icon for HUD weapon display
        try { GameApp.addTexture("piston_icon", "assets/ui/piston.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load Bullet.png for weapon icon"); }
        
        // Load evolved pistol icon for evolution menu
        try { GameApp.addTexture("pistonevo", "assets/ui/pistonevo.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load pistonevo.png for evolution icon"); }
        
        // Load arrow icon for level up menu
        try { GameApp.addTexture("arrow_icon", "assets/ui/arrow.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load arrow.png"); }
        
        // Load star icon for score display
        try { GameApp.addTexture("star_icon", "assets/ui/star.png"); } 
        catch (Exception e) { GameApp.log("Warning: Could not load star.png"); }
        
        GameApp.log("Loaded passive item icons, weapon icon, arrow icon and star icon");

        // Load 16 individual map textures with NEAREST filter for sharp pixel rendering
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
                
                // Load texture with NEAREST filter for sharp pixel art rendering
                loadTextureWithNearestFilter(roomKey, roomPath);
                loadedCount++;
            } catch (Exception e) {
                GameApp.log("Warning: Could not load " + roomPath + " - " + e.getMessage());
            }
        }

        GameApp.log("Loaded " + loadedCount + " map textures with Nearest filter (room_00.png to room_15.png)");
        
        // Load zombie cats sprite sheet (512x417, 32x32 per frame, 13 rows x 16 cols)
        GameApp.addSpriteSheet("zombie_cats_sheet", "assets/ui/ZombieCatsSprites.png", 32, 32);
        GameApp.log("Loaded zombie cats sprite sheet (13 rows x 16 cols)");
        
        // Load zombie hand sprite sheet (800x32, 32x32 per frame, 1 row x 25 cols)
        GameApp.addSpriteSheet("zombie_hand_sheet", "assets/ui/BONUSZombieHand.png", 32, 32);
        GameApp.log("Loaded zombie hand sprite sheet (1 row x 25 cols)");
    }
    
    /**
     * Load a texture with NEAREST filter for sharp pixel-perfect rendering.
     * This prevents blurring when textures are scaled.
     * 
     * @param key The texture key to register
     * @param path The path to the texture file
     */
    private void loadTextureWithNearestFilter(String key, String path) {
        try {
            // First, load the texture normally with GameApp
            GameApp.addTexture(key, path);
            
            // Then, load again with LibGDX to apply Nearest filter
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            
            // Store in our map for direct rendering
            mapTexturesWithNearestFilter.put(key, texture);
            
        } catch (Exception e) {
            GameApp.log("Error loading texture with Nearest filter: " + key + " - " + e.getMessage());
            // Fallback: just load normally
            GameApp.addTexture(key, path);
        }
    }
    
    /**
     * Get a map texture with Nearest filter applied.
     * @param key The texture key
     * @return The Texture object with Nearest filter, or null if not found
     */
    public Texture getMapTextureWithNearestFilter(String key) {
        return mapTexturesWithNearestFilter.get(key);
    }
    
    /**
     * Check if a map texture with Nearest filter exists.
     * @param key The texture key
     * @return true if the texture exists
     */
    public boolean hasMapTextureWithNearestFilter(String key) {
        return mapTexturesWithNearestFilter.containsKey(key);
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

        // Dispose boss animations and sprite sheets
        GameApp.disposeAnimation("boss_idle");
        GameApp.disposeAnimation("boss_run");
        GameApp.disposeAnimation("boss_attack");
        GameApp.disposeAnimation("boss_death");

        GameApp.disposeSpritesheet("boss_idle_sheet");
        GameApp.disposeSpritesheet("boss_run_sheet");
        GameApp.disposeSpritesheet("boss_attack_sheet");
        GameApp.disposeSpritesheet("boss_death_sheet");


        // Dispose XP orb animation and sprite sheet
        GameApp.disposeAnimation("orb_animation");
        GameApp.disposeSpritesheet("orb_sheet");
        
        // Dispose new orb textures
        GameApp.disposeTexture("orb_blue");
        GameApp.disposeTexture("orb_green");
        GameApp.disposeTexture("orb_red");

        // Dispose breakable object animations and sprite sheet (all 6 types)
        String[] objectTypes = {"barrel", "box", "rock", "sign", "mushroom", "chest"};
        for (String type : objectTypes) {
            GameApp.disposeAnimation(type + "_idle");
            GameApp.disposeAnimation(type + "_break");
        }
        GameApp.disposeSpritesheet("object_sheet");
        
        // Dispose chicken healing item texture
        GameApp.disposeTexture("chicken_item");
        
        // Dispose treasure chest textures
        for (int i = 1; i <= 11; i++) {
            GameApp.disposeTexture("chest_shiny_" + i);
        }
        GameApp.disposeTexture("chest_open_1");
        GameApp.disposeTexture("chest_open_2");
        
        // Dispose passive item icons
        GameApp.disposeTexture("passive_powerherb");
        GameApp.disposeTexture("passive_ironshield");
        GameApp.disposeTexture("passive_swiftboots");
        GameApp.disposeTexture("passive_luckycoin");
        GameApp.disposeTexture("passive_magnetstone");
        GameApp.disposeTexture("passive_lifeessence");
        GameApp.disposeTexture("passive_vitalitycore");
        GameApp.disposeTexture("weapon_icon");

        // Dispose all map textures
        for (int i = 0; i < 16; i++) {
            String roomKey = getRoomTextureKey(i);
            GameApp.disposeTexture(roomKey);
        }
        
        // Dispose Nearest filter textures
        for (Texture texture : mapTexturesWithNearestFilter.values()) {
            if (texture != null) {
                texture.dispose();
            }
        }
        mapTexturesWithNearestFilter.clear();
        
        // Dispose zombie cats sprite sheet
        GameApp.disposeSpritesheet("zombie_cats_sheet");
        
        // Dispose zombie hand sprite sheet
        GameApp.disposeSpritesheet("zombie_hand_sheet");
    }

    private String getRoomTextureKey(int mapIndex) {
        return "room_" + String.format("%02d", mapIndex);
    }
    
    /**
     * Helper method to create idle and break animations for a breakable object type.
     * Both animations use the same row: cols 0-2 for idle, cols 3-6 for break.
     * @param name The animation name prefix (e.g., "barrel", "box")
     * @param row The sprite sheet row containing both idle (cols 0-2) and break (cols 3-6) frames
     */
    private void createObjectAnimations(String name, int row) {
        // Create idle animation (cols 0-2, looping)
        String idleAnim = name + "_idle";
        GameApp.addEmptyAnimation(idleAnim, 0.2f, true);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 0);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 1);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 2);
        
        // Create break animation (cols 3-6, NOT looping) - same row!
        String breakAnim = name + "_break";
        GameApp.addEmptyAnimation(breakAnim, 0.15f, false);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 3);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 4);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 5);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 6);
    }
    
    /**
     * Get the SoundManager instance.
     * @return SoundManager instance
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }
}

