package nl.saxion.game.systems;

import nl.saxion.game.entities.DamageText;
import nl.saxion.gameapp.GameApp;

import java.util.List;

// Manages damage text spawning and rendering
public class DamageTextSystem {
    private DamageTextPool pool;

    public DamageTextSystem() {
        pool = new DamageTextPool();
    }

    // Spawn damage text when enemy is hit
    public void spawnDamageText(float enemyX, float enemyY, int damage, boolean isCrit) {
        DamageText text = pool.obtain();
        text.activate(damage, enemyX, enemyY, isCrit);
    }

    // Update all active damage texts
    public void update(float delta) {
        pool.update(delta);
    }

    // Render all active damage texts
    public void render(float playerWorldX, float playerWorldY) {
        List<DamageText> activeTexts = pool.getActive();
        if (activeTexts.isEmpty()) {
            return;
        }

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        GameApp.startSpriteRendering();

        for (DamageText text : activeTexts) {
            if (!text.isActive) continue;

            // Convert world position to screen position
            float offsetX = text.getRenderX() - playerWorldX;
            float offsetY = text.getRenderY() - playerWorldY;
            float screenX = worldW / 2f + offsetX;
            float screenY = worldH / 2f + offsetY;

            // Only render if in viewport
            if (screenX < -50 || screenX > worldW + 50 ||
                    screenY < -50 || screenY > worldH + 50) {
                continue;
            }

            // Format damage value
            String damageStr = String.valueOf(text.value);

            // Use damage font
            String fontName = GameApp.hasFont("damageFont") ? "damageFont" : "default";

            // Color: cam đậm cho normal, vàng sáng cho crit (tăng saturation)
            String color = text.isCrit ? "yellow-300" : "orange-600";

            // Calculate text width for centering (account for scale)
            float baseTextWidth = GameApp.getTextWidth(fontName, damageStr);
            float scaledTextWidth = baseTextWidth * text.scale;
            float centerX = screenX - scaledTextWidth / 2f;

            // Draw outline (black, 0.5px offset - gọn hơn, không dày)
            float outlineOffset = 0.5f;
            GameApp.drawText(fontName, damageStr, centerX - outlineOffset, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, damageStr, centerX + outlineOffset, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, damageStr, centerX - outlineOffset, screenY + outlineOffset, "black");
            GameApp.drawText(fontName, damageStr, centerX + outlineOffset, screenY + outlineOffset, "black");
            GameApp.drawText(fontName, damageStr, centerX - outlineOffset, screenY, "black");
            GameApp.drawText(fontName, damageStr, centerX + outlineOffset, screenY, "black");
            GameApp.drawText(fontName, damageStr, centerX, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, damageStr, centerX, screenY + outlineOffset, "black");

            // Draw main text (centered)
            // Note: Scale effect is simulated by font size, alpha is handled by color fade
            GameApp.drawText(fontName, damageStr, centerX, screenY, color);
        }

        GameApp.endSpriteRendering();
    }

    public void reset() {
        pool.clear();
    }
}
