package nl.saxion.game.entities;


import nl.saxion.gameapp.GameApp;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.utils.CollisionChecker;
import com.badlogic.gdx.graphics.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    // Animation states
    public enum AnimationState {
        IDLE, RUNNING_LEFT, RUNNING_RIGHT, HIT, DEAD
    }

    // Position - WORLD COORDINATES
    private float worldX;
    private float worldY;

    // ===== XP / LEVEL SYSTEM =====
    // Balanced for max level at minute 8-9 of 10-minute game
    private int currentLevel = 1;
    private int currentXP = 0;
    private int xpToNextLevel = 55; // Formula: 40 + 1 * 15 = 55 at level 1 (easier early game)
    private float xpMagnetRange = 100f;
    private float damageMultiplier = 1f;
    
    // ===== STAT UPGRADE LEVELS (Legacy system - kept for compatibility) =====
    private int healthRegenLevel = 0; // HEALTH_REGEN level (max 5)
    private int maxHealthLevel = 0; // MAX_HEALTH level (max 3)
    private int damageLevel = 0; // DAMAGE level (max 5)
    private int speedLevel = 0; // SPEED level (max 2)
    private int xpMagnetLevel = 0; // XP_MAGNET level (max 2)
    
    // ===== PASSIVE ITEMS SYSTEM (New) =====
    private Map<PassiveItemType, PassiveItem> passiveItems = new HashMap<>();
    
    // ===== HEALTH REGEN SYSTEM =====
    private float healthRegenAccumulator = 0f; // Accumulated regen (not applied to health bar yet)
    private float healthRegenUpdateTimer = 0f; // Timer for 10-second updates
    private static final float REGEN_UPDATE_INTERVAL = 10f; // Update health every 10 seconds
    private static final float REGEN_PER_LEVEL = 0.1f; // 0.1 HP/s per level (stat upgrade)
    private static final int MAX_REGEN_LEVEL = 5; // Max level (0.5 HP/s)
    private HealthTextCallback healthTextCallback = null;
    
    // ===== BLOOD PARTICLES =====
    private List<BloodParticle> bloodParticles = new ArrayList<>();
    
    // Blood particle class for damage effect
    private static class BloodParticle {
        float x, y;
        float vx, vy;
        float lifetime;
        float maxLifetime;
        float size;
        Color color;
        
        BloodParticle(float x, float y) {
            this.x = x;
            this.y = y;
            // Random velocity direction (spread outward)
            float angle = GameApp.random(0f, 360f) * (float) Math.PI / 180f;
            float speed = GameApp.random(60f, 120f);
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.lifetime = GameApp.random(0.3f, 0.6f);
            this.maxLifetime = lifetime;
            this.size = GameApp.random(2f, 5f);
            // Red blood color with slight variation
            float r = GameApp.random(0.8f, 1.0f);
            float g = GameApp.random(0.0f, 0.2f);
            float b = 0f;
            this.color = new Color(r, g, b, 1f);
        }
        
        void update(float delta) {
            x += vx * delta;
            y += vy * delta;
            vy -= 150f * delta; // Gravity
            vx *= 0.98f; // Air resistance
            lifetime -= delta;
            // Fade out - clamp alpha to 0-1
            color.a = Math.max(0f, Math.min(1f, lifetime / maxLifetime));
        }
        
        boolean isAlive() {
            return lifetime > 0;
        }
    }
    
    // ===== DAMAGE REDUCTION FROM ARMOR =====
    private float damageReductionMultiplier = 1f; // 1.0 = no reduction, 0.5 = 50% reduction
    
    // Base stats (stored for percentage calculations)
    private float baseSpeed;
    private int baseMaxHealth;
    
    // Callback interface for health text
    public interface HealthTextCallback {
        void onHeal(int amount, float playerX, float playerY);
    }

    // Speed
    private float speed;

    // Health
    private int health;
    private int maxHealth;

    // Animation
    private AnimationState animationState = AnimationState.IDLE;
    private float hitAnimationTimer = 0f;
    private boolean facingRight = true;
    private static final float HIT_ANIMATION_DURATION = 0.3f;

    // Death state
    private boolean isDying = false;

    public static final int SPRITE_SIZE = 36; // Larger sprite for zoomed out view
    // Wall hitbox (small, for wall collision)
    public static final int HITBOX_WIDTH = 12;
    public static final int HITBOX_HEIGHT = 12;
    // Damage hitbox (larger, covers body and head for player-enemy collision)
    public static final int DAMAGE_HITBOX_WIDTH = 18;
    public static final int DAMAGE_HITBOX_HEIGHT = 20;

    private float targetShootDirX = 0f;
    private float targetShootDirY = -1f;
    private float smoothShootDirX = 0f;
    private float smoothShootDirY = -1f;

    static final float SHOOT_DIRECTION_SMOOTHING = 12f;

    // Corner Correction constants
    private static final float CORNER_CHECK_DIST = 6.0f;  // Distance to check for gaps beside wall
    private static final float NUDGE_SPEED = 60.0f;       // Speed to auto-slide around corners

    // HitBox (world coordinates)
    // wallHitbox: for wall collision
    private Rectangle wallHitbox;
    // damageHitbox: for enemy interaction
    private Rectangle damageHitbox;

    public Player(float startWorldX, float startWorldY, float speed, int maxHealth, Image sprite) {
        this.worldX = startWorldX;
        this.worldY = startWorldY;
        this.speed = speed;
        this.baseSpeed = speed; // Store base speed for percentage calculations

        this.maxHealth = maxHealth;
        this.baseMaxHealth = maxHealth; // Store base max health for percentage calculations
        this.health = maxHealth;

        // Wall hitbox: smaller than sprite, centered (world coordinates)
        int wallOffsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int wallOffsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        wallHitbox = new Rectangle((int) (worldX + wallOffsetX), (int) (worldY + wallOffsetY), HITBOX_WIDTH, HITBOX_HEIGHT);

        // Damage hitbox: larger to cover body and head
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox = new Rectangle((int) (worldX + damageOffsetX), (int) (worldY + damageOffsetY), DAMAGE_HITBOX_WIDTH, DAMAGE_HITBOX_HEIGHT);
    }

    // Movement update
    public void update(float delta, InputController input, int worldWidth, int worldHeight, CollisionChecker collisionChecker) {
        // Don't update movement if dying - just update animation state
        if (isDying) {
            animationState = AnimationState.DEAD;
            return;
        }

        // Calculate total health regen from both stat upgrade AND passive item (Pummarola)
        float totalRegenRate = 0f;
        
        // Stat upgrade regen
        if (healthRegenLevel > 0) {
            int effectiveLevel = Math.min(healthRegenLevel, MAX_REGEN_LEVEL);
            totalRegenRate += REGEN_PER_LEVEL * effectiveLevel;
        }
        
        // Passive item regen (Life Essence)
        if (hasPassiveItem(PassiveItemType.LIFE_ESSENCE)) {
            totalRegenRate += getPassiveItem(PassiveItemType.LIFE_ESSENCE).getMultiplier();
        }
        
        // Apply health regen
        if (totalRegenRate > 0 && health < maxHealth && !isDying) {
            // Accumulate regen (not applied to health bar yet)
            healthRegenAccumulator += totalRegenRate * delta;
            
            // Update timer for 10-second health updates
            healthRegenUpdateTimer += delta;
            
            if (healthRegenUpdateTimer >= REGEN_UPDATE_INTERVAL) {
                // Round accumulated regen to integer and heal
                int healAmount = Math.round(healthRegenAccumulator);
                if (healAmount > 0) {
                    heal(healAmount);
                    healthRegenAccumulator = 0f; // Reset accumulator after healing
                }
                healthRegenUpdateTimer = 0f;
            }
        } else {
            // Reset timers if no regen or at full health
            healthRegenAccumulator = 0f;
            healthRegenUpdateTimer = 0f;
        }

        // Update hit animation timer
        if (hitAnimationTimer > 0) {
            hitAnimationTimer -= delta;
        }
        
        // Update blood particles
        updateBloodParticles(delta);

        float dirX = 0f;
        float dirY = 0f;
        if (input.isMoveUp()) dirY += 1f;
        if (input.isMoveDown()) dirY -= 1f;
        if (input.isMoveLeft()) dirX -= 1f;
        if (input.isMoveRight()) dirX += 1f;
        
        // Calculate effective speed with passive item bonus (Wings)
        float effectiveSpeed = getEffectiveSpeed();
        
        float dx = dirX * effectiveSpeed * delta;
        float dy = dirY * effectiveSpeed * delta;
        float offsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2f;
        float offsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2f;

        // Save original position
        float originalWorldX = worldX;
        float originalWorldY = worldY;

        // Normalize diagonal movement to maintain consistent speed
        if (dx != 0 && dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float normalizedDx = dx / length;
            float normalizedDy = dy / length;
            dx = normalizedDx * effectiveSpeed * delta;
            dy = normalizedDy * effectiveSpeed * delta;
        }

        // Simple collision check with epsilon for smooth movement near walls
        final float EPSILON = 2.0f;
        final float SLIDE_MARGIN = 1.0f;

        // Move X first (standard 2D game approach)
        if (dx != 0 && collisionChecker != null) {
            float newWorldX = originalWorldX + dx;
            float hitboxWorldX = newWorldX + offsetX;
            float hitboxWorldY = originalWorldY + offsetY;

            // When moving horizontal (X), keep width standard but shrink HEIGHT
            // to avoid friction with ceiling or floor
            float checkWidth = (float)HITBOX_WIDTH - EPSILON;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON - SLIDE_MARGIN;
            float checkX = hitboxWorldX + EPSILON / 2f;
            float checkY = hitboxWorldY + (EPSILON + SLIDE_MARGIN) / 2f;

            boolean collisionX = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionX) {
                worldX = newWorldX;
            } else {
                // === CORNER CORRECTION X ===
                // Moving horizontal and hit wall -> Check if can slide up/down
                boolean nudgeUp = !collisionChecker.checkCollision(checkX, checkY + CORNER_CHECK_DIST, checkWidth, checkHeight);
                boolean nudgeDown = !collisionChecker.checkCollision(checkX, checkY - CORNER_CHECK_DIST, checkWidth, checkHeight);

                // Smart Nudge: Prioritize player's intended direction
                if (nudgeUp && nudgeDown) {
                    // Both directions clear -> Prioritize based on Y input
                    if (dirY > 0) {
                        worldY += NUDGE_SPEED * delta;
                    } else if (dirY < 0) {
                        worldY -= NUDGE_SPEED * delta;
                    } else {
                        // No Y input -> Binary search to find safe position
                        doBinarySearchX(dx, originalWorldX, checkY, checkWidth, checkHeight, offsetX, collisionChecker, EPSILON);
                    }
                } else if (nudgeUp && dirY >= 0) {
                    worldY += NUDGE_SPEED * delta;
                } else if (nudgeDown && dirY <= 0) {
                    worldY -= NUDGE_SPEED * delta;
                } else {
                    // Real collision -> Binary search to stop at safe position
                    doBinarySearchX(dx, originalWorldX, checkY, checkWidth, checkHeight, offsetX, collisionChecker, EPSILON);
                }
            }
        } else if (dx != 0) {
            worldX = originalWorldX + dx;
        }

        // Move Y (using updated worldX position) - standard 2D approach
        if (dy != 0 && collisionChecker != null) {
            float newWorldY = originalWorldY + dy;
            float hitboxWorldX = worldX + offsetX;
            float hitboxWorldY = newWorldY + offsetY;

            // When moving vertical (Y), keep height standard but shrink WIDTH
            // to avoid friction with left/right walls
            float checkWidth = (float)HITBOX_WIDTH - EPSILON - SLIDE_MARGIN;
            float checkHeight = (float)HITBOX_HEIGHT - EPSILON;
            float checkX = hitboxWorldX + (EPSILON + SLIDE_MARGIN) / 2f;
            float checkY = hitboxWorldY + EPSILON / 2f;

            boolean collisionY = collisionChecker.checkCollision(checkX, checkY, checkWidth, checkHeight);

            if (!collisionY) {
                worldY = newWorldY;
            } else {
                // === CORNER CORRECTION Y ===
                // Moving vertical and hit wall -> Check if can slide left/right
                boolean nudgeRight = !collisionChecker.checkCollision(checkX + CORNER_CHECK_DIST, checkY, checkWidth, checkHeight);
                boolean nudgeLeft = !collisionChecker.checkCollision(checkX - CORNER_CHECK_DIST, checkY, checkWidth, checkHeight);

                // Smart Nudge: Prioritize player's intended direction
                if (nudgeRight && nudgeLeft) {
                    if (dirX > 0) {
                        worldX += NUDGE_SPEED * delta;
                    } else if (dirX < 0) {
                        worldX -= NUDGE_SPEED * delta;
                    } else {
                        doBinarySearchY(dy, originalWorldY, checkX, checkWidth, checkHeight, offsetY, collisionChecker, EPSILON);
                    }
                } else if (nudgeRight && dirX >= 0) {
                    worldX += NUDGE_SPEED * delta;
                } else if (nudgeLeft && dirX <= 0) {
                    worldX -= NUDGE_SPEED * delta;
                } else {
                    doBinarySearchY(dy, originalWorldY, checkX, checkWidth, checkHeight, offsetY, collisionChecker, EPSILON);
                }
            }
        } else if (dy != 0) {
            worldY = originalWorldY + dy;
        }

        // Update shooting direction
        updateShootingDirection(dirX, dirY, delta);

        // Clamp world bounds
        clampPosition(worldWidth, worldHeight);

        // Update wall hitbox position
        wallHitbox.x = (int) (worldX + offsetX);
        wallHitbox.y = (int) (worldY + offsetY);

        // Update damage hitbox position (larger, covers body and head)
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox.x = (int) (worldX + damageOffsetX);
        damageHitbox.y = (int) (worldY + damageOffsetY);

        // Update animation state
        updateAnimationState(dirX, dirY);
    }

    // Determine which animation to play based on state
    private void updateAnimationState(float dirX, float dirY) {
        // Dead state has highest priority
        if (isDead()) {
            animationState = AnimationState.DEAD;
            return;
        }

        // Track facing direction - only update when actually moving horizontally
        // This preserves facing direction when standing still or being hit
        if (dirX < 0) {
            facingRight = false;
        } else if (dirX > 0) {
            facingRight = true;
        }
        // If dirX == 0, keep current facingRight value (don't reset)

        // Hit animation plays while timer is active
        if (hitAnimationTimer > 0) {
            animationState = AnimationState.HIT;
            return;
        }

        // Running or idle based on movement
        boolean isMoving = (dirX != 0 || dirY != 0);
        if (isMoving) {
            animationState = facingRight ? AnimationState.RUNNING_RIGHT : AnimationState.RUNNING_LEFT;
        } else {
            animationState = AnimationState.IDLE;
        }
    }

    public float getX() {
        return worldX;
    }

    public float getY() {
        return worldY;
    }

    public void setPosition(float newWorldX, float newWorldY) {
        this.worldX = newWorldX;
        this.worldY = newWorldY;
        // Update wall hitbox position
        int wallOffsetX = (SPRITE_SIZE - HITBOX_WIDTH) / 2;
        int wallOffsetY = (SPRITE_SIZE - HITBOX_HEIGHT) / 2;
        wallHitbox.x = (int) (worldX + wallOffsetX);
        wallHitbox.y = (int) (worldY + wallOffsetY);

        // Update damage hitbox position
        int damageOffsetX = (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2;
        int damageOffsetY = (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2;
        damageHitbox.x = (int) (worldX + damageOffsetX);
        damageHitbox.y = (int) (worldY + damageOffsetY);
    }

    public float getLastMoveDirectionX() {
        return smoothShootDirX;
    }

    public float getLastMoveDirectionY() {
        return smoothShootDirY;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }


    // --- HEALTH SYSTEM ---

    public void takeDamage(int amount) {
        // Don't take damage if already dying
        if (isDying) return;

        // Apply damage reduction from Iron Shield passive item
        float effectiveReduction = 1f;
        if (hasPassiveItem(PassiveItemType.IRON_SHIELD)) {
            effectiveReduction = getPassiveItem(PassiveItemType.IRON_SHIELD).getMultiplier();
        }
        
        int finalDamage = Math.max(1, (int)(amount * effectiveReduction));
        
        health -= finalDamage;
        health = (int) GameApp.clamp(health, 0, maxHealth);
        
        // Spawn blood particles when taking damage
        spawnBloodParticles();

        if (health <= 0) {
            // Start death sequence
            isDying = true;
            GameApp.resetAnimation("player_death");
        } else {
            // Trigger hit animation and reset to first frame
            hitAnimationTimer = HIT_ANIMATION_DURATION;
            GameApp.resetAnimation("player_hit");
        }
    }

    // Spawn blood particles when player takes damage
    private void spawnBloodParticles() {
        // Don't spawn if player is dying
        if (isDying) return;
        
        // Spawn 8-12 blood particles
        int particleCount = GameApp.randomInt(8, 13);
        float centerX = worldX + SPRITE_SIZE / 2f;
        float centerY = worldY + SPRITE_SIZE / 2f;
        
        for (int i = 0; i < particleCount; i++) {
            bloodParticles.add(new BloodParticle(centerX, centerY));
        }
        
    }
    
    // Update blood particles
    private void updateBloodParticles(float delta) {
        bloodParticles.removeIf(p -> !p.isAlive());
        for (BloodParticle p : bloodParticles) {
            p.update(delta);
        }
    }
    
    // Render blood particles (call this from PlayScreen)
    // Converts world coordinates to screen coordinates using player position as camera center
    public void renderBloodParticles(float playerWorldX, float playerWorldY) {
        if (bloodParticles.isEmpty()) return;
        
        float screenCenterX = GameApp.getWorldWidth() / 2f;
        float screenCenterY = GameApp.getWorldHeight() / 2f;
        
        GameApp.startShapeRenderingFilled();
        for (BloodParticle p : bloodParticles) {
            // Convert world coordinates to screen coordinates
            float screenX = screenCenterX + (p.x - playerWorldX);
            float screenY = screenCenterY + (p.y - playerWorldY);
            
            // Clamp alpha to valid range 0-255
            int alpha = Math.max(0, Math.min(255, (int)(255 * p.color.a)));
            int r = Math.max(0, Math.min(255, (int)(p.color.r * 255)));
            int g = Math.max(0, Math.min(255, (int)(p.color.g * 255)));
            int b = Math.max(0, Math.min(255, (int)(p.color.b * 255)));
            
            // Skip nearly invisible particles
            if (alpha < 5) continue;
            
            GameApp.setColor(r, g, b, alpha);
            GameApp.drawRect(screenX - p.size/2, screenY - p.size/2, p.size, p.size);
        }
        GameApp.endShapeRendering();
    }
    
    public void heal(int amount) {
        int oldHealth = health;
        health += amount;
        health = (int) GameApp.clamp(health, 0, maxHealth);
        int actualHeal = health - oldHealth;
        
        // Spawn health text if callback is set
        if (actualHeal > 0 && healthTextCallback != null) {
            // Use damage hitbox center for health text position
            float centerX = worldX + (SPRITE_SIZE - DAMAGE_HITBOX_WIDTH) / 2f + DAMAGE_HITBOX_WIDTH / 2f;
            float centerY = worldY + (SPRITE_SIZE - DAMAGE_HITBOX_HEIGHT) / 2f + DAMAGE_HITBOX_HEIGHT / 2f;
            healthTextCallback.onHeal(actualHeal, centerX, centerY);
        }
        
    }
    
    // Set callback for health text
    public void setHealthTextCallback(HealthTextCallback callback) {
        this.healthTextCallback = callback;
    }

    public boolean isDead() {
        return health <= 0;
    }

    // Check if player is in dying state (health <= 0)
    public boolean isDying() {
        return isDying;
    }

    // Check if death animation has finished playing
    public boolean isDeathAnimationFinished() {
        if (!isDying) return false;
        return GameApp.isAnimationFinished("player_death");
    }

    // Get current animation key for rendering
    public String getCurrentAnimation() {
        switch (animationState) {
            case DEAD:
                return "player_death";
            case HIT:
                return "player_hit";
            case RUNNING_LEFT:
                return "player_run_left";
            case RUNNING_RIGHT:
                return "player_run_right";
            case IDLE:
            default:
                return "player_idle";
        }
    }

    // Get facing direction for sprite flipping
    public boolean isFacingRight() {
        return facingRight;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }

    // Wall HitBox getter (for wall collision)
    public Rectangle getHitBox() {
        return wallHitbox;
    }

    // Damage HitBox getter (for enemy interaction)
    public Rectangle getDamageHitBox() {
        return damageHitbox;
    }


    // Render (now uses animations)
    public void render() {
        String animKey = getCurrentAnimation();
        GameApp.drawAnimation(animKey, worldX, worldY, SPRITE_SIZE, SPRITE_SIZE);
    }

    // Helper: Binary search X to find closest safe position
    private void doBinarySearchX(float dx, float originalX, float checkY, float w, float h, float offsetX, CollisionChecker checker, float epsilon) {
        float minX = dx > 0 ? originalX : originalX + dx;
        float maxX = dx > 0 ? originalX + dx : originalX;
        float bestX = originalX;

        for (int i = 0; i < 8; i++) {
            float midX = (minX + maxX) / 2f;
            float midCheckX = (midX + offsetX) + epsilon / 2f;
            if (!checker.checkCollision(midCheckX, checkY, w, h)) {
                bestX = midX;
                if (dx > 0) minX = midX; else maxX = midX;
            } else {
                if (dx > 0) maxX = midX; else minX = midX;
            }
        }
        worldX = bestX;
    }

    // Helper: Binary search Y to find closest safe position
    private void doBinarySearchY(float dy, float originalY, float checkX, float w, float h, float offsetY, CollisionChecker checker, float epsilon) {
        float minY = dy > 0 ? originalY : originalY + dy;
        float maxY = dy > 0 ? originalY + dy : originalY;
        float bestY = originalY;

        for (int i = 0; i < 8; i++) {
            float midY = (minY + maxY) / 2f;
            float midCheckY = (midY + offsetY) + epsilon / 2f;
            if (!checker.checkCollision(checkX, midCheckY, w, h)) {
                bestY = midY;
                if (dy > 0) minY = midY; else maxY = midY;
            } else {
                if (dy > 0) maxY = midY; else minY = midY;
            }
        }
        worldY = bestY;
    }

    private void updateShootingDirection(float dirX, float dirY, float delta) {
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        
        // Only update target direction if player is moving
        // This preserves the last direction when standing still
        if (length > 0) {
            targetShootDirX = dirX / length;
            targetShootDirY = dirY / length;

            if (dirY == 0 && dirX != 0) {
                targetShootDirY = 0f;
                targetShootDirX = (dirX > 0) ? 1f : -1f;
            }

            if (dirX == 0 && dirY != 0) {
                targetShootDirX = 0f;
                targetShootDirY = (dirY > 0) ? 1f : -1f;
            }

            if (targetShootDirY == 0f && targetShootDirX != 0f) {
                smoothShootDirX = targetShootDirX;
                smoothShootDirY = 0f;
            } else if (targetShootDirX == 0f && targetShootDirY != 0f) {
                smoothShootDirX  = 0f;
                smoothShootDirY = targetShootDirY;
            } else {
                float lerpFactor = 1f - (float) Math.exp(-SHOOT_DIRECTION_SMOOTHING * delta);
                smoothShootDirX = smoothShootDirX + (targetShootDirX - smoothShootDirX) * lerpFactor;
                smoothShootDirY = smoothShootDirY + (targetShootDirY - smoothShootDirY) * lerpFactor;
            }

            float smoothedLength = (float) Math.sqrt(smoothShootDirX * smoothShootDirX + smoothShootDirY * smoothShootDirY);
            if (smoothedLength > 0.001f) {
                smoothShootDirX = smoothShootDirX / smoothedLength;
                smoothShootDirY = smoothShootDirY / smoothedLength;
            }
        }
        // If not moving (length == 0), keep current smoothShootDirX and smoothShootDirY unchanged
    }


    private void clampPosition(int worldWidth, int worldHeight) {
        if (worldWidth < Integer.MAX_VALUE / 2 && worldHeight < Integer.MAX_VALUE / 2) {
            float maxX = worldWidth - SPRITE_SIZE;
            float maxY = worldHeight - SPRITE_SIZE;
            worldX = GameApp.clamp(worldX, 0, maxX);
            worldY = GameApp.clamp(worldY, 0, maxY);
        }
    }
    public void addXP(int amount) {
        currentXP += amount;
    }

    public boolean checkLevelUp() {
        return currentXP >= xpToNextLevel;
    }

    public void levelUp() {
        currentXP -= xpToNextLevel;
        currentLevel++;
        // Balanced XP curve: player should max level at minute 7-8 of 10-minute game
        // Early levels (1-5): Faster progression for easier start
        // Later levels: Normal progression
        if (currentLevel <= 5) {
            // Levels 1-5: Much easier (40 + level * 15)
            xpToNextLevel = 40 + currentLevel * 15;
        } else {
            // Levels 6+: Normal progression (70 + level * 30)
            xpToNextLevel = 70 + currentLevel * 30;
        }
    }

    public int getCurrentLevel() { return currentLevel; }
    public int getCurrentXP() { return currentXP; }
    public int getXPToNextLevel() { return xpToNextLevel; }

    public float getXPMagnetRange() { return xpMagnetRange; }
    
    /**
     * Get total damage multiplier from stat upgrades AND passive items.
     */
    public float getDamageMultiplier() {
        float total = damageMultiplier;
        
        // Add Power Herb passive item bonus
        if (hasPassiveItem(PassiveItemType.POWER_HERB)) {
            total *= getPassiveItem(PassiveItemType.POWER_HERB).getMultiplier();
        }
        
        return total;
    }
    
    /**
     * Get effective speed including passive item bonuses.
     */
    public float getEffectiveSpeed() {
        float effectiveSpeed = speed;
        
        // Add Swift Boots passive item bonus
        if (hasPassiveItem(PassiveItemType.SWIFT_BOOTS)) {
            effectiveSpeed *= getPassiveItem(PassiveItemType.SWIFT_BOOTS).getMultiplier();
        }
        
        return effectiveSpeed;
    }
    
    /**
     * Get cooldown reduction multiplier.
     * Note: EMPTY_TOME has been removed, cooldown is now fixed.
     */
    public float getCooldownMultiplier() {
        // No cooldown reduction passive in current version
        return 1f;
    }
    
    /**
     * Get effective max health including passive item bonuses.
     */
    public int getEffectiveMaxHealth() {
        float effectiveMax = maxHealth;
        
        // Add Vitality Core passive item bonus
        if (hasPassiveItem(PassiveItemType.VITALITY_CORE)) {
            effectiveMax = baseMaxHealth * getPassiveItem(PassiveItemType.VITALITY_CORE).getMultiplier();
        }
        
        return (int) effectiveMax;
    }

    public void applyStatUpgrade(StatUpgradeType type) {
        switch (type) {
            case SPEED -> {
                if (speedLevel < 5) { // Max 5 levels (like VS Wings item)
                    speedLevel++;
                    // Recalculate speed: base + 10% per level (Wings gives +50% total at max)
                    speed = baseSpeed * (1f + (speedLevel * 0.10f));
                }
            }
            case DAMAGE -> {
                if (damageLevel < 5) { // Max 5 levels
                    damageLevel++;
                    // Recalculate damage multiplier: +5% per level
                    damageMultiplier = 1f + (damageLevel * 0.05f);
                }
            }
            case XP_MAGNET -> {
                if (xpMagnetLevel < 2) { // Max 2 levels
                    xpMagnetLevel++;
                    // Recalculate magnet range: base + 25% per level
                    xpMagnetRange = 100f * (1f + (xpMagnetLevel * 0.25f));
                }
            }
            case MAX_HEALTH -> {
                if (maxHealthLevel < 3) { // Max 3 levels
                    maxHealthLevel++;
                    // Recalculate max health: base + 10% per level
                    int newMaxHealth = (int)(baseMaxHealth * (1f + (maxHealthLevel * 0.1f)));
                    maxHealth = newMaxHealth;
                    // Keep current health, don't auto-heal (like Vampire Survivors)
                    // Health percentage will decrease as max increases
                }
            }
            case HEALTH_REGEN -> {
                // Cap at 5 levels (0.5 HP/s max)
                if (healthRegenLevel < MAX_REGEN_LEVEL) {
                    healthRegenLevel++;
                }
            }
        }
    }
    
    // Get upgrade level for display
    public int getUpgradeLevel(StatUpgradeType type) {
        return switch (type) {
            case SPEED -> speedLevel;
            case DAMAGE -> damageLevel;
            case XP_MAGNET -> xpMagnetLevel;
            case MAX_HEALTH -> maxHealthLevel;
            case HEALTH_REGEN -> healthRegenLevel;
        };
    }
    
    // Get max level for upgrade type
    public int getMaxUpgradeLevel(StatUpgradeType type) {
        return switch (type) {
            case SPEED -> 2;
            case DAMAGE -> 5;
            case XP_MAGNET -> 2;
            case MAX_HEALTH -> 3;
            case HEALTH_REGEN -> 5;
        };
    }
    
    // Check if upgrade is at max level
    public boolean isUpgradeMaxed(StatUpgradeType type) {
        return getUpgradeLevel(type) >= getMaxUpgradeLevel(type);
    }

    // ============================================
    // PASSIVE ITEMS SYSTEM
    // ============================================

    /**
     * Add a new passive item or level up existing one.
     * @return true if successfully added/leveled up
     */
    public boolean addOrLevelUpPassiveItem(PassiveItemType type) {
        if (passiveItems.containsKey(type)) {
            // Level up existing
            PassiveItem item = passiveItems.get(type);
            if (item.levelUp()) {
                GameApp.log("Passive item " + type.displayName + " leveled up to " + item.getLevel());
                applyPassiveItemEffects();
                return true;
            }
            return false; // Already maxed
        } else {
            // Add new
            PassiveItem item = new PassiveItem(type);
            passiveItems.put(type, item);
            GameApp.log("Acquired new passive item: " + type.displayName);
            applyPassiveItemEffects();
            return true;
        }
    }

    /**
     * Check if player has a specific passive item.
     */
    public boolean hasPassiveItem(PassiveItemType type) {
        return passiveItems.containsKey(type);
    }

    /**
     * Get a specific passive item.
     */
    public PassiveItem getPassiveItem(PassiveItemType type) {
        return passiveItems.get(type);
    }

    /**
     * Get level of a passive item (0 if not owned).
     */
    public int getPassiveItemLevel(PassiveItemType type) {
        if (passiveItems.containsKey(type)) {
            return passiveItems.get(type).getLevel();
        }
        return 0;
    }

    /**
     * Check if a passive item is at max level.
     */
    public boolean isPassiveItemMaxed(PassiveItemType type) {
        if (passiveItems.containsKey(type)) {
            return passiveItems.get(type).isMaxLevel();
        }
        return false;
    }

    /**
     * Get list of all owned passive items.
     */
    public List<PassiveItem> getOwnedPassiveItems() {
        return new ArrayList<>(passiveItems.values());
    }

    /**
     * Get count of owned passive items.
     */
    public int getPassiveItemCount() {
        return passiveItems.size();
    }

    /**
     * Check if ALL passive items are owned AND at max level.
     * This is required for weapon evolution.
     */
    public boolean areAllPassiveItemsMaxed() {
        // Must have all 6 passive items
        if (passiveItems.size() < PassiveItemType.values().length) {
            return false;
        }
        
        // All must be at max level
        for (PassiveItem item : passiveItems.values()) {
            if (!item.isMaxLevel()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Apply passive item effects to player stats.
     * Called after adding or leveling up passive items.
     */
    private void applyPassiveItemEffects() {
        // Vitality Core - Max HP increase (recalculate max health)
        if (hasPassiveItem(PassiveItemType.VITALITY_CORE)) {
            float multiplier = getPassiveItem(PassiveItemType.VITALITY_CORE).getMultiplier();
            int newMaxHealth = (int)(baseMaxHealth * multiplier);
            // Also add stat upgrade bonus
            newMaxHealth = (int)(newMaxHealth * (1f + (maxHealthLevel * 0.1f)));
            maxHealth = newMaxHealth;
            // Don't change current health percentage
        }
        
        // Other effects are calculated on-the-fly in their respective methods
        // (getDamageMultiplier, getEffectiveSpeed, getCooldownMultiplier, etc.)
    }

    /**
     * Get list of available passive items that can still be upgraded.
     */
    public List<PassiveItemType> getAvailablePassiveItemUpgrades() {
        List<PassiveItemType> available = new ArrayList<>();
        
        for (PassiveItemType type : PassiveItemType.values()) {
            if (!hasPassiveItem(type)) {
                // Don't have it yet - can acquire
                available.add(type);
            } else if (!isPassiveItemMaxed(type)) {
                // Have it but not maxed - can level up
                available.add(type);
            }
        }
        
        return available;
    }
}
