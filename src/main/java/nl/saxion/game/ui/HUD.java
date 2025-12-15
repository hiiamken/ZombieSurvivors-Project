package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.gameapp.GameApp;

public class HUD {

    String TEXT_COLOR = "white";

    public void render(PlayerStatus status) {
        // Draw shapes first (XP bar)
        renderXPBar(status);

        // Then draw all text with sprite rendering
        GameApp.startSpriteRendering();
        renderScore(status);
        renderXPText(status);
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

    // Draw XP level text - right side, vertically centered with bar
    private void renderXPText(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 12f;
        float barY = screenHeight - barHeight;
        // Lower Y position - move text down in the bar
        float textY = barY + barHeight - 10f; // Near bottom of bar with small padding

        // Level text on right side
        String levelText = "LV " + status.level;
        // Use styled font if available, otherwise default
        String fontName = GameApp.hasFont("levelFont") ? "levelFont" : "default";
        // Calculate text width to position it properly inside the bar
        float textWidth = GameApp.getTextWidth(fontName, levelText);
        float textX = screenWidth - textWidth - 5f; // Right side with padding, accounting for text width

        GameApp.drawText(fontName, levelText, textX, textY, "white");
    }


    private void renderScore(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 12f;
        float barY = screenHeight - barHeight;

        // Position score on right side, below XP bar
        float scoreY = barY - 18f; // Below XP bar with spacing
        String scoreText = formatScore(status.score);

        // Use styled font if available, otherwise default
        String fontName = GameApp.hasFont("scoreFont") ? "scoreFont" : "default";
        float textWidth = GameApp.getTextWidth(fontName, scoreText);
        float scoreX = screenWidth - textWidth - 10f; // Right side with padding

        GameApp.drawText(fontName, scoreText, scoreX, scoreY, "white");
    }

    // Format score with commas for readability
    private String formatScore(int score) {
        return String.format("SCORE: %,d", score);
    }

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
