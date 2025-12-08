package nl.saxion.game.utils;

import nl.saxion.gameapp.GameApp;
import java.util.ArrayList;
import java.util.List;

import static nl.saxion.game.utils.TMXMapObjects.PolygonObject;
import static nl.saxion.game.utils.TMXMapObjects.RectangleObject;
import static nl.saxion.game.utils.CoordinateConverter.tmxToWorldY;
import static nl.saxion.game.utils.CoordinateConverter.flipY;

public class TMXMapData {

    private static final int TILE_SIZE = 32;
    private static final int ROOM_TILES_W = 30;
    private static final int ROOM_TILES_H = 20;

    // Tile layer data: [row][col] = tile GID (0 = empty, >0 = tile)
    private boolean[][] wallTileMap; // true = wall, false = passable

    // Object group polygons for precise collision
    private List<PolygonObject> wallPolygons;
    private List<RectangleObject> wallRectangles;
    private List<PolygonObject> objectPolygons;
    private List<RectangleObject> objectRectangles;

    private int mapWidth;
    private int mapHeight;
    private int tileWidth;
    private int tileHeight;

    public TMXMapData(int mapWidth, int mapHeight, int tileWidth, int tileHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.wallTileMap = new boolean[ROOM_TILES_H][ROOM_TILES_W];
        this.wallPolygons = new ArrayList<>();
        this.wallRectangles = new ArrayList<>();
        this.objectPolygons = new ArrayList<>();
        this.objectRectangles = new ArrayList<>();
    }

    // =========================
    // Data setters (used by TMXParser)
    // =========================

    public void setWallTile(int row, int col, boolean isWall) {
        if (row >= 0 && row < ROOM_TILES_H && col >= 0 && col < ROOM_TILES_W) {
            wallTileMap[row][col] = isWall;
        }
    }

    public void addWallPolygon(PolygonObject poly) {
        wallPolygons.add(poly);
    }

    public void addWallRectangle(RectangleObject rect) {
        wallRectangles.add(rect);
    }

    public void addObjectPolygon(PolygonObject poly) {
        objectPolygons.add(poly);
    }

    public void addObjectRectangle(RectangleObject rect) {
        objectRectangles.add(rect);
    }

    // =========================
    // Data getters
    // =========================

    public List<PolygonObject> getWallPolygons() {
        return wallPolygons;
    }

    public List<RectangleObject> getWallRectangles() {
        return wallRectangles;
    }

    public List<PolygonObject> getObjectPolygons() {
        return objectPolygons;
    }

    public List<RectangleObject> getObjectRectangles() {
        return objectRectangles;
    }

    public boolean[][] getWallTileMap() {
        return wallTileMap;
    }

    public int getMapWidth() { return mapWidth; }
    public int getMapHeight() { return mapHeight; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }

    // =========================
    // Collision Detection
    // =========================

    public boolean isWallAt(int tileX, int tileY) {
        if (tileX < 0 || tileX >= ROOM_TILES_W || tileY < 0 || tileY >= ROOM_TILES_H) {
            return true; // Out of bounds = blocked
        }
        return wallTileMap[tileY][tileX];
    }

    public boolean isWallAtWorldPos(float worldX, float worldY) {
        int tileX = (int) Math.floor(worldX / TILE_SIZE);
        int tileY = (int) Math.floor(worldY / TILE_SIZE);

        return isWallAt(tileX, tileY);
    }

    public boolean checkCollision(float worldX, float worldY, float width, float height) {
        float left = worldX;
        float right = worldX + width;
        float bottom = worldY;
        float top = worldY + height;

        float hitboxW = right - left;
        float hitboxH = top - bottom;

        // ----------------------------------------------------
        // 1. RECTANGLE collision (ObjectGroup "wall")
        // ----------------------------------------------------
        for (RectangleObject rect : wallRectangles) {
            float rLeft = rect.x;
            float rBottom = tmxToWorldY(rect.y, rect.height);

            if (GameApp.rectOverlap(left, bottom, hitboxW, hitboxH,
                    rLeft, rBottom, rect.width, rect.height)) {
                return true;
            }
        }

        // ----------------------------------------------------
        // 2. POLYGON collision
        // ----------------------------------------------------
        for (int polyIdx = 0; polyIdx < wallPolygons.size(); polyIdx++) {
            PolygonObject poly = wallPolygons.get(polyIdx);
            // Convert polygon points once to world coords
            List<float[]> pts = new ArrayList<>();

            // In TMX: poly.y is the Y coordinate of base position (top-left origin, Y increases downward)
            // In GameApp: bottom-left origin, Y increases upward
            // Strategy: Convert base position, then flip relative Y offsets

            // Convert base Y from TMX to GameApp
            // Formula: flipY converts a point's Y coordinate (not top of rectangle)
            float baseYGameApp = flipY(poly.y);

            // Convert each polygon point
            // Strategy: Convert absolute TMX Y coordinate to GameApp Y coordinate
            // In TMX: absolute Y = poly.y + p[1] (where p[1] is relative offset, positive = down)
            // Convert absolute TMX Y to GameApp Y using flipY formula
            // FIX: Collision detection is shifted LEFT compared to visual wall (red overlay)
            // Need to offset collision to the RIGHT by half tile (16px) and UP by 16px to match visual wall
            final float OFFSET_X = 16f;  // +0.5 tile right (to match visual wall)
            final float OFFSET_Y = 16f;  // +16px up (to adjust height)
            for (int i = 0; i < poly.points.size(); i++) {
                float[] p = poly.points.get(i);
                float gameAppX = poly.x + p[0] + OFFSET_X;  // X: offset +1 tile right to match visual

                // Calculate absolute TMX Y coordinate
                float absoluteTmxY = poly.y + p[1];
                // Convert absolute TMX Y to GameApp Y (no Y offset needed)
                float gameAppY = flipY(absoluteTmxY) + OFFSET_Y;

                pts.add(new float[]{gameAppX, gameAppY});
            }

            // AABB optimization
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

            for (float[] p : pts) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }

            boolean aabbOverlap = GameApp.rectOverlap(left, bottom, hitboxW, hitboxH,
                    minX, minY, maxX - minX, maxY - minY);
            if (!aabbOverlap) {
                continue;
            }

            // Fine check: hitbox 4x4 sample points
            float sx = hitboxW / 3f;
            float sy = hitboxH / 3f;

            boolean collisionFound = false;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    float px = left + i * sx;
                    float py = bottom + j * sy;

                    if (isPointInPolygon(px, py, pts)) {
                        collisionFound = true;
                    }
                }
            }

            // If any polygon point is inside hitbox
            for (float[] p : pts) {
                if (GameApp.pointInRect(p[0], p[1], left, bottom, hitboxW, hitboxH)) {
                    collisionFound = true;
                }
            }

            if (collisionFound) {
                return true;
            }
        }

        return false;
    }
    private boolean isPointInPolygon(float px, float py, List<float[]> polyPoints) {
        if (polyPoints.size() < 3) {
            return false;
        }

        // Ray casting algorithm
        boolean inside = false;
        int j = polyPoints.size() - 1;

        for (int i = 0; i < polyPoints.size(); i++) {
            float xi = polyPoints.get(i)[0];
            float yi = polyPoints.get(i)[1];
            float xj = polyPoints.get(j)[0];
            float yj = polyPoints.get(j)[1];

            if (((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }
}
