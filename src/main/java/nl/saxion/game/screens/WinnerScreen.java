package nl.saxion.game.screens;

import nl.saxion.game.systems.SoundManager;
import nl.saxion.game.ui.Button;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import java.util.ArrayList;
import java.util.List;

/**
 * Winner Screen - Displayed when player survives the full 10 minutes.
 * Features celebratory design with gold/green victory theme.
 */
public class WinnerScreen extends ScalableGameScreen {
    private static final float FADE_DURATION = 1.5f;
    
    private static int storedScore = 0;
    private static String storedPlayerName = "SURVIVOR";
    private static float storedSurvivalTime = 600f;
    
    public static void setScore(int score) { storedScore = score; }
    public static void setPlayerName(String name) { storedPlayerName = name != null ? name : "SURVIVOR"; }
    public static void setSurvivalTime(float time) { storedSurvivalTime = time; }
    
    private int score = 0;
    private String playerName = "SURVIVOR";
    private float survivalTime = 600f;
    private float fadeTimer = 0f;
    private float celebrationTimer = 0f;
    private float starSpawnTimer = 0f;
    
    private List<Button> winnerButtons;
    private boolean buttonsInitialized = false;
    private boolean resourcesLoaded = false;
    private SoundManager soundManager;
    
    private float pressDelay = 0.15f;
    private float pressTimer = 0f;
    private Runnable pendingAction = null;
    private Button pressedButton = null;
    
    private Cursor cursorPointer;
    private Cursor cursorHover;
    private boolean isHoveringButton = false;
    
    private List<CelebrationStar> celebrationStars = new ArrayList<>();
    
    private static class CelebrationStar {
        float x, y, speed, size, rotation, rotationSpeed, alpha;
        float initialSize; // Store initial size for shrinking effect
        float startY;      // Store start Y for fade calculation
        int colorType;
        CelebrationStar(float x, float y, float speed, float size, float screenHeight) {
            this.x = x; this.y = y; this.speed = speed; 
            this.initialSize = size;
            this.size = size;
            this.startY = y;
            this.rotation = GameApp.random(0f, 360f);
            this.rotationSpeed = GameApp.random(-180f, 180f); // Faster rotation
            this.alpha = 1f; // Start fully visible
            this.colorType = (int) GameApp.random(0, 3);
        }
        
        // Update size and alpha based on fall progress
        void updateFadeAndSize(float screenHeight) {
            // Calculate how far the star has fallen (0 = top, 1 = bottom)
            float fallProgress = 1f - (y / startY);
            fallProgress = Math.max(0f, Math.min(1f, fallProgress));
            
            // Shrink from full size to 20% as it falls
            size = initialSize * (1f - fallProgress * 0.8f);
            
            // Fade from full alpha to 0 as it approaches bottom
            alpha = 1f - (fallProgress * fallProgress); // Quadratic fade for smoother effect
            alpha = Math.max(0f, alpha);
        }
    }
    
    public WinnerScreen() {
        super(1280, 720);
        winnerButtons = new ArrayList<>();
    }
    
    @Override
    public void show() {
        score = storedScore;
        playerName = storedPlayerName;
        survivalTime = storedSurvivalTime;
        
        soundManager = new SoundManager();
        soundManager.loadAllSounds();
        
        loadResources();
        initializeButtons();
        loadCursors();
        initializeCelebrationStars();
        
        fadeTimer = 0f;
        celebrationTimer = 0f;
        
        if (soundManager != null) {
            soundManager.playSound("levelup", 1.5f);
            // Play winner music (loops)
            soundManager.playWinnerMusic(true);
        }
    }
    
    private void loadResources() {
        if (resourcesLoaded) return;
        
        GameApp.addStyledFont("winnerTitle", "fonts/upheavtt.ttf", 72, "yellow-400", 2f, "black", 4, 4, "yellow-700", true);
        GameApp.addFont("winnerText", "fonts/PressStart2P-Regular.ttf", 16, true);
        GameApp.addStyledFont("winnerSubtitle", "fonts/upheavtt.ttf", 32, "green-400", 1f, "black", 2, 2, "green-700", true);
        
        if (!GameApp.hasColor("button_green_text")) GameApp.addColor("button_green_text", 25, 50, 25);
        if (!GameApp.hasColor("button_red_text")) GameApp.addColor("button_red_text", 60, 15, 30);
        if (!GameApp.hasColor("button_blue_text")) GameApp.addColor("button_blue_text", 25, 35, 60);
        if (!GameApp.hasColor("winner_gold")) GameApp.addColor("winner_gold", 255, 215, 0);
        if (!GameApp.hasColor("winner_green")) GameApp.addColor("winner_green", 50, 205, 50);
        
        GameApp.addStyledFont("winnerButtonFont", "fonts/upheavtt.ttf", 28, "white", 0f, "black", 2, 2, "gray-700", true);
        
        if (!GameApp.hasTexture("green_long")) GameApp.addTexture("green_long", "assets/ui/green_long.png");
        if (!GameApp.hasTexture("green_pressed_long")) GameApp.addTexture("green_pressed_long", "assets/ui/green_pressed_long.png");
        if (!GameApp.hasTexture("red_long")) GameApp.addTexture("red_long", "assets/ui/red_long.png");
        if (!GameApp.hasTexture("red_pressed_long")) GameApp.addTexture("red_pressed_long", "assets/ui/red_pressed_long.png");
        if (!GameApp.hasTexture("blue_long")) GameApp.addTexture("blue_long", "assets/ui/blue_long.png");
        if (!GameApp.hasTexture("blue_pressed_long")) GameApp.addTexture("blue_pressed_long", "assets/ui/blue_pressed_long.png");
        if (!GameApp.hasTexture("mainmenu_bg")) GameApp.addTexture("mainmenu_bg", "assets/ui/mainmenu.png");
        if (!GameApp.hasTexture("winner_title")) GameApp.addTexture("winner_title", "assets/ui/winner.png");
        if (!GameApp.hasTexture("star")) GameApp.addTexture("star", "assets/ui/star.png");
        
        resourcesLoaded = true;
    }
    
    private void loadCursors() {
        try {
            Pixmap pointerSource = new Pixmap(Gdx.files.internal("assets/ui/pointer.png"));
            int targetSize = 32;
            Pixmap pointerPixmap = new Pixmap(targetSize, targetSize, pointerSource.getFormat());
            pointerPixmap.drawPixmap(pointerSource, 0, 0, pointerSource.getWidth(), pointerSource.getHeight(), 0, 0, targetSize, targetSize);
            cursorPointer = Gdx.graphics.newCursor(pointerPixmap, 0, 0);
            pointerPixmap.dispose();
            pointerSource.dispose();
            
            Pixmap cursorSource = new Pixmap(Gdx.files.internal("assets/ui/cursor.png"));
            Pixmap cursorPixmap = new Pixmap(targetSize, targetSize, cursorSource.getFormat());
            cursorPixmap.drawPixmap(cursorSource, 0, 0, cursorSource.getWidth(), cursorSource.getHeight(), 0, 0, targetSize, targetSize);
            cursorHover = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
            cursorPixmap.dispose();
            cursorSource.dispose();
            
            if (cursorPointer != null) Gdx.graphics.setCursor(cursorPointer);
            else GameApp.showCursor();
        } catch (Exception e) {
            GameApp.showCursor();
        }
    }
    
    private void initializeCelebrationStars() {
        celebrationStars.clear();
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        // More stars (60 instead of 30) and bigger initial sizes
        for (int i = 0; i < 60; i++) {
            celebrationStars.add(new CelebrationStar(
                GameApp.random(0, screenWidth), 
                GameApp.random(screenHeight, screenHeight + 400),
                GameApp.random(40f, 240f), // Faster falling
                GameApp.random(25f, 95f),  // Bigger stars
                screenHeight));
        }
    }
    
    private void initializeButtons() {
        if (buttonsInitialized) return;
        winnerButtons.clear();
        
        float screenWidth = GameApp.getWorldWidth();
        float centerX = screenWidth / 2;
        float centerY = GameApp.getWorldHeight() / 2;
        
        int texW = GameApp.getTextureWidth("green_long");
        int texH = GameApp.getTextureHeight("green_long");
        float buttonWidth = texW / 1.4f;
        float buttonHeight = texH / 1.4f;
        float buttonSpacing = 12f;
        
        float startY = centerY - 120;
        
        // Play Again button (green)
        Button playAgainButton = new Button(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight, "");
        playAgainButton.setSprites("green_long", "green_long", "green_long", "green_pressed_long");
        winnerButtons.add(playAgainButton);
        
        // Credits button (blue)
        Button creditsButton = new Button(centerX - buttonWidth / 2, startY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight, "");
        creditsButton.setSprites("blue_long", "blue_long", "blue_long", "blue_pressed_long");
        winnerButtons.add(creditsButton);
        
        // Back to Menu button (red)
        Button backButton = new Button(centerX - buttonWidth / 2, startY - (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight, "");
        backButton.setSprites("red_long", "red_long", "red_long", "red_pressed_long");
        winnerButtons.add(backButton);
        
        buttonsInitialized = true;
    }
    
    @Override
    public void hide() {
        if (soundManager != null) soundManager.stopMusic();
        if (cursorPointer != null) { cursorPointer.dispose(); cursorPointer = null; }
        if (cursorHover != null) { cursorHover.dispose(); cursorHover = null; }
    }
    
    @Override
    public void render(float delta) {
        super.render(delta);
        
        if (fadeTimer < FADE_DURATION) fadeTimer += delta;
        celebrationTimer += delta;
        
        starSpawnTimer += delta;
        if (starSpawnTimer > 0.05f) { // Spawn more frequently
            starSpawnTimer = 0f;
            spawnCelebrationStar();
            spawnCelebrationStar(); // Spawn 2 at a time for more density
        }
        updateCelebrationStars(delta);
        
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
        
        if (pendingAction == null) handleInput();
        else if (pressedButton != null) pressedButton.setPressed(true);
        
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        float centerX = screenWidth / 2;
        float centerY = screenHeight / 2;
        float fadeAlpha = Math.min(fadeTimer / FADE_DURATION, 1.0f);
        
        drawBackground(screenWidth, screenHeight);
        drawCelebrationOverlay(screenWidth, screenHeight, fadeAlpha);
        drawCelebrationStars();
        
        for (Button button : winnerButtons) button.render();
        
        GameApp.startSpriteRendering();
        drawTitle(centerX, centerY);
        
        // MOVED TEXT HIGHER to avoid overlapping with buttons
        float textBaseY = centerY + 80; // Much higher position
        
        // Line 1: CONGRATULATION, USERNAME
        String congratsText = "CONGRATULATION, " + playerName + "!";
        GameApp.drawTextCentered("winnerSubtitle", congratsText, centerX, textBaseY, "winner_green");
        
        // Line 2: Final Score
        String scoreText = String.format("Final Score: %,d", score);
        GameApp.drawTextCentered("winnerText", scoreText, centerX, textBaseY - 45, "winner_gold");
        
        // Line 3: Vote message (nice English)
        String voteText = "If you enjoyed our game, please vote for us :). Thank you!";
        GameApp.drawTextCentered("winnerText", voteText, centerX, textBaseY - 85, "white");
        
        drawButtonText(centerX);
        GameApp.endSpriteRendering();
    }
    
    private void spawnCelebrationStar() {
        float screenWidth = GameApp.getWorldWidth();
        float screenHeight = GameApp.getWorldHeight();
        celebrationStars.add(new CelebrationStar(
                GameApp.random(0, screenWidth), 
                screenHeight + GameApp.random(20f, 100f),
                GameApp.random(70f, 160f),  // Faster
                GameApp.random(30f, 60f),   // Bigger
                screenHeight));
    }
    
    private void updateCelebrationStars(float delta) {
        float screenHeight = GameApp.getWorldHeight();
        List<CelebrationStar> toRemove = new ArrayList<>();
        for (CelebrationStar star : celebrationStars) {
            star.y -= star.speed * delta;
            star.rotation += star.rotationSpeed * delta;
            star.x += (float) Math.sin(celebrationTimer * 2 + star.rotation * 0.01f) * 1.5f; // More sway
            
            // Update shrinking and fading
            star.updateFadeAndSize(screenHeight);
            
            // Remove if too small, invisible, or off screen
            if (star.y < -star.size || star.alpha < 0.05f || star.size < 3f) {
                toRemove.add(star);
            }
        }
        celebrationStars.removeAll(toRemove);
    }
    
    private void drawBackground(float width, float height) {
        if (!GameApp.hasTexture("mainmenu_bg")) return;
        GameApp.startSpriteRendering();
        GameApp.drawTexture("mainmenu_bg", 0, 0, width, height);
        GameApp.endSpriteRendering();
    }
    
    private void drawCelebrationOverlay(float width, float height, float alpha) {
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        
        // Golden gradient overlay from top
        int steps = 50;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            float y = height - (height * 0.4f / steps) * (i + 1);
            float rectHeight = height * 0.4f / steps;
            int goldAlpha = (int) (60 * (1 - t) * alpha);
            GameApp.setColor(255, 215, 0, goldAlpha);
            GameApp.drawRect(0, y, width, rectHeight);
        }
        
        // Dark overlay for readability
        GameApp.setColor(0, 0, 0, (int)(120 * alpha));
        GameApp.drawRect(0, 0, width, height);
        
        GameApp.endShapeRendering();
    }
    
    private void drawCelebrationStars() {
        if (!GameApp.hasTexture("star")) return;
        GameApp.enableTransparency();
        GameApp.startSpriteRendering();
        for (CelebrationStar star : celebrationStars) {
            // Draw star (size already reflects fade effect via shrinking)
            // Alpha is simulated through size reduction - smaller = more faded appearance
            if (star.alpha > 0.1f) { // Only draw visible stars
                GameApp.drawTexture("star", star.x - star.size/2, star.y - star.size/2, star.size, star.size);
            }
        }
        GameApp.endSpriteRendering();
    }
    
    private void drawTitle(float centerX, float centerY) {
        if (!GameApp.hasTexture("winner_title")) return;
        float screenWidth = GameApp.getWorldWidth();
        float titleWidth = 500f, titleHeight = 150f;
        try {
            int texWidth = GameApp.getTextureWidth("winner_title");
            int texHeight = GameApp.getTextureHeight("winner_title");
            if (texWidth > 0 && texHeight > 0) {
                // 2x LARGER title (was 0.5f, now 1.0f of screen width)
                float targetWidth = screenWidth * 0.7f;
                titleWidth = targetWidth;
                titleHeight = targetWidth * texHeight / texWidth;
            }
        } catch (Exception e) {}
        
        float titleX = (screenWidth - titleWidth) / 2f;
        float titleY = centerY - 100f; // Move higher
        GameApp.drawTexture("winner_title", titleX, titleY, titleWidth, titleHeight);
    }
    
    private void drawButtonText(float centerX) {
        if (winnerButtons.size() < 3) return;
        
        String[] labels = {"PLAY AGAIN", "CREDITS", "BACK TO MENU"};
        String[] colors = {"button_green_text", "button_blue_text", "button_red_text"};
        
        for (int i = 0; i < 3; i++) {
            Button btn = winnerButtons.get(i);
            float btnCenterX = btn.getX() + btn.getWidth() / 2;
            float btnCenterY = btn.getY() + btn.getHeight() / 2;
            float textHeight = GameApp.getTextHeight("winnerButtonFont", labels[i]);
            GameApp.drawTextCentered("winnerButtonFont", labels[i], btnCenterX, btnCenterY + textHeight * 0.15f, colors[i]);
        }
    }
    
    private void handleInput() {
        com.badlogic.gdx.math.Vector2 mouseWorld = getMouseWorldPosition();
        float worldMouseX = mouseWorld.x;
        float worldMouseY = mouseWorld.y;
        
        if (winnerButtons == null || winnerButtons.isEmpty()) return;
        
        boolean hoveringAnyButton = false;
        for (Button button : winnerButtons) {
            if (button.containsPoint(worldMouseX, worldMouseY)) {
                hoveringAnyButton = true;
                break;
            }
        }
        
        if (cursorPointer != null && cursorHover != null) {
            if (GameApp.isButtonPressed(0) || GameApp.isButtonJustPressed(0)) {
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringButton = false;
            } else if (hoveringAnyButton && !isHoveringButton) {
                Gdx.graphics.setCursor(cursorHover);
                isHoveringButton = true;
            } else if (!hoveringAnyButton && isHoveringButton) {
                Gdx.graphics.setCursor(cursorPointer);
                isHoveringButton = false;
            }
        }
        
        for (Button button : winnerButtons) button.update(worldMouseX, worldMouseY);
        
        if (GameApp.isButtonJustPressed(0) && pendingAction == null) {
            for (int i = 0; i < winnerButtons.size(); i++) {
                Button button = winnerButtons.get(i);
                if (button.containsPoint(worldMouseX, worldMouseY)) {
                    if (soundManager != null) soundManager.playSound("clickbutton", 2.5f);
                    
                    pressedButton = button;
                    button.setPressed(true);
                    
                    final int buttonIndex = i;
                    pendingAction = () -> {
                        switch (buttonIndex) {
                            case 0: GameApp.switchScreen("play"); break;
                            case 1: 
                                CreditsScreen.setPreviousScreen("winner"); // Return to winner screen
                                GameApp.switchScreen("credits"); 
                                break;
                            case 2: GameApp.switchScreen("menu"); break;
                        }
                    };
                    pressTimer = 0f;
                    break;
                }
            }
        }
        
        if (pendingAction == null) {
            boolean isMouseDown = GameApp.isButtonPressed(0);
            for (Button button : winnerButtons) {
                button.setPressed(isMouseDown && button.containsPoint(worldMouseX, worldMouseY));
            }
        }
    }
}
