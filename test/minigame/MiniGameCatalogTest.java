package minigame;

import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameDefinition;
import model.miniGame.MiniGameLevelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameCatalogTest {

    @Test
    void onlyMandatoryRepositoryMinigamesAreRegistered() {
        MiniGameCatalog catalog = MiniGameCatalog.mandatoryDefaults();

        assertEquals(3, catalog.getDefinitions().size());
        assertEquals("Vase Breaker", catalog.getDefinitions().get(0).getDisplayName());
        assertEquals("Bowling Wall-nut", catalog.getDefinitions().get(1).getDisplayName());
        assertEquals("I, Zombie", catalog.getDefinitions().get(2).getDisplayName());
        assertNull(catalog.find("Beghouled"));
        assertNull(catalog.find("Zombotany"));
    }

    @Test
    void everyMandatoryMinigameHasThreeExplicitlyHarderLevels() {
        for (MiniGameDefinition definition
                : MiniGameCatalog.mandatoryDefaults().getDefinitions()) {
            assertEquals(3, definition.getLevels().size());
            for (int index = 1; index < definition.getLevels().size(); index++) {
                MiniGameLevelConfig previous = definition.getLevels().get(index - 1);
                MiniGameLevelConfig current = definition.getLevels().get(index);
                assertTrue(current.isHarderThan(previous),
                        definition.getDisplayName() + " level " + current.getLevelNumber());
            }
        }
    }
}
