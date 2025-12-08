package nl.saxion.game.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.saxion.gameapp.GameApp;

import static nl.saxion.game.utils.TMXMapObjects.PolygonObject;
import static nl.saxion.game.utils.TMXMapObjects.RectangleObject;

public class TMXParser {

    private static final int ROOM_TILES_W = 30;
    private static final int ROOM_TILES_H = 20;

    public static TMXMapData loadFromTMX(String tmxPath) {
        try {
            InputStream is = TMXParser.class.getClassLoader().getResourceAsStream(tmxPath);
            if (is == null) {
                GameApp.log("Cannot find TMX file: " + tmxPath);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            String xml = content.toString();

            // Parse map attributes
            int mapWidth = extractIntAttribute(xml, "<map", "width", 30);
            int mapHeight = extractIntAttribute(xml, "<map", "height", 20);
            int tileWidth = extractIntAttribute(xml, "<map", "tilewidth", 32);
            int tileHeight = extractIntAttribute(xml, "<map", "tileheight", 32);

            TMXMapData mapData = new TMXMapData(mapWidth, mapHeight, tileWidth, tileHeight);

            // Parse wall layer (layer name="wall")
            parseWallLayer(xml, mapData);

            // Parse wall object group (objectgroup name="wall")
            parseObjectGroup(xml, mapData, "wall", true);

            // Parse objects object group (objectgroup name="objects")
            parseObjectGroup(xml, mapData, "objects", false);

            GameApp.log("TMX loaded: " + mapWidth + "x" + mapHeight +
                    ", tiles: " + tileWidth + "x" + tileHeight);
            GameApp.log("Wall polygons: " + mapData.getWallPolygons().size());
            GameApp.log("Wall rectangles: " + mapData.getWallRectangles().size());
            GameApp.log("Object polygons: " + mapData.getObjectPolygons().size());
            GameApp.log("Object rectangles: " + mapData.getObjectRectangles().size());

            return mapData;

        } catch (Exception e) {
            GameApp.log("Error loading TMX file: " + tmxPath + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static int extractIntAttribute(String xml, String tag, String attr, int defaultValue) {
        Pattern pattern = Pattern.compile(tag + "[^>]*" + attr + "=\"(\\d+)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static float extractFloatAttribute(String xml, String attr, float defaultValue) {
        Pattern pattern = Pattern.compile(attr + "=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            try {
                return Float.parseFloat(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static void parseWallLayer(String xml, TMXMapData mapData) {
        Pattern layerPattern = Pattern.compile(
                "<layer[^>]*name=\"wall\"[^>]*>\\s*<data[^>]*encoding=\"csv\"[^>]*>\\s*(.*?)\\s*</data>",
                Pattern.DOTALL
        );
        Matcher layerMatcher = layerPattern.matcher(xml);

        if (!layerMatcher.find()) {
            GameApp.log("Warning: Could not find wall layer in TMX");
            return;
        }

        String csvData = layerMatcher.group(1).trim();
        // Split by newlines, but handle cases where data might be all on one line with commas
        String[] lines = csvData.split("[\r\n]+");

        // If we only got one line, try splitting by pattern of 30 values
        if (lines.length == 1) {
            // Split into chunks of 30 values
            String[] allValues = csvData.split(",");
            int totalRows = (allValues.length + ROOM_TILES_W - 1) / ROOM_TILES_W;
            lines = new String[totalRows];
            for (int r = 0; r < totalRows; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < ROOM_TILES_W && (r * ROOM_TILES_W + c) < allValues.length; c++) {
                    if (c > 0) sb.append(",");
                    sb.append(allValues[r * ROOM_TILES_W + c].trim());
                }
                lines[r] = sb.toString();
            }
        }

        // Parse CSV lines
        for (int row = 0; row < ROOM_TILES_H && row < lines.length; row++) {
            String line = lines[row].trim();
            if (line.isEmpty() || line.equals(",")) {
                continue;
            }

            // Remove trailing comma if exists
            if (line.endsWith(",")) {
                line = line.substring(0, line.length() - 1);
            }

            String[] values = line.split(",");
            // Flip row: TMX row 0 (top) → GameApp row 19 (top)
            int flippedRow = ROOM_TILES_H - 1 - row;

            for (int col = 0; col < ROOM_TILES_W && col < values.length; col++) {
                try {
                    String value = values[col].trim();
                    if (value.isEmpty()) {
                        mapData.setWallTile(flippedRow, col, false);
                        continue;
                    }
                    // Parse GID (Global Tile ID)
                    // Tiled format: 0 = empty, >0 = tile exists
                    long gid = Long.parseLong(value);
                    // Extract base GID (remove flip flags: mask with 0x0FFFFFFF)
                    long baseGid = gid & 0x0FFFFFFF;
                    mapData.setWallTile(flippedRow, col, baseGid != 0);
                } catch (NumberFormatException e) {
                    mapData.setWallTile(flippedRow, col, false);
                }
            }
        }

        GameApp.log("Parsed wall layer: " + ROOM_TILES_W + "x" + ROOM_TILES_H + " tiles");

        // DEBUG: Print wallTileMap pattern to verify flip correctness
        GameApp.log("\n=== WALL TILE MAP PATTERN ===");
        boolean[][] wallTileMap = mapData.getWallTileMap();
        StringBuilder patternBuilder = new StringBuilder();
        for (int r = 0; r < ROOM_TILES_H; r++) {
            patternBuilder.append("Row ").append(r).append(": ");
            for (int c = 0; c < ROOM_TILES_W; c++) {
                patternBuilder.append(wallTileMap[r][c] ? "1" : "0");
            }
            patternBuilder.append("\n");
        }
        GameApp.log(patternBuilder.toString());
        GameApp.log("=== END WALL TILE MAP ===\n");
    }

    private static void parseObjectGroup(String xml, TMXMapData mapData, String groupName, boolean isWallGroup) {
        Pattern objectGroupPattern = Pattern.compile(
                "<objectgroup[^>]*name=\"" + groupName + "\"[^>]*>(.*?)</objectgroup>",
                Pattern.DOTALL
        );
        Matcher objectGroupMatcher = objectGroupPattern.matcher(xml);

        if (!objectGroupMatcher.find()) {
            GameApp.log("Info: No '" + groupName + "' object group found in TMX");
            return;
        }

        String objectGroupXml = objectGroupMatcher.group(1);

        Pattern selfClosingPattern = Pattern.compile(
                "<object[^>]*/>",
                Pattern.DOTALL
        );
        Matcher selfClosingMatcher = selfClosingPattern.matcher(objectGroupXml);

        while (selfClosingMatcher.find()) {
            try {
                String objectTag = selfClosingMatcher.group(0);

                // Extract các attributes từ object tag
                float objX = extractFloatAttribute(objectTag, "x", 0f);
                float objY = extractFloatAttribute(objectTag, "y", 0f);
                float width = extractFloatAttribute(objectTag, "width", -1f);
                float height = extractFloatAttribute(objectTag, "height", -1f);

                // Self-closing tags thường là rectangles
                if (width > 0 && height > 0) {
                    RectangleObject rect = new RectangleObject(objX, objY, width, height);
                    if (isWallGroup) {
                        mapData.addWallRectangle(rect);
                    } else {
                        mapData.addObjectRectangle(rect);
                        GameApp.log("Parsed object rectangle [self-closing]: x=" + objX + " y=" + objY + " w=" + width + " h=" + height);
                    }
                }
            } catch (Exception e) {
                GameApp.log("Error parsing self-closing object in group '" + groupName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        Pattern objectWithContentPattern = Pattern.compile(
                "<object[^>]*>(.*?)</object>",
                Pattern.DOTALL
        );
        Matcher contentMatcher = objectWithContentPattern.matcher(objectGroupXml);

        while (contentMatcher.find()) {
            try {
                String fullMatch = contentMatcher.group(0);
                String objectContent = contentMatcher.group(1);

                // Extract attributes từ opening tag
                int openTagEnd = fullMatch.indexOf('>');
                String objectTag = fullMatch.substring(0, openTagEnd + 1);

                float objX = extractFloatAttribute(objectTag, "x", 0f);
                float objY = extractFloatAttribute(objectTag, "y", 0f);

                // Check if it's a polygon
                Pattern polygonPattern = Pattern.compile(
                        "<polygon[^>]*points=\"([^\"]+)\"",
                        Pattern.DOTALL
                );
                Matcher polygonMatcher = polygonPattern.matcher(objectContent);

                if (polygonMatcher.find()) {
                    String pointsStr = polygonMatcher.group(1);
                    List<float[]> points = parsePolygonPoints(pointsStr);
                    if (!points.isEmpty()) {
                        PolygonObject poly = new PolygonObject(objX, objY, points);
                        if (isWallGroup) {
                            mapData.addWallPolygon(poly);
                        } else {
                            mapData.addObjectPolygon(poly);
                        }
                    }
                }
            } catch (Exception e) {
                GameApp.log("Error parsing object with content in group '" + groupName + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static List<float[]> parsePolygonPoints(String pointsStr) {
        List<float[]> points = new ArrayList<>();
        String[] pointStrings = pointsStr.trim().split("\\s+");

        for (String pointStr : pointStrings) {
            String[] coords = pointStr.split(",");
            if (coords.length == 2) {
                try {
                    float x = Float.parseFloat(coords[0].trim());
                    float y = Float.parseFloat(coords[1].trim());
                    points.add(new float[]{x, y});
                } catch (NumberFormatException e) {
                }
            }
        }

        return points;
    }
}
