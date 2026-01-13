package nl.saxion.game.systems;

import nl.saxion.game.entities.DamageText;
import nl.saxion.game.entities.HealthText;
import nl.saxion.gameapp.GameApp;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

// Manages damage text and health text spawning and rendering
public class DamageTextSystem {
    private DamageTextPool damagePool;
    private HealthTextPool healthPool;

    public DamageTextSystem() {
        damagePool = new DamageTextPool();
        healthPool = new HealthTextPool();
    }

    // Spawn damage text when enemy is hit
    public void spawnDamageText(float enemyX, float enemyY, int damage, boolean isCrit) {
        DamageText text = damagePool.obtain();
        text.activate(damage, enemyX, enemyY, isCrit);
    }

    // Spawn health text when player heals
    public void spawnHealthText(float playerX, float playerY, int healAmount) {
        HealthText text = healthPool.obtain();
        text.activate(healAmount, playerX, playerY);
    }

    // Update all active texts
    public void update(float delta) {
        damagePool.update(delta);
        healthPool.update(delta);
    }

    // Render all active texts (damage and health)
    public void render(float playerWorldX, float playerWorldY) {
        List<DamageText> activeDamageTexts = damagePool.getActive();
        List<HealthText> activeHealthTexts = healthPool.getActive();
        
        if (activeDamageTexts.isEmpty() && activeHealthTexts.isEmpty()) {
            return;
        }

        float worldW = GameApp.getWorldWidth();
        float worldH = GameApp.getWorldHeight();

        GameApp.startSpriteRendering();

        // Render damage texts
        for (DamageText text : activeDamageTexts) {
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

        // Render health texts (green color)
        for (HealthText text : activeHealthTexts) {
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

            // Format health value (no prefix, just the number)
            String healthStr = String.valueOf(text.value);

            // Use damage font
            String fontName = GameApp.hasFont("damageFont") ? "damageFont" : "default";

            // Calculate text width for centering (account for scale)
            float baseTextWidth = GameApp.getTextWidth(fontName, healthStr);
            float scaledTextWidth = baseTextWidth * text.scale;
            float centerX = screenX - scaledTextWidth / 2f;

            // Draw outline (black, 1px offset for better visibility)
            float outlineOffset = 1f;
            GameApp.drawText(fontName, healthStr, centerX - outlineOffset, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, healthStr, centerX + outlineOffset, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, healthStr, centerX - outlineOffset, screenY + outlineOffset, "black");
            GameApp.drawText(fontName, healthStr, centerX + outlineOffset, screenY + outlineOffset, "black");
            GameApp.drawText(fontName, healthStr, centerX - outlineOffset, screenY, "black");
            GameApp.drawText(fontName, healthStr, centerX + outlineOffset, screenY, "black");
            GameApp.drawText(fontName, healthStr, centerX, screenY - outlineOffset, "black");
            GameApp.drawText(fontName, healthStr, centerX, screenY + outlineOffset, "black");

            // Draw main text (centered, lime green for healing - visible and bright)
            GameApp.drawText(fontName, healthStr, centerX, screenY, Color.LIME);
        }

        GameApp.endSpriteRendering();
    }

    public void reset() {
        damagePool.clear();
        healthPool.clear();
    }
}
