package nl.saxion.game.systems;

import nl.saxion.game.utils.TMXMapData;
import nl.saxion.gameapp.GameApp;

import java.util.Map;

// Handles map rendering and coordinate conversion
public class MapRenderer {
    private static final int MAP_TILE_WIDTH = 960;   // 30 tiles * 32px
    private static final int MAP_TILE_HEIGHT = 640;  // 20 tiles * 32px
    private static final int MAPS_TO_RENDER = 3;     // 3x3 grid around player

    private final Map<Integer, TMXMapData> tmxMapDataByRoomIndex;

    public MapRenderer(Map<Integer, TMXMapData> tmxMapDataByRoomIndex) {
        this.tmxMapDataByRoomIndex = tmxMapDataByRoomIndex;
    }

    public void render(float playerWorldX, float playerWorldY) {
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

    public int getMapRowFromWorldY(float worldY) {
        return (int) Math.floor(worldY / MAP_TILE_HEIGHT);
    }

    public int getMapColFromWorldX(float worldX) {
        return (int) Math.floor(worldX / MAP_TILE_WIDTH);
    }

    public int wrapMapCoordinate(int coord, int max) {
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

    public TMXMapData getTMXDataForPosition(float worldX, float worldY) {
        int mapRowUnwrapped = getMapRowFromWorldY(worldY);
        int mapColUnwrapped = getMapColFromWorldX(worldX);

        // Wrap mapRow and mapCol to 0-3 range
        int mapRow = wrapMapCoordinate(mapRowUnwrapped, 4);
        int mapCol = wrapMapCoordinate(mapColUnwrapped, 4);
        int mapIndex = mapRow * 4 + mapCol;

        return tmxMapDataByRoomIndex.get(mapIndex);
    }

    public boolean checkWallCollision(float worldX, float worldY, float width, float height) {
        // Get map indices (unwrapped)
        int mapColUnwrapped = getMapColFromWorldX(worldX);
        int mapRowUnwrapped = getMapRowFromWorldY(worldY);

        // Wrap mapRow and mapCol separately BEFORE calculating mapIndex
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

        // Don't clamp checkW and checkH for more accurate collision detection
        // This allows enemy to get close to walls
        // TMXMapData.checkCollision handles bounds checking internally
        float checkW = width;
        float checkH = height;

        // Check collision using TMX data
        if (checkW > 0 && checkH > 0) {
            return mapData.checkCollision(localX, localY, checkW, checkH);
        }

        return false;
    }

    public static int getMapTileWidth() {
        return MAP_TILE_WIDTH;
    }

    public static int getMapTileHeight() {
        return MAP_TILE_HEIGHT;
    }
}

