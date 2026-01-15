package nl.saxion.game.systems;

import nl.saxion.game.entities.LevelUpOption;
import nl.saxion.game.entities.PassiveItemType;
import nl.saxion.game.entities.StatUpgradeType;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.Weapon;
import nl.saxion.gameapp.GameApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GachaSystem - Vampire Survivors style gacha with ornate frame
 * 
 * Animation Flow:
 * 1. CHEST_SHINY - Chest glows (shiny1-11)
 * 2. CHEST_OPENING - Chest opens (open1 -> open2)
 * 3. BEAM_RISE - Blue beam shoots up from chest
 * 4. ITEMS_SCROLL - Items scroll smoothly in beam
 * 5. FINAL_REVEAL - Single item appears in center
 * 6. COMPLETED - Ready to claim
 */
public class GachaSystem {
    
    // Gacha states
    public enum GachaState {
        INACTIVE,
        CHEST_SHINY,
        CHEST_OPENING,
        BEAM_RISE,
        ITEMS_SCROLL,
        FINAL_REVEAL,
        COMPLETED
    }
    
    private GachaState state = GachaState.INACTIVE;
    
    // Animation timers
    private float stateTimer = 0f;
    private static final float SHINY_DURATION = 1.0f;
    private static final float OPENING_DURATION = 0.6f;
    private static final float BEAM_DURATION = 0.8f;
    private static final float SCROLL_DURATION = 15.0f; // Increased from 10.0f to 15.0f (+5 seconds) to match music
    private static final float SLOW_DURATION = 3.0f;
    private static final float REVEAL_DURATION = 1.5f;
    
    // Track if upgrade was already applied
    private boolean upgradeApplied = false;
    private boolean wasMusicPlaying = false;
    
    // Scroll system
    private List<GachaItem> scrollItems = new ArrayList<>();
    private float scrollOffset = 0f;
    private float scrollSpeed = 500f;
    private GachaItem selectedItem = null;
    private int selectedItemIndex = -1;
    private float targetScrollOffset = 0f;
    
    // Sound manager reference
    private SoundManager soundManager;
    
    // Player/Weapon references
    private Player player;
    private Weapon weapon;
    
    // Visual effects
    private float flashAlpha = 0f;
    private float celebrateScale = 1f;
    
    // Frame dimensions (Vampire Survivors style)
    private float frameX, frameY, frameWidth, frameHeight;
    private float innerFrameX, innerFrameY, innerFrameWidth, innerFrameHeight;
    
    // Chest position (relative to frame center)
    private float chestX, chestY;
    private boolean chestPositionSet = false;
    
    // Random score points
    private int randomPoints = 0;
    private float finalPoints = 0f; // Final points value (stops animating)
    
    // Burst particles
    private List<Particle> particles = new ArrayList<>();
    
    // Coin particles (casino effect - burst from chest)
    private List<CoinParticle> coinParticles = new ArrayList<>();
    private float coinSpawnTimer = 0f;
    private static final float COIN_BURST_INTERVAL = 0.03f; // Spawn burst faster for dense fountain
    private static final int COINS_PER_BURST = 12; // More coins per burst for impressive effect
    
    // Random
    private Random random = new Random();
    
    // Skip animation flag
    private boolean skipRequested = false;
    
    // Snake ray effect (white rays circling the beam)
    private List<SnakeRay> snakeRays = new ArrayList<>();
    private static final int NUM_SNAKE_RAYS = 6;
    
    // Coin particle class (for casino effect - gold and silver)
    // Coins shoot UP then fall down in parabolic arc like a fountain
    private class CoinParticle {
        float x, y, vx, vy;
        float rotation, rotationSpeed;
        float life, maxLife;
        float size;
        float alpha;
        int coinType; // 0 = drawn gold, 1 = drawn silver, 2 = gold.png, 3 = silver.png
        
        CoinParticle(float x, float y, float vx, float vy, float life, float size, int coinType) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.rotation = 0f;
            this.rotationSpeed = (random.nextFloat() - 0.5f) * 720f; // Random rotation speed
            this.life = life; this.maxLife = life;
            this.size = size;
            this.alpha = 1f;
            this.coinType = coinType;
        }
        
        void update(float delta) {
            x += vx * delta;
            y += vy * delta;
            vy -= 450f * delta; // Gravity pulls coins down after they shoot up
            vx *= 0.99f; // Slight air resistance
            rotation += rotationSpeed * delta;
            life -= delta;
            
            // Fade out only in last 30% of life
            if (life < maxLife * 0.3f) {
                alpha = life / (maxLife * 0.3f);
            } else {
                alpha = 1f;
            }
        }
        
        boolean isDead() { return life <= 0 || y < -100f; }
    }
    
    // Snake ray class - white rays that circle the beam in snake-like motion
    private class SnakeRay {
        float angle; // Current angle around the beam
        float yOffset; // Vertical offset (snake-like undulation)
        float speed; // Angular speed
        float amplitude; // How far from center
        float phase; // Phase offset for snake motion
        float length; // Ray length
        
        SnakeRay(float startAngle, float speed, float amplitude) {
            this.angle = startAngle;
            this.yOffset = 0f;
            this.speed = speed;
            this.amplitude = amplitude;
            this.phase = random.nextFloat() * (float)Math.PI * 2f;
            this.length = 30f + random.nextFloat() * 20f;
        }
        
        void update(float delta, float time) {
            angle += speed * delta;
            if (angle > 360f) angle -= 360f;
            // Snake-like vertical undulation
            yOffset = (float)Math.sin(time * 3f + phase) * 50f;
        }
    }
    
    // Particle class
    private static class Particle {
        float x, y, vx, vy;
        float life, maxLife;
        int r, g, b;
        float size;
        
        Particle(float x, float y, float vx, float vy, float life, int r, int g, int b, float size) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life;
            this.r = r; this.g = g; this.b = b;
            this.size = size;
        }
        
        void update(float delta) {
            x += vx * delta;
            y += vy * delta;
            vy -= 150 * delta;
            life -= delta;
        }
        
        boolean isDead() { return life <= 0; }
        float getAlpha() { return Math.max(0, life / maxLife); }
    }
    
    // Rarity enum
    public enum Rarity {
        COMMON(60, new int[]{180, 180, 180}, "gray-400"),
        UNCOMMON(25, new int[]{100, 220, 100}, "green-500"),
        RARE(12, new int[]{100, 150, 255}, "blue-500"),
        EPIC(2.5f, new int[]{200, 100, 255}, "purple-500"),
        LEGENDARY(0.5f, new int[]{255, 215, 0}, "yellow-400");
        
        public final float weight;
        public final int[] rgb;
        public final String color;
        
        Rarity(float weight, int[] rgb, String color) {
            this.weight = weight;
            this.rgb = rgb;
            this.color = color;
        }
    }
    
    // GachaItem class
    public static class GachaItem {
        public final String name;
        public final String description;
        public final String textureKey;
        public final Rarity rarity;
        public final LevelUpOption.Type upgradeType;
        public final StatUpgradeType statType;
        public final PassiveItemType passiveType;
        
        public GachaItem(String name, String description, Rarity rarity, PassiveItemType passiveType) {
            this.name = name;
            this.description = description;
            this.textureKey = passiveType.getTextureKey();
            this.rarity = rarity;
            this.upgradeType = LevelUpOption.Type.PASSIVE;
            this.statType = null;
            this.passiveType = passiveType;
        }
        
        public GachaItem(String name, String description, Rarity rarity) {
            this.name = name;
            this.description = description;
            this.textureKey = "piston_icon";  // Use piston icon for weapon
            this.rarity = rarity;
            this.upgradeType = LevelUpOption.Type.WEAPON;
            this.statType = null;
            this.passiveType = null;
        }
        
        public GachaItem(String name, String description, String textureKey, Rarity rarity, boolean isSpecial) {
            this.name = name;
            this.description = description;
            this.textureKey = textureKey;
            this.rarity = rarity;
            this.upgradeType = LevelUpOption.Type.STAT;
            this.statType = null;
            this.passiveType = null;
        }
    }
    
    public GachaSystem() {
        buildItemPool();
        calculateFrameDimensions();
    }
    
    private void calculateFrameDimensions() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        // Frame size (smaller to fit background - reduced margins)
        frameWidth = screenWidth * 0.36f;  // Smaller width (was 0.4f)
        frameHeight = screenHeight * 0.54f; // Smaller height (was 0.6f)
        
        // Center frame
        frameX = (screenWidth - frameWidth) / 2f;
        frameY = (screenHeight - frameHeight) / 2f;
        
        // Inner frame (dark blue background)
        float borderThickness = 8f;
        innerFrameX = frameX + borderThickness;
        innerFrameY = frameY + borderThickness;
        innerFrameWidth = frameWidth - borderThickness * 2;
        innerFrameHeight = frameHeight - borderThickness * 2;
    }
    
    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }
    
    private void buildItemPool() {
        scrollItems.clear();
        
        // COMMON
        scrollItems.add(new GachaItem("Power Herb", "+10% Damage", Rarity.COMMON, PassiveItemType.POWER_HERB));
        scrollItems.add(new GachaItem("Iron Shield", "-5% Damage Taken", Rarity.COMMON, PassiveItemType.IRON_SHIELD));
        scrollItems.add(new GachaItem("Swift Boots", "+10% Move Speed", Rarity.COMMON, PassiveItemType.SWIFT_BOOTS));
        
        // UNCOMMON
        scrollItems.add(new GachaItem("Lucky Coin", "+5% Critical Chance", Rarity.UNCOMMON, PassiveItemType.LUCKY_COIN));
        scrollItems.add(new GachaItem("Magnet Stone", "+20% Pickup Range", Rarity.UNCOMMON, PassiveItemType.MAGNET_STONE));
        
        // RARE
        scrollItems.add(new GachaItem("Life Essence", "+0.2 HP/sec Regen", Rarity.RARE, PassiveItemType.LIFE_ESSENCE));
        scrollItems.add(new GachaItem("Vitality Core", "+20% Max HP", Rarity.RARE, PassiveItemType.VITALITY_CORE));
        
        // EPIC
        scrollItems.add(new GachaItem("Weapon Power", "Upgrade Weapon!", Rarity.EPIC));
        
        // LEGENDARY
        scrollItems.add(new GachaItem("MEGA BOOST", "+1 ALL Passives!", "piston_icon", Rarity.LEGENDARY, true));
    }
    
    /**
     * Set chest position (from PlayScreen, will be converted to frame coordinates)
     */
    public void setChestPosition(float worldX, float worldY) {
        // Convert world position to frame-relative position
        // Chest will be at bottom center of inner frame
        // innerFrameY is bottom, so add value to go up from bottom
        chestX = innerFrameX + innerFrameWidth / 2f;
            chestY = innerFrameY + 140f; // 140f from bottom (higher to give space for points/button)
        chestPositionSet = true;
    }
    
    /**
     * Calculate random points based on item rarity (balanced)
     */
    private int calculateRandomPoints(GachaItem item) {
        int basePoints = 50;
        
        switch (item.rarity) {
            case COMMON:
                return basePoints + random.nextInt(50); // 50-100
            case UNCOMMON:
                return basePoints * 2 + random.nextInt(100); // 100-200
            case RARE:
                return basePoints * 3 + random.nextInt(150); // 150-300
            case EPIC:
                return basePoints * 5 + random.nextInt(250); // 250-500
            case LEGENDARY:
                return basePoints * 10 + random.nextInt(500); // 500-1000
            default:
                return basePoints;
        }
    }
    
    /**
     * Start the gacha sequence
     */
    public void start() {
        if (state != GachaState.INACTIVE) return;
        
        calculateFrameDimensions();
        
        if (!chestPositionSet) {
            // Default chest position - at bottom of frame (higher to avoid overlap)
            // innerFrameY is bottom, so add value to go up from bottom
            chestX = innerFrameX + innerFrameWidth / 2f;
            chestY = innerFrameY + 140f; // 140f from bottom (higher to give space for points/button)
        }
        
        state = GachaState.CHEST_SHINY;
        stateTimer = 0f;
        scrollOffset = 0f;
        scrollSpeed = 500f;
        flashAlpha = 0f;
        selectedItem = null;
        selectedItemIndex = -1;
        upgradeApplied = false;
        particles.clear();
        coinParticles.clear();
        coinSpawnTimer = 0f;
        
        // Load fonts if not already loaded
        ensureFontsLoaded();
        
        // Stop game music
        if (soundManager != null) {
            wasMusicPlaying = soundManager.isIngameMusicPlaying();
            soundManager.stopIngameMusic();
        }
        
        // Pre-select result
        selectedItem = selectRandomItem();
        randomPoints = calculateRandomPoints(selectedItem);
        finalPoints = randomPoints; // Store final points
        
        // Find selected item index
        for (int i = 0; i < scrollItems.size(); i++) {
            if (scrollItems.get(i) == selectedItem) {
                selectedItemIndex = i;
                break;
            }
        }
        
        // Calculate target scroll offset
        float itemSpacing = 80f;
        // Beam from chest to top of inner frame
        float beamTop = innerFrameY + innerFrameHeight; // Top of inner frame
        float beamHeight = beamTop - chestY;
        float beamCenterY = chestY + beamHeight / 2f; // Center of beam
        float scrollStartY = chestY + 80f;
        
        targetScrollOffset = scrollStartY - beamCenterY + (selectedItemIndex * itemSpacing);
        targetScrollOffset += scrollItems.size() * itemSpacing * 4; // Multiple cycles
        
        // Initialize snake rays for beam effect
        initializeSnakeRays();
    }
    
    /**
     * Initialize snake rays that circle around the beam
     */
    private void initializeSnakeRays() {
        snakeRays.clear();
        for (int i = 0; i < NUM_SNAKE_RAYS; i++) {
            float startAngle = (360f / NUM_SNAKE_RAYS) * i;
            float speed = 120f + random.nextFloat() * 60f; // 120-180 degrees per second
            float amplitude = 40f + random.nextFloat() * 30f; // 40-70 pixels from center
            snakeRays.add(new SnakeRay(startAngle, speed, amplitude));
        }
    }
    
    /**
     * Skip animation when SPACE is pressed - jump to final result
     */
    public void requestSkip() {
        if (state == GachaState.INACTIVE || state == GachaState.COMPLETED) return;
        skipRequested = true;
    }
    
    /**
     * Update the gacha animation
     */
    public void update(float delta) {
        if (state == GachaState.INACTIVE) return;
        
        // Handle skip request - jump directly to completed state
        if (skipRequested && state != GachaState.COMPLETED) {
            skipRequested = false;
            state = GachaState.COMPLETED;
            stateTimer = 0f;
            scrollOffset = targetScrollOffset;
            finalPoints = randomPoints;
            coinParticles.clear(); // Clear coin effects
            GameApp.stopAllSounds(); // Stop gacha sounds when skipping
            GameApp.log("Gacha animation skipped - showing final result");
            return;
        }
        
        stateTimer += delta;
        
        // Update snake rays
        for (SnakeRay ray : snakeRays) {
            ray.update(delta, stateTimer);
        }
        
        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) {
            p.update(delta);
        }
        
        // Update coin particles (casino effect)
        coinParticles.removeIf(CoinParticle::isDead);
        for (CoinParticle coin : coinParticles) {
            coin.update(delta);
        }
        
        // State machine
        switch (state) {
            case CHEST_SHINY:
                if (random.nextFloat() < 0.3f) {
                    spawnSparkle();
                }
                
                if (stateTimer >= SHINY_DURATION) {
                    state = GachaState.CHEST_OPENING;
                    stateTimer = 0f;
                }
                break;
                
            case CHEST_OPENING:
                if (stateTimer >= OPENING_DURATION) {
                    state = GachaState.BEAM_RISE;
                    stateTimer = 0f;
                    if (soundManager != null) {
                        soundManager.playSound("jackpot", 1.0f);
                    }
                    createBeamParticles();
                }
                break;
                
            case BEAM_RISE:
                if (stateTimer >= BEAM_DURATION) {
                    state = GachaState.ITEMS_SCROLL;
                    stateTimer = 0f;
                }
                break;
                
            case ITEMS_SCROLL:
                scrollOffset += scrollSpeed * delta;
                
                // Spawn coin particles (casino effect - burst from chest)
                coinSpawnTimer += delta;
                if (coinSpawnTimer >= COIN_BURST_INTERVAL) {
                    coinSpawnTimer = 0f;
                    spawnCoinBurst(); // Spawn burst of coins
                }
                
                if (stateTimer >= SCROLL_DURATION) {
                    state = GachaState.FINAL_REVEAL;
                    stateTimer = 0f;
                }
                break;
                
            case FINAL_REVEAL:
                // Ease to final position
                float revealProgress = stateTimer / REVEAL_DURATION;
                float ease = easeOutCubic(Math.min(1f, revealProgress));
                
                float startOffset = scrollOffset;
                scrollOffset = startOffset + (targetScrollOffset - startOffset) * ease;
                scrollSpeed = 500f * (1f - ease);
                
                flashAlpha = 1f - ease;
                celebrateScale = 1f + 0.3f * (float)Math.sin(stateTimer * 8f);
                
                if (stateTimer >= REVEAL_DURATION) {
                    scrollOffset = targetScrollOffset;
                    state = GachaState.COMPLETED;
                    stateTimer = 0f;
                    finalPoints = randomPoints; // Stop points animation
                }
                break;
                
            case COMPLETED:
                celebrateScale = 1f + 0.1f * (float)Math.sin(stateTimer * 5f);
                break;
                
            default:
                break;
        }
    }
    
    private float easeOutCubic(float t) {
        return 1f - (float)Math.pow(1 - t, 3);
    }
    
    private void spawnSparkle() {
        float angle = random.nextFloat() * 360f;
        float dist = 25f + random.nextFloat() * 40f;
        float px = chestX + (float)Math.cos(Math.toRadians(angle)) * dist;
        float py = chestY + (float)Math.sin(Math.toRadians(angle)) * dist;
        
        particles.add(new Particle(
            px, py,
            (random.nextFloat() - 0.5f) * 40f,
            random.nextFloat() * 80f + 40f,
            0.4f + random.nextFloat() * 0.4f,
            255, 255, 150,
            2f + random.nextFloat() * 2f
        ));
    }
    
    private void createBeamParticles() {
        for (int i = 0; i < 40; i++) {
            float angle = -90f + (random.nextFloat() - 0.5f) * 30f;
            float speed = 200f + random.nextFloat() * 300f;
            float vx = (float)Math.cos(Math.toRadians(angle)) * speed;
            float vy = (float)Math.sin(Math.toRadians(angle)) * speed + 300f;
            
            int[][] colors = {{100, 150, 255}, {150, 200, 255}, {200, 230, 255}}; // Blue tones
            int[] c = colors[random.nextInt(colors.length)];
            
            particles.add(new Particle(
                chestX + (random.nextFloat() - 0.5f) * 20f,
                chestY + 15f,
                vx, vy,
                1f + random.nextFloat() * 1.5f,
                c[0], c[1], c[2],
                4f + random.nextFloat() * 5f
            ));
        }
    }
    
    /**
     * Spawn coin burst from chest - FOUNTAIN STYLE!
     * Coins shoot UP and spread to ALL directions, then fall down in parabolic arcs
     * Coins have rotation and fall to multiple points across the screen
     */
    private void spawnCoinBurst() {
        // Spawn coins in multiple directions for better visual effect
        for (int i = 0; i < COINS_PER_BURST; i++) {
            // Varied angles - mostly upward but some go to sides for wider spread
            float angle;
            float speedMultiplier;
            
            if (i % 5 == 0) {
                // Every 5th coin goes to left side
                angle = -135f + (random.nextFloat() - 0.5f) * 30f;
                speedMultiplier = 0.8f;
            } else if (i % 5 == 1) {
                // Every 5th+1 coin goes to right side
                angle = -45f + (random.nextFloat() - 0.5f) * 30f;
                speedMultiplier = 0.8f;
            } else if (i % 5 == 2) {
                // Some coins go straight up high
                angle = -90f + (random.nextFloat() - 0.5f) * 20f;
                speedMultiplier = 1.2f;
            } else {
                // Rest spread in wide upward arc
                angle = -90f + (random.nextFloat() - 0.5f) * 120f; // ±60 degree spread
                speedMultiplier = 1.0f;
            }
            
            // Varied initial velocity for natural fountain look
            float baseSpeed = 350f + random.nextFloat() * 300f;
            float speed = baseSpeed * speedMultiplier;
            
            // Calculate velocity
            float vx = (float)Math.cos(Math.toRadians(angle)) * speed;
            float vy = Math.abs((float)Math.sin(Math.toRadians(angle)) * speed);
            vy += 150f + random.nextFloat() * 100f; // Varied upward boost
            
            // Extra horizontal variance for more spread
            vx += (random.nextFloat() - 0.5f) * 300f;
            
            // Prefer texture coins (gold.png and silver.png) for better visuals
            // 0 = drawn gold, 1 = drawn silver, 2 = gold.png, 3 = silver.png
            int coinType;
            float typeRoll = random.nextFloat();
            if (typeRoll < 0.15f) coinType = 0; // 15% drawn gold
            else if (typeRoll < 0.25f) coinType = 1; // 10% drawn silver
            else if (typeRoll < 0.65f) coinType = 2; // 40% gold.png texture
            else coinType = 3; // 35% silver.png texture
            
            float life = 2.8f + random.nextFloat() * 1.5f; // Long life for full arc
            float size = 10f + random.nextFloat() * 14f; // 10-24 pixels (bigger coins)
            
            // Spawn from chest with wider spread
            float spawnX = chestX + (random.nextFloat() - 0.5f) * 40f;
            float spawnY = chestY + 15f + random.nextFloat() * 15f; // Varied height
            
            coinParticles.add(new CoinParticle(spawnX, spawnY, vx, vy, life, size, coinType));
        }
    }
    
    /**
     * Ensure fonts are loaded
     */
    private void ensureFontsLoaded() {
        // Load button font if not exists
        if (!GameApp.hasFont("gachaButtonFont")) {
            try {
                GameApp.addStyledFont("gachaButtonFont", "fonts/upheavtt.ttf", 32,
                    "white", 2f, "black", 2, 2, "gray-700", true);
            } catch (Exception e) {
                GameApp.log("Warning: Could not load gachaButtonFont");
            }
        }
        
        // Load points font if not exists
        if (!GameApp.hasFont("gachaPointsFont")) {
            try {
                GameApp.addStyledFont("gachaPointsFont", "fonts/PressStart2P-Regular.ttf", 18,
                    "yellow-500", 1.5f, "black", 2, 2, "orange-900", true);
            } catch (Exception e) {
                GameApp.log("Warning: Could not load gachaPointsFont");
            }
        }
    }
    
    private GachaItem selectRandomItem() {
        float totalWeight = 0;
        for (GachaItem item : scrollItems) {
            totalWeight += item.rarity.weight;
        }
        
        float roll = random.nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (GachaItem item : scrollItems) {
            cumulative += item.rarity.weight;
            if (roll <= cumulative) {
                return item;
            }
        }
        
        return scrollItems.get(0);
    }
    
    /**
     * Render the gacha with Vampire Survivors style frame
     */
    public void render() {
        if (state == GachaState.INACTIVE) return;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        // Recalculate frame if needed
        calculateFrameDimensions();
        
        // 1. Render semi-transparent dark overlay (similar to pause/gameover)
        // Game background is rendered by PlayScreen before this, so we can see it through overlay
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(0, 0, 0, 100); // Lighter overlay (40% opacity) - game should be clearly visible
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // 2. Render ornate golden frame (outer border)
        renderOrnateFrame();
        
        // 3. Render dark blue inner frame background
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(20, 30, 60, 255); // Dark blue background
        GameApp.drawRect(innerFrameX, innerFrameY, innerFrameWidth, innerFrameHeight);
        GameApp.endShapeRendering();
        
        // 4. Render coin particles (casino effect - render first so they appear behind frame)
        renderCoinParticles();
        
        // 4.5. Snake rays removed - design looked bad
        
        // 5. Render particles
        renderParticles();
        
        // 6. Render gacha content based on state
        switch (state) {
            case CHEST_SHINY:
                renderChestShiny();
                break;
            case CHEST_OPENING:
                renderChestOpening();
                break;
            case BEAM_RISE:
                renderBeamRise();
                break;
            case ITEMS_SCROLL:
            case FINAL_REVEAL:
                renderItemsScroll();
                break;
            case COMPLETED:
                renderFinalResult();
                break;
        }
        
        // 7. Render random points display (bottom of frame)
        renderPointsDisplay();
    }
    
    private void renderCoinParticles() {
        if (coinParticles.isEmpty()) return;
        
        // Load gold and silver textures if not loaded
        if (!GameApp.hasTexture("gold_coin")) {
            try { GameApp.addTexture("gold_coin", "assets/ui/gold.png"); } catch (Exception e) {}
        }
        if (!GameApp.hasTexture("silver_coin")) {
            try { GameApp.addTexture("silver_coin", "assets/ui/silver.png"); } catch (Exception e) {}
        }
        
        // First pass: Draw shape-based coins (types 0 and 1)
        GameApp.startShapeRenderingFilled();
        for (CoinParticle coin : coinParticles) {
            if (coin.coinType > 1) continue; // Skip texture coins
            
            int alpha = (int)(255 * coin.alpha);
            
            // Gold or silver coin color based on coinType
            if (coin.coinType == 0) {
                // Gold coin (bright yellow)
                GameApp.setColor(255, 215, 0, alpha);
            } else {
                // Silver coin (muted gray, lighter)
                GameApp.setColor(192, 192, 200, (int)(alpha * 0.7f));
            }
            
            // Draw coin as overlapping rectangles (simple coin shape)
            float halfSize = coin.size / 2f;
            GameApp.drawRect(coin.x - halfSize, coin.y - halfSize/2, coin.size, halfSize);
            GameApp.drawRect(coin.x - halfSize/2, coin.y - halfSize, halfSize, coin.size);
        }
        GameApp.endShapeRendering();
        
        // Second pass: Draw texture-based coins (types 2 and 3) WITH ROTATION
        GameApp.startSpriteRendering();
        for (CoinParticle coin : coinParticles) {
            if (coin.coinType < 2) continue; // Skip shape coins
            
            String textureKey = (coin.coinType == 2) ? "gold_coin" : "silver_coin";
            if (GameApp.hasTexture(textureKey)) {
                float halfSize = coin.size / 2f;
                // Draw with rotation for spinning coin effect
                GameApp.drawTexture(textureKey, coin.x - halfSize, coin.y - halfSize, 
                    coin.size, coin.size, coin.rotation, false, false);
            }
        }
        GameApp.endSpriteRendering();
    }
    
    /**
     * Render snake rays - white rays that circle around the beam in semicircle pattern
     * Like snakes undulating around the blue pillar
     */
    private void renderSnakeRays() {
        if (snakeRays.isEmpty()) return;
        
        float beamCenterX = chestX;
        float beamTop = innerFrameY + innerFrameHeight;
        float beamHeight = beamTop - chestY;
        float beamCenterY = chestY + beamHeight / 2f;
        
        GameApp.startShapeRenderingFilled();
        
        for (SnakeRay ray : snakeRays) {
            // Calculate position on semicircle around beam
            float angleRad = (float) Math.toRadians(ray.angle);
            
            // Snake undulation - rays move up and down while circling
            float snakeY = beamCenterY + ray.yOffset;
            
            // Position on circle around beam center
            float rayX = beamCenterX + (float) Math.cos(angleRad) * ray.amplitude;
            float rayY = snakeY + (float) Math.sin(angleRad * 2f) * 30f; // Extra vertical undulation
            
            // Draw white ray (glow effect with multiple sizes)
            // Outer glow
            GameApp.setColor(255, 255, 255, 60);
            GameApp.drawRect(rayX - ray.length/2, rayY - 4f, ray.length, 8f);
            
            // Inner bright
            GameApp.setColor(255, 255, 255, 180);
            GameApp.drawRect(rayX - ray.length/2 + 5f, rayY - 2f, ray.length - 10f, 4f);
            
            // Core white
            GameApp.setColor(255, 255, 255, 255);
            GameApp.drawRect(rayX - ray.length/2 + 10f, rayY - 1f, ray.length - 20f, 2f);
        }
        
        GameApp.endShapeRendering();
    }
    
    private void renderOrnateFrame() {
        GameApp.startShapeRenderingFilled();
        
        // Golden outer border (thinner - reduced by 2f)
        GameApp.setColor(255, 215, 0, 255);
        GameApp.drawRect(frameX, frameY, frameWidth, 4); // Top (was 6)
        GameApp.drawRect(frameX, frameY + frameHeight - 4, frameWidth, 4); // Bottom (was 6)
        GameApp.drawRect(frameX, frameY, 4, frameHeight); // Left (was 6)
        GameApp.drawRect(frameX + frameWidth - 4, frameY, 4, frameHeight); // Right (was 6)
        
        // Corner decorations (4 yellow squares at corners)
        float cornerSize = 10f;
        GameApp.setColor(255, 200, 50, 255);
        // Top-left corner
        GameApp.drawRect(frameX - 2, frameY - 2, cornerSize, cornerSize);
        // Top-right corner
        GameApp.drawRect(frameX + frameWidth - 8, frameY - 2, cornerSize, cornerSize);
        // Bottom-left corner
        GameApp.drawRect(frameX - 2, frameY + frameHeight - 8, cornerSize, cornerSize);
        // Bottom-right corner
        GameApp.drawRect(frameX + frameWidth - 8, frameY + frameHeight - 8, cornerSize, cornerSize);
        
        GameApp.endShapeRendering();
    }
    
    private void renderParticles() {
        if (particles.isEmpty()) return;
        
        GameApp.startShapeRenderingFilled();
        for (Particle p : particles) {
            int alpha = (int)(255 * p.getAlpha());
            GameApp.setColor(p.r, p.g, p.b, alpha);
            GameApp.drawRect(p.x - p.size/2, p.y - p.size/2, p.size, p.size);
        }
        GameApp.endShapeRendering();
    }
    
    private void renderChestShiny() {
        GameApp.startShapeRenderingFilled();
        
        float glowSize = 60f + 15f * (float)Math.sin(stateTimer * 10f);
        GameApp.setColor(255, 215, 0, 70);
        GameApp.drawRect(chestX - glowSize/2, chestY - glowSize/2, glowSize, glowSize);
        
        GameApp.endShapeRendering();
        
        GameApp.startSpriteRendering();
        int frame = 1 + (int)(stateTimer * 8) % 11;
        String textureKey = "chest_shiny_" + frame;
        float chestSize = 72f; // Larger chest
        if (GameApp.hasTexture(textureKey)) {
            GameApp.drawTexture(textureKey, chestX - chestSize/2, chestY - chestSize/2, chestSize, chestSize);
        }
        GameApp.endSpriteRendering();
    }
    
    private void renderChestOpening() {
        // No yellow glow square - just render the chest opening animation
        float progress = stateTimer / OPENING_DURATION;
        
        GameApp.startSpriteRendering();
        String textureKey = progress < 0.5f ? "chest_open_1" : "chest_open_2";
        float chestSize = 72f; // Larger chest
        if (GameApp.hasTexture(textureKey)) {
            GameApp.drawTexture(textureKey, chestX - chestSize/2, chestY - chestSize/2, chestSize, chestSize);
        }
        GameApp.endSpriteRendering();
    }
    
    private void renderBeamRise() {
        GameApp.startShapeRenderingFilled();
        
        // Blue beam shooting up from chest
        float beamProgress = stateTimer / BEAM_DURATION;
        float beamWidth = 45f;
        float beamHeight = (innerFrameHeight * 0.6f) * beamProgress; // Up to 60% of frame height
        float beamY = chestY;
        
        // Beam gradient (lighter at top)
        GameApp.setColor(100, 150, 255, 200); // Bright blue
        GameApp.drawRect(chestX - beamWidth/2, beamY, beamWidth, beamHeight);
        
        GameApp.setColor(150, 200, 255, 150); // Lighter blue center
        GameApp.drawRect(chestX - beamWidth/4, beamY, beamWidth/2, beamHeight);
        
        // Bright center at chest
        GameApp.setColor(200, 230, 255, 180);
        float centerSize = 30f + 10f * beamProgress;
        GameApp.drawRect(chestX - centerSize/2, chestY - centerSize/2, centerSize, centerSize);
        
        GameApp.endShapeRendering();
        
        // Opened chest
        GameApp.startSpriteRendering();
        float chestSize = 72f; // Larger chest
        if (GameApp.hasTexture("chest_open_2")) {
            GameApp.drawTexture("chest_open_2", chestX - chestSize/2, chestY - chestSize/2, chestSize, chestSize);
        }
        GameApp.endSpriteRendering();
    }
    
    private void renderItemsScroll() {
        float itemSpacing = 80f;
        float iconSize = 36f; // Smaller icons to fit beam better
        float scrollStartY = chestY + 80f;
        float beamWidth = 45f;
        
        // Draw blue beam (full height to top of frame)
        GameApp.startShapeRenderingFilled();
        float beamHeight = innerFrameY + innerFrameHeight - chestY; // Full height to top
        GameApp.setColor(100, 150, 255, 180); // Blue beam
        GameApp.drawRect(chestX - beamWidth/2, chestY, beamWidth, beamHeight);
        GameApp.setColor(150, 200, 255, 120); // Lighter center
        GameApp.drawRect(chestX - beamWidth/4, chestY, beamWidth/2, beamHeight);
        GameApp.endShapeRendering();
        
        // Calculate beam center (middle of beam height)
        float beamCenterY = chestY + beamHeight / 2f;
        
        // Draw scrolling items in beam
        GameApp.startSpriteRendering();
        
        float currentY = scrollStartY - scrollOffset;
        int centerItemIndex = (int)(scrollOffset / itemSpacing) % scrollItems.size();
        
        if (state == GachaState.FINAL_REVEAL) {
            // Force selected item at center
            centerItemIndex = selectedItemIndex;
        }
        
        // Draw items around center
        int visibleRange = 5;
        for (int i = -visibleRange; i <= visibleRange; i++) {
            int itemIndex = (centerItemIndex + i + scrollItems.size() * 10) % scrollItems.size();
            GachaItem item = scrollItems.get(itemIndex);
            
            float y = beamCenterY + i * itemSpacing;
            
            // Only draw if within beam
            if (y >= chestY && y <= chestY + beamHeight) {
                float distFromCenter = Math.abs(y - beamCenterY);
                float scale = 1f;
                int alpha = 255;
                
                if (distFromCenter < itemSpacing) {
                    scale = 1f + 0.3f * (1f - distFromCenter / itemSpacing);
                } else {
                    alpha = (int)(255 * Math.max(0.4f, 1f - (distFromCenter - itemSpacing) / (itemSpacing * 2)));
                }
                
                if (item.textureKey != null && GameApp.hasTexture(item.textureKey)) {
                    float size = iconSize * scale;
                    GameApp.setColor(255, 255, 255, alpha);
                    GameApp.drawTexture(item.textureKey, chestX - size/2, y - size/2, size, size);
                    GameApp.setColor(255, 255, 255, 255);
                }
            }
        }
        
        // Opened chest at bottom
        if (GameApp.hasTexture("chest_open_2")) {
            float chestSize = 72f; // Larger chest
            GameApp.drawTexture("chest_open_2", chestX - chestSize/2, chestY - chestSize/2, chestSize, chestSize);
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void renderFinalResult() {
        if (selectedItem == null) return;
        
        float centerX = innerFrameX + innerFrameWidth / 2f;
        float beamWidth = 45f;
        // Beam from chest to top of inner frame
        float beamTop = innerFrameY + innerFrameHeight;
        float beamHeight = beamTop - chestY; // Full height from chest to top
        
        // Draw blue beam (keep it visible)
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(100, 150, 255, 180); // Blue beam
        GameApp.drawRect(chestX - beamWidth/2, chestY, beamWidth, beamHeight);
        GameApp.setColor(150, 200, 255, 120); // Lighter center
        GameApp.drawRect(chestX - beamWidth/4, chestY, beamWidth/2, beamHeight);
        GameApp.endShapeRendering();
        
        // Calculate center of beam (where item should be)
        float beamCenterY = chestY + beamHeight / 2f;
        
        GameApp.startSpriteRendering();
        
        // Icon (large, centered in beam)
        float iconSize = 64f * celebrateScale;
        if (selectedItem.textureKey != null && GameApp.hasTexture(selectedItem.textureKey)) {
            GameApp.drawTexture(selectedItem.textureKey, centerX - iconSize/2, beamCenterY - iconSize/2, iconSize, iconSize);
        }
        
        // Render chest (at bottom of frame)
        float chestSize = 72f;
        if (GameApp.hasTexture("chest_open_2")) {
            GameApp.drawTexture("chest_open_2", chestX - chestSize/2, chestY - chestSize/2, chestSize, chestSize);
        }
        
        GameApp.endSpriteRendering();
        
        // Done button and points (at bottom, below chest) - render separately
        renderDoneButton();
    }
    
    // Done button properties (for click detection)
    private float doneButtonX, doneButtonY, doneButtonWidth, doneButtonHeight;
    
    /**
     * Check if Done button was clicked
     */
    public boolean isDoneButtonClicked() {
        if (state != GachaState.COMPLETED) return false;
        
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float windowWidth = GameApp.getWindowWidth();
        float windowHeight = GameApp.getWindowHeight();
        float scaleX = screenWidth / windowWidth;
        float scaleY = screenHeight / windowHeight;
        float worldMouseX = mouseX * scaleX;
        float worldMouseY = (windowHeight - mouseY) * scaleY;
        
        // Check if mouse click is within button bounds
        return GameApp.isButtonJustPressed(0) &&
               worldMouseX >= doneButtonX - doneButtonWidth/2 &&
               worldMouseX <= doneButtonX + doneButtonWidth/2 &&
               worldMouseY >= doneButtonY - doneButtonHeight/2 &&
               worldMouseY <= doneButtonY + doneButtonHeight/2;
    }
    
    private void renderDoneButton() {
        // Ensure textures are loaded (RED button)
        if (!GameApp.hasTexture("red_long")) {
            GameApp.addTexture("red_long", "assets/ui/red_long.png");
        }
        if (!GameApp.hasTexture("red_pressed_long")) {
            GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        }
        
        doneButtonX = innerFrameX + innerFrameWidth / 2f;
        // Position at bottom of frame
        // GameApp uses bottom-left origin: innerFrameY is bottom, innerFrameY + innerFrameHeight is top
        // To place at bottom: innerFrameY + smallValue (from bottom up)
        doneButtonY = innerFrameY + 40f; // 40f from bottom (bottom-most element, slightly higher for spacing)
        doneButtonWidth = 170f; // Smaller button width (was 200f)
        doneButtonHeight = 42f; // Smaller button height (was 50f)
        
        // Check if mouse is hovering/pressing (simple check)
        float mouseX = GameApp.getMousePositionInWindowX();
        float mouseY = GameApp.getMousePositionInWindowY();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float windowWidth = GameApp.getWindowWidth();
        float windowHeight = GameApp.getWindowHeight();
        float scaleX = screenWidth / windowWidth;
        float scaleY = screenHeight / windowHeight;
        float worldMouseX = mouseX * scaleX;
        float worldMouseY = (windowHeight - mouseY) * scaleY;
        
        boolean isPressed = GameApp.isButtonPressed(0) && 
                           worldMouseX >= doneButtonX - doneButtonWidth/2 && 
                           worldMouseX <= doneButtonX + doneButtonWidth/2 &&
                           worldMouseY >= doneButtonY - doneButtonHeight/2 && 
                           worldMouseY <= doneButtonY + doneButtonHeight/2;
        
        // Use pressed texture if pressed, otherwise normal (RED button)
        String textureKey = isPressed ? "red_pressed_long" : "red_long";
        
        // Render button texture
        GameApp.startSpriteRendering();
        if (GameApp.hasTexture(textureKey)) {
            GameApp.drawTexture(textureKey, doneButtonX - doneButtonWidth/2, doneButtonY - doneButtonHeight/2, doneButtonWidth, doneButtonHeight);
        } else {
            // Fallback to solid color if texture not loaded
            GameApp.endSpriteRendering();
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(60, 100, 200, 255);
            GameApp.drawRect(doneButtonX - doneButtonWidth/2, doneButtonY - doneButtonHeight/2, doneButtonWidth, doneButtonHeight);
            GameApp.endShapeRendering();
            GameApp.startSpriteRendering();
        }
        
        // Button text (styled font)
        String fontKey = GameApp.hasFont("gachaButtonFont") ? "gachaButtonFont" : "default";
        GameApp.drawTextCentered(fontKey, "DONE", doneButtonX, doneButtonY, "white");
        GameApp.endSpriteRendering();
    }
    
    private void renderPointsDisplay() {
        // Render random points (below chest, same position for all states)
        float displayX = innerFrameX + innerFrameWidth / 2f;
        // Always show below chest (consistent position)
        float displayY = chestY - 65f;
        
        GameApp.startSpriteRendering();
        String pointsText;
        
        if (state == GachaState.COMPLETED && finalPoints > 0) {
            // Stop animation - show final integer value (no decimals)
            pointsText = String.valueOf((int) finalPoints);
        } else {
            // Animate from 0 to finalPoints during scroll/reveal
            // Calculate animation progress based on total animation time
            float totalAnimTime = SCROLL_DURATION + REVEAL_DURATION;
            float currentAnimTime = 0f;
            
            if (state == GachaState.ITEMS_SCROLL) {
                currentAnimTime = stateTimer;
            } else if (state == GachaState.FINAL_REVEAL) {
                currentAnimTime = SCROLL_DURATION + stateTimer;
            }
            
            // Progress from 0 to 1
            float progress = Math.min(1f, currentAnimTime / totalAnimTime);
            
            // Animate from 0 to finalPoints (easing for nice effect)
            float easedProgress = 1f - (float) Math.pow(1f - progress, 3f); // Ease out cubic
            int animatedPoints = (int) (finalPoints * easedProgress);
            
            pointsText = String.valueOf(animatedPoints);
        }
        
        // Use styled font for points
        String fontKey = GameApp.hasFont("gachaPointsFont") ? "gachaPointsFont" : "default";
        float textWidth = GameApp.getTextWidth(fontKey, pointsText);
        
        // Draw score text and star icon centered together (matching HUD style)
        float iconSize = 50f; // Same size as HUD star icon
        float iconGap = 1f;
        float totalWidth = textWidth + iconGap + iconSize;
        float startX = displayX - totalWidth / 2f;
        
        // Draw points text
        GameApp.drawText(fontKey, pointsText, startX, displayY, "yellow-500");
        
        // Draw star icon to the right of points (same as HUD alignment)
        if (GameApp.hasTexture("star_icon")) {
            float iconX = startX + textWidth + iconGap + 5f;
            float iconY = displayY - 20f; // Same alignment as HUD
            GameApp.drawTexture("star_icon", iconX, iconY, iconSize, iconSize);
        }
        
        GameApp.endSpriteRendering();
    }
    
    /**
     * Apply the selected upgrade
     */
    public void applyUpgrade() {
        if (selectedItem == null || upgradeApplied) return;
        upgradeApplied = true;
        
        switch (selectedItem.upgradeType) {
            case PASSIVE:
                if (selectedItem.passiveType != null && player != null) {
                    player.addOrLevelUpPassiveItem(selectedItem.passiveType);
                }
                break;
            case WEAPON:
                if (weapon != null) {
                    weapon.levelUp();
                }
                break;
            case STAT:
                if (selectedItem.rarity == Rarity.LEGENDARY && player != null) {
                    for (PassiveItemType type : PassiveItemType.values()) {
                        player.addOrLevelUpPassiveItem(type);
                    }
                }
                break;
            default:
                break;
        }
        
        GameApp.log("Gacha upgrade applied: " + selectedItem.name + " | Points earned: " + randomPoints);
    }
    
    /**
     * Close the gacha system
     */
    public void close() {
        state = GachaState.INACTIVE;
        upgradeApplied = false;
        chestPositionSet = false;
        
        if (soundManager != null && wasMusicPlaying) {
            soundManager.playIngameMusic(true);
        }
    }
    
    public boolean isActive() {
        return state != GachaState.INACTIVE;
    }
    
    public boolean isCompleted() {
        return state == GachaState.COMPLETED;
    }
    
    public GachaState getState() {
        return state;
    }
}
