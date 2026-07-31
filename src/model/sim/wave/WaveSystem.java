package model.sim.wave;

import model.GameEngine;
import model.inGame.GameOutcome;
import model.enums.ZombieType;
import model.inGame.zombie.Zombie;
import model.level.SpecialLevelType;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;
import util.RandomSource;
import util.RandomSourceAdapter;
import java.util.List;
import java.util.Random;

public class WaveSystem {

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

    public void tick(GameEngine engine) {
        if (!engine.isRunning() || !engine.areWavesStarted()) {
            return;
        }
        if (!started) {
            startWave(engine, 1);
            started = true;
            return;
        }
        if (!allWavesSpawned && shouldStartNextWave(engine)) {
            startWave(engine, currentWave + 1);
            return;
        }
        if (allWavesSpawned && !engine.hasHostileZombiesRemaining()) {
            declareWin(engine);
        }
    }

    private boolean shouldStartNextWave(GameEngine engine) {
        int currentHealth = aggregateHealth(engine);
        int lost = currentWaveInitialHealth - currentHealth;
        return lost >= (int) Math.ceil(currentWaveInitialHealth * NEXT_WAVE_THRESHOLD);
    }

    private void startWave(GameEngine engine, int waveNumber) {
        currentWave = waveNumber;
        engine.setCurrentWave(waveNumber);

        boolean finalWave = config.isFinalWave(waveNumber);
        if (finalWave) {
            engine.recordEvent("The final wave has come.");
        }
        engine.recordEvent("Wave " + waveNumber + " started.");

        spawnWave(engine, waveNumber);
        currentWaveInitialHealth = aggregateHealth(engine);

        if (finalWave) {
            allWavesSpawned = true;
        }
    }

    /**
     * The general cost formula (×1.25 growth) has no visibility into which
     * zombies are actually available for this level's chapter, so its target
     * is not guaranteed to be composable even when it is a multiple of 50 (some
     * multiples of 50, like 150, still cannot be formed from every available
     * cost set). Search outward in steps of 50 — the shared unit every zombie's
     * wave cost is defined in — for the closest cost that is composable, so a
     * plain formula mismatch never crashes the game.
     */
    private static int resolveComposableCost(int targetCost, List<ZombieSpec> availableSpecs) {
        if (WaveComposer.isComposable(targetCost, availableSpecs)) {
            return targetCost;
        }

        int maxSearchDistance = 5000;

        for (int distance = 1; distance <= maxSearchDistance; distance++) {
            int higher = targetCost + distance;
            if (WaveComposer.isComposable(higher, availableSpecs)) {
                return higher;
            }
            int lower = targetCost - distance;
            if (lower >= 0 && WaveComposer.isComposable(lower, availableSpecs)) {
                return lower;
            }
        }

        return targetCost;
    }

    private void spawnWave(GameEngine engine, int waveNumber) {
        int targetCost = config.costOfWave(waveNumber);
        Random random = engine.getRandom();
        RandomSource randomSource = RandomSourceAdapter.wrap(random); // ← اصلاح شد

        List<ZombieSpec> availableSpecs = specSource.availableSpecs();
        int resolvedCost = resolveComposableCost(targetCost, availableSpecs);
        List<ZombieSpec> composition =
                WaveComposer.compose(resolvedCost, availableSpecs, randomSource);

        if (composition == null) {
            throw new IllegalStateException(
                    "Wave " + waveNumber + " cost " + targetCost
                            + " cannot be composed from the available zombies.");
        }

        int rows = engine.getGameMap().getRows();
        int spawnColumn = engine.getGameMap().getColumns();
        boolean tornadoes = config.isFinalWave(waveNumber)
                && engine.getAdventureState() != null
                && engine.getAdventureState().getConfig().getChapterRules().hasFinalWaveTornadoes();

        for (ZombieSpec spec : composition) {
            int lane = random.nextInt(rows);
            double spawnX = spawnColumn;
            if (tornadoes && random.nextInt(2) == 0) {
                int advance = 1 + random.nextInt(4);
                spawnX = Math.max(0.5, spawnColumn - advance);
                engine.recordEvent("A tornado carried " + spec.getName() + " "
                        + advance + " columns into lane " + lane + ".");
            }
            engine.recordEvent("Zombie " + spec.getName() + " spawned at wave " + waveNumber
                    + " in lane " + lane + " which costed " + spec.getWaveCost() + ".");
        }
    }

    private int aggregateHealth(GameEngine engine) {
        int total = 0;
        for (Zombie zombie : engine.getZombies()) {
            total += zombie.getHealth();
        }
        return total;
    }

    private void declareWin(GameEngine engine) {
        if (engine.getAdventureState() != null
                && engine.getAdventureState().getConfig().getSpecialType() == SpecialLevelType.TIMED_WAR) {
            return;
        }
        engine.setOutcome(GameOutcome.WON);
        engine.recordEvent("Dear humanz, zis is not done yet; "
                + "we will come back to eat your brainz, humanz.");
    }

}