package nl.saxion.game.entities;

public class LevelUpOption {

    public enum Type { STAT }

    public final String title;
    public final String description;
    public final StatUpgradeType stat;

    public LevelUpOption(StatUpgradeType stat) {
        this.stat = stat;
        this.title = stat.title;
        this.description = stat.description;
    }
}
