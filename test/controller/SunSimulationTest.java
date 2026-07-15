package controller;

import model.Result;
import model.sim.Damageable;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.sun.FallingSunSystem;
import model.sim.sun.Sun;
import model.sim.sun.SunCollector;
import model.sim.sun.SunDropSchedule;
import model.sim.sun.SunProducer;
import model.sim.sun.SunType;
import org.junit.jupiter.api.Test;
import util.RandomSource;

import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The simulation clock and the mandatory sun mechanics, made deterministic with a scripted RNG. */
class SunSimulationTest {

    /** RNG that returns queued values, so probability and tile choice are exact. */
    private static final class ScriptedRandom implements RandomSource {
        private final Deque<Double> doubles = new ArrayDeque<>();
        private final Deque<Integer> ints = new ArrayDeque<>();

        ScriptedRandom queueDouble(double value) {
            doubles.add(value);
            return this;
        }

        ScriptedRandom queueInt(int value) {
            ints.add(value);
            return this;
        }

        @Override
        public int nextInt(int bound) {
            return ints.isEmpty() ? 0 : ints.poll();
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.0 : doubles.poll();
        }
    }

    private static final class FakeUnit implements Damageable {
        private final int x;
        private final int y;
        private int hp = 1000;

        FakeUnit(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getTileX() {
            return x;
        }

        @Override
        public int getTileY() {
            return y;
        }

        @Override
        public void takeDamage(int amount) {
            hp -= amount;
        }

        @Override
        public boolean isDead() {
            return hp <= 0;
        }

        int damageTaken() {
            return 1000 - hp;
        }
    }

    // ---------- clock ----------

    @Test
    void tenTicksAreOneSecondAndTheCountMustBePositive() {
        Simulation sim = new Simulation(new ScriptedRandom(), new SimulationWorld(), false);

        sim.advance(10);
        assertEquals(10, sim.getCurrentTick());

        sim.advance(5);
        assertEquals(15, sim.getCurrentTick());

        assertThrows(IllegalArgumentException.class, () -> sim.advance(0));
        assertThrows(IllegalArgumentException.class, () -> sim.advance(-3));
    }

    @Test
    void advanceTimeCommandRejectsNonPositiveCounts() {
        GameplayController controller = new GameplayController(
                new Simulation(new ScriptedRandom(), new SimulationWorld(), false));

        assertFalse(controller.advanceTime(0).getStatus());
        assertFalse(controller.advanceTime(-1).getStatus());
        assertTrue(controller.advanceTime(3).getStatus());
    }

    // ---------- producers ----------

    @Test
    void aProducerEmitsTheExactMessageAtItsIntervalAndHoldsTheSun() {
        SimulationWorld world = new SimulationWorld();
        world.addProducer(new SunProducer("Sunflower", 2, 3, 20));
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);

        List<String> before = sim.advance(19);
        assertTrue(before.isEmpty(), "no sun before the interval elapses");

        List<String> atInterval = sim.advance(1);
        assertEquals(List.of("plant Sunflower produced a sun at (2, 3)"), atInterval);
    }

    @Test
    void aProducerDoesNotStartTheNextCycleWhileItsSunIsUncollected() {
        SimulationWorld world = new SimulationWorld();
        world.addProducer(new SunProducer("Sunflower", 0, 0, 10));
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);

        assertEquals(1, sim.advance(10).size(), "first sun produced");
        assertTrue(sim.advance(100).isEmpty(), "blocked while uncollected");

        SunCollector.Outcome outcome = sim.collectSun(0, 0);
        assertTrue(outcome.isCollected());
        assertEquals(25, sim.getSunAmount());

        assertEquals(1, sim.advance(10).size(), "next cycle resumes after collection");
    }

    @Test
    void collectingAbsentOrInvalidCoordinatesDoesNotChangeTheBalance() {
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);
        sim.addSun(40);

        assertFalse(sim.collectSun(1, 1).isCollected());
        assertEquals(40, sim.getSunAmount());

        assertFalse(sim.collectSun(-5, 2).isCollected(), "off-board rejected");
        assertFalse(sim.collectSun(99, 99).isCollected(), "off-board rejected");
        assertEquals(40, sim.getSunAmount());
    }

    // ---------- falling suns ----------

    @Test
    void theDropIntervalFollowsTheExactWrittenFormula() {
        assertEquals(12.0, SunDropSchedule.intervalSeconds(0), 1e-9);
        assertEquals(12.0, SunDropSchedule.intervalSeconds(100), 1e-9);
        assertEquals(12.0, SunDropSchedule.intervalSeconds(120), 1e-9);
        assertEquals(6 + 0.05 * 240, SunDropSchedule.intervalSeconds(240), 1e-9);
    }

    @Test
    void aFallingSunSpawnsOnScheduleTakesFiveSecondsAndLands() {
        ScriptedRandom random = new ScriptedRandom()
                .queueDouble(0.10)  // < 0.80 -> normal
                .queueInt(4)        // column
                .queueInt(2);       // row
        SimulationWorld world = new SimulationWorld();
        Simulation sim = new Simulation(random, world, true);

        // Interval is 12 s = 120 ticks; the spawn happens on the 120th tick.
        List<String> upToSpawn = sim.advance(120);
        assertTrue(upToSpawn.contains("New normal sun is dropping at position (4, 2)"));

        // 5 s = 50 ticks later it lands.
        List<String> untilLanding = sim.advance(FallingSunSystem.FALL_TICKS);
        assertTrue(untilLanding.contains("Sun reached the ground at position (4, 2)"));
    }

    @Test
    void sunTypeProbabilitiesMapToTheSpecifiedBands() {
        assertEquals(SunType.NORMAL, SunType.roll(new ScriptedRandom().queueDouble(0.0)));
        assertEquals(SunType.NORMAL, SunType.roll(new ScriptedRandom().queueDouble(0.79)));
        assertEquals(SunType.SPECIAL, SunType.roll(new ScriptedRandom().queueDouble(0.80)));
        assertEquals(SunType.SPECIAL, SunType.roll(new ScriptedRandom().queueDouble(0.94)));
        assertEquals(SunType.RADIOACTIVE, SunType.roll(new ScriptedRandom().queueDouble(0.95)));
        assertEquals(SunType.RADIOACTIVE, SunType.roll(new ScriptedRandom().queueDouble(0.99)));
    }

    @Test
    void normalAndSpecialFallingSunsAreWorthTwentyFiveAndOneHundred() {
        SimulationWorld world = new SimulationWorld();
        world.getSuns().add(Sun.falling(SunType.SPECIAL, 3, 1, 50));
        Simulation sim = new Simulation(new ScriptedRandom(), world, false);

        assertEquals(100, sim.collectSun(3, 1).getGained());
        assertEquals(100, sim.getSunAmount());
    }

    // ---------- radioactive ----------

    @Test
    void aRadioactiveSunCollectedWhileFallingExplodesInTheRightAreas() {
        SimulationWorld world = new SimulationWorld(9, 9);
        world.getSuns().add(Sun.falling(SunType.RADIOACTIVE, 4, 4, 50));

        FakeUnit zombieInside = new FakeUnit(6, 6);   // dx=2, dy=2 -> in 5x5
        FakeUnit zombieOutside = new FakeUnit(7, 4);  // dx=3 -> outside 5x5
        FakeUnit plantInside = new FakeUnit(5, 5);    // dx=1, dy=1 -> in 3x3
        FakeUnit plantOutside = new FakeUnit(6, 4);   // dx=2 -> outside 3x3
        world.addZombie(zombieInside);
        world.addZombie(zombieOutside);
        world.addPlant(plantInside);
        world.addPlant(plantOutside);

        Simulation sim = new Simulation(new ScriptedRandom(), world, false);
        SunCollector.Outcome outcome = sim.collectSun(4, 4);

        assertTrue(outcome.isExploded());
        assertEquals(0, sim.getSunAmount(), "an explosion grants no sun");
        assertEquals(150, zombieInside.damageTaken());
        assertEquals(0, zombieOutside.damageTaken());
        assertEquals(80, plantInside.damageTaken());
        assertEquals(0, plantOutside.damageTaken());
    }

    @Test
    void aRadioactiveSunThatReachesTheGroundBecomesANormalSun() {
        SimulationWorld world = new SimulationWorld();
        world.getSuns().add(Sun.falling(SunType.RADIOACTIVE, 2, 2, 1));
        Simulation sim = new Simulation(new ScriptedRandom(), world, true);

        sim.advance(1); // it lands and converts

        SunCollector.Outcome outcome = sim.collectSun(2, 2);
        assertTrue(outcome.isCollected());
        assertFalse(outcome.isExploded());
        assertEquals(25, outcome.getGained(), "it is now a normal sun worth 25");
        assertEquals(25, sim.getSunAmount());
    }

    // ---------- commands ----------

    @Test
    void showSunAmountAndCheatAddSuns() {
        GameplayController controller = new GameplayController(
                new Simulation(new ScriptedRandom(), new SimulationWorld(), false));

        assertEquals(0, controller.showSunAmount().getData());

        Result<Integer> cheat = controller.cheatAddSuns(75);
        assertTrue(cheat.getStatus());
        assertEquals(75, cheat.getData());
        assertEquals(75, controller.showSunAmount().getData());

        assertFalse(controller.cheatAddSuns(0).getStatus());
        assertEquals(75, controller.showSunAmount().getData());
    }

    @Test
    void collectSunCommandReportsBalanceAndRejectsAbsentSun() {
        SimulationWorld world = new SimulationWorld();
        world.getSuns().add(Sun.falling(SunType.NORMAL, 1, 1, 50));
        GameplayController controller = new GameplayController(
                new Simulation(new ScriptedRandom(), world, false));

        Result<Integer> absent = controller.collectSun(8, 8);
        assertFalse(absent.getStatus());
        assertEquals(0, absent.getData());

        Result<Integer> collected = controller.collectSun(1, 1);
        assertTrue(collected.getStatus());
        assertEquals(25, collected.getData());
    }
}
