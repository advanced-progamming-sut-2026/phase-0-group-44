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
        if (allWavesSpawned && engine.getZombies().isEmpty()) {
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

    private void spawnWave(GameEngine engine, int waveNumber) {
        int targetCost = config.costOfWave(waveNumber);
        Random random = engine.getRandom();
        RandomSource randomSource = RandomSourceAdapter.wrap(random); // ← اصلاح شد

        List<ZombieSpec> composition =
                WaveComposer.compose(targetCost, specSource.availableSpecs(), randomSource);

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
            Zombie zombie = engine.spawnZombie(spec.getType(), lane, spawnX);
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

    public int getCurrentWave() {
        return currentWave;
    }
}