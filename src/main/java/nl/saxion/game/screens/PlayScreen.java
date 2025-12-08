package nl.saxion.game.screens;

import com.badlogic.gdx.Input;
import nl.saxion.game.core.GameState;
import nl.saxion.game.entities.Bullet;
import nl.saxion.game.entities.Enemy;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.PlayerStatus;
import nl.saxion.game.entities.Weapon;
import nl.saxion.game.systems.InputController;
import nl.saxion.game.ui.HUD;
import nl.saxion.game.utils.CollisionChecker;
import nl.saxion.game.utils.TMXMapData;
import nl.saxion.game.utils.TMXParser;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import static nl.saxion.game.utils.TMXMapObjects.PolygonObject;
import static nl.saxion.game.utils.TMXMapObjects.RectangleObject;

public class PlayScreen extends ScalableGameScreen {

    private InputController input;

    private Player player;
    private Weapon weapon;

    private List<Bullet> bullets;

    // Enemy system - List of all active enemies in the game
    private List<Enemy> enemies;

    // Enemy spawning system - Controls difficulty curve
    private float enemySpawnTimer = 0f;
    private float enemySpawnInterval = 3f; // spawn every 3 seconds
    private float enemyBaseSpeed = 34f;
    private int enemyBaseHealth = 15;

    // Max enemies to prevent performance issues
    private static final int MAX_ENEMIES = 50;

    // Total time this run has been going (for difficulty scaling)
    private float gameTime = 0f;

    private float playerDamageCooldown = 0f;
    private static final float DAMAGE_COOLDOWN_DURATION = 0.5f;
    private static final int ENEMY_TOUCH_DAMAGE = 1;

    private int score = 0;

    private GameState currentState = GameState.MENU;

    private HUD hud;

    // Map system constants
    private static final int MAP_TILE_WIDTH = 960;   // 30 tiles * 32px
    private static final int MAP_TILE_HEIGHT = 640;  // 20 tiles * 32px
    private static final int MAPS_TO_RENDER = 3;     // 3x3 grid around player

    // Multi-map collision system
    private Map<Integer, TMXMapData> tmxMapDataByRoomIndex;

    // Player world position tracking
    private float playerWorldX;
    private float playerWorldY;

    // Debug flag
    private boolean debugCollision = false;

    public PlayScreen() {
        super(800, 600);
    }

    @Override
    public void show() {

        GameApp.log("PlayScreen loaded");

        GameApp.addTexture("player", "assets/player/auraRambo.png");
        GameApp.addTexture("bullet", "assets/Bullet/bullet.png");

        GameApp.addSpriteSheet("zombie_idle_sheet", "assets/enemy/Zombie_Idle.png", 32, 32);
        GameApp.addSpriteSheet("zombie_run_sheet", "assets/enemy/Zombie_run.png", 32, 32);
        GameApp.addSpriteSheet("zombie_hit_sheet", "assets/enemy/Zombie_Hit.png", 32, 32);
        GameApp.addSpriteSheet("zombie_death1_sheet", "assets/enemy/Zombie_Death_1.png", 32, 32);
        GameApp.addSpriteSheet("zombie_death2_sheet", "assets/enemy/Zombie_Death_2.png", 32, 32);

        GameApp.addAnimationFromSpritesheet("zombie_idle", "zombie_idle_sheet", 0.2f, true);

        GameApp.addAnimationFromSpritesheet("zombie_run", "zombie_run_sheet", 0.1f, true);

        GameApp.addAnimationFromSpritesheet("zombie_hit", "zombie_hit_sheet", 0.15f, false);

        GameApp.addAnimationFromSpritesheet("zombie_death", "zombie_death1_sheet", 0.2f, false);

        GameApp.addTexture("enemy", "assets/Bullet/bullet.png");

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

        // Load 16 TMX maps for collision detection
        tmxMapDataByRoomIndex = new HashMap<>();
        int loadedMaps = 0;
        for (int i = 0; i < 16; i++) {
            int mapNumber = i + 1; // map1, map2, ..., map16
            String tmxPath = "assets/maps/map" + mapNumber + ".tmx";
            TMXMapData mapData = TMXParser.loadFromTMX(tmxPath);
            if (mapData != null) {
                tmxMapDataByRoomIndex.put(i, mapData); // room index i tương ứng với map(i+1)
                loadedMaps++;
            } else {
                GameApp.log("❌ Warning: Could not load " + tmxPath);
            }
        }
        GameApp.log("✅ Successfully loaded " + loadedMaps + "/16 TMX maps for collision");

        input = new InputController();
        hud = new HUD();

        currentState = GameState.MENU;

        // Hide cursor for better game experience
        GameApp.hideCursor();

        resetGame();
    }

    @Override
    public void hide() {
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

    @Override
    public void render(float delta) {
        super.render(delta);

        GameApp.clearScreen("black");

        // ----- STATE : MENU -----
        if (currentState == GameState.MENU) {
            handleMenuInput();
            renderMenuScreen();
            return;
        }

        // ----- STATE : GAME OVER -----
        if (currentState == GameState.GAME_OVER) {
            handleGameOverInput();
            renderGameOverScreen();
            return;
        }

        // ----- GAMEPLAY STATE -----

        // Difficulty time
        gameTime += delta;

        float oldPlayerWorldX = playerWorldX;
        float oldPlayerWorldY = playerWorldY;

        CollisionChecker collisionChecker = this::checkWallCollision;
        player.update(delta, input, Integer.MAX_VALUE, Integer.MAX_VALUE, collisionChecker);

        // Update player world position
        float newPlayerWorldX = player.getX();
        float newPlayerWorldY = player.getY();

        // Debug log player movement
        if (oldPlayerWorldX != newPlayerWorldX || oldPlayerWorldY != newPlayerWorldY) {
        }

        playerWorldX = newPlayerWorldX;
        playerWorldY = newPlayerWorldY;

        // Player can move, so update weapon

        weapon.update(delta);

        // Auto-shooting - automatically fires when cooldown is ready
        Bullet newBullet = weapon.tryFire(player);
        if (newBullet != null) {
            bullets.add(newBullet);
        }

        // Update bullets
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            b.update(delta);

            // Check wall collision
            if (checkWallCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight())) {
                b.destroy();
            }

            if (b.isOffScreen()) {
                b.destroy();
            }
        }

        // Enemies chase the player (Task 9) - với smooth sliding collision (dùng cùng CollisionChecker như player)
        for (Enemy e : enemies) {
            e.update(delta, player.getX(), player.getY(), collisionChecker);
        }

        // Update zombie animations
        GameApp.updateAnimation("zombie_idle");
        GameApp.updateAnimation("zombie_run");
        GameApp.updateAnimation("zombie_hit");
        GameApp.updateAnimation("zombie_death");

        // Enemy spawning system - handles difficulty curve (Task 10)
        updateEnemySpawning(delta);

        // Collision detection - Bullet vs Enemy
        handleBulletEnemyCollisions();

        // Cleanup - Remove dead enemies and destroyed bullets
        removeDeadEnemies();
        removeDestroyedBullets();

        // Player Damage cooldown
        playerDamageCooldown -= delta;
        if (playerDamageCooldown < 0f) {
            playerDamageCooldown = 0f;
        }

        // Enemy and player collision
        handleEnemyPlayerCollisions();

        // Player death check
        if (player.isDead()) {
            GameApp.log("Player died!");
            currentState = GameState.GAME_OVER;
        }

        // ----- RENDER -----
        // Render map background first
        renderMap();

        // DEBUG: Render collision overlay
        if (debugCollision) {
            renderFullDebugOverlay();   // Full debug overlay with wall tiles
        }

        GameApp.startSpriteRendering();

        // Render player relative to viewport
        renderPlayerRelativeToViewport();

        // Render enemies relative to viewport
        for (Enemy e : enemies) {
            renderEnemyRelativeToViewport(e);
        }

        // Render bullets relative to viewport
        for (Bullet b : bullets) {
            if (!b.isDestroyed()) {
                renderBulletRelativeToViewport(b);
            }
        }

        renderHUD();

        GameApp.endSpriteRendering();
    }

    // =========================
    // MAP RENDERING - VAMPIRE SURVIVORS STYLE
    // =========================

    private void renderMap() {
        GameApp.startSpriteRendering();

        // Calculate which map player is currently in
        int centerMapRow = getMapRowFromWorldY(playerWorldY);
        int centerMapCol = getMapColFromWorldX(playerWorldX);

        // Render 3x3 grid around player
        int offset = MAPS_TO_RENDER / 2;

        for (int rowOffset = -offset; rowOffset <= offset; rowOffset++) {
            for (int colOffset = -offset; colOffset <= offset; colOffset++) {
                int mapRow = centerMapRow + rowOffset;
                int mapCol = centerMapCol + colOffset;

                // Wrap around for infinite map
                mapRow = wrapMapCoordinate(mapRow, 4);
                mapCol = wrapMapCoordinate(mapCol, 4);

                // Calculate screen position (world → screen)
                float mapWorldX = (centerMapCol + colOffset) * MAP_TILE_WIDTH;
                float mapWorldY = (centerMapRow + rowOffset) * MAP_TILE_HEIGHT;

                float cameraOffsetX = mapWorldX - playerWorldX;
                float cameraOffsetY = mapWorldY - playerWorldY;

                float screenX = (GameApp.getWorldWidth() / 2f) + cameraOffsetX;
                float screenY = (GameApp.getWorldHeight() / 2f) + cameraOffsetY;

                // Only render if in viewport
                if (isMapInViewport(screenX, screenY)) {
                    renderSingleMap(mapRow, mapCol, screenX, screenY);
                }
            }
        }

        GameApp.endSpriteRendering();
    }

    private void renderSingleMap(int mapRow, int mapCol, float screenX, float screenY) {
        int mapIndex = mapRow * 4 + mapCol;
        mapIndex = wrapMapCoordinate(mapIndex, 16);
        String roomKey = getRoomTextureKey(mapIndex);

        if (GameApp.hasTexture(roomKey)) {
            GameApp.drawTexture(roomKey, screenX, screenY, MAP_TILE_WIDTH, MAP_TILE_HEIGHT);
        }
    }

    private String getRoomTextureKey(int mapIndex) {
        return "room_" + String.format("%02d", mapIndex);
    }

    private int getMapRowFromWorldY(float worldY) {
        return (int) Math.floor(worldY / MAP_TILE_HEIGHT);
    }

    private int getMapColFromWorldX(float worldX) {
        return (int) Math.floor(worldX / MAP_TILE_WIDTH);
    }

    private int wrapMapCoordinate(int coord, int max) {
        coord = coord % max;
        if (coord < 0) {
            coord += max;
        }
        return coord;
    }

    private boolean isMapInViewport(float mapWorldX, float mapWorldY) {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        return !(mapWorldX + MAP_TILE_WIDTH < 0 ||
                mapWorldX > worldW ||
                mapWorldY + MAP_TILE_HEIGHT < 0 ||
                mapWorldY > worldH);
    }

    private TMXMapData getTMXDataForPosition(float worldX, float worldY) {
        int mapRowUnwrapped = getMapRowFromWorldY(worldY);
        int mapColUnwrapped = getMapColFromWorldX(worldX);

        // Wrap mapRow and mapCol to 0-3 range
        int mapRow = wrapMapCoordinate(mapRowUnwrapped, 4);
        int mapCol = wrapMapCoordinate(mapColUnwrapped, 4);
        int mapIndex = mapRow * 4 + mapCol;

        return tmxMapDataByRoomIndex.get(mapIndex);
    }

    private void renderPlayerRelativeToViewport() {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Player always at center of viewport
        float playerScreenX = worldW / 2f;
        float playerScreenY = worldH / 2f;

        GameApp.drawTexture("player",
                playerScreenX - Player.SPRITE_SIZE / 2f,
                playerScreenY - Player.SPRITE_SIZE / 2f,
                Player.SPRITE_SIZE,
                Player.SPRITE_SIZE
        );
    }

    private void renderEnemyRelativeToViewport(Enemy enemy) {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Calculate screen position (world → screen)
        float offsetX = enemy.getX() - playerWorldX;
        float offsetY = enemy.getY() - playerWorldY;
        float screenX = worldW / 2f + offsetX;
        float screenY = worldH / 2f + offsetY;

        // Only render if in viewport
        if (screenX + Enemy.SPRITE_SIZE > 0 && screenX < worldW &&
                screenY + Enemy.SPRITE_SIZE > 0 && screenY < worldH) {

            if (GameApp.hasAnimation("zombie_run")) {
                GameApp.drawAnimation("zombie_run", screenX, screenY, Enemy.SPRITE_SIZE, Enemy.SPRITE_SIZE);
            } else {
                GameApp.drawTexture("enemy", screenX, screenY, Enemy.SPRITE_SIZE, Enemy.SPRITE_SIZE);
            }
        }
    }

    private void renderBulletRelativeToViewport(Bullet bullet) {
        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // Calculate screen position (world → screen)
        float offsetX = bullet.getX() - playerWorldX;
        float offsetY = bullet.getY() - playerWorldY;
        float screenX = worldW / 2f + offsetX;
        float screenY = worldH / 2f + offsetY;

        // Only render if in viewport
        if (screenX + bullet.getWidth() > 0 && screenX < worldW &&
                screenY + bullet.getHeight() > 0 && screenY < worldH) {

            GameApp.drawTexture("bullet", screenX, screenY, bullet.getWidth(), bullet.getHeight());
        }
    }

    public int getCurrentMapRow() {
        return getMapRowFromWorldY(playerWorldY);
    }

    public int getCurrentMapCol() {
        return getMapColFromWorldX(playerWorldX);
    }

    private void drawPolygonObject(float screenX, float screenY, PolygonObject poly) {
        List<float[]> pts = poly.points;
        if (pts.size() < 3) return;

        float baseYGameApp = MAP_TILE_HEIGHT - poly.y;  // flipY equivalent

        float[] vertices = new float[pts.size() * 2];
        int pointCount = pts.size();
        for (int i = 0; i < pointCount; i++) {

            int idx = pointCount - 1 - i;
            float px = poly.x + pts.get(idx)[0];
            float py = baseYGameApp - pts.get(idx)[1];  // base (flipped) - relative offset

            vertices[i * 2]     = screenX + px;
            vertices[i * 2 + 1] = screenY + py;
        }

        GameApp.drawPolygon(vertices);
    }

    // =========================
    // FULL DEBUG OVERLAY SYSTEM
    // =========================

    private void renderFullDebugOverlay() {
        // Calculate which maps are in viewport (same logic as renderMap)
        int centerMapRow = getMapRowFromWorldY(playerWorldY);
        int centerMapCol = getMapColFromWorldX(playerWorldX);

        // Render 3x3 grid around player
        int offset = MAPS_TO_RENDER / 2;

        for (int rowOffset = -offset; rowOffset <= offset; rowOffset++) {
            for (int colOffset = -offset; colOffset <= offset; colOffset++) {
                int mapRow = centerMapRow + rowOffset;
                int mapCol = centerMapCol + colOffset;

                // Wrap around for infinite map
                mapRow = wrapMapCoordinate(mapRow, 4);
                mapCol = wrapMapCoordinate(mapCol, 4);

                // Calculate screen position (world → screen)
                float mapWorldX = (centerMapCol + colOffset) * MAP_TILE_WIDTH;
                float mapWorldY = (centerMapRow + rowOffset) * MAP_TILE_HEIGHT;

                float cameraOffsetX = mapWorldX - playerWorldX;
                float cameraOffsetY = mapWorldY - playerWorldY;

                float screenX = (GameApp.getWorldWidth() / 2f) + cameraOffsetX;
                float screenY = (GameApp.getWorldHeight() / 2f) + cameraOffsetY;

                // Only render if in viewport
                if (isMapInViewport(screenX, screenY)) {
                    // Get map index và TMX data
                    int mapIndex = mapRow * 4 + mapCol;
                    mapIndex = wrapMapCoordinate(mapIndex, 16);
                    TMXMapData mapData = tmxMapDataByRoomIndex.get(mapIndex);

                    if (mapData != null) {
                        drawWallObjects(screenX, screenY, mapData);
                        drawOtherObjects(screenX, screenY, mapData);
                    }
                }
            }
        }
    }

    private float tmxToWorldY(float y, float height) {
        return MAP_TILE_HEIGHT - y - height;
    }

    private void drawWallObjects(float screenX, float screenY, TMXMapData mapData) {
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(255, 0, 0, 120);

        // Render rectangles
        for (RectangleObject rect : mapData.getWallRectangles()) {
            float wx = screenX + rect.x;
            float wy = screenY + tmxToWorldY(rect.y, rect.height);
            GameApp.drawRect(wx, wy, rect.width, rect.height);
        }

        // Render polygons
        for (PolygonObject poly : mapData.getWallPolygons()) {
            // Convert absolute TMX Y to GameApp Y for each point
            List<float[]> tempPts = new ArrayList<>();

            for (float[] p : poly.points) {
                float px = poly.x + p[0];
                float absoluteTmxY = poly.y + p[1];
                float py = MAP_TILE_HEIGHT - absoluteTmxY;
                tempPts.add(new float[]{px, py});
            }

            // Convert to vertex array for rendering
            float[] verts = new float[tempPts.size() * 2];
            for (int i = 0; i < tempPts.size(); i++) {
                float[] p = tempPts.get(i);
                verts[i*2]   = screenX + p[0];
                verts[i*2+1] = screenY + p[1];
            }

            GameApp.drawPolygon(verts);
        }

        GameApp.endShapeRendering();
    }

    private void drawOtherObjects(float screenX, float screenY, TMXMapData mapData) {
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(255, 140, 0, 120);

        for (RectangleObject rect : mapData.getObjectRectangles()) {
            float rx = screenX + rect.x;
            float ry = screenY + tmxToWorldY(rect.y, rect.height);
            GameApp.drawRect(rx, ry, rect.width, rect.height);
        }

        for (PolygonObject poly : mapData.getObjectPolygons()) {
            drawPolygonObject(screenX, screenY, poly);
        }

        GameApp.endShapeRendering();
    }

    // =========================
    // WALL COLLISION DETECTION
    // =========================

    private boolean checkWallCollision(float worldX, float worldY, float width, float height) {
        // Get map indices (unwrapped)
        int mapColUnwrapped = getMapColFromWorldX(worldX);
        int mapRowUnwrapped = getMapRowFromWorldY(worldY);

        // Wrap mapRow và mapCol riêng biệt TRƯỚC KHI tính mapIndex
        int mapRow = wrapMapCoordinate(mapRowUnwrapped, 4);
        int mapCol = wrapMapCoordinate(mapColUnwrapped, 4);
        int mapIndex = mapRow * 4 + mapCol;

        // Get TMX data for the map at this position
        TMXMapData mapData = tmxMapDataByRoomIndex.get(mapIndex);
        if (mapData == null) {
            return false; // No TMX data for this map
        }

        float localX = worldX % MAP_TILE_WIDTH;
        if (localX < 0) {
            localX += MAP_TILE_WIDTH;
        }

        float localY = worldY % MAP_TILE_HEIGHT;
        if (localY < 0) {
            localY += MAP_TILE_HEIGHT;
        }

        localX = GameApp.clamp(localX, 0, MAP_TILE_WIDTH - 1);
        localY = GameApp.clamp(localY, 0, MAP_TILE_HEIGHT - 1);

        float checkW = GameApp.clamp(width, 0, MAP_TILE_WIDTH - localX);
        float checkH = GameApp.clamp(height, 0, MAP_TILE_HEIGHT - localY);

        // Check collision using TMX data
        if (checkW > 0 && checkH > 0) {
            return mapData.checkCollision(localX, localY, checkW, checkH);
        }

        return false;
    }

    // =========================
    // PLAYER STATUS / HUD
    // =========================

    public PlayerStatus getPlayerStatus() {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();

        return new PlayerStatus(health, maxHealth, score);
    }

    public void addScore(int amount) {
        score += amount;
        score = (int) GameApp.clamp(score, 0, Integer.MAX_VALUE);
    }

    private void renderHUD() {
        PlayerStatus status = getPlayerStatus();
        hud.render(status);
    }

    // =========================
    // ENEMY SPAWNING (TASK 10)
    // =========================

    private void updateEnemySpawning(float delta) {
        // 1. Max enemy limit
        if (enemies.size() >= MAX_ENEMIES) {
            return;
        }

        // 2. Spawn timer (count up)
        enemySpawnTimer += delta;
        if (enemySpawnTimer < enemySpawnInterval) {
            return;
        }

        // Time to spawn
        enemySpawnTimer = 0f;

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        // 3. Choose edge: 0 = top, 1 = right, 2 = bottom, 3 = left
        int edge = GameApp.randomInt(0, 4);

        float spawnX;
        float spawnY;

        if (edge == 0) {
            // TOP
            spawnX = GameApp.random(0f, worldW - Enemy.SPRITE_SIZE);
            spawnY = worldH; // just above top
        } else if (edge == 1) {
            // RIGHT
            spawnX = worldW; // just outside right
            spawnY = GameApp.random(0f, worldH - Enemy.SPRITE_SIZE);
        } else if (edge == 2) {
            // BOTTOM
            spawnX = GameApp.random(0f, worldW - Enemy.SPRITE_SIZE);
            spawnY = -Enemy.SPRITE_SIZE; // just below bottom
        } else {
            // LEFT
            spawnX = -Enemy.SPRITE_SIZE; // just outside left
            spawnY = GameApp.random(0f, worldH - Enemy.SPRITE_SIZE);
        }

        // 4. Difficulty scaling: 1.0x → 3.0x
        float difficultyMultiplier = 1f + (gameTime * 0.01f);
        difficultyMultiplier = GameApp.clamp(difficultyMultiplier, 1f, 3f);

        float currentSpeed = enemyBaseSpeed * difficultyMultiplier;
        int currentHealth = (int) (enemyBaseHealth * difficultyMultiplier);

        // 5. Spawn enemy
        enemies.add(new Enemy(spawnX, spawnY, currentSpeed, currentHealth));

        // 6. Spawn interval scaling (faster spawns over time, clamped)
        if (enemySpawnInterval > 1.5f) {
            enemySpawnInterval -= delta * 0.02f;
        }
        enemySpawnInterval = GameApp.clamp(enemySpawnInterval, 0.5f, 10f);
    }

    // =========================
    // COLLISIONS & CLEANUP
    // =========================

    private void handleBulletEnemyCollisions() {
        for (Bullet b : bullets) {
            if (b.isDestroyed()) {
                continue;
            }

            float bX = b.getX();
            float bY = b.getY();
            float bW = b.getWidth();
            float bH = b.getHeight();

            for (Enemy e : enemies) {
                if (e.isDead()) {
                    continue;
                }

                // Use hitbox instead of sprite size for fair collision (Vampire Survivors style)
                Rectangle enemyHitbox = e.getHitBox();
                float eX = enemyHitbox.x;
                float eY = enemyHitbox.y;
                float eW = enemyHitbox.width;
                float eH = enemyHitbox.height;

                if (GameApp.rectOverlap(bX, bY, bW, bH, eX, eY, eW, eH)) {
                    e.takeDamage(b.getDamage());
                    b.destroy();

                    if (e.isDead()) {
                        addScore(10);
                    }

                    break;
                }
            }
        }
    }

    private void handleEnemyPlayerCollisions() {
        // Use hitbox instead of sprite size for fair collision (Vampire Survivors style)
        Rectangle playerHitbox = player.getHitBox();
        float pX = playerHitbox.x;
        float pY = playerHitbox.y;
        float pW = playerHitbox.width;
        float pH = playerHitbox.height;

        for (Enemy e : enemies) {
            Rectangle enemyHitbox = e.getHitBox();
            float eX = enemyHitbox.x;
            float eY = enemyHitbox.y;
            float eW = enemyHitbox.width;
            float eH = enemyHitbox.height;

            boolean overlap = GameApp.rectOverlap(pX, pY, pW, pH, eX, eY, eW, eH);
            if (overlap) {
                if (playerDamageCooldown <= 0f) {
                    player.takeDamage(ENEMY_TOUCH_DAMAGE);
                    playerDamageCooldown = DAMAGE_COOLDOWN_DURATION;
                }
            }
        }
    }

    private void removeDestroyedBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            if (b.isDestroyed() || b.isOffScreen()) {
                it.remove();
            }
        }
    }

    private void removeDeadEnemies() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            // Remove enemy
            if (e.isDead() && e.isDeathAnimationFinished()) {
                it.remove();
            }
        }
    }

    // =========================
    // GAME FLOW / RESET
    // =========================

    private void resetGame() {
        float startX = 300;
        float startY = 250;
        float speed = 80f;
        int maxHealth = 5;

        player = new Player(startX, startY, speed, maxHealth, null);

        bullets = new ArrayList<>();
        weapon = new Weapon(Weapon.WeaponType.PISTOL, 1.5f, 10);

        enemies = new ArrayList<>();

        // difficulty & spawning reset
        gameTime = 0f;
        enemySpawnInterval = 3f;
        enemySpawnTimer = 0f;

        score = 0;
        playerDamageCooldown = 0f;

        playerWorldX = MAP_TILE_WIDTH / 2f; // 480
        playerWorldY = MAP_TILE_HEIGHT / 2f; // 320

        // Kiểm tra và điều chỉnh nếu spawn position có wall
        TMXMapData spawnMapData = getTMXDataForPosition(playerWorldX, playerWorldY);
        if (spawnMapData != null) {
            Rectangle testHitbox = new Rectangle((int)playerWorldX, (int)playerWorldY, 16, 16);
            if (checkWallCollision(testHitbox.x, testHitbox.y, testHitbox.width, testHitbox.height)) {

                for (int offset = 50; offset < 300; offset += 50) {

                    for (int dx = -offset; dx <= offset; dx += 50) {
                        for (int dy = -offset; dy <= offset; dy += 50) {
                            float testX = playerWorldX + dx;
                            float testY = playerWorldY + dy;
                            if (testX >= 0 && testX < MAP_TILE_WIDTH &&
                                    testY >= 0 && testY < MAP_TILE_HEIGHT) {
                                if (!checkWallCollision(testX, testY, 16, 16)) {
                                    playerWorldX = testX;
                                    playerWorldY = testY;
                                    GameApp.log("Adjusted player spawn to safe position: (" + playerWorldX + ", " + playerWorldY + ")");
                                    break;
                                }
                            }
                        }
                    }
                    if (!checkWallCollision(playerWorldX, playerWorldY, 16, 16)) {
                        break;
                    }
                }
            }
        }

        // Set player position
        player.setPosition(playerWorldX, playerWorldY);

        // Reset enemies - spawn a few around the player
        enemies.clear();
        enemies.add(new Enemy(playerWorldX + 200, playerWorldY + 150, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 400, playerWorldY + 200, enemyBaseSpeed, enemyBaseHealth));
        enemies.add(new Enemy(playerWorldX + 600, playerWorldY + 100, enemyBaseSpeed, enemyBaseHealth));

        GameApp.log("Game reset: new run started, player.isDead() = " + player.isDead());
        GameApp.log("Player starting at world position: (" + playerWorldX + ", " + playerWorldY + ")");
    }

    // =========================
    // STATE: MENU & GAME OVER
    // =========================

    private void handleMenuInput() {
        boolean enterPressed = GameApp.isKeyJustPressed(Input.Keys.ENTER);
        if (enterPressed) {
            resetGame();
            currentState = GameState.PLAYING;
        }
    }

    private void handleGameOverInput() {
        boolean rPressed = GameApp.isKeyJustPressed(Input.Keys.R);

        if (rPressed) {
            resetGame();
            currentState = GameState.PLAYING;
        }
    }

    private void renderMenuScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "ZOMBIE SURVIVORS", 260, 200, "white");
        GameApp.drawText("default", "Press ENTER to start", 220, 260, "white");

        GameApp.endSpriteRendering();
    }

    private void renderGameOverScreen() {
        GameApp.startSpriteRendering();

        GameApp.drawText("default", "GAME OVER", 280, 220, "white");

        String scoreText = "Score: " + score;
        GameApp.drawText("default", scoreText, 300, 240, "white");

        GameApp.drawText("default", "Press R to restart", 240, 300, "white");

        GameApp.endSpriteRendering();
    }
}
