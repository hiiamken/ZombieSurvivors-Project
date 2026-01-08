package nl.saxion.game.screens;

import nl.saxion.game.config.ConfigManager;
import nl.saxion.game.config.GameConfig;
import nl.saxion.game.core.LeaderboardEntry;
import nl.saxion.game.core.PlayerData;
import nl.saxion.game.systems.LeaderboardManager;
import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.game.utils.DebugLogger;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for displaying the leaderboard/ranking.
 * Shows top players sorted by score (and survival time as tiebreaker).
 * Supports pagination with 8 entries per page.
 */
public class RanksScreen extends ScalableGameScreen {

    // UI elements
    private List<Button> buttons;
    private boolean resourcesLoaded = false;
    private SoundManager soundManager;
    
    // Button dimensions
    private float buttonWidth;
    private float buttonHeight;
    
    // Delay for button press animation
    private float pressDelay = 0.3f;
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;
    
    // Cursor management
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringButton = false;
    
    // Leaderboard data
    private List<LeaderboardEntry> entries;
    private static final int ENTRIES_PER_PAGE = 8;
    
    // Pagination
    private int currentPage = 0;
    
    // Animation
    private float animTimer = 0f;
    
    // Highlight current player's entry
    private int currentPlayerEntryIndex = -1;
    
    // Navigation button bounds (for click detection)
    private float prevButtonX, prevButtonY, prevButtonW, prevButtonH;
    private float nextButtonX, nextButtonY, nextButtonW, nextButtonH;
    
    public RanksScreen() {
        super(1280, 720);
    }
    
    @Override
    public void show() {
        DebugLogger.log("RanksScreen.show() called");
        
        loadCursors();
        
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        GameConfig config = ConfigManager.loadConfig();
        soundManager.setMasterVolume(config.masterVolume);
        soundManager.setMusicVolume(config.musicVolume);
        soundManager.setSFXVolume(config.sfxVolume);
        
        if (soundManager != null) {
            soundManager.playMusic(true);
        }
        
        loadResources();
        createButtons();
        loadLeaderboardData();
        findCurrentPlayerEntry();
        
        // Set page to show current player if applicable
        if (currentPlayerEntryIndex >= 0) {
            currentPage = currentPlayerEntryIndex / ENTRIES_PER_PAGE;
        } else {
            currentPage = 0;
        }
        
        animTimer = 0f;
        
        DebugLogger.log("RanksScreen initialized with " + (entries != null ? entries.size() : 0) + " entries");
    }
    
    private void loadCursors() {
        try {
            String pointerPath = "assets/ui/pointer.png";
            Pixmap pointerSource = new Pixmap(Gdx.files.internal(pointerPath));
            int targetSize = 32;
            Pixmap pointerPixmap = new Pixmap(targetSize, targetSize, pointerSource.getFormat());
            pointerPixmap.drawPixmap(pointerSource,
                    0, 0, pointerSource.getWidth(), pointerSource.getHeight(),
                    0, 0, targetSize, targetSize);
            cursorPointer = Gdx.graphics.newCursor(pointerPixmap, 0, 0);
            pointerPixmap.dispose();
            pointerSource.dispose();
            
            String cursorPath = "assets/ui/cursor.png";
            Pixmap cursorSource = new Pixmap(Gdx.files.internal(cursorPath));
            Pixmap cursorPixmap = new Pixmap(targetSize, targetSize, cursorSource.getFormat());
            cursorPixmap.drawPixmap(cursorSource,
                    0, 0, cursorSource.getWidth(), cursorSource.getHeight(),
                    0, 0, targetSize, targetSize);
            cursorHover = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
            cursorPixmap.dispose();
            cursorSource.dispose();
            
            if (cursorPointer != null) {
                Gdx.graphics.setCursor(cursorPointer);
            } else {
                GameApp.showCursor();
            }
        } catch (Exception e) {
            GameApp.log("Could not load cursors: " + e.getMessage());
            GameApp.showCursor();
        }
    }
    
    private void loadResources() {
        if (resourcesLoaded) return;
        
        // Title
        if (!GameApp.hasFont("ranksTitle")) {
            GameApp.addStyledFont("ranksTitle", "fonts/upheavtt.ttf", 52,
                    "yellow-400", 0f, "black", 3, 3, "gray-700", true);
        }
        // Subtitle (players count)
        if (!GameApp.hasFont("ranksSubtitle")) {
            GameApp.addStyledFont("ranksSubtitle", "fonts/PixelOperatorMono-Bold.ttf", 17,
                    "gray-300", 0f, "black", 1, 1, "gray-700", true);
        }
        // Page info font
        if (!GameApp.hasFont("ranksPageInfo")) {
            GameApp.addStyledFont("ranksPageInfo", "fonts/PixelOperatorMono-Bold.ttf", 18,
                    "gray-300", 0f, "black", 1, 1, "gray-700", true);
        }
        // Header
        if (!GameApp.hasFont("ranksHeader")) {
            GameApp.addStyledFont("ranksHeader", "fonts/PressStart2P-Regular.ttf", 14,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        // Entry data
        if (!GameApp.hasFont("ranksEntry")) {
            GameApp.addStyledFont("ranksEntry", "fonts/PressStart2P-Regular.ttf", 18,
                    "white", 0f, "black", 1, 1, "gray-700", true);
        }
        // Rank number
        if (!GameApp.hasFont("ranksRank")) {
            GameApp.addStyledFont("ranksRank", "fonts/upheavtt.ttf", 28,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }
        // Empty state
        if (!GameApp.hasFont("ranksEmpty")) {
            GameApp.addStyledFont("ranksEmpty", "fonts/PixelOperatorMono-Bold.ttf", 16,
                    "gray-400", 0f, "black", 1, 1, "gray-700", true);
        }
        // Button font
        if (!GameApp.hasFont("buttonFont")) {
            GameApp.addStyledFont("buttonFont", "fonts/upheavtt.ttf", 40,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }
        // Page button font (smaller)
        if (!GameApp.hasFont("pageButtonFont")) {
            GameApp.addStyledFont("pageButtonFont", "fonts/upheavtt.ttf", 28,
                    "white", 0f, "black", 2, 2, "gray-700", true);
        }
        
        // Colors
        if (!GameApp.hasColor("button_red_text")) {
            GameApp.addColor("button_red_text", 60, 15, 30);
        }
        if (!GameApp.hasColor("rank_gold")) {
            GameApp.addColor("rank_gold", 255, 200, 50);
        }
        if (!GameApp.hasColor("rank_silver")) {
            GameApp.addColor("rank_silver", 180, 180, 190);
        }
        if (!GameApp.hasColor("rank_bronze")) {
            GameApp.addColor("rank_bronze", 200, 130, 60);
        }
        if (!GameApp.hasColor("current_player")) {
            GameApp.addColor("current_player", 100, 180, 255);
        }
        if (!GameApp.hasColor("button_blue_text")) {
            GameApp.addColor("button_blue_text", 20, 40, 80);
        }
        
        // Textures
        if (!GameApp.hasTexture("red_long")) {
            GameApp.addTexture("red_long", "assets/ui/red_long.png");
        }
        if (!GameApp.hasTexture("red_pressed_long")) {
            GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        }
        if (!GameApp.hasTexture("blue_long")) {
            GameApp.addTexture("blue_long", "assets/ui/blue_long.png");
        }
        if (!GameApp.hasTexture("blue_pressed_long")) {
            GameApp.addTexture("blue_pressed_long", "assets/ui/blue_pressed_long.png");
        }
        if (!GameApp.hasTexture("mainmenu_bg")) {
            GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        }
        
        resourcesLoaded = true;
    }
    
    private void createButtons() {
        buttons = new ArrayList<>();
        
        int texW = GameApp.getTextureWidth("red_long");
        int texH = GameApp.getTextureHeight("red_long");
        float scale = 0.7f;
        
        buttonWidth = texW * scale;
        buttonHeight = texH * scale;
        
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        float buttonY = 20f;
        Button backButton = new Button(centerX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight, "");
        backButton.setOnClick(() -> {});
        if (GameApp.hasTexture("red_long")) {
            backButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        }
        buttons.add(backButton);
    }
    
    private void loadLeaderboardData() {
        try {
            LeaderboardManager.reload();
            entries = LeaderboardManager.getEntries();
            if (entries == null) {
                entries = new ArrayList<>();
            }
        } catch (Exception e) {
            GameApp.log("Error loading leaderboard: " + e.getMessage());
            entries = new ArrayList<>();
        }
    }
    
    private void findCurrentPlayerEntry() {
        currentPlayerEntryIndex = -1;
        
        PlayerData currentPlayer = PlayerData.getCurrentPlayer();
        if (currentPlayer == null || entries == null || entries.isEmpty()) {
            return;
        }
        
        long latestTimestamp = 0;
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            if (entry.getUsername().equals(currentPlayer.getUsername()) &&
                entry.getStudentClass().equals(currentPlayer.getStudentClass()) &&
                entry.getGroupNumber().equals(currentPlayer.getGroupNumber())) {
                if (entry.getTimestamp() > latestTimestamp) {
                    latestTimestamp = entry.getTimestamp();
                    currentPlayerEntryIndex = i;
                }
            }
        }
    }
    
    private int getTotalPages() {
        if (entries == null || entries.isEmpty()) return 1;
        return (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE);
    }
    
    private boolean hasPreviousPage() {
        return currentPage > 0;
    }
    
    private boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }
    
    @Override
    public void hide() {
        if (soundManager != null) {
            soundManager.stopMusic();
        }
        if (cursorPointer != null) {
            cursorPointer.dispose();
            cursorPointer = null;
        }
        if (cursorHover != null) {
            cursorHover.dispose();
            cursorHover = null;
        }
    }
    
    @Override
    public void render(float delta) {
        super.render(delta);
        
        animTimer += delta;
        
        if (GameApp.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (soundManager != null) {
                soundManager.playSound("clickbutton", 2.5f);
            }
            GameApp.switchScreen("menu");
            return;
        }
        
        // Handle keyboard pagination
        handleKeyboardPagination();
        
        GameApp.clearScreen("black");
        
        if (pendingAction != null && pressedButton != null) {
            pressTimer += delta;
            if (pressTimer >= pressDelay) {
                Runnable action = pendingAction;
                pendingAction = null;
                pressedButton = null;
                pressTimer = 0f;
                action.run();
            }
        }
        
        if (pendingAction == null) {
            handleButtonInput();
        } else if (pressedButton != null) {
            pressedButton.setPressed(true);
        }
        
        drawBackground();
        drawLeaderboard();
        
        for (Button button : buttons) {
            button.render();
        }
        
        drawButtonText();
    }
    
    private void handleKeyboardPagination() {
        if (entries == null || entries.isEmpty()) return;
        
        // Left/Right arrows or A/D for page navigation
        if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) {
            if (hasPreviousPage()) {
                currentPage--;
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.5f);
                }
            }
        }
        if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) {
            if (hasNextPage()) {
                currentPage++;
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 0.5f);
                }
            }
        }
        
        // Home/End for first/last page
        if (GameApp.isKeyJustPressed(Input.Keys.HOME)) {
            currentPage = 0;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.END)) {
            currentPage = getTotalPages() - 1;
        }
    }
    
    private void drawBackground() {
        if (!GameApp.hasTexture("mainmenu_bg")) return;
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        
        int texWidth = GameApp.getTextureWidth("mainmenu_bg");
        int texHeight = GameApp.getTextureHeight("mainmenu_bg");
        
        float bgWidth = screenWidth;
        float bgHeight = screenHeight;
        
        if (texWidth > 0 && texHeight > 0) {
            float screenAspect = screenWidth / screenHeight;
            float texAspect = (float) texWidth / texHeight;
            
            if (screenAspect > texAspect) {
                bgWidth = screenWidth;
                bgHeight = bgWidth / texAspect;
            } else {
                bgHeight = screenHeight;
                bgWidth = bgHeight * texAspect;
            }
        }
        
        float bgX = (screenWidth - bgWidth) / 2f;
        float bgY = (screenHeight - bgHeight) / 2f;
        
        GameApp.startSpriteRendering();
        GameApp.drawTexture("mainmenu_bg", bgX, bgY, bgWidth, bgHeight);
        GameApp.endSpriteRendering();
    }
    
    private void drawLeaderboard() {
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        
        // Layout constants
        float panelWidth = 1000f;
        float panelHeight = 600f;
        float panelX = centerX - panelWidth / 2;
        float panelY = 85f; // Above BACK button
        
        // Panel background
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(20, 20, 35, 230);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Panel border
        GameApp.startShapeRenderingOutlined();
        GameApp.setLineWidth(2f);
        GameApp.setColor(70, 70, 120, 255);
        GameApp.drawRect(panelX, panelY, panelWidth, panelHeight);
        GameApp.endShapeRendering();
        
        // Title area
        GameApp.startSpriteRendering();
        float titleY = panelY + panelHeight - 45f;
        GameApp.drawTextCentered("ranksTitle", "LEADERBOARD", centerX, titleY, "yellow-400");
        
        // Subtitle with total players
        int totalPlayers = entries != null ? entries.size() : 0;
        String subtitle = totalPlayers + " player" + (totalPlayers != 1 ? "s" : "") + " on the board";
        GameApp.drawTextCentered("ranksSubtitle", subtitle, centerX, titleY - 28f, "gray-400");
        GameApp.endSpriteRendering();
        
        // Column layout
        float tableStartX = panelX + 40f;
        float tableWidth = panelWidth - 80f;
        
        float colWidthRank = 90f;
        float colWidthName = 280f;
        float colWidthClass = 160f;
        float colWidthGroup = 130f;
        float colWidthScore = 140f;
        float colWidthTime = 140f;
        
        float colRankX = tableStartX + colWidthRank / 2;
        float colNameX = tableStartX + colWidthRank + colWidthName / 2;
        float colClassX = tableStartX + colWidthRank + colWidthName + colWidthClass / 2;
        float colGroupX = tableStartX + colWidthRank + colWidthName + colWidthClass + colWidthGroup / 2;
        float colScoreX = tableStartX + colWidthRank + colWidthName + colWidthClass + colWidthGroup + colWidthScore / 2;
        float colTimeX = tableStartX + colWidthRank + colWidthName + colWidthClass + colWidthGroup + colWidthScore + colWidthTime / 2;
        
        // Header row
        float headerY = titleY - 70f;
        
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("ranksHeader", "RANK", colRankX, headerY, "white");
        GameApp.drawTextCentered("ranksHeader", "PLAYER", colNameX, headerY, "white");
        GameApp.drawTextCentered("ranksHeader", "CLASS", colClassX, headerY, "white");
        GameApp.drawTextCentered("ranksHeader", "GROUP", colGroupX, headerY, "white");
        GameApp.drawTextCentered("ranksHeader", "SCORE", colScoreX, headerY, "white");
        GameApp.drawTextCentered("ranksHeader", "TIME", colTimeX, headerY, "white");
        GameApp.endSpriteRendering();
        
        // Header separator
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(100, 100, 150, 220);
        GameApp.drawRect(tableStartX, headerY - 20f, tableWidth, 3f);
        GameApp.endShapeRendering();
        
        // Calculate table area
        float rowStartY = headerY - 50f;
        float rowHeight = 43f;
        
        if (entries == null || entries.isEmpty()) {
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("ranksEmpty", "NO SCORES YET", centerX, rowStartY - 100f, "gray-500");
            GameApp.drawTextCentered("ranksSubtitle", "Play the game to get on the leaderboard!", centerX, rowStartY - 135f, "gray-600");
            GameApp.endSpriteRendering();
        } else {
            int startIndex = currentPage * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, entries.size());
            int displayCount = endIndex - startIndex;
            
            for (int i = 0; i < displayCount; i++) {
                int entryIndex = startIndex + i;
                if (entryIndex >= entries.size()) break;
                
                LeaderboardEntry entry = entries.get(entryIndex);
                float rowY = rowStartY - i * rowHeight;
                int rank = entryIndex + 1;
                boolean isCurrentPlayer = (entryIndex == currentPlayerEntryIndex);
                
                // Row background
                GameApp.startShapeRenderingFilled();
                
                if (isCurrentPlayer) {
                    float pulse = (float) (0.08f + 0.04f * Math.sin(animTimer * 3));
                    GameApp.setColor(40, 80, 140, (int)(pulse * 255));
                } else if (rank == 1) {
                    GameApp.setColor(80, 65, 20, 60);
                } else if (rank == 2) {
                    GameApp.setColor(60, 60, 70, 50);
                } else if (rank == 3) {
                    GameApp.setColor(70, 50, 30, 50);
                } else {
                    if (i % 2 == 0) {
                        GameApp.setColor(35, 35, 55, 120);
                    } else {
                        GameApp.setColor(25, 25, 40, 120);
                    }
                }
                GameApp.drawRect(tableStartX, rowY - rowHeight + 8f, tableWidth, rowHeight - 2f);
                GameApp.endShapeRendering();
                
                // Left border for current player
                if (isCurrentPlayer) {
                    GameApp.startShapeRenderingFilled();
                    GameApp.setColor(100, 180, 255, 220);
                    GameApp.drawRect(tableStartX, rowY - rowHeight + 8f, 5f, rowHeight - 2f);
                    GameApp.endShapeRendering();
                }
                
                // Rank color
                String rankColor;
                if (rank == 1) {
                    rankColor = "rank_gold";
                } else if (rank == 2) {
                    rankColor = "rank_silver";
                } else if (rank == 3) {
                    rankColor = "rank_bronze";
                } else {
                    rankColor = "white";
                }
                
                String textColor = isCurrentPlayer ? "current_player" : "white";
                
                GameApp.startSpriteRendering();
                
                float textCenterY = rowY - rowHeight / 2 + 2f;
                
                // Rank
                GameApp.drawTextCentered("ranksRank", String.valueOf(rank), colRankX, textCenterY, rankColor);
                
                // Player name
                String displayName = entry.getUsername();
                if (displayName.length() > 16) {
                    displayName = displayName.substring(0, 13) + "...";
                }
                if (isCurrentPlayer) {
                    displayName = displayName + " *";
                }
                GameApp.drawTextCentered("ranksEntry", displayName, colNameX, textCenterY, textColor);
                
                // Class & Group
                GameApp.drawTextCentered("ranksEntry", entry.getStudentClass(), colClassX, textCenterY, "gray-300");
                GameApp.drawTextCentered("ranksEntry", entry.getGroupNumber(), colGroupX, textCenterY, "gray-300");
                
                // Score & Time
                GameApp.drawTextCentered("ranksEntry", String.valueOf(entry.getScore()), colScoreX, textCenterY, "yellow-300");
                GameApp.drawTextCentered("ranksEntry", entry.getFormattedSurvivalTime(), colTimeX, textCenterY, "green-300");
                
                GameApp.endSpriteRendering();
            }
        }
        
        // Draw pagination controls in dedicated area at bottom of panel
        drawPaginationControls(panelX, panelY, panelWidth, tableStartX, tableWidth, centerX);
    }
    
    private void drawPaginationControls(float panelX, float panelY, float panelWidth, float tableStartX, float tableWidth, float centerX) {
        int totalPages = getTotalPages();
        
        // Pagination area - dedicated space at bottom of panel
        float paginationY = panelY + 10f;
        float paginationHeight = 45f;
        
        // Separator line above pagination
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(60, 60, 100, 150);
        GameApp.drawRect(tableStartX, paginationY + paginationHeight, tableWidth, 2f);
        GameApp.endShapeRendering();
        
        // Only show navigation if multiple pages
        if (totalPages <= 1) {
            // Show single page indicator
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("ranksPageInfo", "Page 1 / 1", centerX, paginationY + paginationHeight / 2 + 5f, "gray-500");
            GameApp.endSpriteRendering();
            return;
        }
        
        float navButtonW = 110f;
        float navButtonH = 36f;
        float navButtonY = paginationY + (paginationHeight - navButtonH) / 2;
        
        // Previous button (left side)
        prevButtonX = tableStartX + 10f;
        prevButtonY = navButtonY;
        prevButtonW = navButtonW;
        prevButtonH = navButtonH;
        
        // Next button (right side)
        nextButtonX = tableStartX + tableWidth - navButtonW - 10f;
        nextButtonY = navButtonY;
        nextButtonW = navButtonW;
        nextButtonH = navButtonH;
        
        // Draw Previous button if has previous page
        if (hasPreviousPage()) {
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(50, 70, 120, 220);
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 100, 160, 255);
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "< PREV", prevButtonX + prevButtonW / 2, prevButtonY + prevButtonH / 2 + 3f, "white");
            GameApp.endSpriteRendering();
        } else {
            // Disabled state
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(30, 30, 50, 100);
            GameApp.drawRect(prevButtonX, prevButtonY, prevButtonW, prevButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "< PREV", prevButtonX + prevButtonW / 2, prevButtonY + prevButtonH / 2 + 3f, "gray-600");
            GameApp.endSpriteRendering();
        }
        
        // Draw Next button if has next page
        if (hasNextPage()) {
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(50, 70, 120, 220);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startShapeRenderingOutlined();
            GameApp.setLineWidth(2f);
            GameApp.setColor(80, 100, 160, 255);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "NEXT >", nextButtonX + nextButtonW / 2, nextButtonY + nextButtonH / 2 + 3f, "white");
            GameApp.endSpriteRendering();
        } else {
            // Disabled state
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(30, 30, 50, 100);
            GameApp.drawRect(nextButtonX, nextButtonY, nextButtonW, nextButtonH);
            GameApp.endShapeRendering();
            
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("pageButtonFont", "NEXT >", nextButtonX + nextButtonW / 2, nextButtonY + nextButtonH / 2 + 3f, "gray-600");
            GameApp.endSpriteRendering();
        }
        
        // Page indicator in center
        GameApp.startSpriteRendering();
        String pageInfo = "Page " + (currentPage + 1) + " / " + totalPages;
        GameApp.drawTextCentered("ranksPageInfo", pageInfo, centerX, navButtonY + navButtonH / 2 + 3f, "gray-300");
        GameApp.endSpriteRendering();
    }
    
    private void drawButtonText() {
        GameApp.startSpriteRendering();
        
        for (Button button : buttons) {
            float buttonCenterX = button.getX() + button.getWidth() / 2;
            float buttonCenterY = button.getY() + button.getHeight() / 2;
            
            float textHeight = GameApp.getTextHeight("buttonFont", "BACK");
            float adjustedY = buttonCenterY + textHeight * 0.15f;
            
            GameApp.drawTextCentered("buttonFont", "BACK", buttonCenterX, adjustedY, "button_red_text");
        }
        
        GameApp.endSpriteRendering();
    }
    
    private void handleButtonInput() {
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        // Check if hovering any button (including pagination buttons)
        boolean hoveringAnyButton = false;
        for (Button button : buttons) {
            if (button.containsPoint(worldMouseX, worldMouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }
        
        // Check pagination buttons hover
        boolean hoveringPrev = hasPreviousPage() && 
            worldMouseX >= prevButtonX && worldMouseX <= prevButtonX + prevButtonW &&
            worldMouseY >= prevButtonY && worldMouseY <= prevButtonY + prevButtonH;
        boolean hoveringNext = hasNextPage() && 
            worldMouseX >= nextButtonX && worldMouseX <= nextButtonX + nextButtonW &&
            worldMouseY >= nextButtonY && worldMouseY <= nextButtonY + nextButtonH;
        
        if (hoveringPrev || hoveringNext) {
            hoveringAnyButton = true;
        }
        
        // Update cursor
        if (cursorPointer != null && cursorHover != null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
            
            if (isMouseDown || isMouseJustPressed) {
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringButton = false;
            } else if (hoveringAnyButton) {
                if (!isHoveringButton) {
                    Gdx.graphics.setCursor(cursorHover);
                    isHoveringButton = true;
                }
            } else {
                if (isHoveringButton) {
                    Gdx.graphics.setCursor(cursorPointer);
                    isHoveringButton = false;
                }
            }
        }
        
        for (Button button : buttons) {
            button.update(worldMouseX, worldMouseY);
        }
        
        boolean isMouseJustPressed = GameApp.isButtonJustPressed(0);
        if (isMouseJustPressed) {
            // Check pagination button clicks first
            if (hoveringPrev) {
                currentPage--;
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 1.0f);
                }
                return;
            }
            if (hoveringNext) {
                currentPage++;
                if (soundManager != null) {
                    soundManager.playSound("clickbutton", 1.0f);
                }
                return;
            }
            
            // Check main button clicks
            for (Button button : buttons) {
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    if (soundManager != null) {
                        soundManager.playSound("clickbutton", 2.5f);
                    }
                    
                    pressedButton = button;
                    button.setPressed(true);
                    
                    pendingAction = () -> GameApp.switchScreen("menu");
                    
                    pressTimer = 0f;
                    break;
                }
            }
        }
        
        if (pendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : buttons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        }
    }
    
    private void toggleFullscreen() {
        GameConfig config = ConfigManager.loadConfig();
        config.fullscreen = !config.fullscreen;
        ConfigManager.saveConfig(config);
        
        if (config.fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
}
