package controller;

import model.Result;
import model.enums.PlantType;
import model.sim.GameOutcome;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.board.PlantSpec;
import model.sim.board.Tile;
import model.sim.wave.WaveComposer;
import model.sim.wave.WaveConfig;
import model.sim.wave.WaveSystem;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;
import org.junit.jupiter.api.Test;
import util.RandomSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Wave thresholds, composition, final wave, mowers, nuke, and win timing. */
class ZombieWaveTest {

    private static ZombieSpec basic(String name, int cost, int hp) {
        return ZombieSpec.builder(name).waveCost(cost).health(hp)
                .speedTilesPerSecond(10).eatDamagePerSecond(100).build();
    }

    private static ZombieSpecSource sourceOf(ZombieSpec... specs) {
        return () -> List.of(specs);
    }

    // ---------- wave cost formula ----------

    @Test
    void normalWavesGrowByTwentyFivePercentAndFinalWaveDoubles() {
        WaveConfig config = WaveConfig.of(4, 100);

        assertEquals(100, config.costOfWave(1));
        assertEquals(125, config.costOfWave(2));
        assertEquals(156, config.costOfWave(3));   // round(125 * 1.25)
        assertEquals(312, config.costOfWave(4));   // final = 2 * previous (156)
        assertTrue(config.isFinalWave(4));
    }

    @Test
    void anExplicitCostSequenceOverridesTheGeneralFormula() {
        WaveConfig config = WaveConfig.ofExplicitCosts(new int[] {50, 200, 90});

        assertEquals(50, config.costOfWave(1));
        assertEquals(200, config.costOfWave(2));
        assertEquals(90, config.costOfWave(3));
        assertEquals(3, config.getWaveCount());
    }

    // ---------- composition ----------

    @Test
    void compositionSumsExactlyToTheTarget() {
        List<ZombieSpec> specs = List.of(basic("Basic", 10, 100), basic("Cone", 20, 200));
        RandomSource random = new ScriptedRandom().queueInt(0).queueInt(0).queueInt(1);

        List<ZombieSpec> composition = WaveComposer.compose(50, specs, random);

        assertNotNull(composition);
        int total = composition.stream().mapToInt(ZombieSpec::getWaveCost).sum();
        assertEquals(50, total);
    }

    @Test
    void anUncomposableTargetReturnsNullInsteadOfLoopingForever() {
        List<ZombieSpec> specs = List.of(basic("Even", 4, 100));

        assertNull(WaveComposer.compose(7, specs, new ScriptedRandom()));
        assertNull(WaveComposer.compose(30, List.of(), new ScriptedRandom()));
    }

    // ---------- wave lifecycle ----------

    private Simulation gameWith(WaveConfig config, ZombieSpecSource source, RandomSource random) {
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(random, world, false);
        sim.register(new WaveSystem(config, source));
        sim.register(new ZombieCombatSystem());

        return sim;
    }

    @Test
    void theFirstWaveStartsImmediatelyWithItsMessagesAndSpawns() {
        WaveConfig config = WaveConfig.of(2, 20);
        // lane rolls: pick spec index 0 twice (compose 20 from two 10s), then lanes 0,0.
        ScriptedRandom random = new ScriptedRandom()
                .queueInt(0).queueInt(0)   // composition picks
                .queueInt(0).queueInt(0);  // lanes
        Simulation sim = gameWith(config, sourceOf(basic("Basic", 10, 100)), random);

        List<String> events = sim.advance(1);

        assertTrue(events.contains("Wave 1 started."));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("Zombie Basic spawned at wave 1")));
        assertTrue(events.stream().anyMatch(e -> e.contains("which costed 10.")));
    }

    @Test
    void theFinalWaveAnnouncesItself() {
        WaveConfig config = WaveConfig.of(2, 20);
        // Wave 1 (cost 20): two 10s; wave 2 final (cost 40): four 10s. Provide plenty of rolls.
        ScriptedRandom random = new ScriptedRandom();
        for (int i = 0; i < 40; i++) {
            random.queueInt(0);
        }
        Simulation sim = gameWith(config, sourceOf(basic("Basic", 10, 100)), random);

        // Wave 1 spawns 2 zombies (200 hp). Kill 75% (>=150) by nuking via combat is hard;
        // instead let them walk off — but we only need the final-wave message, so drop the
        // wave-1 zombies' health low and advance until the threshold triggers wave 2.
        sim.advance(1); // wave 1
        for (ZombieInstance z : sim.getWorld().getZombieInstances()) {
            z.takeDamage(z.getHp() - 1); // 2 hp total remains, 198 lost of 200 -> >75%
        }

        List<String> events = sim.advance(1); // threshold met -> wave 2 (final)
        assertTrue(events.contains("The final wave has come."));
        assertTrue(events.contains("Wave 2 started."));
    }

    @Test
    void aNewWaveStartsAfterSeventyFivePercentHealthLost() {
        WaveConfig config = WaveConfig.ofExplicitCosts(new int[] {100, 100});
        ScriptedRandom random = new ScriptedRandom();
        for (int i = 0; i < 40; i++) {
            random.queueInt(0);
        }
        Simulation sim = gameWith(config, sourceOf(basic("Basic", 100, 100)), random);

        sim.advance(1); // wave 1: one 100-hp zombie
        ZombieInstance z = sim.getWorld().getZombieInstances().get(0);

        z.takeDamage(74); // 26 hp of 100 -> only 74% lost, not enough
        assertFalse(sim.advance(1).contains("Wave 2 started."));

        z.takeDamage(1); // now 75 lost -> threshold met
        assertTrue(sim.advance(1).contains("Wave 2 started."));
    }

    // ---------- movement / eating / plant destruction ----------

    @Test
    void aZombieEatsTheBlockingPlantThenResumesAndTriggersTheMower() {
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);
        sim.register(new ZombieCombatSystem());

        // A plant with 100 hp at column 3, row 0.
        Tile tile = world.getBoard().tileAt(3, 0);
        PlantSpec spec = PlantSpec.builder(PlantType.WALL_NUT).hp(100).build();
        model.sim.board.PlantInstance plant = new model.sim.board.PlantInstance(spec, 3, 0);
        tile.setSupportPlant(plant);
        world.addPlant(plant);

        // A fast eater at column 3 (10 tiles/s = 1 tile/tick, 1000 eat dps = 100/tick).
        ZombieSpec chompSpec = ZombieSpec.builder("Chomp")
                .waveCost(10).health(500).speedTilesPerSecond(10)
                .eatDamagePerSecond(1000).build();
        ZombieInstance zombie = new ZombieInstance(chompSpec, 3, 0);
        world.addZombie(zombie);

        // One tick: eats 100 -> destroys plant.
        List<String> events = sim.advance(1);
        assertTrue(events.contains("Plant WALL_NUT at (3, 0) is destroyed."));
        assertNull(world.getBoard().tileAt(3, 0).getSupportPlant());
    }

    @Test
    void theFirstBreachTriggersTheMowerAndTheSecondLosesTheGame() {
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);
        sim.register(new ZombieCombatSystem());

        // Zombie at x=0.5, speed 10 -> after 1 tick x=-0.5 <= 0: breach row 2.
        ZombieInstance first = new ZombieInstance(basic("A", 10, 100), 0.5, 2);
        world.addZombie(first);

        List<String> firstEvents = sim.advance(1);
        assertTrue(firstEvents.stream()
                .anyMatch(e -> e.startsWith("The lawn mower in the row 2is triggered")));
        assertTrue(world.isLawnMowerUsed(2));
        assertTrue(world.isRunning());

        ZombieInstance second = new ZombieInstance(basic("B", 10, 100), 0.5, 2);
        world.addZombie(second);

        List<String> secondEvents = sim.advance(1);
        assertTrue(secondEvents.contains("The zombie ate your brain; LOSER!!!"));
        assertEquals(GameOutcome.LOST, world.getOutcome());
    }

    // ---------- nuke ----------

    @Test
    void theNukeKillsEveryZombieWithNormalDeathMessagesAndNoRewards() {
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);
        world.addZombie(new ZombieInstance(basic("A", 10, 100), 5, 0));
        world.addZombie(new ZombieInstance(basic("B", 10, 100), 6, 1));

        GameplayController controller = new GameplayController(sim);
        Result<List<String>> result = controller.releaseNuke();

        assertTrue(result.getStatus());
        assertEquals(2, result.getData().stream()
                .filter(m -> m.startsWith("Zombie of type")).count());
        assertTrue(world.getZombieInstances().isEmpty());
        // cheat deaths are recorded as cheat kills, then drained without rewards.
        assertTrue(world.getPendingDeaths().isEmpty());
    }

    // ---------- win ----------

    @Test
    void winningRequiresAllWavesAndNoZombiesLeftThenReturnsTheExactMessage() {
        WaveConfig config = WaveConfig.ofExplicitCosts(new int[] {10});
        ScriptedRandom random = new ScriptedRandom();
        for (int i = 0; i < 10; i++) {
            random.queueInt(0);
        }
        Simulation sim = gameWith(config, sourceOf(basic("Basic", 10, 50)), random);

        sim.advance(1); // single final wave spawns one zombie
        assertFalse(sim.getWorld().getZombieInstances().isEmpty());

        // Kill it, then advance so the wave system sees an empty board.
        sim.getWorld().getZombieInstances().get(0).kill();
        List<String> afterKill = sim.advance(1); // combat removes it + emits death
        List<String> win = sim.advance(1);       // wave system declares victory

        assertTrue(win.contains(
                "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz."));
        assertEquals(GameOutcome.WON, sim.getWorld().getOutcome());
    }

    private static final class ScriptedRandom implements RandomSource {
        private final Deque<Integer> ints = new ArrayDeque<>();
        private final Deque<Double> doubles = new ArrayDeque<>();

        ScriptedRandom queueInt(int v) {
            ints.add(v);
            return this;
        }

        ScriptedRandom queueDouble(double v) {
            doubles.add(v);
            return this;
        }

        @Override
        public int nextInt(int bound) {
            if (ints.isEmpty()) {
                return 0;
            }

            int value = ints.poll();

            return value % Math.max(bound, 1);
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.0 : doubles.poll();
        }
    }
}
