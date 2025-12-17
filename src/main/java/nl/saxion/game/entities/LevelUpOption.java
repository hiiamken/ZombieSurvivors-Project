package nl.saxion.game.entities;

public class LevelUpOption {

    public enum Type { STAT }

    public final String title;
    public final String description;
    public final StatUpgradeType stat;
    public final int currentLevel;
    public final int nextLevel;

    public LevelUpOption(StatUpgradeType stat, int currentLevel) {
        this.stat = stat;
        this.currentLevel = currentLevel;
        this.nextLevel = currentLevel + 1;
        this.title = stat.getTitleWithLevel(nextLevel);
        this.description = stat.getDescriptionForLevel(nextLevel);
    }
}
