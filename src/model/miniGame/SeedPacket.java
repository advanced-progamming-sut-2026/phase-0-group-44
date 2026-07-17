package model.miniGame;

import model.enums.PlantType;

/** A temporary one-use packet dropped by a vase. Its timer never pauses. */
public final class SeedPacket {
    public enum State {
        DROPPED,
        COLLECTED,
        PLANTED,
        EXPIRED
    }

    private final int id;
    private final PlantType plantType;
    private final int dropX;
    private final int dropY;
    private final long expiresAtTick;
    private State state = State.DROPPED;

    public SeedPacket(
            int id,
            PlantType plantType,
            int dropX,
            int dropY,
            long expiresAtTick
    ) {
        if (id <= 0 || plantType == null || dropX < 0 || dropY < 0 || expiresAtTick <= 0) {
            throw new IllegalArgumentException("Invalid temporary seed packet.");
        }
        this.id = id;
        this.plantType = plantType;
        this.dropX = dropX;
        this.dropY = dropY;
        this.expiresAtTick = expiresAtTick;
    }

    public int getId() {
        return id;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public int getDropX() {
        return dropX;
    }

    public int getDropY() {
        return dropY;
    }

    public long getExpiresAtTick() {
        return expiresAtTick;
    }

    public State getState() {
        return state;
    }

    public boolean collect(long currentTick) {
        expireIfDue(currentTick);
        if (state != State.DROPPED) {
            return false;
        }
        state = State.COLLECTED;
        return true;
    }

    public boolean plant(long currentTick) {
        expireIfDue(currentTick);
        if (state != State.COLLECTED) {
            return false;
        }
        state = State.PLANTED;
        return true;
    }

    public boolean expireIfDue(long currentTick) {
        if ((state == State.DROPPED || state == State.COLLECTED)
                && currentTick >= expiresAtTick) {
            state = State.EXPIRED;
            return true;
        }
        return false;
    }

    public boolean isAvailable() {
        return state == State.DROPPED || state == State.COLLECTED;
    }
}
