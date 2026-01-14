package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

/**
 * Background zombie hand entity - Trap feature!
 * Sử dụng sprite sheet BONUSZombieHand.png với 25 frames (800x32, mỗi frame 32x32).
 * Animation: tay zombie mọc lên từ đất rồi rút lại.
 * 
 * Trap feature: When animation reaches column 9+, player takes damage if nearby.
 * Has 1 minute cooldown per hand after dealing damage.
 */
public class ZombieHand {
    
    // World position
    private float x;
    private float y;
    
    // Sprite size
    public static final int SPRITE_SIZE = 32;
    
    // Render size (nhỏ hơn một chút để trông tự nhiên hơn)
    public static final int RENDER_SIZE = 28;
    
    // Animation timer để chuyển frame
    private float animationTimer = 0f;
    private static final float ANIMATION_FRAME_DURATION = 0.12f; // 0.12 giây mỗi frame
    
    // Số frames trong sprite sheet (25 frames)
    private static final int TOTAL_FRAMES = 25;
    
    // Trap damage system
    private static final float DAMAGE_PROXIMITY_RANGE = 30f; // Distance to trigger damage
    private static final int DAMAGE_START_COLUMN = 9; // Column where hand starts dealing damage
    private static final float DAMAGE_COOLDOWN = 60f; // 1 minute cooldown per hand
    private static final int DAMAGE_AMOUNT = 3; // Small damage amount
    
    private float damageCooldownTimer = 0f; // Cooldown remaining
    private boolean canDamage = true; // Whether this hand can damage (not on cooldown)
    private boolean playerWasInRange = false; // Track if player entered before column 9
    
    public ZombieHand(float x, float y) {
        this.x = x;
        this.y = y;
        // Random start time để mỗi tay có animation khác nhau
        this.animationTimer = GameApp.random(0f, ANIMATION_FRAME_DURATION * TOTAL_FRAMES);
    }
    
    /**
     * Update animation timer and damage cooldown
     */
    public void update(float delta) {
        animationTimer += delta;
        // Animation sẽ loop tự động khi tính frame column
        
        // Update damage cooldown
        if (!canDamage && damageCooldownTimer > 0) {
            damageCooldownTimer -= delta;
            if (damageCooldownTimer <= 0) {
                canDamage = true;
                damageCooldownTimer = 0f;
            }
        }
    }
    
    /**
     * Check if player should take damage from this zombie hand.
     * Damage is dealt when:
     * - Player is in range AND
     * - Animation is at column 9 or higher AND
     * - Hand is not on cooldown
     * 
     * Also handles the case where player enters during column 0-8 and stays until column 9+
     * 
     * @param playerX Player world X position
     * @param playerY Player world Y position
     * @return Damage amount (3) if player should take damage, 0 otherwise
     */
    public int checkPlayerDamage(float playerX, float playerY) {
        if (!canDamage) return 0;
        
        // Calculate distance from hand center to player
        float handCenterX = x + SPRITE_SIZE / 2f;
        float handCenterY = y + SPRITE_SIZE / 2f;
        float dist = GameApp.distance(handCenterX, handCenterY, playerX, playerY);
        
        int currentColumn = getCurrentFrameColumn();
        boolean playerInRange = dist <= DAMAGE_PROXIMITY_RANGE;
        
        if (playerInRange) {
            // Track that player is in range
            if (currentColumn < DAMAGE_START_COLUMN) {
                // Player entered during safe phase (column 0-8)
                playerWasInRange = true;
            }
            
            // Check if animation reached damage phase (column 9+)
            if (currentColumn >= DAMAGE_START_COLUMN) {
                // Deal damage - either player just entered or was waiting
                playerWasInRange = false;
                canDamage = false;
                damageCooldownTimer = DAMAGE_COOLDOWN;
                GameApp.log("Zombie hand trap triggered! Player takes " + DAMAGE_AMOUNT + " damage");
                return DAMAGE_AMOUNT;
            }
        } else {
            // Player left range, reset tracking
            playerWasInRange = false;
        }
        
        return 0;
    }
    
    /**
     * Check if this hand can currently deal damage (not on cooldown)
     */
    public boolean canDealDamage() {
        return canDamage;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public float getRemainingCooldown() {
        return damageCooldownTimer;
    }
    
    /**
     * Get current animation column (0-24)
     */
    public int getCurrentColumn() {
        return getCurrentFrameColumn();
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    /**
     * Tính toán frame column hiện tại dựa trên animation timer
     */
    private int getCurrentFrameColumn() {
        // Tính frame index dựa trên timer, loop từ 0 đến TOTAL_FRAMES-1
        int frameIndex = (int)(animationTimer / ANIMATION_FRAME_DURATION) % TOTAL_FRAMES;
        return frameIndex;
    }
    
    /**
     * Render zombie hand tại vị trí screen (đã tính offset từ player)
     */
    public void render(float screenX, float screenY) {
        if (GameApp.hasSpritesheet("zombie_hand_sheet")) {
            int currentColumn = getCurrentFrameColumn();
            
            // Render với kích thước nhỏ hơn một chút
            GameApp.drawSpritesheetFrame(
                "zombie_hand_sheet",
                0,              // row (chỉ có 1 hàng)
                currentColumn,  // col (0 đến 24, tự động loop)
                screenX,
                screenY,
                RENDER_SIZE,
                RENDER_SIZE
            );
        }
    }
}
