package greenhouse;

import model.miniGame.GreenHouse;
import model.miniGame.GreenhouseSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenhouseLayoutTest {

    @Test
    void layoutHasTwentyFixedSlotsAndOnlyFirstRowUnlocked() {
        GreenHouse greenHouse = new GreenHouse();

        assertEquals(20, greenHouse.getSlots().size());
        assertEquals(5, greenHouse.getSlotCount());

        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                GreenhouseSlot slot = greenHouse.getSlot(x, y);
                assertNotNull(slot);
                assertEquals(x, slot.getX());
                assertEquals(y, slot.getY());
                assertEquals(y == 1, slot.isUnlocked());
            }
        }
    }

    @Test
    void purchasedSlotsStayUnlockedAndCapacityStopsAtTwenty() {
        GreenHouse greenHouse = new GreenHouse();
        for (int expected = 6; expected <= 20; expected++) {
            assertNotNull(greenHouse.unlockNextSlot());
            assertEquals(expected, greenHouse.getSlotCount());
        }

        assertFalse(greenHouse.hasLockedSlot());
        assertNull(greenHouse.unlockNextSlot());

        greenHouse.applyDefaults();
        assertEquals(20, greenHouse.getSlotCount());
        assertTrue(greenHouse.getSlot(5, 4).isUnlocked());
    }

    @Test
    void coordinatesOutsideOneBasedFourByFiveLayoutAreRejected() {
        GreenHouse greenHouse = new GreenHouse();

        assertFalse(greenHouse.isValidCoordinate(0, 1));
        assertFalse(greenHouse.isValidCoordinate(1, 0));
        assertFalse(greenHouse.isValidCoordinate(6, 1));
        assertFalse(greenHouse.isValidCoordinate(1, 5));
        assertNull(greenHouse.getSlot(6, 4));
    }
}
