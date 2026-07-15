package zombie;

import controller.GameplayController;
import model.Result;
import model.enums.Command;
import model.enums.ZombieType;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.level.NormalLevel;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.zombie.ZombieInstance;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieCommandTest {


    @Test
    void commandPatternsAcceptTheDocumentedForms() {
        assertTrue(Command.ZOMBIES_INFO.matches("zombies info"));
        assertTrue(Command.CHEAT_SPAWN_ZOMBIE.matches(
                "cheat spawn-zombie -t Buckethead -l (8, 2)"));
        assertFalse(Command.CHEAT_SPAWN_ZOMBIE.matches(
                "spawn-zombie Buckethead 8 2"));
    }

    @Test
    void zombiesInfoReportsTypePositionHealthArmorAndEffects() {
        Simulation simulation = simulation();
        GameplayController controller = new GameplayController(simulation);
        Result<ZombieInstance> spawned = controller.spawnZombie("buckethead", 8, 2);
        spawned.getData().applyEffect(
                model.inGame.zombie.ZombieEffectType.CHILLED, 3.2, 0);

        Result<List<String>> result = controller.zombiesInfo();

        assertTrue(result.getStatus());
        assertTrue(result.getMessage().contains("type: BUCKETHEAD"));
        assertTrue(result.getMessage().contains("position: 8.00, 2"));
        assertTrue(result.getMessage().contains("health: 190/190"));
        assertTrue(result.getMessage().contains("bucket: 1100"));
        assertTrue(result.getMessage().contains("chilled: 3.2s"));
    }

    @Test
    void spawnCheatRejectsUnknownBonusInvalidAndWrongChapterTypes() {
        GameplayController controller = new GameplayController(simulation());

        assertFalse(controller.spawnZombie("not-a-zombie", 8, 0).getStatus());
        assertFalse(controller.spawnZombie("Arcade Zombie", 8, 0).getStatus());
        assertFalse(controller.spawnZombie("Normal", 9, 0).getStatus());
        assertFalse(controller.spawnZombie("Hunter", 8, 0).getStatus());
    }

    @Test
    void spawnCheatAllowsChapterZombieInMatchingLevel() {
        Simulation simulation = simulation();
        GameSession session = new GameSession(
                new NormalLevel("Frostbite Caves 1-1"),
                new PlantSelection(),
                3,
                Set.of()
        );
        GameplayController controller = new GameplayController(
                simulation, null, null, null, session, null);

        Result<ZombieInstance> result = controller.spawnZombie("Hunter", 8, 0);

        assertTrue(result.getStatus());
        assertTrue(result.getData().getType() == ZombieType.HUNTER);
    }

    private Simulation simulation() {
        return new Simulation(new SeededRandomSource(0), new SimulationWorld(), false);
    }
}
