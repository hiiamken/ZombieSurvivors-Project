package nl.saxion.game.config;

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

            String line = br.readLine();

            while (line != null) {
                String trimmed = line.trim();


                if (trimmed.length() > 0 && !trimmed.startsWith("#")) {

                    int eqIndex = trimmed.indexOf('=');

                    if (eqIndex > 0) {
                        String key = trimmed.substring(0, eqIndex).trim();
                        String value = trimmed.substring(eqIndex + 1).trim();

                        applyConfigLine(cfg, key, value);
                    }
                }

                line = br.readLine();
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

    private static void applyConfigLine(GameConfig cfg, String key, String value) {
        try {
            if (key.equals("masterVolume")) {
                cfg.masterVolume = Float.parseFloat(value);
            } else if (key.equals("musicVolume")) {
                cfg.musicVolume = Float.parseFloat(value);
            } else if (key.equals("sfxVolume")) {
                cfg.sfxVolume = Float.parseFloat(value);
            } else if (key.equals("keyMoveUp")) {
                cfg.keyMoveUp = Integer.parseInt(value);
            } else if (key.equals("keyMoveDown")) {
                cfg.keyMoveDown = Integer.parseInt(value);
            } else if (key.equals("keyMoveLeft")) {
                cfg.keyMoveLeft = Integer.parseInt(value);
            } else if (key.equals("keyMoveRight")) {
                cfg.keyMoveRight = Integer.parseInt(value);
            } else if (key.equals("keyShoot")) {
                cfg.keyShoot = Integer.parseInt(value);
            }
        } catch (NumberFormatException ex) {

            GameApp.log("Invalid value in config for " + key + ": " + value);
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


            bw.write("# Simple game config file");
            bw.newLine();

            bw.write("masterVolume=" + cfg.masterVolume);
            bw.newLine();
            bw.write("musicVolume=" + cfg.musicVolume);
            bw.newLine();
            bw.write("sfxVolume=" + cfg.sfxVolume);
            bw.newLine();

            bw.write("keyMoveUp=" + cfg.keyMoveUp);
            bw.newLine();
            bw.write("keyMoveDown=" + cfg.keyMoveDown);
            bw.newLine();
            bw.write("keyMoveLeft=" + cfg.keyMoveLeft);
            bw.newLine();
            bw.write("keyMoveRight=" + cfg.keyMoveRight);
            bw.newLine();
            bw.write("keyShoot=" + cfg.keyShoot);
            bw.newLine();

            bw.flush();
            bw.close();
            fw.close();

            GameApp.log("Config saved to " + FILE_NAME);

        } catch (IOException e) {
            GameApp.log("Error saving config: " + e.getMessage());
        }
    }
}
