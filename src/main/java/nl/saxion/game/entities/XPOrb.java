package nl.saxion.game.entities;

import nl.saxion.gameapp.GameApp;

public class XPOrb {

    private float x, y;
    private int xpValue;
    private boolean collected = false;
    private OrbType orbType;

    // Reduced magnet range - orbs only attract when player is closer
    private float baseMagnetRange = 50f;
    private float magnetSpeed = 150f;
    
    // Static orb size (no animation)
    private static final float ORB_SIZE = 16f;

    // Constructor with orb type
    public XPOrb(float x, float y, OrbType orbType) {
        this.x = x;
        this.y = y;
        this.orbType = orbType;
        this.xpValue = orbType.getXpValue();
    }

    // Legacy constructor for backward compatibility (defaults to BLUE)
    public XPOrb(float x, float y, int xpValue) {
        this.x = x;
        this.y = y;
        this.orbType = OrbType.BLUE;
        this.xpValue = xpValue;
    }

    public void update(float delta, float playerX, float playerY) {
        update(delta, playerX, playerY, 0f);
    }

    public void update(float delta, float playerX, float playerY, float magnetBonusRange) {
        if (collected) return;

        // Calculate effective magnet range (base + bonus from passive item)
        float effectiveMagnetRange = baseMagnetRange + magnetBonusRange;
        
        float dist = GameApp.distance(x, y, playerX, playerY);

        // Magnet effect - pull toward player when in range
        if (dist < effectiveMagnetRange && dist > 0) {
            float dx = (playerX - x) / dist;
            float dy = (playerY - y) / dist;
            
            // Speed increases as orb gets closer
            float speedMultiplier = 1f + (1f - dist / effectiveMagnetRange) * 0.5f;
            x += dx * magnetSpeed * speedMultiplier * delta;
            y += dy * magnetSpeed * speedMultiplier * delta;
        }

        // Collect when very close to player
        if (dist < 20f) {
            collected = true;
        }
    }

    // Render with texture - static, no animation (requires sprite rendering to be active)
    public void renderWithTexture(float playerWorldX, float playerWorldY) {
        if (collected) return;
        
        float screenX = GameApp.getWorldWidth() / 2f + (x - playerWorldX);
        float screenY = GameApp.getWorldHeight() / 2f + (y - playerWorldY);
        
        // Draw orb texture based on type (static size, no pulsing)
        String textureName = orbType.getTextureName();
        if (GameApp.hasTexture(textureName)) {
            GameApp.drawTexture(textureName, screenX - ORB_SIZE/2, screenY - ORB_SIZE/2, ORB_SIZE, ORB_SIZE);
        } else {
            // Fallback to circle if texture not loaded
            renderWithCircle(playerWorldX, playerWorldY);
        }
    }

    // Render with circle fallback - static, no animation (requires shape rendering to be active)
    public void renderWithCircle(float playerWorldX, float playerWorldY) {
        if (collected) return;
        
        float screenX = GameApp.getWorldWidth() / 2f + (x - playerWorldX);
        float screenY = GameApp.getWorldHeight() / 2f + (y - playerWorldY);

        // Fallback to circle with color based on orb type (static size)
        switch (orbType) {
            case BLUE -> GameApp.setColor(0, 150, 255, 255);
            case GREEN -> GameApp.setColor(0, 255, 100, 255);
            case RED -> GameApp.setColor(255, 50, 50, 255);
        }
        GameApp.drawCircle(screenX, screenY, ORB_SIZE/2);
    }

    public boolean isCollected() { return collected; }
    
    // Orbs no longer expire - they persist until game ends
    public boolean isExpired() { return false; }
    
    public int getXPValue() { return xpValue; }
    
    public OrbType getOrbType() { return orbType; }
    
    public float getX() { return x; }
    
    public float getY() { return y; }
}
