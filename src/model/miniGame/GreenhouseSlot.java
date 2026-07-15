package model.miniGame;

import model.enums.PlantType;

import java.time.Duration;
import java.time.Instant;

/**
 * One fixed greenhouse coordinate and its persisted crop state.
 *
 * <p>Both the planting and ready instants are stored as epoch seconds. Growth
 * therefore continues while the program is closed and acceleration can make a
 * crop ready without depending on a process-local countdown.</p>
 */
public class GreenhouseSlot {
    public static final long MARIGOLD_GROWTH_SECONDS = Duration.ofHours(2).getSeconds();
    public static final long PLANT_GROWTH_SECONDS = Duration.ofHours(8).getSeconds();

    private int index;
    private boolean unlocked;
    private PlantType plantType;
    private boolean marigold;
    private long plantedAtEpochSecond;
    private long readyAtEpochSecond;

    public GreenhouseSlot() {
        // Required by Gson.
    }

    public GreenhouseSlot(int index) {
        this(index, false);
    }

    public GreenhouseSlot(int index, boolean unlocked) {
        this.index = index;
        this.unlocked = unlocked;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getX() {
        return index % GreenHouse.COLUMNS + 1;
    }

    public int getY() {
        return index / GreenHouse.COLUMNS + 1;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isLocked() {
        return !unlocked;
    }

    public void unlock() {
        unlocked = true;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public boolean isMarigold() {
        return marigold;
    }

    public String getCropName() {
        if (marigold) {
            return "Marigold";
        }
        return plantType == null ? null : plantType.name();
    }

    public long getPlantedAtEpochSecond() {
        return plantedAtEpochSecond;
    }

    public long getReadyAtEpochSecond() {
        return readyAtEpochSecond;
    }

    public boolean isOccupied() {
        return marigold || plantType != null;
    }

    /** Compatibility method retained for older callers and save tests. */
    public void plant(PlantType plantType, long plantedAtEpochSecond) {
        plant(plantType, plantedAtEpochSecond,
                plantedAtEpochSecond + PLANT_GROWTH_SECONDS);
    }

    public void plant(
            PlantType plantType,
            long plantedAtEpochSecond,
            long readyAtEpochSecond
    ) {
        if (plantType == null) {
            throw new IllegalArgumentException("Plant type is null.");
        }
        validateTimes(plantedAtEpochSecond, readyAtEpochSecond);

        this.plantType = plantType;
        this.marigold = false;
        this.plantedAtEpochSecond = plantedAtEpochSecond;
        this.readyAtEpochSecond = readyAtEpochSecond;
    }

    public void plantMarigold(long plantedAtEpochSecond) {
        plantMarigold(plantedAtEpochSecond,
                plantedAtEpochSecond + MARIGOLD_GROWTH_SECONDS);
    }

    public void plantMarigold(long plantedAtEpochSecond, long readyAtEpochSecond) {
        validateTimes(plantedAtEpochSecond, readyAtEpochSecond);

        this.plantType = null;
        this.marigold = true;
        this.plantedAtEpochSecond = plantedAtEpochSecond;
        this.readyAtEpochSecond = readyAtEpochSecond;
    }

    public boolean isReady(Instant now) {
        return isOccupied() && remainingSeconds(now) == 0L;
    }

    public long remainingSeconds(Instant now) {
        if (!isOccupied()) {
            return 0L;
        }
        if (now == null) {
            throw new IllegalArgumentException("Current instant is required.");
        }
        return Math.max(0L, readyAtEpochSecond - now.getEpochSecond());
    }

    public void makeReady(Instant now) {
        if (!isOccupied()) {
            throw new IllegalStateException("The greenhouse slot is empty.");
        }
        if (now == null) {
            throw new IllegalArgumentException("Current instant is required.");
        }
        readyAtEpochSecond = now.getEpochSecond();
    }

    public void clear() {
        plantType = null;
        marigold = false;
        plantedAtEpochSecond = 0L;
        readyAtEpochSecond = 0L;
    }

    /** Repairs fields missing from an older save file. */
    public void applyDefaults() {
        if (!isOccupied()) {
            clear();
            return;
        }

        if (readyAtEpochSecond <= 0L) {
            long duration = marigold
                    ? MARIGOLD_GROWTH_SECONDS : PLANT_GROWTH_SECONDS;
            readyAtEpochSecond = plantedAtEpochSecond + duration;
        }
    }

    private void validateTimes(long plantedAtEpochSecond, long readyAtEpochSecond) {
        if (plantedAtEpochSecond < 0L || readyAtEpochSecond < plantedAtEpochSecond) {
            throw new IllegalArgumentException("Invalid greenhouse timestamps.");
        }
    }
}
