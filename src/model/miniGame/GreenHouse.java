package model.miniGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted greenhouse state: the slots the player owns and what is planted in
 * them. Growth and harvesting rules are not implemented here.
 */
public class GreenHouse {
    private List<GreenhouseSlot> slots = new ArrayList<>();

    public List<GreenhouseSlot> getSlots() {
        if (slots == null) {
            slots = new ArrayList<>();
        }

        return slots;
    }

    public GreenhouseSlot getSlot(int index) {
        for (GreenhouseSlot slot : getSlots()) {
            if (slot.getIndex() == index) {
                return slot;
            }
        }

        return null;
    }

    public GreenhouseSlot addSlot() {
        GreenhouseSlot slot = new GreenhouseSlot(getSlots().size());
        getSlots().add(slot);

        return slot;
    }

    public int getSlotCount() {
        return getSlots().size();
    }

    /** Recreates the slot list when a save file did not contain one. */
    public void applyDefaults() {
        getSlots();
    }
}
