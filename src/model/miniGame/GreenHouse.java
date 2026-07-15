package model.miniGame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent 4-by-5 greenhouse board. */
public class GreenHouse {
    public static final int ROWS = 4;
    public static final int COLUMNS = 5;
    public static final int TOTAL_SLOTS = ROWS * COLUMNS;
    public static final int INITIAL_UNLOCKED_SLOTS = COLUMNS;

    private List<GreenhouseSlot> slots = new ArrayList<>();

    public GreenHouse() {
        initializeFreshLayout();
    }

    public List<GreenhouseSlot> getSlots() {
        if (slots == null) {
            slots = new ArrayList<>();
        }
        return slots;
    }

    public GreenhouseSlot getSlot(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) {
            return null;
        }
        for (GreenhouseSlot slot : getSlots()) {
            if (slot != null && slot.getIndex() == index) {
                return slot;
            }
        }
        return null;
    }

    public GreenhouseSlot getSlot(int x, int y) {
        if (!isValidCoordinate(x, y)) {
            return null;
        }
        return getSlot(toIndex(x, y));
    }

    public boolean isValidCoordinate(int x, int y) {
        return x >= 1 && x <= COLUMNS && y >= 1 && y <= ROWS;
    }

    /**
     * Permanently unlocks the next locked slot in row-major order.
     *
     * @return the newly unlocked slot, or {@code null} when all twenty are owned
     */
    public GreenhouseSlot unlockNextSlot() {
        applyDefaults();
        for (GreenhouseSlot slot : slots) {
            if (slot.isLocked()) {
                slot.unlock();
                return slot;
            }
        }
        return null;
    }

    /** Compatibility alias used by pot rewards and older code. */
    public GreenhouseSlot addSlot() {
        return unlockNextSlot();
    }

    /** Number of permanently unlocked pots. */
    public int getSlotCount() {
        int count = 0;
        for (GreenhouseSlot slot : getSlots()) {
            if (slot != null && slot.isUnlocked()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasLockedSlot() {
        return getSlotCount() < TOTAL_SLOTS;
    }

    public boolean hasAvailablePot() {
        for (GreenhouseSlot slot : getSlots()) {
            if (slot != null && slot.isUnlocked() && !slot.isOccupied()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes old dynamic-slot saves into the fixed twenty-slot layout.
     * Existing legacy entries are treated as already purchased/unlocked.
     */
    public void applyDefaults() {
        boolean legacyDynamicLayout = getSlots().size() < TOTAL_SLOTS;
        Map<Integer, GreenhouseSlot> existing = new LinkedHashMap<>();
        for (GreenhouseSlot slot : getSlots()) {
            if (slot == null || slot.getIndex() < 0 || slot.getIndex() >= TOTAL_SLOTS) {
                continue;
            }
            if (legacyDynamicLayout || slot.getIndex() < INITIAL_UNLOCKED_SLOTS) {
                slot.unlock();
            }
            slot.applyDefaults();
            existing.putIfAbsent(slot.getIndex(), slot);
        }

        List<GreenhouseSlot> normalized = new ArrayList<>(TOTAL_SLOTS);
        for (int index = 0; index < TOTAL_SLOTS; index++) {
            GreenhouseSlot slot = existing.get(index);
            if (slot == null) {
                slot = new GreenhouseSlot(index, index < INITIAL_UNLOCKED_SLOTS);
            } else if (index < INITIAL_UNLOCKED_SLOTS) {
                slot.unlock();
            }
            normalized.add(slot);
        }
        normalized.sort(Comparator.comparingInt(GreenhouseSlot::getIndex));
        slots = normalized;
    }

    private int toIndex(int x, int y) {
        return (y - 1) * COLUMNS + (x - 1);
    }

    private void initializeFreshLayout() {
        slots.clear();
        for (int index = 0; index < TOTAL_SLOTS; index++) {
            slots.add(new GreenhouseSlot(index, index < INITIAL_UNLOCKED_SLOTS));
        }
    }
}
