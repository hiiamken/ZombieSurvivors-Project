package nl.saxion.game.core;

/**
 * Stores player information entered before starting the game.
 * Used for tracking who is playing (for school demo/presentation).
 */
public class PlayerData {
    private String username;
    private String studentClass;
    private String groupNumber;
    
    // Static instance to persist across screens
    private static PlayerData currentPlayer = null;
    
    public PlayerData(String username, String studentClass, String groupNumber) {
        this.username = username;
        this.studentClass = studentClass;
        this.groupNumber = groupNumber;
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
    
    // Static methods for managing current player
    public static void setCurrentPlayer(PlayerData player) {
        currentPlayer = player;
    }
    
    public static PlayerData getCurrentPlayer() {
        return currentPlayer;
    }
    
    public static boolean hasCurrentPlayer() {
        return currentPlayer != null;
    }
    
    public static void clearCurrentPlayer() {
        currentPlayer = null;
    }
    
    @Override
    public String toString() {
        return username + " (" + studentClass + ", Group " + groupNumber + ")";
    }
}
