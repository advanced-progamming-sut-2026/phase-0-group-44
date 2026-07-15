package model.sim.sun;

/**
 * The interval between falling suns.
 *
 * <p>The specification writes this as {@code x = max(6 + 0.05t, 12)} seconds,
 * where {@code t} is elapsed game time. That is preserved here exactly as
 * written. (Read literally it yields a constant 12 s until t = 120 s and then
 * grows, i.e. suns fall less often over time; no repository test or config was
 * found that establishes a different interpretation, so the written rule stands.
 * The formula lives here alone, so a corrected version needs a one-line change.)</p>
 */
public final class SunDropSchedule {

    private SunDropSchedule() {
    }

    /** Interval in seconds at the given elapsed game time (seconds). */
    public static double intervalSeconds(double elapsedSeconds) {
        return Math.max(6 + 0.05 * elapsedSeconds, 12);
    }

    /** Interval in ticks, using the same rule. */
    public static long intervalTicks(double elapsedSeconds, int ticksPerSecond) {
        return Math.round(intervalSeconds(elapsedSeconds) * ticksPerSecond);
    }
}
