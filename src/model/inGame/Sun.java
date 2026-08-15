package model.inGame;

import model.sim.sun.SunType;

public class Sun {
    public enum State { ON_PLANT, FALLING, ON_GROUND }

    private SunType type;
    private final int tileX;
    private final int tileY;
    private State state;
    private double remainingFallSeconds;
    private final Integer customValue; // null => از SunType.getValue() استفاده کن

    private Sun(SunType type, int tileX, int tileY, State state,
                double remainingFallSeconds, Integer customValue) {
        this.type = type;
        this.tileX = tileX;
        this.tileY = tileY;
        this.state = state;
        this.remainingFallSeconds = remainingFallSeconds;
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

    private void land() {
        state = State.ON_GROUND;
        remainingFallSeconds = 0;
        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
        }
    }
}