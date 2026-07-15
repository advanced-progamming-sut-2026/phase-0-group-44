package model.sim.wave;

import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;
import util.RandomSource;

import java.util.List;

/**
 * Drives the wave lifecycle: starting each wave (the first immediately, each
 * later one after 75% of the previous wave's initial aggregate health has been
 * lost), composing and placing its zombies, and declaring victory once every
 * wave has run and no zombie remains.
 */
public class WaveSystem implements SimulationSystem {

    /** Fraction of a wave's initial health that must be lost before the next wave. */
    public static final double NEXT_WAVE_THRESHOLD = 0.75;

    private final WaveConfig config;
    private final ZombieSpecSource specSource;

    private int currentWave;
    private int currentWaveInitialHealth;
    private boolean started;
    private boolean allWavesSpawned;

    public WaveSystem(WaveConfig config, ZombieSpecSource specSource) {
        this.config = config;
        this.specSource = specSource;
    }

    @Override
    public void tick(TickContext context) {
        SimulationWorld world = context.getWorld();

        if (!world.isRunning()) {
            return;
        }

        if (!started) {
            startWave(context, world, 1);
            started = true;
            return;
        }

        if (!allWavesSpawned && shouldStartNextWave(world)) {
            startWave(context, world, currentWave + 1);
            return;
        }

        if (allWavesSpawned && world.getZombieInstances().isEmpty()) {
            declareWin(context, world);
        }
    }

    private boolean shouldStartNextWave(SimulationWorld world) {
        int currentHealth = aggregateHealth(world);
        int lost = currentWaveInitialHealth - currentHealth;

        return lost >= (int) Math.ceil(currentWaveInitialHealth * NEXT_WAVE_THRESHOLD);
    }

    private void startWave(TickContext context, SimulationWorld world, int waveNumber) {
        currentWave = waveNumber;
        world.setCurrentWave(waveNumber);

        boolean finalWave = config.isFinalWave(waveNumber);

        if (finalWave) {
            context.emit("The final wave has come.");
        }

        context.emit("Wave " + waveNumber + " started.");

        spawnWave(context, world, waveNumber);

        currentWaveInitialHealth = aggregateHealth(world);

        if (finalWave) {
            allWavesSpawned = true;
        }
    }

    private void spawnWave(TickContext context, SimulationWorld world, int waveNumber) {
        int targetCost = config.costOfWave(waveNumber);
        RandomSource random = context.getRandom();

        List<ZombieSpec> composition =
                WaveComposer.compose(targetCost, specSource.availableSpecs(), random);

        if (composition == null) {
            throw new IllegalStateException(
                    "Wave " + waveNumber + " cost " + targetCost
                            + " cannot be composed from the available zombies.");
        }

        int rows = world.getBoard().getRows();
        int spawnColumn = world.getBoard().getColumns();

        for (ZombieSpec spec : composition) {
            int lane = random.nextInt(rows);
            ZombieInstance zombie = new ZombieInstance(spec, spawnColumn, lane);
            world.addZombie(zombie);

            context.emit("Zombie " + spec.getName() + " spawned at wave " + waveNumber
                    + " in lane " + lane + " which costed " + spec.getWaveCost() + ".");
        }
    }

    private int aggregateHealth(SimulationWorld world) {
        int total = 0;

        for (ZombieInstance zombie : world.getZombieInstances()) {
            total += zombie.getHp();
        }

        return total;
    }

    private void declareWin(TickContext context, SimulationWorld world) {
        world.setOutcome(GameOutcome.WON);
        context.emit("Dear humanz, zis is not done yet; "
                + "we will come back to eat your brainz, humanz.");
    }

    public int getCurrentWave() {
        return currentWave;
    }
}
