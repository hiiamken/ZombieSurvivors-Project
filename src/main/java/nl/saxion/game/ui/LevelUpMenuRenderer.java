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
    
    // Font loading flag to ensure fonts are loaded only once
    private static boolean fontsLoaded = false;
    
    /**
     * Render level up menu with VS style
     */
    public void render(List<LevelUpOption> options, int selectedIndex, 
                      float delta, float animTimer, float selectAnim, 
                      boolean isOpening, Player player, Weapon weapon) {
        if (options == null || options.isEmpty()) return;
        
        // Ensure fonts are loaded
        ensureFontsLoaded();
        
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
        
        // Menu panel dimensions (Vampire Survivors style - improved spacing)
        int count = Math.min(3, options.size()); // Show max 3 options
        float menuW = screenWidth * 0.50f; // Slightly wider for better text space
        float optionH = 100f; // Increased height for better text spacing
        float gap = 18f; // Increased gap between options
        float padding = 25f; // More padding
        float titleAreaH = 80f; // More space for title
        
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
        // Position from bottom: menuY + totalH - 40f (40f from top)
        GameApp.startSpriteRendering();
        float titleY = menuY + totalH - 40f;
        String titleFont = GameApp.hasFont("levelUpTitleFont") ? "levelUpTitleFont" : "default";
        GameApp.drawTextCentered(titleFont, "LEVEL UP!", centerX, titleY, "yellow-400");
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
        
        // Text content with improved spacing
        float textX = x + 105f;
        float titleY = y + height - 30f; // More space from top
        
        // Title with better font
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
        
        // Use better font for title
        String titleFont = GameApp.hasFont("upgradeItemFont") ? "upgradeItemFont" : "default";
        GameApp.drawText(titleFont, titleText, textX, titleY, "white");
        
        // "NEW!" label with highlight (for new passive items)
        if (isNew) {
            String newFont = GameApp.hasFont("upgradeNewFont") ? "upgradeNewFont" : "default";
            float newLabelX = textX + GameApp.getTextWidth(titleFont, titleText) + 15f;
            // Draw background highlight for NEW label
            GameApp.endSpriteRendering();
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(255, 215, 0, (int)(180 * menuAlpha)); // Gold background
            float newBgWidth = GameApp.getTextWidth(newFont, "NEW!") + 8f;
            GameApp.drawRect(newLabelX - 4f, titleY - 2f, newBgWidth, 18f);
            GameApp.endShapeRendering();
            GameApp.startSpriteRendering();
            GameApp.drawText(newFont, "NEW!", newLabelX, titleY, "black");
        }
        
        // Description with better font and spacing
        float descY = y + 30f; // Adjusted spacing for larger fonts
        String descFont = GameApp.hasFont("upgradeDescFont") ? "upgradeDescFont" : "default";
        GameApp.drawText(descFont, option.description, textX, descY, "gray-200");
        
        // Level text (right side) with better font
        String levelText = getLevelText(option, player, weapon);
        String levelFont = GameApp.hasFont("upgradeLevelFont") ? "upgradeLevelFont" : "default";
        float levelX = x + width - GameApp.getTextWidth(levelFont, levelText) - 25f;
        GameApp.drawText(levelFont, levelText, levelX, titleY, "cyan-300");
        
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
    
    /**
     * Ensure all fonts for the upgrade menu are loaded
     */
    private void ensureFontsLoaded() {
        if (fontsLoaded) return;
        
        try {
            // Title font - large and bold for "LEVEL UP!"
            if (!GameApp.hasFont("levelUpTitleFont")) {
                GameApp.addStyledFont("levelUpTitleFont", "fonts/PressStart2P-Regular.ttf", 24,
                    "yellow-400", 2.5f, "black", 3, 3, "orange-800", true);
            }
            
            // Item name font - larger size, very readable
            if (!GameApp.hasFont("upgradeItemFont")) {
                GameApp.addStyledFont("upgradeItemFont", "fonts/PressStart2P-Regular.ttf", 14,
                    "white", 2.0f, "black", 2, 2, "gray-800", true);
            }
            
            // Description font - readable size for descriptions
            if (!GameApp.hasFont("upgradeDescFont")) {
                GameApp.addStyledFont("upgradeDescFont", "fonts/VT323-Regular.ttf", 16,
                    "gray-200", 1.5f, "black", 1, 1, "gray-900", true);
            }
            
            // Level font - larger, more visible for level indicators
            if (!GameApp.hasFont("upgradeLevelFont")) {
                GameApp.addStyledFont("upgradeLevelFont", "fonts/PressStart2P-Regular.ttf", 16,
                    "cyan-300", 2.0f, "black", 2, 2, "blue-900", true);
            }
            
            // NEW label font - attention-grabbing
            if (!GameApp.hasFont("upgradeNewFont")) {
                GameApp.addStyledFont("upgradeNewFont", "fonts/PressStart2P-Regular.ttf", 10,
                    "black", 1.5f, "yellow-400", 1, 1, "orange-600", true);
            }
            
            fontsLoaded = true;
            GameApp.log("Upgrade menu fonts loaded successfully");
            
        } catch (Exception e) {
            GameApp.log("Warning: Could not load some upgrade menu fonts: " + e.getMessage());
            fontsLoaded = true; // Set to true anyway to avoid repeated attempts
        }
    }
}
