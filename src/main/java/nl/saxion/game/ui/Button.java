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
    private boolean isVisible = true;

    private Runnable onClickAction;

    // Colors for button states
    private String normalColor = "darkgray";
    private String hoverColor = "gray";
    private String borderColor = "white";
    private String textColor = "white";

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
        if (onClickAction != null) {
            onClickAction.run();
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
}

