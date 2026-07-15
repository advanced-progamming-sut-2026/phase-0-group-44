package model.sim.sun;

/**
 * A sun in play: either sitting on the plant that produced it, falling from the
 * sky, or resting on the ground. Positions are integer tile coordinates.
 */
public class Sun {

    /** Where a sun is in its life cycle. */
    public enum State {
        ON_PLANT,
        FALLING,
        ON_GROUND
    }

    private SunType type;
    private final int tileX;
    private final int tileY;
    private State state;
    private int remainingFallTicks;

    private Sun(SunType type, int tileX, int tileY, State state, int remainingFallTicks) {
        this.type = type;
        this.tileX = tileX;
        this.tileY = tileY;
        this.state = state;
        this.remainingFallTicks = remainingFallTicks;
    }

    /** A sun produced by a plant, resting on it until collected. */
    public static Sun onPlant(SunType type, int tileX, int tileY) {
        return new Sun(type, tileX, tileY, State.ON_PLANT, 0);
    }

    /** A sun falling from the sky toward the given tile. */
    public static Sun falling(SunType type, int tileX, int tileY, int fallTicks) {
        return new Sun(type, tileX, tileY, State.FALLING, fallTicks);
    }

    public SunType getType() {
        return type;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public State getState() {
        return state;
    }

    public int getRemainingFallTicks() {
        return remainingFallTicks;
    }

    public boolean isFalling() {
        return state == State.FALLING;
    }

    public boolean isAt(int x, int y) {
        return tileX == x && tileY == y;
    }

    public int getValue() {
        return type.getValue();
    }

    /** Advances the fall by one tick; returns true when it has just landed. */
    public boolean tickFall() {
        if (state != State.FALLING) {
            return false;
        }

        remainingFallTicks--;

        if (remainingFallTicks <= 0) {
            land();
            return true;
        }

        return false;
    }

    /** A radioactive sun that reaches the ground becomes a normal sun. */
    private void land() {
        state = State.ON_GROUND;
        remainingFallTicks = 0;

        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
        }
    }
}
