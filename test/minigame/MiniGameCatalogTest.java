package minigame;

import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameDefinition;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameLevelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameCatalogTest {

    @Test
    void allPhaseOneMinigamesAreRegisteredAndBonusScopeIsMarked() {
        MiniGameCatalog catalog = MiniGameCatalog.phaseOneDefaults();

        assertEquals(5, catalog.getDefinitions().size());
        assertEquals("Vase Breaker", catalog.getDefinitions().get(0).getDisplayName());
        assertEquals("Bowling Wall-nut", catalog.getDefinitions().get(1).getDisplayName());
        assertEquals("I, Zombie", catalog.getDefinitions().get(2).getDisplayName());
        assertEquals("Beghouled", catalog.getDefinitions().get(3).getDisplayName());
        assertEquals("Zombotany", catalog.getDefinitions().get(4).getDisplayName());
        assertTrue(!MiniGameId.VASE_BREAKER.isBonus());
        assertTrue(!MiniGameId.BOWLING_WALLNUT.isBonus());
        assertTrue(!MiniGameId.I_ZOMBIE.isBonus());
        assertTrue(MiniGameId.BEGHOULED.isBonus());
        assertTrue(MiniGameId.ZOMBOTANY.isBonus());
    }

    @Test
    void everyPhaseOneMinigameHasThreeExplicitlyHarderLevels() {
        for (MiniGameDefinition definition
                : MiniGameCatalog.phaseOneDefaults().getDefinitions()) {
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
