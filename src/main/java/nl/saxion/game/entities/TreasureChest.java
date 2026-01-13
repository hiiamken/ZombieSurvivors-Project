package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

import java.awt.Rectangle;

/**
 * TreasureChest entity - spawns when MiniBoss is killed
 * Uses frame-based animation:
 * - IDLE: shiny1 -> shiny11 (looping)
 * - OPENING: open1 -> open2 (plays once, stops at open2)
 * - OPENED: shows open2 (final frame)
 */
public class TreasureChest {

    private float x;
    private float y;
    
    public static final float SPRITE_SIZE = 28f;  // Smaller chest size
    private static final float INTERACTION_DISTANCE = 45f; // Distance to trigger open (adjusted for smaller size)
    
    // Animation constants
    private static final int SHINY_FRAME_COUNT = 11;  // shiny1 to shiny11
    private static final float SHINY_FRAME_DURATION = 0.08f; // Time per frame (faster = smoother)
    private static final float OPEN_FRAME_DURATION = 0.3f; // Time per open frame
    
    // Chest states
    public enum ChestState {
        IDLE,       // Waiting, showing shiny animation (looping)
        OPENING,    // Playing open animation (once)
        OPENED,     // Ready for gacha (shows open2)
        COLLECTED   // Gacha completed, can be removed
    }
    
    private ChestState state = ChestState.IDLE;
    
    // Animation state
    private float animTimer = 0f;
    private int currentShinyFrame = 1; // 1 to 11
    private int currentOpenFrame = 1;  // 1 or 2
    
    // Track if gacha was triggered
    private boolean gachaTriggered = false;
    
    public TreasureChest(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Update chest state and animations
     */
    public void update(float delta, float playerX, float playerY) {
        animTimer += delta;
        
        switch (state) {
            case IDLE:
                // Update shiny animation (looping through frames 1-11)
                if (animTimer >= SHINY_FRAME_DURATION) {
                    animTimer = 0f;
                    currentShinyFrame++;
                    if (currentShinyFrame > SHINY_FRAME_COUNT) {
                        currentShinyFrame = 1; // Loop back to frame 1
                    }
                }
                
                // Check if player is close enough to open
                float dx = playerX - x;
                float dy = playerY - y;
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                
                if (distance < INTERACTION_DISTANCE) {
                    state = ChestState.OPENING;
                    animTimer = 0f;
                    currentOpenFrame = 1;
                    GameApp.log("Chest opening! Player distance: " + distance);
                }
                break;
                
            case OPENING:
                // Play open animation (frame 1 -> frame 2, then stop)
                if (animTimer >= OPEN_FRAME_DURATION && currentOpenFrame < 2) {
                    animTimer = 0f;
                    currentOpenFrame = 2; // Move to frame 2
                }
                
                // After showing frame 2 for a moment, transition to OPENED
                if (currentOpenFrame == 2 && animTimer >= OPEN_FRAME_DURATION) {
                    state = ChestState.OPENED;
                    GameApp.log("Chest fully opened!");
                }
                break;
                
            case OPENED:
                // Wait for gacha to be triggered - show open2
                break;
                
            case COLLECTED:
                // Ready for removal
                break;
        }
    }
    
    /**
     * Render the chest with appropriate animation
     * - IDLE: shiny1 -> shiny11 (looping)
     * - OPENING: open1 -> open2 (plays once)
     * - OPENED: shows open2 (stays on final frame)
     * 
     * NOTE: Must be called within GameApp.startSpriteRendering() / endSpriteRendering() block
     * @param playerWorldX Player world X for camera offset
     * @param playerWorldY Player world Y for camera offset
     */
    public void render(float playerWorldX, float playerWorldY) {
        // Don't render collected chests
        if (state == ChestState.COLLECTED) {
            return;
        }
        
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();
        
        // Calculate screen position
        float offsetX = x - playerWorldX;
        float offsetY = y - playerWorldY;
        float screenX = worldW / 2f + offsetX - SPRITE_SIZE / 2f;
        float screenY = worldH / 2f + offsetY - SPRITE_SIZE / 2f;
        
        // Only render if on screen (with margin)
        float margin = SPRITE_SIZE * 2f;
        if (screenX + margin < 0 || screenX - margin > worldW ||
            screenY + margin < 0 || screenY - margin > worldH) {
            return;
        }
        
        // Get the correct texture key based on state
        String textureKey = getTextureKeyForCurrentState();
        
        // Render the texture if it exists
        if (textureKey != null && GameApp.hasTexture(textureKey)) {
            GameApp.drawTexture(textureKey, screenX, screenY, SPRITE_SIZE, SPRITE_SIZE);
        } else {
            // Fallback: try to render any available texture
            if (GameApp.hasTexture("chest_shiny_1")) {
                GameApp.drawTexture("chest_shiny_1", screenX, screenY, SPRITE_SIZE, SPRITE_SIZE);
            }
        }
    }
    
    /**
     * Get the correct texture key based on current state and animation frame
     */
    private String getTextureKeyForCurrentState() {
        switch (state) {
            case IDLE:
                // Use shiny1 to shiny11 (looping animation)
                return "chest_shiny_" + currentShinyFrame;
                
            case OPENING:
                // Frame 1: use open1 (chest starting to open)
                // Frame 2: use open2 (chest fully open)
                return "chest_open_" + currentOpenFrame;
                
            case OPENED:
                // Always show open2 (final frame - stays here)
                return "chest_open_2";
                
            default:
                return null;
        }
    }
    
    /**
     * Check if chest is ready to trigger gacha
     */
    public boolean isReadyForGacha() {
        return state == ChestState.OPENED && !gachaTriggered;
    }
    
    /**
     * Mark gacha as triggered
     */
    public void triggerGacha() {
        gachaTriggered = true;
    }
    
    /**
     * Mark chest as collected (after gacha completes)
     */
    public void collect() {
        state = ChestState.COLLECTED;
    }
    
    /**
     * Check if chest can be removed
     */
    public boolean isCollected() {
        return state == ChestState.COLLECTED;
    }
    
    /**
     * Check if chest is currently opening
     */
    public boolean isOpening() {
        return state == ChestState.OPENING;
    }
    
    /**
     * Check if chest is opened (for gacha trigger)
     */
    public boolean isOpened() {
        return state == ChestState.OPENED;
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    
    public Rectangle getHitbox() {
        return new Rectangle(
            (int)(x - SPRITE_SIZE / 2f),
            (int)(y - SPRITE_SIZE / 2f),
            (int)SPRITE_SIZE,
            (int)SPRITE_SIZE
        );
    }
}
