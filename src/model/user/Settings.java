package model.user;

public class Settings {
    private static final int DEFAULT_DIFFICULTY = 3;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    private int difficultyLevel;

    public Settings() {
        this.difficultyLevel = DEFAULT_DIFFICULTY;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        if (difficultyLevel < MIN_DIFFICULTY
                || difficultyLevel > MAX_DIFFICULTY) {
            throw new IllegalArgumentException(
                    "Difficulty must be between 1 and 5."
            );
        }

        this.difficultyLevel = difficultyLevel;
    }
}
