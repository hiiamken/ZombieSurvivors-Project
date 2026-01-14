package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

/**
 * Background cat entity - Easter egg! Heals player when staying nearby for 2 seconds.
 * Sử dụng sprite sheet ZombieCatsSprites.png với 13 hàng (13 con mèo khác nhau).
 * Mỗi frame là 32x32 pixels.
 * 
 * Easter egg feature: Player stays near cat for 2 seconds -> heals small amount
 * Each cat has 1 minute cooldown after healing.
 */
public class Cat {
    

    private static final int[] CAT_COLUMNS = {
        10,  // Mèo 1 (index 0)
        10,  // Mèo 2 (index 1)
        4,   // Mèo 3 (index 2)
        4,   // Mèo 4 (index 3)
        15,  // Mèo 5 (index 4)
        11,  // Mèo 6 (index 5)
        3,   // Mèo 7 (index 6)
        12,  // Mèo 8 (index 7)
        4,   // Mèo 9 (index 8)
        4,   // Mèo 10 (index 9)
        4,   // Mèo 11 (index 10)
        2,   // Mèo 12 (index 11)
        6    // Mèo 13 (index 12)
    };
    
    // World position
    private float x;
    private float y;
    
    // Cat type
    private int catType;
    
    // Sprite size (original frame size)
    public static final int SPRITE_SIZE = 32;
    
    // Render size
    public static final int RENDER_SIZE = 26;
    
    // Animation timer để chuyển frame
    private float animationTimer = 0f;
    private static final float ANIMATION_FRAME_DURATION = 0.15f; // 0.15 giây mỗi frame
    
    // Easter egg healing system
    private static final float HEAL_PROXIMITY_RANGE = 50f; // Distance to trigger healing (hitbox touch)
    private static final float HEAL_COOLDOWN = 60f; // 1 minute cooldown per cat
    private static final int HEAL_AMOUNT = 8; // Heal amount on touch
    
    private float healCooldownTimer = 0f; // Cooldown remaining
    private boolean canHeal = true; // Whether this cat can heal (not on cooldown)
    
    public Cat(float x, float y, int catType) {
        this.x = x;
        this.y = y;
        this.catType = catType;
        // Random start time để mỗi con mèo có animation khác nhau
        this.animationTimer = GameApp.random(0f, ANIMATION_FRAME_DURATION * CAT_COLUMNS[catType]);
    }
    
    /**
     * Update animation timer and healing cooldown
     */
    public void update(float delta) {
        animationTimer += delta;
        
        // Update healing cooldown
        if (!canHeal && healCooldownTimer > 0) {
            healCooldownTimer -= delta;
            if (healCooldownTimer <= 0) {
                canHeal = true;
                healCooldownTimer = 0f;
            }
        }
    }
    
    /**
     * Check if player touches cat hitbox - instant heal on touch.
     * @param playerX Player world X position
     * @param playerY Player world Y position
     * @param delta Time since last frame (unused, kept for compatibility)
     * @return Heal amount (8 HP) if player touches cat, 0 otherwise
     */
    public int checkPlayerProximityHeal(float playerX, float playerY, float delta) {
        if (!canHeal) return 0;
        
        // Calculate distance from cat center to player
        float catCenterX = x + SPRITE_SIZE / 2f;
        float catCenterY = y + SPRITE_SIZE / 2f;
        float dist = GameApp.distance(catCenterX, catCenterY, playerX, playerY);
        
        if (dist <= HEAL_PROXIMITY_RANGE) {
            // Player touched cat - instant heal and start cooldown
            canHeal = false;
            healCooldownTimer = HEAL_COOLDOWN;
            GameApp.log("Cat easter egg triggered! Healed player " + HEAL_AMOUNT + " HP");
            return HEAL_AMOUNT;
        }
        
        return 0;
    }
    
    /**
     * Check if this cat can currently heal (not on cooldown)
     */
    public boolean canHealPlayer() {
        return canHeal;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public float getRemainingCooldown() {
        return healCooldownTimer;
    }
    
    /**
     * Lấy số columns của một con mèo cụ thể
     */
    public static int getCatColumns(int catType) {
        if (catType >= 0 && catType < CAT_COLUMNS.length) {
            return CAT_COLUMNS[catType];
        }
        return 0;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public int getCatType() {
        return catType;
    }
    
    /**
     * Tính toán frame column hiện tại dựa trên animation timer
     */
    private int getCurrentFrameColumn() {
        int maxColumns = CAT_COLUMNS[catType];
        // Tính frame index dựa trên timer, loop từ 0 đến maxColumns-1
        int frameIndex = (int)(animationTimer / ANIMATION_FRAME_DURATION) % maxColumns;
        return frameIndex;
    }
    
    /**
     * Render cat tại vị trí screen (đã tính offset từ player)
     */
    public void render(float screenX, float screenY) {
        if (GameApp.hasSpritesheet("zombie_cats_sheet")) {
            int currentColumn = getCurrentFrameColumn();
            
            // Render với kích thước nhỏ hơn một chút
            GameApp.drawSpritesheetFrame(
                "zombie_cats_sheet",
                catType,        // row (0-12)
                currentColumn,  // col (0 đến maxColumns-1, tự động loop)
                screenX,
                screenY,
                RENDER_SIZE,
                RENDER_SIZE
            );
        }
    }
}
