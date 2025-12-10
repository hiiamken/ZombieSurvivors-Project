package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.gameapp.GameApp;


public class HUD {

    int HP_BAR_X = 20;
    int HP_BAR_Y = 20;

    int SCORE_X = 20;
    int SCORE_Y = 60;

    String TEXT_COLOR = "white";

    float HP_LOW_THRESHOLD = 0.3f;   // Below 30% = use different character
    float HP_MEDIUM_THRESHOLD = 0.6f; // Below 60% = use different character

    public void render(PlayerStatus status) {
        renderHPBar(status);
        renderScore(status);
    }

    private void renderHPBar(PlayerStatus status) {

        float hpPercent = status.getHealthPercentage();


        int barLength = (int) (20 * hpPercent); // 20 characters max
        StringBuilder barBuilder = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (i < barLength) {

                if (hpPercent <= HP_LOW_THRESHOLD) {
                    barBuilder.append("▒"); // Low HP - medium block
                } else if (hpPercent <= HP_MEDIUM_THRESHOLD) {
                    barBuilder.append("▓"); // Medium HP - dark block
                } else {
                    barBuilder.append("█"); // Full HP - full block
                }
            } else {
                barBuilder.append("░"); // Empty - light block
            }
        }
        String barVisual = barBuilder.toString();

        GameApp.drawText("default", barVisual, HP_BAR_X, HP_BAR_Y, TEXT_COLOR);

        String hpText = String.format("HP: %d / %d", status.health, status.maxHealth);
        GameApp.drawText("default", hpText, HP_BAR_X, HP_BAR_Y + 25, TEXT_COLOR);
    }

    void renderScore(PlayerStatus status) {
        String scoreText = "Score: " + status.score;
        GameApp.drawText("default", scoreText, SCORE_X, SCORE_Y, TEXT_COLOR);
    }

}

