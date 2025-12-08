package nl.saxion.game.utils;

import java.util.List;

public class TMXMapObjects {

    public static class PolygonObject {
        public float x, y; // Base position
        public List<float[]> points; // Polygon points relative to (x, y)

        public PolygonObject(float x, float y, List<float[]> points) {
            this.x = x;
            this.y = y;
            this.points = points;
        }
    }

    public static class RectangleObject {
        public float x, y; // Top-left position
        public float width, height;

        public RectangleObject(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
