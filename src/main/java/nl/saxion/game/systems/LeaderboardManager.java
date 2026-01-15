package nl.saxion.game.systems;

import nl.saxion.game.core.LeaderboardEntry;
import nl.saxion.game.core.PlayerData;
import nl.saxion.gameapp.GameApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the leaderboard - saving and loading scores from JSON file.
 */
public class LeaderboardManager {
    
    private static final String FILE_NAME = "leaderboard.json";
    private static final int MAX_ENTRIES = 100; // Keep top 100 entries
    
    private static List<LeaderboardEntry> entries = new ArrayList<>();
    private static boolean loaded = false;
    
    /**
     * Add a new entry to the leaderboard.
     */
    public static void addEntry(PlayerData player, int score, float survivalTime) {
        if (player == null) {
            GameApp.log("Cannot add leaderboard entry: no player data");
            return;
        }
        
        // Validate input data
        String username = player.getUsername();
        if (username == null || username.trim().isEmpty()) {
            GameApp.log("Cannot add leaderboard entry: invalid username");
            return;
        }
        
        // Load existing entries if not loaded
        if (!loaded) {
            loadLeaderboard();
        }
        
        // Ensure entries list is not null
        if (entries == null) {
            entries = new ArrayList<>();
        }
        
        // Create new entry with sanitized data
        LeaderboardEntry entry = new LeaderboardEntry(
            username.trim(),
            player.getStudentClass() != null ? player.getStudentClass().trim() : "",
            player.getGroupNumber() != null ? player.getGroupNumber().trim() : "",
            Math.max(0, score), // Ensure non-negative score
            Math.max(0, survivalTime) // Ensure non-negative time
        );
        
        entries.add(entry);
        
        // Sort entries (best first)
        Collections.sort(entries);
        
        // Trim to max entries
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
        
        // Save to file
        saveLeaderboard();
    }
    
    /**
     * Get all leaderboard entries (sorted best to worst).
     * Never returns null - returns empty list if no entries.
     */
    public static List<LeaderboardEntry> getEntries() {
        if (!loaded) {
            loadLeaderboard();
        }
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return new ArrayList<>(entries); // Return copy
    }
    
    /**
     * Get top N entries.
     * Never returns null - returns empty list if no entries.
     */
    public static List<LeaderboardEntry> getTopEntries(int count) {
        List<LeaderboardEntry> all = getEntries();
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        int limit = Math.min(count, all.size());
        return new ArrayList<>(all.subList(0, limit));
    }
    
    /**
     * Get the rank of a specific score/time combination.
     * Returns 1-based rank. Returns 1 if leaderboard is empty.
     */
    public static int getRank(int score, float survivalTime) {
        if (!loaded) {
            loadLeaderboard();
        }
        
        if (entries == null || entries.isEmpty()) {
            return 1; // First place if no entries
        }
        
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (entry != null) {
                if (entry.getScore() > score) {
                    rank++;
                } else if (entry.getScore() == score && entry.getSurvivalTime() > survivalTime) {
                    rank++;
                }
            }
        }
        return rank;
    }
    
    /**
     * Get total number of entries in the leaderboard.
     */
    public static int getEntryCount() {
        if (!loaded) {
            loadLeaderboard();
        }
        return entries != null ? entries.size() : 0;
    }
    
    /**
     * Load leaderboard from JSON file.
     */
    public static void loadLeaderboard() {
        entries.clear();
        File file = new File(FILE_NAME);
        
        if (!file.exists()) {
            GameApp.log("Leaderboard file not found - starting fresh");
            loaded = true;
            return;
        }
        
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            
            br.close();
            fr.close();
            
            // Parse JSON
            parseJSON(content.toString());
            
            // Sort entries after loading
            Collections.sort(entries);
            
            loaded = true;
            
        } catch (Exception e) {
            loaded = true;
        }
    }
    
    /**
     * Parse JSON content into entries.
     * Format: {"entries": [{...}, {...}, ...]}
     */
    private static void parseJSON(String json) {
        try {
            // Remove outer braces and whitespace
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return;
            }
            
            // Find the entries array
            int entriesStart = json.indexOf("[");
            int entriesEnd = json.lastIndexOf("]");
            
            if (entriesStart == -1 || entriesEnd == -1) {
                return;
            }
            
            String entriesJson = json.substring(entriesStart + 1, entriesEnd);
            
            // Split into individual entry objects
            // Simple approach: split by "},{" pattern
            String[] entryStrings = entriesJson.split("\\},\\s*\\{");
            
            for (String entryStr : entryStrings) {
                // Clean up brackets
                entryStr = entryStr.replace("{", "").replace("}", "").trim();
                
                if (entryStr.isEmpty()) continue;
                
                // Parse individual entry
                LeaderboardEntry entry = parseEntry(entryStr);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Parse a single entry from JSON string.
     */
    private static LeaderboardEntry parseEntry(String entryJson) {
        try {
            String username = "";
            String studentClass = "";
            String groupNumber = "";
            int score = 0;
            float survivalTime = 0f;
            long timestamp = 0L;
            
            // Split by comma, handling quotes
            String[] pairs = entryJson.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            
            for (String pair : pairs) {
                pair = pair.trim();
                String[] kv = pair.split(":", 2);
                if (kv.length != 2) continue;
                
                String key = kv[0].replace("\"", "").trim();
                String value = kv[1].replace("\"", "").trim();
                
                switch (key) {
                    case "username":
                        username = value;
                        break;
                    case "studentClass":
                        studentClass = value;
                        break;
                    case "groupNumber":
                        groupNumber = value;
                        break;
                    case "score":
                        score = Integer.parseInt(value);
                        break;
                    case "survivalTime":
                        survivalTime = Float.parseFloat(value);
                        break;
                    case "timestamp":
                        timestamp = Long.parseLong(value);
                        break;
                }
            }
            
            if (!username.isEmpty()) {
                return new LeaderboardEntry(username, studentClass, groupNumber, 
                                           score, survivalTime, timestamp);
            }
            
        } catch (Exception e) {
            GameApp.log("Error parsing entry: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Save leaderboard to JSON file.
     */
    public static void saveLeaderboard() {
        try {
            FileWriter fw = new FileWriter(FILE_NAME);
            BufferedWriter bw = new BufferedWriter(fw);
            
            bw.write("{\n");
            bw.write("  \"entries\": [\n");
            
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                bw.write("    {\n");
                bw.write("      \"username\": \"" + escapeJson(entry.getUsername()) + "\",\n");
                bw.write("      \"studentClass\": \"" + escapeJson(entry.getStudentClass()) + "\",\n");
                bw.write("      \"groupNumber\": \"" + escapeJson(entry.getGroupNumber()) + "\",\n");
                bw.write("      \"score\": " + entry.getScore() + ",\n");
                bw.write("      \"survivalTime\": " + entry.getSurvivalTime() + ",\n");
                bw.write("      \"timestamp\": " + entry.getTimestamp() + "\n");
                bw.write("    }");
                
                if (i < entries.size() - 1) {
                    bw.write(",");
                }
                bw.write("\n");
            }
            
            bw.write("  ]\n");
            bw.write("}");
            
            bw.flush();
            bw.close();
            fw.close();
            
        } catch (IOException e) {
            GameApp.log("Error saving leaderboard: " + e.getMessage());
        }
    }
    
    /**
     * Escape special characters in JSON strings.
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Clear all entries (for testing).
     */
    public static void clearLeaderboard() {
        entries.clear();
        saveLeaderboard();
        GameApp.log("Leaderboard cleared");
    }
    
    /**
     * Reload leaderboard from file.
     */
    public static void reload() {
        loaded = false;
        loadLeaderboard();
    }
}
