package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.gameapp.GameApp;

public class HUD {

    String TEXT_COLOR = "white";

    public void render(PlayerStatus status, float gameTime) {
        // Draw shapes first (XP bar)
        renderXPBar(status);

        // Then draw all text with sprite rendering
        GameApp.startSpriteRendering();
        renderScore(status);
        renderXPText(status);
        renderSurvivalTime(gameTime);
        GameApp.endSpriteRendering();
    }

    // Draw XP bar shapes
    private void renderXPBar(PlayerStatus status) {
        float percent = status.currentXP / (float) status.xpToNext;
        percent = GameApp.clamp(percent, 0f, 1f);

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 12f; // Thin bar at top
        float barY = screenHeight - barHeight; // Top of screen

        GameApp.startShapeRenderingFilled();

        // Background - dark gray to match map
        GameApp.setColor(40, 40, 40, 255);
        GameApp.drawRect(0, barY, screenWidth, barHeight);

        // Fill - blue/cyan to match game style
        if (percent > 0) {
            GameApp.setColor(70, 130, 255, 255); // Blue
            GameApp.drawRect(0, barY, screenWidth * percent, barHeight);
        }

        GameApp.endShapeRendering();

        // Draw border outline - thicker and darker
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(3f);
        GameApp.setColor(100, 100, 100, 255); // Dark gray border
        GameApp.drawRect(0, barY, screenWidth, barHeight);
        GameApp.endShapeRendering();
    }

    private void renderSurvivalTime(float gameTime) {
        int totalSeconds = (int) gameTime;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        String timeText = String.format("%02d:%02d", minutes, seconds);

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();

        // Position: centered horizontally, below XP bar
        String fontName = GameApp.hasFont("timerFont") ? "timerFont" : "default";
        float textWidth = GameApp.getTextWidth(fontName, timeText);
        float x = (screenWidth - textWidth) / 2f; // Center horizontally
        float y = screenHeight - 30f;

        GameApp.drawText(fontName, timeText, x, y, "white");
    }

    // Draw XP level text - right side, vertically centered with bar
    private void renderXPText(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 12f;
        float barY = screenHeight - barHeight;
        float textY = barY + barHeight - 10f;

        String levelText = "LV " + status.level;
        String fontName = GameApp.hasFont("levelFont") ? "levelFont" : "default";

        float textWidth = GameApp.getTextWidth(fontName, levelText);
        float textX = screenWidth - textWidth - 5f;

        GameApp.drawText(fontName, levelText, textX, textY, "white");
    }

    private void renderScore(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 12f;
        float barY = screenHeight - barHeight;

        float scoreY = barY - 18f;
        String scoreText = formatScore(status.score);

        String fontName = GameApp.hasFont("scoreFont") ? "scoreFont" : "default";
        float textWidth = GameApp.getTextWidth(fontName, scoreText);
        float scoreX = screenWidth - textWidth - 10f;

        GameApp.drawText(fontName, scoreText, scoreX, scoreY, "white");
    }

    private String formatScore(int score) {
        return String.format("SCORE: %,d", score);
    }

    // (Optional old helpers - safe to keep if other code uses them)
    public void renderXPBarOnly(PlayerStatus status) {
        float percent = status.currentXP / (float) status.xpToNext;
        percent = GameApp.clamp(percent, 0f, 1f);

        GameApp.setColor(120, 120, 120, 255);
        GameApp.drawRect(20, 90, 200, 10);

        GameApp.setColor(255, 215, 0, 255);
        GameApp.drawRect(20, 90, 200 * percent, 10);
    }

    public void renderTextOnly(PlayerStatus status) {
        renderScore(status);
        GameApp.drawText("default", "LV " + status.level, 230, 95, "white");
    }
}
