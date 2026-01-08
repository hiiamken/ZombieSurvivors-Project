package nl.saxion.game.ui;

import nl.saxion.gameapp.GameApp;

// Simple interactive button class for menu UI
public class Button {

    private float x;
    private float y;
    private float width;
    private float height;
    private String text;

    private boolean isHovered = false;
    private boolean isSelected = false;
    private boolean isPressed = false;
    private boolean isVisible = true;

    private Runnable onClickAction;

    // Colors for button states
    private String normalColor = "gray-600";
    private String hoverColor = "gray-400";
    private String borderColor = "white";
    private String textColor = "white";

    // Sprite support for Pixel UI buttons
    private String normalSprite = null;
    private String hoverSprite = null;
    private String selectedSprite = null;
    private String pressedSprite = null;
    private boolean useSprite = false;

    public Button(float x, float y, float width, float height, String text) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
    }

    public void update(float mouseX, float mouseY) {
        // Check if mouse is hovering over button
        isHovered = containsPoint(mouseX, mouseY);
    }

    public void render() {
        if (!isVisible) return;

        // Use sprite if available
        if (useSprite) {
            String spriteToUse = normalSprite;
            if (isPressed && pressedSprite != null) {
                spriteToUse = pressedSprite;
            } else if (isSelected && selectedSprite != null) {
                spriteToUse = selectedSprite;
            } else if (isHovered && hoverSprite != null) {
                spriteToUse = hoverSprite;
            }

            if (spriteToUse != null && GameApp.hasTexture(spriteToUse)) {
                GameApp.startSpriteRendering();
                GameApp.drawTexture(spriteToUse, x, y, width, height);
                GameApp.endSpriteRendering();
            }
        } else {
            // Draw button background
            String bgColor = (isHovered || isSelected) ? hoverColor : normalColor;

            GameApp.startShapeRenderingFilled();
            GameApp.drawRect(x, y, width, height, bgColor);
            GameApp.endShapeRendering();

            // Draw border
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.drawRect(x, y, width, height, borderColor);
            GameApp.endShapeRendering();
        }

        // Draw text centered if text is not empty
        if (text != null && !text.isEmpty()) {
            GameApp.startSpriteRendering();
            float textX = x + width / 2;
            float textY = y + height / 2;
            GameApp.drawTextCentered("default", text, textX, textY, textColor);
            GameApp.endSpriteRendering();
        }
    }

    // Render only hover/selection effect (for image-based buttons)
    public void renderHoverEffect(int alpha) {
        if (isHovered || isSelected) {
            // Draw semi-transparent highlight overlay
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(255, 255, 255, alpha);
            GameApp.drawRect(x, y, width, height);
            GameApp.endShapeRendering();
        }
    }

    public boolean containsPoint(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public void setOnClick(Runnable action) {
        this.onClickAction = action;
    }

    public void click() {
        nl.saxion.game.utils.DebugLogger.log("Button.click() called. onClickAction is %s",
                onClickAction != null ? "SET" : "NULL");
        if (onClickAction != null) {
            try {
                onClickAction.run();
                nl.saxion.game.utils.DebugLogger.log("Button.click() - onClickAction executed successfully");
            } catch (Exception e) {
                nl.saxion.game.utils.DebugLogger.log("Button.click() - ERROR: %s", e.getMessage());
                e.printStackTrace();
            }
        } else {
            nl.saxion.game.utils.DebugLogger.log("Button.click() - WARNING: onClickAction is NULL!");
        }
    }

    public boolean isClicked(float mouseX, float mouseY, boolean mousePressed) {
        return containsPoint(mouseX, mouseY) && mousePressed;
    }

    // Getters and setters
    public boolean isHovered() {
        return isHovered;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setColors(String normal, String hover, String border, String text) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.borderColor = border;
        this.textColor = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    // Sprite methods for Pixel UI
    public void setSprites(String normal, String hover, String selected) {
        this.normalSprite = normal;
        this.hoverSprite = hover;
        this.selectedSprite = selected;
        this.useSprite = (normal != null);
    }

    public void setSprites(String normal, String hover, String selected, String pressed) {
        this.normalSprite = normal;
        this.hoverSprite = hover;
        this.selectedSprite = selected;
        this.pressedSprite = pressed;
        this.useSprite = (normal != null);
    }

    public void setNormalSprite(String sprite) {
        this.normalSprite = sprite;
        this.useSprite = (sprite != null);
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public boolean isUsingSprite() {
        return useSprite;
    }
}

