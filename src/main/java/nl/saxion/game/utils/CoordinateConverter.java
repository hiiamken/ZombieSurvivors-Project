package nl.saxion.game.utils;

public class CoordinateConverter {

    private static final int TILE_SIZE = 32;
    private static final int ROOM_TILES_H = 20;

    public static float flipY(float tmxY) {
        return (ROOM_TILES_H * TILE_SIZE) - tmxY;
    }

    public static float tmxToWorldY(float tmxY, float height) {
        return (ROOM_TILES_H * TILE_SIZE) - tmxY - height;
    }
}
