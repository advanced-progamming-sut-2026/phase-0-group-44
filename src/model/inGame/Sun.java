package model.inGame;

import model.sim.sun.SunType;

public class Sun {
    public enum State { ON_PLANT, FALLING, ON_GROUND }

    private SunType type;
    private final int tileX;
    private final int tileY;
    public static final double UNCOLLECTED_LIFETIME_SECONDS = 7.0;

    private State state;
    private final double totalFallSeconds;
    private double remainingFallSeconds;
    private double remainingVisibleSeconds;
    private final Integer customValue; // null => از SunType.getValue() استفاده کن

    private Sun(SunType type, int tileX, int tileY, State state,
                double remainingFallSeconds, Integer customValue) {
        this.type = type;
        this.tileX = tileX;
        this.tileY = tileY;
        this.state = state;
        this.totalFallSeconds = Math.max(0.0, remainingFallSeconds);
        this.remainingFallSeconds = Math.max(0.0, remainingFallSeconds);
        this.remainingVisibleSeconds = UNCOLLECTED_LIFETIME_SECONDS;
        this.customValue = customValue;
    }

    /** خورشیدِ روی زمینِ آسمانی (سقوطی)، همیشه با ارزشِ ثابتِ نوعش. */
    public static Sun falling(SunType type, int tileX, int tileY, double fallSeconds) {
        return new Sun(type, tileX, tileY, State.FALLING, fallSeconds, null);
    }

    /** خورشیدی که یک گیاه تولید کرده، با مقدارِ
     *  خودِ گیاه (نه مقدارِ ثابتِ نوع). */
    public static Sun onPlant(int tileX, int tileY, int value) {
        return new Sun(SunType.NORMAL, tileX, tileY, State.ON_PLANT, 0, value);
    }

    public SunType getType() { return type; }
    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public State getState() { return state; }
    public boolean isFalling() { return state == State.FALLING; }
    public boolean isAt(int x, int y) { return tileX == x && tileY == y; }
    public int getValue() { return customValue != null ? customValue : type.getValue(); }

    /** 0 at spawn and 1 once the falling sun reaches its target tile. */
    public double getFallDurationSeconds() {
        return totalFallSeconds;
    }

    public double getFallProgress() {
        if (state != State.FALLING || totalFallSeconds <= 1e-9) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0,
                1.0 - remainingFallSeconds / totalFallSeconds));
    }

    public double getRemainingVisibleSeconds() {
        return remainingVisibleSeconds;
    }

    /** یک ثانیه/دلتا از سقوط رو جلو می‌بره؛
     *  true اگه تازه فرود اومده باشه. */
    public boolean tickFall(double deltaSeconds) {
        if (state != State.FALLING) {
            return false;
        }
        remainingFallSeconds -= deltaSeconds;
        if (remainingFallSeconds <= 1e-9) {
            land();
            return true;
        }
        return false;
    }

    /**
     * Advances the seven-second pickup window after the sun becomes available.
     * Falling sky suns do not spend this lifetime until they have landed.
     * Returns true exactly when the sun has expired and should be removed.
     */
    public boolean tickVisibleLifetime(double deltaSeconds) {
        if (state == State.FALLING) {
            return false;
        }
        remainingVisibleSeconds -= Math.max(0.0, deltaSeconds);
        return remainingVisibleSeconds <= 1e-9;
    }

    private void land() {
        state = State.ON_GROUND;
        remainingFallSeconds = 0;
        remainingVisibleSeconds = UNCOLLECTED_LIFETIME_SECONDS;
        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
        }
    }
}