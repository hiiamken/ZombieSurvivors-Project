package nl.saxion.game.systems;

import nl.saxion.gameapp.GameApp;

import java.util.HashSet;
import java.util.Set;

/**
 * Lazy Resource Manager - Loads resources on demand instead of all at startup.
 * Helps reduce initial load time and memory usage.
 * 
 * Usage:
 * - Call ensureLoaded(ResourceType) before using a resource
 * - Resources are only loaded once, subsequent calls are no-ops
 */
public class LazyResourceManager {
    
    // Track which resource groups have been loaded
    private static final Set<ResourceGroup> loadedGroups = new HashSet<>();
    
    // Resource groups that can be loaded lazily
    public enum ResourceGroup {
        PLAYER,          // Player sprites and animations
        ZOMBIE_TYPE1,    // Zombie type 1 sprites
        ZOMBIE_TYPE3,    // Zombie type 3 sprites
        ZOMBIE_TYPE4,    // Zombie type 4 sprites
        BOSS,            // Boss sprites
        XP_ORBS,         // XP orb textures
        BREAKABLES,      // Breakable object sprites
        HEALING_ITEMS,   // Healing items (chicken)
        TREASURE_CHEST,  // Treasure chest animations
        PASSIVE_ITEMS,   // Passive item icons
        WEAPON_ICONS,    // Weapon related icons
        UI_ICONS,        // UI icons (star, skull, arrow)
        MAPS,            // Map textures (handled separately due to complexity)
        CATS,            // Zombie cats decorations
        ZOMBIE_HANDS     // Zombie hand decorations
    }
    
    /**
     * Ensure a resource group is loaded. If already loaded, this is a no-op.
     * @param group The resource group to load
     */
    public static void ensureLoaded(ResourceGroup group) {
        if (loadedGroups.contains(group)) {
            return; // Already loaded
        }
        
        long startTime = System.currentTimeMillis();
        
        switch (group) {
            case PLAYER -> loadPlayerResources();
            case ZOMBIE_TYPE1 -> loadZombieType1Resources();
            case ZOMBIE_TYPE3 -> loadZombieType3Resources();
            case ZOMBIE_TYPE4 -> loadZombieType4Resources();
            case BOSS -> loadBossResources();
            case XP_ORBS -> loadXPOrbResources();
            case BREAKABLES -> loadBreakableResources();
            case HEALING_ITEMS -> loadHealingItemResources();
            case TREASURE_CHEST -> loadTreasureChestResources();
            case PASSIVE_ITEMS -> loadPassiveItemResources();
            case WEAPON_ICONS -> loadWeaponIconResources();
            case UI_ICONS -> loadUIIconResources();
            case CATS -> loadCatResources();
            case ZOMBIE_HANDS -> loadZombieHandResources();
            case MAPS -> {} // Maps are handled by ResourceLoader due to special filter requirements
        }
        
        loadedGroups.add(group);
        long loadTime = System.currentTimeMillis() - startTime;
        GameApp.log("LazyLoad: " + group.name() + " loaded in " + loadTime + "ms");
    }
    
    /**
     * Check if a resource group is loaded.
     */
    public static boolean isLoaded(ResourceGroup group) {
        return loadedGroups.contains(group);
    }
    
    /**
     * Preload essential resources needed for gameplay.
     * Call this during initial game load.
     */
    public static void preloadEssentials() {
        ensureLoaded(ResourceGroup.PLAYER);
        ensureLoaded(ResourceGroup.XP_ORBS);
        ensureLoaded(ResourceGroup.UI_ICONS);
    }
    
    /**
     * Preload all combat-related resources.
     * Call this before starting actual gameplay.
     */
    public static void preloadCombat() {
        ensureLoaded(ResourceGroup.ZOMBIE_TYPE1);
        ensureLoaded(ResourceGroup.ZOMBIE_TYPE3);
        ensureLoaded(ResourceGroup.ZOMBIE_TYPE4);
        ensureLoaded(ResourceGroup.BOSS);
        ensureLoaded(ResourceGroup.WEAPON_ICONS);
    }
    
    /**
     * Preload all resources. Useful for ensuring smooth gameplay.
     */
    public static void preloadAll() {
        for (ResourceGroup group : ResourceGroup.values()) {
            if (group != ResourceGroup.MAPS) { // Maps handled separately
                ensureLoaded(group);
            }
        }
    }
    
    /**
     * Clear loaded state (for testing or reloading).
     */
    public static void reset() {
        loadedGroups.clear();
    }
    
    // ==========================================
    // Private resource loading methods
    // ==========================================
    
    private static void loadPlayerResources() {
        if (!GameApp.hasSpritesheet("player_idle_sheet")) {
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
        }
        
        if (!GameApp.hasTexture("bullet")) {
            GameApp.addTexture("bullet", "assets/Bullet/Bullet.png");
        }
    }
    
    private static void loadZombieType1Resources() {
        if (!GameApp.hasSpritesheet("zombie_run_sheet")) {
            GameApp.addSpriteSheet("zombie_idle_sheet", "assets/enemy/Zombie_Idle.png", 32, 32);
            GameApp.addSpriteSheet("zombie_run_sheet", "assets/enemy/Zombie_run.png", 32, 32);
            GameApp.addSpriteSheet("zombie_hit_sheet", "assets/enemy/Zombie_Hit.png", 32, 32);
            GameApp.addSpriteSheet("zombie_death1_sheet", "assets/enemy/Zombie_Death_1.png", 32, 32);
            
            GameApp.addAnimationFromSpritesheet("zombie_idle", "zombie_idle_sheet", 0.2f, true);
            GameApp.addAnimationFromSpritesheet("zombie_run", "zombie_run_sheet", 0.1f, true);
            // Hit animation: 96x32 sprite sheet = 3 frames (row 0, cols 0-2)
            GameApp.addEmptyAnimation("zombie_hit", 0.1f, false);
            GameApp.addAnimationFrameFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0, 0);
            GameApp.addAnimationFrameFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0, 1);
            GameApp.addAnimationFrameFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0, 2);
            GameApp.addAnimationFromSpritesheet("zombie_death", "zombie_death1_sheet", 0.2f, false);
        }
    }
    
    private static void loadZombieType3Resources() {
        if (!GameApp.hasSpritesheet("zombie3_run_sheet")) {
            GameApp.addSpriteSheet("zombie3_idle_sheet", "assets/enemy/Zombie 3_idle .png", 32, 32);
            GameApp.addSpriteSheet("zombie3_run_sheet", "assets/enemy/Zombie 3_run .png", 32, 32);
            GameApp.addSpriteSheet("zombie3_hit_sheet", "assets/enemy/Zombie 3_Hit .png", 32, 32);
            GameApp.addSpriteSheet("zombie3_death_sheet", "assets/enemy/Zombie 3_death.png", 32, 32);
            
            GameApp.addAnimationFromSpritesheet("zombie3_idle", "zombie3_idle_sheet", 0.2f, true);
            GameApp.addAnimationFromSpritesheet("zombie3_run", "zombie3_run_sheet", 0.1f, true);
            // Hit animation: 96x32 sprite sheet = 3 frames (row 0, cols 0-2)
            GameApp.addEmptyAnimation("zombie3_hit", 0.1f, false);
            GameApp.addAnimationFrameFromSpritesheet("zombie3_hit", "zombie3_hit_sheet", 0, 0);
            GameApp.addAnimationFrameFromSpritesheet("zombie3_hit", "zombie3_hit_sheet", 0, 1);
            GameApp.addAnimationFrameFromSpritesheet("zombie3_hit", "zombie3_hit_sheet", 0, 2);
            GameApp.addAnimationFromSpritesheet("zombie3_death", "zombie3_death_sheet", 0.2f, false);
        }
    }
    
    private static void loadZombieType4Resources() {
        if (!GameApp.hasSpritesheet("zombie4_run_sheet")) {
            GameApp.addSpriteSheet("zombie4_idle_sheet", "assets/enemy/Zombie 4_idle.png", 32, 32);
            GameApp.addSpriteSheet("zombie4_run_sheet", "assets/enemy/Zombie 4_run.png", 32, 32);
            // Note: Zombie 4 has no hit sprite, fallback to run sprite
            GameApp.addSpriteSheet("zombie4_hit_sheet", "assets/enemy/Zombie 4_run.png", 32, 32);
            GameApp.addSpriteSheet("zombie4_death_sheet", "assets/enemy/Zombie 4_death 4.png", 32, 32);
            
            GameApp.addAnimationFromSpritesheet("zombie4_idle", "zombie4_idle_sheet", 0.2f, true);
            GameApp.addAnimationFromSpritesheet("zombie4_run", "zombie4_run_sheet", 0.1f, true);
            // Hit animation: 96x32 sprite sheet = 3 frames (row 0, cols 0-2)
            GameApp.addEmptyAnimation("zombie4_hit", 0.1f, false);
            GameApp.addAnimationFrameFromSpritesheet("zombie4_hit", "zombie4_hit_sheet", 0, 0);
            GameApp.addAnimationFrameFromSpritesheet("zombie4_hit", "zombie4_hit_sheet", 0, 1);
            GameApp.addAnimationFrameFromSpritesheet("zombie4_hit", "zombie4_hit_sheet", 0, 2);
            GameApp.addAnimationFromSpritesheet("zombie4_death", "zombie4_death_sheet", 0.2f, false);
        }
    }
    
    private static void loadBossResources() {
        if (!GameApp.hasSpritesheet("boss_run_sheet")) {
            GameApp.addSpriteSheet("boss_idle_sheet", "assets/enemy/Boss_Idle.png", 32, 32);
            GameApp.addSpriteSheet("boss_run_sheet", "assets/enemy/Boss_run.png", 32, 32);
            GameApp.addSpriteSheet("boss_hit_sheet", "assets/enemy/Boss_Hit.png", 32, 32);
            GameApp.addSpriteSheet("boss_death_sheet", "assets/enemy/Boss_Hit.png", 32, 32);
            
            GameApp.addAnimationFromSpritesheet("boss_idle", "boss_idle_sheet", 0.2f, true);
            GameApp.addAnimationFromSpritesheet("boss_run", "boss_run_sheet", 0.1f, true);
            // Hit animation: 96x32 sprite sheet = 3 frames (row 0, cols 0-2)
            GameApp.addEmptyAnimation("boss_hit", 0.1f, false);
            GameApp.addAnimationFrameFromSpritesheet("boss_hit", "boss_hit_sheet", 0, 0);
            GameApp.addAnimationFrameFromSpritesheet("boss_hit", "boss_hit_sheet", 0, 1);
            GameApp.addAnimationFrameFromSpritesheet("boss_hit", "boss_hit_sheet", 0, 2);
            // Death animation uses same hit sheet
            GameApp.addEmptyAnimation("boss_death", 0.15f, false);
            GameApp.addAnimationFrameFromSpritesheet("boss_death", "boss_death_sheet", 0, 0);
            GameApp.addAnimationFrameFromSpritesheet("boss_death", "boss_death_sheet", 0, 1);
            GameApp.addAnimationFrameFromSpritesheet("boss_death", "boss_death_sheet", 0, 2);
        }
    }
    
    private static void loadXPOrbResources() {
        try {
            if (!GameApp.hasTexture("orb_blue")) {
                GameApp.addTexture("orb_blue", "assets/ui/orbblue.png");
            }
            if (!GameApp.hasTexture("orb_green")) {
                GameApp.addTexture("orb_green", "assets/ui/orbgreen.png");
            }
            if (!GameApp.hasTexture("orb_red")) {
                GameApp.addTexture("orb_red", "assets/ui/orbred.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load orb textures");
        }
    }
    
    private static void loadBreakableResources() {
        if (!GameApp.hasSpritesheet("object_sheet")) {
            GameApp.addSpriteSheet("object_sheet", "assets/tiles/object.png", 64, 64);
            
            // Create animations for all 6 object types
            createObjectAnimations("barrel", 1);
            createObjectAnimations("box", 3);
            createObjectAnimations("rock", 5);
            createObjectAnimations("sign", 7);
            createObjectAnimations("mushroom", 9);
            createObjectAnimations("chest", 11);
        }
    }
    
    private static void createObjectAnimations(String name, int row) {
        String idleAnim = name + "_idle";
        GameApp.addEmptyAnimation(idleAnim, 0.2f, true);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 0);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 1);
        GameApp.addAnimationFrameFromSpritesheet(idleAnim, "object_sheet", row, 2);
        
        String breakAnim = name + "_break";
        GameApp.addEmptyAnimation(breakAnim, 0.15f, false);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 3);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 4);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 5);
        GameApp.addAnimationFrameFromSpritesheet(breakAnim, "object_sheet", row, 6);
    }
    
    private static void loadHealingItemResources() {
        try {
            if (!GameApp.hasTexture("chicken_item")) {
                GameApp.addTexture("chicken_item", "assets/ui/chicken.png");
            }
            if (!GameApp.hasTexture("chicken")) {
                GameApp.addTexture("chicken", "assets/ui/chicken.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load healing item textures");
        }
    }
    
    private static void loadTreasureChestResources() {
        try {
            // Load shiny animation frames
            for (int i = 1; i <= 11; i++) {
                String key = "chest_shiny_" + i;
                if (!GameApp.hasTexture(key)) {
                    GameApp.addTexture(key, "assets/ui/shiny" + i + ".png");
                }
            }
            // Load open animation frames
            if (!GameApp.hasTexture("chest_open_1")) {
                GameApp.addTexture("chest_open_1", "assets/ui/open1.png");
            }
            if (!GameApp.hasTexture("chest_open_2")) {
                GameApp.addTexture("chest_open_2", "assets/ui/open2.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load treasure chest textures");
        }
    }
    
    private static void loadPassiveItemResources() {
        try {
            if (!GameApp.hasTexture("passive_powerherb")) {
                GameApp.addTexture("passive_powerherb", "assets/ui/spinach.png");
                GameApp.addTexture("passive_ironshield", "assets/ui/armor.png");
                GameApp.addTexture("passive_swiftboots", "assets/ui/wings.png");
                GameApp.addTexture("passive_luckycoin", "assets/ui/clover.png");
                GameApp.addTexture("passive_magnetstone", "assets/ui/Attractorb.png");
                GameApp.addTexture("passive_lifeessence", "assets/ui/pummarola.png");
                GameApp.addTexture("passive_vitalitycore", "assets/ui/hollowhear.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load passive item textures");
        }
    }
    
    private static void loadWeaponIconResources() {
        try {
            if (!GameApp.hasTexture("weapon_icon")) {
                GameApp.addTexture("weapon_icon", "assets/Bullet/Bullet.png");
            }
            if (!GameApp.hasTexture("piston_icon")) {
                GameApp.addTexture("piston_icon", "assets/ui/piston.png");
            }
            if (!GameApp.hasTexture("pistonevo")) {
                GameApp.addTexture("pistonevo", "assets/ui/pistonevo.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load weapon icon textures");
        }
    }
    
    private static void loadUIIconResources() {
        try {
            if (!GameApp.hasTexture("arrow_icon")) {
                GameApp.addTexture("arrow_icon", "assets/ui/arrow.png");
            }
            if (!GameApp.hasTexture("star_icon")) {
                GameApp.addTexture("star_icon", "assets/ui/star.png");
            }
            if (!GameApp.hasTexture("skull_icon")) {
                GameApp.addTexture("skull_icon", "assets/ui/skull.png");
            }
            if (!GameApp.hasTexture("star")) {
                GameApp.addTexture("star", "assets/ui/star.png");
            }
        } catch (Exception e) {
            GameApp.log("Warning: Could not load UI icon textures");
        }
    }
    
    private static void loadCatResources() {
        if (!GameApp.hasSpritesheet("zombie_cats_sheet")) {
            GameApp.addSpriteSheet("zombie_cats_sheet", "assets/ui/ZombieCatsSprites.png", 32, 32);
        }
    }
    
    private static void loadZombieHandResources() {
        if (!GameApp.hasSpritesheet("zombie_hand_sheet")) {
            GameApp.addSpriteSheet("zombie_hand_sheet", "assets/ui/BONUSZombieHand.png", 32, 32);
        }
    }
}
