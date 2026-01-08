package nl.saxion.game.config;

import com.badlogic.gdx.Input;
import nl.saxion.gameapp.GameApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final String FILE_NAME = "config.json";

    public static GameConfig loadConfig() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            GameApp.log("Config not found → creating default.");
            GameConfig def = GameConfig.createDefault();
            saveConfig(def);
            return def;
        }

        GameConfig cfg = GameConfig.createDefault();

        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String firstLine = br.readLine();
            if (firstLine == null) {
                br.close();
                fr.close();
                return cfg;
            }

            // Detect format: JSON starts with {, key=value format has =
            boolean isJSON = firstLine.trim().startsWith("{");

            if (isJSON) {
                // JSON format
                StringBuilder jsonContent = new StringBuilder();
                jsonContent.append(firstLine.trim());
                String line;
                while ((line = br.readLine()) != null) {
                    jsonContent.append(line.trim());
                }
                parseJSON(cfg, jsonContent.toString());
            } else {
                // key=value format
                br.close();
                fr.close();

                // Reopen to read from start
                fr = new FileReader(file);
                br = new BufferedReader(fr);

                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.length() > 0 && !trimmed.startsWith("#")) {
                        int eqIndex = trimmed.indexOf('=');
                        if (eqIndex > 0) {
                            String key = trimmed.substring(0, eqIndex).trim();
                            String value = trimmed.substring(eqIndex + 1).trim();
                            applyConfigLine(cfg, key, value);
                        }
                    }
                }
            }

            br.close();
            fr.close();

            cfg.validate();
            GameApp.log("Config loaded from " + FILE_NAME);
            return cfg;

        } catch (Exception e) {
            GameApp.log("Error loading config: " + e.getMessage());
            GameConfig def = GameConfig.createDefault();
            saveConfig(def);
            return def;
        }
    }

    // Parse key=value format
    private static void applyConfigLine(GameConfig cfg, String key, String value) {
        try {
            if (key.equals("masterVolume")) {
                cfg.masterVolume = Float.parseFloat(value);
            } else if (key.equals("musicVolume")) {
                cfg.musicVolume = Float.parseFloat(value);
            } else if (key.equals("sfxVolume")) {
                cfg.sfxVolume = Float.parseFloat(value);
            } else if (key.equals("keyMoveUp")) {
                cfg.keyMoveUp = parseKeyValue(value);
            } else if (key.equals("keyMoveDown")) {
                cfg.keyMoveDown = parseKeyValue(value);
            } else if (key.equals("keyMoveLeft")) {
                cfg.keyMoveLeft = parseKeyValue(value);
            } else if (key.equals("keyMoveRight")) {
                cfg.keyMoveRight = parseKeyValue(value);
            } else if (key.equals("keyShoot")) {
                cfg.keyShoot = parseKeyValue(value);
            } else if (key.equals("debugEnabled")) {
                cfg.debugEnabled = Boolean.parseBoolean(value);
            } else if (key.equals("fullscreen")) {
                cfg.fullscreen = Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException ex) {
            GameApp.log("Invalid value in config for " + key + ": " + value);
        }
    }

    // Simple JSON parser for flat structure
    private static void parseJSON(GameConfig cfg, String json) {
        // Remove whitespace and braces
        json = json.replaceAll("\\s+", "").replace("{", "").replace("}", "");

        // Split by comma
        String[] pairs = json.split(",");

        for (String pair : pairs) {
            if (pair.isEmpty()) continue;

            // Split key:value
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue;

            String key = kv[0].replace("\"", "").trim();
            String value = kv[1].replace("\"", "").trim();

            try {
                if (key.equals("masterVolume")) {
                    cfg.masterVolume = Float.parseFloat(value);
                } else if (key.equals("musicVolume")) {
                    cfg.musicVolume = Float.parseFloat(value);
                } else if (key.equals("sfxVolume")) {
                    cfg.sfxVolume = Float.parseFloat(value);
                } else if (key.equals("keyMoveUp")) {
                    cfg.keyMoveUp = parseKeyValue(value);
                } else if (key.equals("keyMoveDown")) {
                    cfg.keyMoveDown = parseKeyValue(value);
                } else if (key.equals("keyMoveLeft")) {
                    cfg.keyMoveLeft = parseKeyValue(value);
                } else if (key.equals("keyMoveRight")) {
                    cfg.keyMoveRight = parseKeyValue(value);
                } else if (key.equals("keyShoot")) {
                    cfg.keyShoot = parseKeyValue(value);
                } else if (key.equals("debugEnabled")) {
                    cfg.debugEnabled = Boolean.parseBoolean(value);
                } else if (key.equals("fullscreen")) {
                    cfg.fullscreen = Boolean.parseBoolean(value);
                }
            } catch (NumberFormatException ex) {
                GameApp.log("Invalid value in config for " + key + ": " + value);
            }
        }
    }

    // Convert key name string to Input.Keys constant, or parse as integer
    private static int parseKeyValue(String value) {
        // Try to parse as integer first
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // If not a number, try to convert key name to constant
            return keyNameToConstant(value.toUpperCase());
        }
    }

    // Convert key name to Input.Keys constant
    private static int keyNameToConstant(String keyName) {
        switch (keyName) {
            case "A": return Input.Keys.A;
            case "B": return Input.Keys.B;
            case "C": return Input.Keys.C;
            case "D": return Input.Keys.D;
            case "E": return Input.Keys.E;
            case "F": return Input.Keys.F;
            case "G": return Input.Keys.G;
            case "H": return Input.Keys.H;
            case "I": return Input.Keys.I;
            case "J": return Input.Keys.J;
            case "K": return Input.Keys.K;
            case "L": return Input.Keys.L;
            case "M": return Input.Keys.M;
            case "N": return Input.Keys.N;
            case "O": return Input.Keys.O;
            case "P": return Input.Keys.P;
            case "Q": return Input.Keys.Q;
            case "R": return Input.Keys.R;
            case "S": return Input.Keys.S;
            case "T": return Input.Keys.T;
            case "U": return Input.Keys.U;
            case "V": return Input.Keys.V;
            case "W": return Input.Keys.W;
            case "X": return Input.Keys.X;
            case "Y": return Input.Keys.Y;
            case "Z": return Input.Keys.Z;
            case "SPACE": return Input.Keys.SPACE;
            case "ENTER": return Input.Keys.ENTER;
            case "ESCAPE": return Input.Keys.ESCAPE;
            case "SHIFT": return Input.Keys.SHIFT_LEFT;
            case "CTRL": return Input.Keys.CONTROL_LEFT;
            case "ALT": return Input.Keys.ALT_LEFT;
            case "UP": return Input.Keys.UP;
            case "DOWN": return Input.Keys.DOWN;
            case "LEFT": return Input.Keys.LEFT;
            case "RIGHT": return Input.Keys.RIGHT;
            default:
                GameApp.log("Unknown key name: " + keyName + ", using default");
                return Input.Keys.UNKNOWN;
        }
    }

    public static void saveConfig(GameConfig cfg) {
        if (cfg == null) {
            return;
        }

        try {
            cfg.validate();

            FileWriter fw = new FileWriter(FILE_NAME);
            BufferedWriter bw = new BufferedWriter(fw);

            // Write JSON format
            bw.write("{\n");
            bw.write("  \"masterVolume\": " + cfg.masterVolume + ",\n");
            bw.write("  \"musicVolume\": " + cfg.musicVolume + ",\n");
            bw.write("  \"sfxVolume\": " + cfg.sfxVolume + ",\n");
            bw.write("  \"keyMoveUp\": " + cfg.keyMoveUp + ",\n");
            bw.write("  \"keyMoveDown\": " + cfg.keyMoveDown + ",\n");
            bw.write("  \"keyMoveLeft\": " + cfg.keyMoveLeft + ",\n");
            bw.write("  \"keyMoveRight\": " + cfg.keyMoveRight + ",\n");
            bw.write("  \"keyShoot\": " + cfg.keyShoot + ",\n");
            bw.write("  \"debugEnabled\": " + cfg.debugEnabled + ",\n");
            bw.write("  \"fullscreen\": " + cfg.fullscreen + "\n");
            bw.write("}");

            bw.flush();
            bw.close();
            fw.close();

            GameApp.log("Config saved to " + FILE_NAME);

        } catch (IOException e) {
            GameApp.log("Error saving config: " + e.getMessage());
        }
    }
}
