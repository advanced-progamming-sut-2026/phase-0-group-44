package model.sim.wave;

/**
 * A level's wave setup: how many waves, the first wave's cost, and an optional
 * explicit per-wave cost override that replaces the general formula.
 *
 * <p>The general rule is: each normal wave costs 25% more than the previous, and
 * the final flag wave costs twice the preceding wave. A level may override the
 * whole cost sequence.</p>
 */
public final class WaveConfig {

    private final int waveCount;
    private final int firstWaveCost;
    private final int[] costOverride;

    private WaveConfig(int waveCount, int firstWaveCost, int[] costOverride) {
        if (waveCount <= 0) {
            throw new IllegalArgumentException("A level must have at least one wave.");
        }

        this.waveCount = waveCount;
        this.firstWaveCost = firstWaveCost;
        this.costOverride = costOverride;
    }

    public static WaveConfig of(int waveCount, int firstWaveCost) {
        return new WaveConfig(waveCount, firstWaveCost, null);
    }

    /** A level-specific cost sequence that overrides the general formula. */
    public static WaveConfig ofExplicitCosts(int[] costs) {
        if (costs == null || costs.length == 0) {
            throw new IllegalArgumentException("Explicit costs must not be empty.");
        }

        return new WaveConfig(costs.length, costs[0], costs.clone());
    }

    public int getWaveCount() {
        return waveCount;
    }

    /**
     * The target cost of a wave (1-based). Uses the override when present,
     * otherwise the general rule: ×1.25 per wave, and ×2 of the previous wave
     * for the final wave.
     */
    public int costOfWave(int waveNumber) {
        if (waveNumber < 1 || waveNumber > waveCount) {
            throw new IllegalArgumentException("Wave number out of range: " + waveNumber);
        }

        if (costOverride != null) {
            return costOverride[waveNumber - 1];
        }

        int cost = firstWaveCost;

        for (int wave = 2; wave <= waveNumber; wave++) {
            if (wave == waveCount) {
                cost = cost * 2;
            } else {
                cost = roundToNearest50(cost * 1.25);
            }
        }

        return cost;
    }

    /**
     * Every zombie's wave cost is a multiple of 50 (see zombies.csv), so a wave
     * target must stay a multiple of 50 to remain exactly composable. Rounding
     * to a plain integer (e.g. 100 * 1.25 = 125) can produce an uncomposable
     * target and crash {@link WaveComposer}.
     */
    private static int roundToNearest50(double value) {
        return (int) (Math.round(value / 50.0) * 50);
    }

    public boolean isFinalWave(int waveNumber) {
        return waveNumber == waveCount;
    }
}
