package nl.saxion.game.utils;

import nl.saxion.gameapp.GameApp;

// Simple debug logger that uses GameApp.log
public class DebugLogger {

    private static boolean enabled = false; // Controlled by config

    // Set enabled state from config
    public static void setEnabled(boolean enabled) {
        DebugLogger.enabled = enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String message) {
        // Only log if debug is enabled
        if (!enabled) {
            return;
        }

        // Log using GameApp.log
        GameApp.log("[DEBUG] " + message);
    }

    public static void log(String format, Object... args) {
        // Only log if debug is enabled
        if (!enabled) {
            return;
        }

        // Log using GameApp.log
        GameApp.log("[DEBUG] " + String.format(format, args));
    }
}
