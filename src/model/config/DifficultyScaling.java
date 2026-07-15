package model.config;

/**
 * The single place that turns a difficulty level (1-5) into gameplay multipliers.
 *
 * <p>Difficulty 3 is the neutral point: every multiplier is exactly 1.0 there.
 * As difficulty rises, zombie health, zombie damage and game speed scale up,
 * while wave cost and sky-sun appearance rate scale down; as it falls, the
 * reverse. All five are derived from one {@link #STEP_PER_LEVEL} constant so the
 * balance lives in a single spot.</p>
 *
 * <p><b>Source note:</b> the specification states only the direction of each
 * axis, not the magnitude, and no formula was found in the project's data files
 * or tests. The 10%-per-level step below is therefore a placeholder assumption,
 * deliberately isolated here so the real numbers can be dropped in without
 * touching any gameplay or menu code.</p>
 */
public final class DifficultyScaling {

    public static final int NEUTRAL_DIFFICULTY = 3;
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;

    /** Placeholder magnitude: fraction of change per level away from neutral. */
    public static final double STEP_PER_LEVEL = 0.10;

    private DifficultyScaling() {
    }

    /** Distance above (positive) or below (negative) the neutral difficulty. */
    private static int stepsFromNeutral(int difficulty) {
        require(difficulty);

        return difficulty - NEUTRAL_DIFFICULTY;
    }

    /** Zombie health multiplier: rises with difficulty. */
    public static double zombieHealthMultiplier(int difficulty) {
        return 1.0 + STEP_PER_LEVEL * stepsFromNeutral(difficulty);
    }

    /** Zombie damage multiplier: rises with difficulty. */
    public static double zombieDamageMultiplier(int difficulty) {
        return 1.0 + STEP_PER_LEVEL * stepsFromNeutral(difficulty);
    }

    /** Game speed multiplier: rises with difficulty. */
    public static double gameSpeedMultiplier(int difficulty) {
        return 1.0 + STEP_PER_LEVEL * stepsFromNeutral(difficulty);
    }

    /** Wave cost multiplier: falls with difficulty (cheaper waves mean more zombies). */
    public static double waveCostMultiplier(int difficulty) {
        return 1.0 - STEP_PER_LEVEL * stepsFromNeutral(difficulty);
    }

    /** Sky-sun appearance rate multiplier: falls with difficulty. */
    public static double skySunRateMultiplier(int difficulty) {
        return 1.0 - STEP_PER_LEVEL * stepsFromNeutral(difficulty);
    }

    private static void require(int difficulty) {
        if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
            throw new IllegalArgumentException(
                    "Difficulty must be between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY + "."
            );
        }
    }
}
