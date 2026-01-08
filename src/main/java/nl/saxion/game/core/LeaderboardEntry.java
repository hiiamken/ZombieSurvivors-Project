package nl.saxion.game.core;

/**
 * Represents a single entry in the leaderboard.
 * Contains player info, score, and survival time.
 */
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    private String username;
    private String studentClass;
    private String groupNumber;
    private int score;
    private float survivalTime; // in seconds
    private long timestamp; // when the score was recorded
    
    public LeaderboardEntry(String username, String studentClass, String groupNumber, 
                           int score, float survivalTime) {
        this.username = username;
        this.studentClass = studentClass;
        this.groupNumber = groupNumber;
        this.score = score;
        this.survivalTime = survivalTime;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Constructor with timestamp (for loading from file)
    public LeaderboardEntry(String username, String studentClass, String groupNumber,
                           int score, float survivalTime, long timestamp) {
        this.username = username;
        this.studentClass = studentClass;
        this.groupNumber = groupNumber;
        this.score = score;
        this.survivalTime = survivalTime;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getUsername() {
        return username;
    }
    
    public String getStudentClass() {
        return studentClass;
    }
    
    public String getGroupNumber() {
        return groupNumber;
    }
    
    public int getScore() {
        return score;
    }
    
    public float getSurvivalTime() {
        return survivalTime;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Format survival time as MM:SS
     */
    public String getFormattedSurvivalTime() {
        int totalSeconds = (int) survivalTime;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Compare entries for sorting.
     * Primary: Higher score is better (descending)
     * Secondary: If same score, longer survival time is better (descending)
     */
    @Override
    public int compareTo(LeaderboardEntry other) {
        // First compare by score (descending - higher is better)
        int scoreCompare = Integer.compare(other.score, this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        
        // If same score, compare by survival time (descending - longer is better)
        return Float.compare(other.survivalTime, this.survivalTime);
    }
    
    @Override
    public String toString() {
        return username + " - " + score + " pts - " + getFormattedSurvivalTime();
    }
}
