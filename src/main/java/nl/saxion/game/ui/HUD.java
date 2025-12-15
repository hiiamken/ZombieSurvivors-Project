package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.gameapp.GameApp;

public class HUD {

    int HP_BAR_X = 20;
    int HP_BAR_Y = 20;

    int SCORE_X = 20;
    int SCORE_Y = 60;

    String TEXT_COLOR = "white";

    float HP_LOW_THRESHOLD = 0.3f;    // Below 30%
    float HP_MEDIUM_THRESHOLD = 0.6f; // Below 60%

    public void render(PlayerStatus status) {
        renderHPBar(status);
        renderScore(status);
        renderXP(status);
    }

    // ✅ XP BAR (FIXED & SAFE)
    private void renderXP(PlayerStatus status) {
        float percent = status.currentXP / (float) status.xpToNext;
        percent = GameApp.clamp(percent, 0f, 1f);

        // Draw XP bar shapes
        GameApp.startShapeRenderingFilled();

        // Background
        GameApp.setColor(120, 120, 120, 255);
        GameApp.drawRect(20, 90, 200, 10);

        // Fill
        GameApp.setColor(255, 215, 0, 255); // gold
        GameApp.drawRect(20, 90, 200 * percent, 10);

        GameApp.endShapeRendering();

        // Draw level text (no sprite start/end needed)
        GameApp.drawText("default", "LV " + status.level, 230, 95, "white");
    }

    private void renderHPBar(PlayerStatus status) {
        float hpPercent = status.getHealthPercentage();

        int barLength = (int) (20 * hpPercent);
        StringBuilder barBuilder = new StringBuilder();

        for (int i = 0; i < 20; i++) {
            if (i < barLength) {
                if (hpPercent <= HP_LOW_THRESHOLD) {
                    barBuilder.append("▒");
                } else if (hpPercent <= HP_MEDIUM_THRESHOLD) {
                    barBuilder.append("▓");
                } else {
                    barBuilder.append("█");
                }
            } else {
                barBuilder.append("░");
            }
        }

        GameApp.drawText("default", barBuilder.toString(), HP_BAR_X, HP_BAR_Y, TEXT_COLOR);

        String hpText = String.format("HP: %d / %d", status.health, status.maxHealth);
        GameApp.drawText("default", hpText, HP_BAR_X, HP_BAR_Y + 25, TEXT_COLOR);
    }

    private void renderScore(PlayerStatus status) {
        GameApp.drawText("default", "Score: " + status.score, SCORE_X, SCORE_Y, TEXT_COLOR);
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
        renderHPBar(status);
        renderScore(status);
        GameApp.drawText("default", "LV " + status.level, 230, 95, "white");
    }

}
