package greenhouse;

import model.enums.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenhouseCommandTest {
    @Test
    void exactGreenhouseCommandsParseCoordinates() {
        assertTrue(Command.SHOW_GREENHOUSE.matches("show greenhouse"));
        assertTrue(Command.PLANT_POT.matches("plant pot at (5, 4)"));
        assertTrue(Command.COLLECT_GREENHOUSE.matches("collect (1, 2)"));
        assertTrue(Command.GROW_GREENHOUSE.matches("grow (3, 1)"));

        assertFalse(Command.PLANT_POT.matches("plant pot at 5, 4"));
        assertFalse(Command.COLLECT_GREENHOUSE.matches("collect sun (1, 2)"));
    }
}
