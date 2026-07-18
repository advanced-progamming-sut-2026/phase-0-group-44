package minigame;

import model.miniGame.*;
import model.sim.board.PlantInstance;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import static org.junit.jupiter.api.Assertions.*;

class BeghouledTest {
    @Test
    void levelsAndMatchRewardsAreExplicit() {
        assertTrue(Beghouled.rulesFor(2).getTargetMatches() > Beghouled.rulesFor(1).getTargetMatches());
        assertTrue(Beghouled.rulesFor(3).getDangerScore() > Beghouled.rulesFor(2).getDangerScore());
        assertEquals(5, Beghouled.rulesFor(1).getPlantPool().size());
        assertEquals(1, BeghouledState.rewardUnits(3, false));
        assertEquals(2, BeghouledState.rewardUnits(4, false));
        assertEquals(3, BeghouledState.rewardUnits(5, false));
        assertEquals(2, BeghouledState.rewardUnits(3, true));
    }

    @Test
    void boardStartsPlayableAndEatenPlantsBecomeCraters() {
        MiniGameCatalog catalog = MiniGameCatalog.phaseOneDefaults();
        MiniGameDefinition definition = catalog.find(MiniGameId.BEGHOULED);
        MiniGameSession session = new MiniGameSessionFactory(new SeededRandomSource(7))
                .create("player", definition, definition.getLevel(1));
        assertTrue(session.start().getStatus());
        BeghouledState state = session.getStrategyState(BeghouledState.class);
        assertTrue(state.hasLegalMove());
        int sunBefore = session.getSimulation().getSunAmount();
        boolean accepted = false;
        for (int y = 0; y < 5 && !accepted; y++) {
            for (int x = 0; x < 9 && !accepted; x++) {
                if (x + 1 < 9) accepted = state.swap(session, x, y, x + 1, y).getStatus();
                if (!accepted && y + 1 < 5) accepted = state.swap(session, x, y, x, y + 1).getStatus();
            }
        }
        assertTrue(accepted);
        assertTrue(session.getSimulation().getSunAmount() >= sunBefore + 50);

        PlantInstance plant = session.getSimulation().getWorld().getBoard()
                .tileAt(0, 0).getStackedPlant();
        session.getSimulation().getWorld().getBoard().tileAt(0, 0).clearPlants();
        session.getSimulation().getWorld().getPlants().remove(plant);
        session.advance(1);
        assertTrue(state.isCrater(0, 0));
    }

    @Test
    void suppliedUpgradeGraphContainsTwoStageChainsAndThreeBasePlants() {
        assertEquals(6, Beghouled.upgrades().size());
        assertEquals(model.enums.PlantType.REPEATER,
                Beghouled.upgrades().get(model.enums.PlantType.PEASHOOTER).to());
        assertEquals(model.enums.PlantType.MEGA_GATLING_PEA,
                Beghouled.upgrades().get(model.enums.PlantType.REPEATER).to());
        assertTrue(Beghouled.upgrades().containsKey(model.enums.PlantType.WALL_NUT));
        assertTrue(Beghouled.upgrades().containsKey(model.enums.PlantType.PUFF_SHROOM));
        assertTrue(Beghouled.upgrades().containsKey(model.enums.PlantType.CABBAGE_PULT));
    }
}
