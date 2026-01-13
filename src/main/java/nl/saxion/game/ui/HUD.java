package nl.saxion.game.ui;

import nl.saxion.game.core.PlayerStatus;
import nl.saxion.game.entities.PassiveItem;
import nl.saxion.game.entities.Weapon;
import nl.saxion.gameapp.GameApp;

import java.util.List;

/**
 * HUD class for displaying game information.
 * Shows: XP bar, level, score, timer, weapon info, and passive items.
 */
public class HUD {

    String TEXT_COLOR = "white";

    // Reference to weapon for displaying weapon info
    private Weapon weapon;
    
    // Reference to player's passive items
    private List<PassiveItem> passiveItems;

    public void render(PlayerStatus status, float gameTime) {
        // Draw shapes first (XP bar)
        renderXPBar(status);

        // Draw item icons with framed boxes (weapon + passive items)
        GameApp.startSpriteRendering();
        renderItemIcons();
        
        // Then draw all text with sprite rendering
        renderScore(status);
        renderXPText(status);
        renderSurvivalTime(gameTime);
        GameApp.endSpriteRendering();
    }
    
    /**
     * Set weapon reference for displaying weapon info.
     */
    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }
    
    /**
     * Set passive items reference for displaying in HUD.
     */
    public void setPassiveItems(List<PassiveItem> items) {
        this.passiveItems = items;
    }

    // Draw XP bar shapes - scaled for 960x540 world view
    private void renderXPBar(PlayerStatus status) {
        float percent = status.currentXP / (float) status.xpToNext;
        percent = GameApp.clamp(percent, 0f, 1f);

        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f; // Thicker bar at top for better visibility
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

        // Position: centered horizontally, below XP bar - adjusted for larger bar
        String fontName = GameApp.hasFont("timerFont") ? "timerFont" : "default";
        float textWidth = GameApp.getTextWidth(fontName, timeText);
        float x = (screenWidth - textWidth) / 2f; // Center horizontally
        float y = screenHeight - 40f; // Adjusted for thicker XP bar

        GameApp.drawText(fontName, timeText, x, y, "white");
    }

    // Draw XP level text - right side, vertically centered with bar
    private void renderXPText(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f; // Match the thicker bar height
        float barY = screenHeight - barHeight;
        float textY = barY + barHeight - 14f; // Adjusted for larger bar

        String levelText = "LV " + status.level;
        String fontName = GameApp.hasFont("levelFont") ? "levelFont" : "default";

        float textWidth = GameApp.getTextWidth(fontName, levelText);
        float textX = screenWidth - textWidth - 8f; // More padding from edge

        GameApp.drawText(fontName, levelText, textX, textY, "white");
    }

    private void renderScore(PlayerStatus status) {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f; // Match the thicker bar height
        float barY = screenHeight - barHeight;

        float scoreY = barY - 22f; // Adjusted spacing below XP bar
        String scoreText = formatScore(status.score);

        String fontName = GameApp.hasFont("scoreFont") ? "scoreFont" : "default";
        float textWidth = GameApp.getTextWidth(fontName, scoreText);
        
        // Star icon size and position (larger and better aligned with text)
        float iconSize = 50f; // Icon size to match text height
        float iconGap = 1f; // Gap between text and icon
        float totalWidth = textWidth + iconGap + iconSize;
        
        float scoreX = screenWidth - totalWidth - 10f; // More padding from edge

        // Draw score text
        GameApp.drawText(fontName, scoreText, scoreX, scoreY, "white");
        
        // Draw star icon to the right of score (aligned with text baseline)
        if (GameApp.hasTexture("star_icon")) {
            float iconX = scoreX + textWidth + iconGap + 5f;
            float iconY = scoreY - 20f; // Align with text (same level)
            GameApp.drawTexture("star_icon", iconX, iconY, iconSize, iconSize);
        }
    }
    
    /**
     * Render weapon info - no longer used (moved to renderItemIcons).
     */
    private void renderWeaponInfo() {
        // Weapon info is now rendered in renderItemIcons()
    }
    
    /**
     * Render passive items - no longer used (moved to renderItemIcons).
     */
    private void renderPassiveItems() {
        // Passive items are now rendered in renderItemIcons()
    }
    
    /**
     * Render weapon and passive item icons in framed boxes.
     * Layout: 5 icons per row, weapon always first, then passive items.
     * Smaller icons with semi-transparent border only (no background).
     */
    public void renderItemIcons() {
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f;
        
        // Smaller icon box settings
        float boxSize = 26f;
        float iconSize = boxSize - 2f; // Icons fill most of the box
        float pistonSize = boxSize + 2f; // Piston larger to fill box better
        float startX = 6f;
        float startY = screenHeight - barHeight - boxSize - 6f;
        float gap = 3f;
        int maxPerRow = 5;
        
        int iconIndex = 0;
        
        // End any current sprite rendering to draw shapes
        GameApp.endSpriteRendering();
        
        // Draw weapon icon box (always first) - using piston.png
        if (weapon != null) {
            float col = iconIndex % maxPerRow;
            float row = iconIndex / maxPerRow;
            float boxX = startX + col * (boxSize + gap);
            float boxY = startY - row * (boxSize + gap);
            
            // Draw semi-transparent border only (no filled background)
            GameApp.enableTransparency();
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(1.5f);
            if (weapon.isEvolved()) {
                GameApp.setColor(148, 0, 211, 180); // Purple, semi-transparent
            } else if (weapon.isMaxLevel()) {
                GameApp.setColor(255, 215, 0, 180); // Gold, semi-transparent
            } else {
                GameApp.setColor(150, 150, 150, 120); // Gray, more transparent
            }
            GameApp.drawRect(boxX, boxY, boxSize, boxSize);
            GameApp.endShapeRendering();
            
            // Draw weapon icon using piston.png (larger size to fill box)
            GameApp.startSpriteRendering();
            if (GameApp.hasTexture("piston_icon")) {
                float iconX = boxX + (boxSize - pistonSize) / 2f;
                float iconY = boxY + (boxSize - pistonSize) / 2f;
                GameApp.drawTexture("piston_icon", iconX, iconY, pistonSize, pistonSize);
            } else if (GameApp.hasTexture("weapon_icon")) {
                float iconX = boxX + (boxSize - iconSize) / 2f;
                float iconY = boxY + (boxSize - iconSize) / 2f;
                GameApp.drawTexture("weapon_icon", iconX, iconY, iconSize, iconSize);
            }
            GameApp.endSpriteRendering();
            
            iconIndex++;
        }
        
        // Draw passive item icon boxes
        if (passiveItems != null) {
            for (PassiveItem item : passiveItems) {
                float col = iconIndex % maxPerRow;
                float row = iconIndex / maxPerRow;
                float boxX = startX + col * (boxSize + gap);
                float boxY = startY - row * (boxSize + gap);
                
                // Draw semi-transparent border only (no filled background)
                GameApp.enableTransparency();
                GameApp.startShapeRenderingOutlined();
                GameApp.setLineWidth(1.5f);
                if (item.getLevel() >= item.getMaxLevel()) {
                    GameApp.setColor(255, 215, 0, 180); // Gold, semi-transparent
                } else {
                    GameApp.setColor(150, 150, 150, 120); // Gray, more transparent
                }
                GameApp.drawRect(boxX, boxY, boxSize, boxSize);
                GameApp.endShapeRendering();
                
                // Draw passive item icon
                GameApp.startSpriteRendering();
                String textureKey = item.getTextureKey();
                if (textureKey != null && GameApp.hasTexture(textureKey)) {
                    float iconX = boxX + (boxSize - iconSize) / 2f;
                    float iconY = boxY + (boxSize - iconSize) / 2f;
                    GameApp.drawTexture(textureKey, iconX, iconY, iconSize, iconSize);
                }
                GameApp.endSpriteRendering();
                
                iconIndex++;
            }
        }
        
        // Resume sprite rendering for other HUD elements
        GameApp.startSpriteRendering();
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
    
    // Public methods for rendering individual HUD components (for level up menu)
    public void renderScoreOnly(PlayerStatus status) {
        renderScore(status);
    }
    
    public void renderXPTextOnly(PlayerStatus status) {
        renderXPText(status);
    }
    
    public void renderSurvivalTimeOnly(float gameTime) {
        renderSurvivalTime(gameTime);
    }
    
    public void renderWeaponInfoOnly() {
        // Now handled by renderItemIconsOnly()
    }
    
    public void renderPassiveItemsOnly() {
        // Now handled by renderItemIconsOnly()
    }
    
    /**
     * Render item icons only (for level up menu overlay).
     * Note: This manages its own sprite batch state internally.
     */
    public void renderItemIconsOnly() {
        renderItemIconsStandalone();
    }
    
    /**
     * Standalone version that manages its own rendering state.
     * Smaller icons with semi-transparent border only (no background).
     */
    private void renderItemIconsStandalone() {
        float screenHeight = GameApp.getWorldHeight();
        float barHeight = 18f;
        
        // Smaller icon box settings
        float boxSize = 26f;
        float iconSize = boxSize - 2f; // Icons fill most of the box
        float pistonSize = boxSize + 2f; // Piston larger to fill box better
        float startX = 6f;
        float startY = screenHeight - barHeight - boxSize - 6f;
        float gap = 3f;
        int maxPerRow = 5;
        
        int iconIndex = 0;
        
        // Draw weapon icon box (always first) - using piston.png
        if (weapon != null) {
            float col = iconIndex % maxPerRow;
            float row = iconIndex / maxPerRow;
            float boxX = startX + col * (boxSize + gap);
            float boxY = startY - row * (boxSize + gap);
            
            // Draw semi-transparent border only (no filled background)
            GameApp.enableTransparency();
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(1.5f);
            if (weapon.isEvolved()) {
                GameApp.setColor(148, 0, 211, 180); // Purple, semi-transparent
            } else if (weapon.isMaxLevel()) {
                GameApp.setColor(255, 215, 0, 180); // Gold, semi-transparent
            } else {
                GameApp.setColor(150, 150, 150, 120); // Gray, more transparent
            }
            GameApp.drawRect(boxX, boxY, boxSize, boxSize);
            GameApp.endShapeRendering();
            
            // Draw weapon icon using piston.png (larger size to fill box)
            GameApp.startSpriteRendering();
            if (GameApp.hasTexture("piston_icon")) {
                float iconX = boxX + (boxSize - pistonSize) / 2f;
                float iconY = boxY + (boxSize - pistonSize) / 2f;
                GameApp.drawTexture("piston_icon", iconX, iconY, pistonSize, pistonSize);
            } else if (GameApp.hasTexture("weapon_icon")) {
                float iconX = boxX + (boxSize - iconSize) / 2f;
                float iconY = boxY + (boxSize - iconSize) / 2f;
                GameApp.drawTexture("weapon_icon", iconX, iconY, iconSize, iconSize);
            }
            GameApp.endSpriteRendering();
            
            iconIndex++;
        }
        
        // Draw passive item icon boxes
        if (passiveItems != null) {
            for (PassiveItem item : passiveItems) {
                float col = iconIndex % maxPerRow;
                float row = iconIndex / maxPerRow;
                float boxX = startX + col * (boxSize + gap);
                float boxY = startY - row * (boxSize + gap);
                
                // Draw semi-transparent border only (no filled background)
                GameApp.enableTransparency();
                GameApp.startShapeRenderingOutlined();
                GameApp.setLineWidth(1.5f);
                if (item.getLevel() >= item.getMaxLevel()) {
                    GameApp.setColor(255, 215, 0, 180); // Gold, semi-transparent
                } else {
                    GameApp.setColor(150, 150, 150, 120); // Gray, more transparent
                }
                GameApp.drawRect(boxX, boxY, boxSize, boxSize);
                GameApp.endShapeRendering();
                
                // Draw passive item icon
                GameApp.startSpriteRendering();
                String textureKey = item.getTextureKey();
                if (textureKey != null && GameApp.hasTexture(textureKey)) {
                    float iconX = boxX + (boxSize - iconSize) / 2f;
                    float iconY = boxY + (boxSize - iconSize) / 2f;
                    GameApp.drawTexture(textureKey, iconX, iconY, iconSize, iconSize);
                }
                GameApp.endSpriteRendering();
                
                iconIndex++;
            }
        }
    }
}
