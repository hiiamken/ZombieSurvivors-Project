package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.gameapp.GameApp;

public class HUD {

    public void render(PlayerStatus s) {

        // ---- TEXT (PlayScreen already started sprite rendering) ----
        GameApp.drawText("default",
                "HP: " + s.health + "/" + s.maxHealth,
                20, 20, "white");

        GameApp.drawText("default",
                "LVL " + s.level,
                20, 45, "white");

        GameApp.drawText("default",
                "Score: " + s.score,
                20, 70, "white");


        // ---- XP BAR (shapes must be inside shape rendering begin/end) ----
        float barX = 20f;
        float barY = 95f;
        float barW = 200f;
        float barH = 10f;

        float pct = s.getXPPercentage();
        float fillW = barW * pct;

        // background + fill
        GameApp.endSpriteRendering();              // close sprite batch BEFORE shapes
        GameApp.startShapeRenderingFilled();

        GameApp.drawRect(barX, barY, barW, barH, "gray");
        GameApp.drawRect(barX, barY, fillW, barH, "green");

        GameApp.endShapeRendering();
        GameApp.startSpriteRendering();            // reopen sprite batch after shapes
    }
}
