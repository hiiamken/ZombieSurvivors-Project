package nl.saxion.game.entities;

public enum StatUpgradeType {
    SPEED("Speed Boost", "+15% movement speed"),
    DAMAGE("Damage Boost", "+20% damage"),
    MAX_HEALTH("Max Health", "+20 HP"),
    HEALTH_REGEN("Health Regen", "+0.5 HP/sec"),
    XP_MAGNET("XP Magnet", "+50% pickup range");

    public final String title;
    public final String description;

    StatUpgradeType(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
