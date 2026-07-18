package model.scored;

import model.sim.GameOutcome;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Mutable attempt score with one-time final bonuses. */
public final class ScoredGameScore {
    public static final int QUICK_KILL_LIMIT_TICKS = 30;
    public static final int QUICK_KILL_POINTS = 300;
    public static final int EXTRA_PROJECTILE_KILL_POINTS = 200;
    public static final int SIMULTANEOUS_KILL_POINTS = 150;
    public static final int PERFECT_DEFENSE_POINTS = 1000;

    private final Map<ScorePattern, Integer> points = new EnumMap<>(ScorePattern.class);
    private boolean finalized;

    public ScoredGameScore() {
        for (ScorePattern pattern : ScorePattern.values()) {
            points.put(pattern, 0);
        }
    }

    public void recordQuickKill(long spawnTick, long deathTick) {
        if (deathTick - spawnTick <= QUICK_KILL_LIMIT_TICKS) {
            add(ScorePattern.QUICK_KILL, QUICK_KILL_POINTS);
        }
    }

    public void recordProjectileKillCount(int killedByOneProjectile) {
        if (killedByOneProjectile > 1) {
            add(ScorePattern.MULTI_KILL_PROJECTILE,
                    (killedByOneProjectile - 1) * EXTRA_PROJECTILE_KILL_POINTS);
        }
    }

    public void recordSimultaneousKillCount(int killedOnSameTick) {
        if (killedOnSameTick > 1) {
            add(ScorePattern.SIMULTANEOUS_KILLS,
                    killedOnSameTick * SIMULTANEOUS_KILL_POINTS);
        }
    }

    public boolean finalizeScore(
            GameOutcome outcome,
            int remainingSun,
            int plantLosses,
            boolean mowerUsed
    ) {
        if (finalized) {
            return false;
        }
        finalized = true;
        if (outcome == GameOutcome.WON) {
            add(ScorePattern.SUN_EFFICIENCY, Math.max(0, remainingSun));
            if (plantLosses == 0 && !mowerUsed) {
                add(ScorePattern.PERFECT_DEFENSE, PERFECT_DEFENSE_POINTS);
            }
        }
        return true;
    }

    public int getPoints(ScorePattern pattern) {
        return points.getOrDefault(pattern, 0);
    }

    public int total() {
        return points.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isFinalized() {
        return finalized;
    }

    public Map<ScorePattern, Integer> breakdown() {
        return Collections.unmodifiableMap(new EnumMap<>(points));
    }

    private void add(ScorePattern pattern, int amount) {
        if (amount > 0) {
            points.put(pattern, points.get(pattern) + amount);
        }
    }
}
