package model.user;

public class Settings {
    private static final int DEFAULT_DIFFICULTY = 3;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    private static final int DEFAULT_GAME_SPEED = 1;
    private static final int MIN_GAME_SPEED = 1;
    private static final int MAX_GAME_SPEED = 3;

    private int difficultyLevel;
    private int gameSpeed;
    private boolean gridVisible;
    private boolean debugMode;

    public Settings() {
        this.difficultyLevel = DEFAULT_DIFFICULTY;
        this.gameSpeed = DEFAULT_GAME_SPEED;
        this.gridVisible = false;
        this.debugMode = false;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        if (difficultyLevel < MIN_DIFFICULTY || difficultyLevel > MAX_DIFFICULTY) {
            throw new IllegalArgumentException("Difficulty must be between 1 and 5.");
        }

        this.difficultyLevel = difficultyLevel;
    }

    public int getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(int gameSpeed) {
        if (gameSpeed < MIN_GAME_SPEED || gameSpeed > MAX_GAME_SPEED) {
            throw new IllegalArgumentException("Game speed must be between 1 and 3.");
        }

        this.gameSpeed = gameSpeed;
    }

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}