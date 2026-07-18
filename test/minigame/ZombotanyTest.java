package minigame;

import model.enums.PlantType;
import model.miniGame.*;
import model.sim.zombie.ZombieInstance;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZombotanyTest {
    @Test
    void allThreeLevelsContainAllFourTraitsAndIncreasePressure() {
        for (int level = 1; level <= 3; level++) {
            assertTrue(Zombotany.rulesFor(level).getSequence().containsAll(
                    java.util.List.of(ZombotanyTrait.values())));
        }
        assertTrue(Zombotany.rulesFor(2).getZombieCount() > Zombotany.rulesFor(1).getZombieCount());
        assertTrue(Zombotany.rulesFor(3).getSpawnIntervalTicks()
                < Zombotany.rulesFor(2).getSpawnIntervalTicks());
    }

    @Test
    void selectionIsRequiredAndWallnutVariantUsesCanonicalWallnutHealth() {
        MiniGameDefinition definition = MiniGameCatalog.phaseOneDefaults().find(MiniGameId.ZOMBOTANY);
        MiniGameSession session = new MiniGameSessionFactory(new SeededRandomSource(4))
                .create("player", definition, definition.getLevel(1));
        assertFalse(session.start().getStatus());
        assertTrue(session.executeStrategyCommand("add plant -t peashooter").getStatus());
        assertTrue(session.start().getStatus());
        session.advance(121);
        ZombotanyState state = session.getStrategyState(ZombotanyState.class);
        ZombieInstance wallnut = null;
        for (Map.Entry<ZombieInstance, ZombotanyTrait> entry : state.getVariants().entrySet()) {
            if (entry.getValue() == ZombotanyTrait.WALL_NUT) wallnut = entry.getKey();
        }
        assertNotNull(wallnut);
        assertEquals(model.inGame.plant.PlantRegistry.getDefault()
                .requireMandatory(PlantType.WALL_NUT).getHp(), wallnut.getSpec().getHealth());
    }
    @Test
    void peashooterJalapenoAndSquashTraitsResolveDeterministically() {
        MiniGameDefinition definition = MiniGameCatalog.phaseOneDefaults().find(MiniGameId.ZOMBOTANY);
        MiniGameSession session = new MiniGameSessionFactory(new SeededRandomSource(3))
                .create("player", definition, definition.getLevel(1));
        assertTrue(session.executeStrategyCommand("add plant -t sunflower").getStatus());
        assertTrue(session.executeStrategyCommand("add plant -t wall-nut").getStatus());
        assertTrue(session.executeStrategyCommand("add plant -t peashooter").getStatus());
        assertTrue(session.start().getStatus());
        session.getSimulation().addSun(2000);
        assertTrue(session.executeStrategyCommand(
                "plant plant -t sunflower -l (2, 0)").getStatus());
        assertTrue(session.executeStrategyCommand(
                "plant plant -t wall-nut -l (2, 2)").getStatus());
        assertTrue(session.executeStrategyCommand(
                "plant plant -t peashooter -l (7, 3)").getStatus());

        var tile = session.getSimulation().getWorld().getBoard().tileAt(2, 0);
        var target = tile.getStackedPlant() != null
                ? tile.getStackedPlant() : tile.getSupportPlant();
        int initialHp = target.getHp();
        session.advance(30);
        assertTrue(target.getHp() < initialHp, "Peashooter Zombie pea must damage left-side plants");

        session.advance(320);
        assertFalse(session.getSimulation().getWorld().getBoard()
                .tileAt(2, 2).hasAnyPlant(), "Jalapeno Zombie burns its entire row");

        session.advance(40);
        assertFalse(session.getSimulation().getWorld().getBoard()
                .tileAt(7, 3).hasAnyPlant(), "Squash Zombie destroys itself and its contact plant");
    }

}
