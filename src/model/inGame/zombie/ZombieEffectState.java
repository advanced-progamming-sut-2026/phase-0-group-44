package model.inGame.zombie;

public final class ZombieEffectState {
    private final ZombieEffectType type;
    private double remainingSeconds;
    private int magnitude;
    private double accumulator;

    public ZombieEffectState(ZombieEffectType type, double remainingSeconds, int magnitude) {
        this.type = type;
        this.remainingSeconds = Math.max(0.0, remainingSeconds);
        this.magnitude = Math.max(0, magnitude);
    }

    public void refresh(double seconds, int newMagnitude) {
        remainingSeconds = Math.max(remainingSeconds, Math.max(0.0, seconds));
        magnitude = Math.max(magnitude, Math.max(0, newMagnitude));
    }

    public int advanceAndCountWholeSeconds(double deltaSeconds) {
        if (remainingSeconds <= 0.0 || deltaSeconds <= 0.0) {
            return 0;
        }
        double activeDelta = Math.min(deltaSeconds, remainingSeconds);
        remainingSeconds = Math.max(0.0, remainingSeconds - deltaSeconds);
        accumulator += activeDelta;
        int ticks = (int) Math.floor(accumulator);
        accumulator -= ticks;
        return ticks;
    }

    public ZombieEffectType getType() {
        return type;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public boolean isExpired() {
        return remainingSeconds <= 0.0;
    }
}
