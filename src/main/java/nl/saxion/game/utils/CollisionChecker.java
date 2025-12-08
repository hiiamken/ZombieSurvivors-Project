package nl.saxion.game.utils;

@FunctionalInterface
public interface CollisionChecker {
    boolean checkCollision(float worldX, float worldY, float width, float height);
}
