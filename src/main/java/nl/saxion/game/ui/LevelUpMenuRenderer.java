package nl.saxion.game.ui;

import nl.saxion.game.entities.LevelUpOption;
import nl.saxion.game.entities.Player;
import nl.saxion.game.entities.Weapon;
import nl.saxion.gameapp.GameApp;

import java.util.List;

/**
 * Renders level up menu in Vampire Survivors style
 */
public class LevelUpMenuRenderer {
    
    /**
     * Render level up menu with VS style
     */
    public void render(List<LevelUpOption> options, int selectedIndex, 
                      float delta, float animTimer, float selectAnim, 
                      boolean isOpening, Player player, Weapon weapon) {
        if (options == null || options.isEmpty()) return;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // Animation update
        float t = easeOutCubic(Math.min(1f, animTimer));
        
        // Light overlay (game background visible)
        GameApp.startShapeRenderingFilled();
        int overlayAlpha = (int)(150 * t);
        GameApp.setColor(0, 0, 0, overlayAlpha);
        GameApp.drawRect(0, 0, screenWidth, screenHeight);
        GameApp.endShapeRendering();
        
        // Menu panel dimensions (Vampire Survivors style - compact)
        int count = Math.min(3, options.size()); // Show max 3 options
        float menuW = screenWidth * 0.45f; // Narrower panel
        float optionH = 90f; // Option height
        float gap = 14f; // Gap between options
        float padding = 20f;
        float titleAreaH = 70f;
        
        float totalH = (count * optionH) + ((count - 1) * gap) + (padding * 2) + titleAreaH;
        float menuX = centerX - menuW / 2f;
        // GameApp uses bottom-left Y origin: y is bottom of rectangle
        // Center the panel: menuY is bottom of panel
        float menuY = centerY - totalH / 2f;
        
        // Draw main panel background (dark frame)
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(25, 25, 35, (int)(240 * t)); // Dark blue-gray background
        GameApp.drawRect(menuX, menuY, menuW, totalH);
        GameApp.endShapeRendering();
        
        // Draw golden border (4 sides)
        float borderThickness = 4f;
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(255, 215, 0, (int)(255 * t)); // Gold
        // Top border (y = menuY + totalH - borderThickness, so it sits on top)
        GameApp.drawRect(menuX - borderThickness, menuY + totalH - borderThickness, menuW + borderThickness * 2, borderThickness);
        // Bottom border (y = menuY - borderThickness, below panel)
        GameApp.drawRect(menuX - borderThickness, menuY - borderThickness, menuW + borderThickness * 2, borderThickness);
        // Left border (y = menuY - borderThickness, height = totalH + borderThickness * 2)
        GameApp.drawRect(menuX - borderThickness, menuY - borderThickness, borderThickness, totalH + borderThickness * 2);
        // Right border
        GameApp.drawRect(menuX + menuW, menuY - borderThickness, borderThickness, totalH + borderThickness * 2);
        GameApp.endShapeRendering();
        
        // Title "LEVEL UP!" inside panel with beautiful font
        // Position from bottom: menuY + totalH - 35f (35f from top)
        GameApp.startSpriteRendering();
        float titleY = menuY + totalH - 35f;
        String titleFont = GameApp.hasFont("levelUpTitleFont") ? "levelUpTitleFont" : "default";
        GameApp.drawTextCentered(titleFont, "LEVEL UP!", centerX, titleY, "yellow-500");
        GameApp.endSpriteRendering();
        
        // Render options
        // Option positions from bottom: start from menuY + totalH - titleAreaH - padding
        float optionStartY = menuY + totalH - titleAreaH - padding;
        for (int i = 0; i < count; i++) {
            LevelUpOption option = options.get(i);
            boolean selected = (i == selectedIndex);
            
            // Each option is lower (smaller Y): subtract (i * (optionH + gap))
            float optionY = optionStartY - i * (optionH + gap);
            
            renderOption(option, menuX, optionY, menuW, optionH, selected, selectAnim, t, centerX, player, weapon);
        }
    }
    
    /**
     * Render a single option card
     */
    private void renderOption(LevelUpOption option, float x, float y, float width, float height,
                             boolean selected, float selectAnim, float menuAlpha, float centerX,
                             Player player, Weapon weapon) {
        // Card background
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(35, 35, 45, (int)(235 * menuAlpha)); // Darker card background
        GameApp.drawRect(x, y, width, height);
        
        // Theme strip (left side)
        int[] rgb = option.themeRGB;
        GameApp.setColor(rgb[0], rgb[1], rgb[2], (int)(255 * menuAlpha));
        GameApp.drawRect(x, y, 6f, height);
        
        // Selection glow
        if (selected) {
            float glowAlpha = 0.3f + 0.4f * selectAnim;
            GameApp.setColor(rgb[0], rgb[1], rgb[2], (int)(glowAlpha * 200 * menuAlpha));
            GameApp.drawRect(x - 4f, y - 4f, width + 8f, height + 8f);
        }
        
        GameApp.endShapeRendering();
        
        // Render content
        GameApp.startSpriteRendering();
        
        // Arrows on both sides if selected
        float arrowSize = 24f;
        float arrowY = y + height / 2f - arrowSize / 2f;
        
        // Arrow on left (flipped)
        float arrowLeftX = x + 12f;
        if (selected && GameApp.hasTexture("arrow")) {
            GameApp.drawTexture("arrow", arrowLeftX, arrowY, arrowSize, arrowSize, 0f, true, false);
        }
        
        // Arrow on right (normal)
        float arrowRightX = x + width - 12f - arrowSize;
        if (selected && GameApp.hasTexture("arrow")) {
            GameApp.drawTexture("arrow", arrowRightX, arrowY, arrowSize, arrowSize);
        }
        
        // Icon
        float iconSize = 40f;
        float iconX = x + 50f;
        float iconY = y + height / 2f - iconSize / 2f;
        if (option.icon != null && GameApp.hasTexture(option.icon)) {
            GameApp.drawTexture(option.icon, iconX, iconY, iconSize, iconSize);
        }
        
        // Text content
        float textX = x + 100f;
        float titleY = y + height - 25f;
        
        // Title
        String titleText = option.title;
        // Remove "NEW: " prefix if exists (we'll show "New!" separately)
        boolean isNew = false;
        if (titleText.startsWith("NEW: ")) {
            titleText = titleText.substring(5);
            isNew = true;
        }
        // Also check for passive items that are new
        if (option.type == LevelUpOption.Type.PASSIVE && option.passiveCurrentLevel == 0) {
            isNew = true;
        }
        
        GameApp.drawText("default", titleText, textX, titleY, "white");
        
        // "New!" label (for new passive items)
        if (isNew) {
            float newLabelX = textX + GameApp.getTextWidth("default", titleText) + 12f;
            GameApp.drawText("default", "New!", newLabelX, titleY, "yellow-500");
        }
        
        // Description
        float descY = y + 28f;
        GameApp.drawText("default", option.description, textX, descY, "gray-300");
        
        // Level text (right side)
        String levelText = getLevelText(option, player, weapon);
        float levelX = x + width - GameApp.getTextWidth("default", levelText) - 20f;
        GameApp.drawText("default", levelText, levelX, titleY, "white");
        
        GameApp.endSpriteRendering();
    }
    
    private String getLevelText(LevelUpOption option, Player player, Weapon weapon) {
        return switch (option.type) {
            case WEAPON -> {
                int maxLevel = weapon != null ? weapon.getMaxLevel() : 8;
                yield "Lv" + option.weaponNextLevel + "/" + maxLevel;
            }
            case PASSIVE -> {
                int maxLevel = option.passiveItem != null ? option.passiveItem.maxLevel : 5;
                yield "Lv" + option.passiveNextLevel + "/" + maxLevel;
            }
            case EVOLUTION -> "EVO!";
            default -> "";
        };
    }
    
    private float easeOutCubic(float t) {
        return 1f - (float)Math.pow(1 - t, 3);
    }
}
