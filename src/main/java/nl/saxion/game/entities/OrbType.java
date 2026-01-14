package nl.saxion.game.entities;

/**
 * Enum representing different types of XP orbs in the game.
 * XP values balanced so player can max level at minute 8-9 of 10-minute game.
 * - BLUE: Most common, spawns from enemies (75%) and breakable objects - lowest XP
 * - GREEN: Rare, spawns only from killing enemies (10%) - medium XP
 * - RED: Special, spawns only when killing miniboss - highest XP
 */
public enum OrbType {
    BLUE("orb_blue", 5),      // Common orb, 5 XP (lowest)
    GREEN("orb_green", 15),   // Rare orb, 15 XP (medium)
    RED("orb_red", 50);       // Miniboss orb, 50 XP (highest)
    
    private final String textureName;
    private final int xpValue;
    
    OrbType(String textureName, int xpValue) {
        this.textureName = textureName;
        this.xpValue = xpValue;
    }
    
    public String getTextureName() {
        return textureName;
    }
    
    public int getXpValue() {
        return xpValue;
    }
}
